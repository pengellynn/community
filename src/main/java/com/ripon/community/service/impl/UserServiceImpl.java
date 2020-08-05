package com.ripon.community.service.impl;

import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.dao.LoginTicketMapper;
import com.ripon.community.dao.UserMapper;
import com.ripon.community.entity.LoginTicket;
import com.ripon.community.entity.LoginTicketExample;
import com.ripon.community.entity.User;
import com.ripon.community.entity.UserExample;
import com.ripon.community.service.UserService;
import com.ripon.community.util.MailClient;
import com.ripon.community.util.Md5Utils;
import com.ripon.community.util.RedisKeyUtils;
import com.ripon.community.util.UUIDUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    TemplateEngine templateEngine;
    @Autowired
    MailClient mailClient;
    @Autowired
    LoginTicketMapper loginTicketMapper;
    @Autowired
    RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Override
    public User getUserById(int id) {
//        return userMapper.selectByPrimaryKey(id);
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    @Override
    public User getUserByName(String username) {
        UserExample example = new UserExample();
        UserExample.Criteria criteria = example.createCriteria();
        criteria.andUsernameEqualTo(username);
        List<User> users = userMapper.selectByExample(example);
        if (users.size() != 0) {
            return users.get(0);
        }
        return null;
    }

    @Override
    public User getUserByEmail(String email) {
        UserExample example = new UserExample();
        UserExample.Criteria criteria = example.createCriteria();
        criteria.andEmailEqualTo(email);
        List<User> users = userMapper.selectByExample(example);
        if (users.size() != 0) {
            return users.get(0);
        }
        return null;
    }

    @Override
    public LoginTicket getLoginTick(String ticket) {
        String ticketKey = RedisKeyUtils.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
//        LoginTicketExample example = new LoginTicketExample();
//        LoginTicketExample.Criteria criteria = example.createCriteria();
//        criteria.andTicketEqualTo(ticket);
//        List<LoginTicket> loginTickets = loginTicketMapper.selectByExample(example);
//        if (loginTickets.size() != 0) {
//            return loginTickets.get(0);
//        }
//        return null;
    }

    @Override
    public Map<String, Object> insertUser(User user) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        // 验证账号
        User u = getUserByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱
        u = getUserByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户
        String dbSalt = RandomStringUtils.random(6, true, true);
        String dbPassword = Md5Utils.Md5WithSalt(user.getPassword(), dbSalt);
        user.setSalt(dbSalt);
        user.setPassword(dbPassword);
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(UUIDUtils.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insert(user);

        // 激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    @Override
    public int updateStatus(int id, int status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        int rows = userMapper.updateByPrimaryKeySelective(user);
        clearCache(id);
        return rows;
    }

    @Override
    public int updateHeader(int id, String headerUrl) {
        User user = new User();
        user.setId(id);
        user.setHeaderUrl(headerUrl);
        int rows = userMapper.updateByPrimaryKeySelective(user);
        clearCache(id);
        return rows;
    }

    @Override
    public int updatePassword(int id, String password) {
        User user = new User();
        user.setId(id);
        user.setPassword(password);
        int rows = userMapper.updateByPrimaryKeySelective(user);
        clearCache(id);
        return rows;
    }

    @Override
    public int activation(int userId, String code) {
        User user = getUserById(userId);
        if (user == null) {
            return CommunityConstant.ACTIVATION_FAILURE;
        }
        if (user.getStatus() == 1) {
            return CommunityConstant.ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            if (updateStatus(userId, 1) == 1) {
                return CommunityConstant.ACTIVATION_SUCCESS;
            }
        }
        return CommunityConstant.ACTIVATION_FAILURE;
    }

    @Override
    public Map<String, Object> login(String username, String password, int expireSeconds) {
        HashMap<String, Object> map = new HashMap<>();
        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "用户名不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("password", "密码不能为空");
            return map;
        }
        // 验证用户
        User user = getUserByName(username);
        if (user == null) {
            map.put("usernameMsg", "账号不存在");
            return map;
        }
        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "账号未激活");
            return map;
        }
        // 验证密码
        password = Md5Utils.Md5WithSalt(password, user.getSalt());
        if (!password.equals(user.getPassword())) {
            map.put("passwordMsg", "密码错误");
            return map;
        }
        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(UUIDUtils.generateUUID());
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expireSeconds * 1000));
        loginTicket.setStatus(0);
//        loginTicketMapper.insert(loginTicket);
        String ticketKey = RedisKeyUtils.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    @Override
    public void logout(String ticket) {
        String ticketKey = RedisKeyUtils.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
//        LoginTicketExample example = new LoginTicketExample();
//        LoginTicketExample.Criteria criteria = example.createCriteria();
//        criteria.andTicketEqualTo(ticket);
//        LoginTicket loginTicket = new LoginTicket();
//        loginTicket.setStatus(1);
//        loginTicketMapper.updateByExampleSelective(loginTicket, example);
    }

    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtils.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        String redisKey = RedisKeyUtils.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtils.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }
}
