package com.ripon.community.controller;

import com.google.code.kaptcha.Producer;
import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.User;
import com.ripon.community.service.UserService;
import com.ripon.community.util.RedisKeyUtils;
import com.ripon.community.util.UUIDUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {
    @Autowired
    UserService userService;

    @Autowired
    Producer kaptchaProducer;

    @Autowired
    RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    String contextPath;

    @GetMapping("/register")
    public String getRegisterPage() {
        return "/site/register";
    }

    @PostMapping("/register")
    public String register(Model model, User user) {
        Map<String, Object> map = userService.insertUser(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    @GetMapping("/activation/{userId}/{code}")
    public String activation(Model model,
                             @PathVariable("userId") Integer userId,
                             @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == CommunityConstant.ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == CommunityConstant.ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    @GetMapping("/login")
    public String getLoginPage() {
        return "/site/login";
    }


    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response, HttpSession session) {
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
//        session.setAttribute("captcha", text);
        // 验证码的归属
        String captchaId = UUIDUtils.generateUUID();
        Cookie cookie = new Cookie("captchaId", captchaId);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 验证码存入redis
        String captchaKey = RedisKeyUtils.getCaptchaKey(captchaId);
        redisTemplate.opsForValue().set(captchaKey, text, 60, TimeUnit.SECONDS);
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @PostMapping("/login")
    public String login(String username, String password, String code,
                        boolean rememberMe, Model model,
                        HttpServletRequest request, HttpServletResponse response,
                        @CookieValue("captchaId") String captchaId) {
//        String captcha = session.getAttribute("captcha").toString();
        String captcha = null;
        if (StringUtils.isNotBlank(captchaId)) {
            String redisKey = RedisKeyUtils.getCaptchaKey(captchaId);
            captcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(captcha) || StringUtils.isBlank(code) || !StringUtils.equalsIgnoreCase(captcha, code)) {
            model.addAttribute("codeMsg", "验证码不正确");
            return "site/login";
        }
        int expireSconds = rememberMe ? CommunityConstant.REMEMBER_EXPIRED_SECONDS : CommunityConstant.DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expireSconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expireSconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/login";
        }
    }

    @GetMapping("/logout")
    public String logout(@CookieValue String ticket) {
        userService.logout(ticket);
        return "redirect:/login";
    }

}
