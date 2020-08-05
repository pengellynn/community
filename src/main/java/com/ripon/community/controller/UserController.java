package com.ripon.community.controller;

import com.ripon.community.annotation.LoginRequired;
import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.User;
import com.ripon.community.service.FollowService;
import com.ripon.community.service.LikeService;
import com.ripon.community.service.UserService;
import com.ripon.community.util.HostHolder;
import com.ripon.community.util.Md5Utils;
import com.ripon.community.util.UUIDUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@LoginRequired
@Controller
public class UserController {
    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    UserService userService;

    @Autowired
    HostHolder hostHolder;

    @Autowired
    LikeService likeService;

    @Autowired
    FollowService followService;

    @GetMapping("/user/setting")
    public String getSettingPage() {
        return "/site/setting";
    }

    @PostMapping("/user/setting/header")
    public String setting(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("headerMsg", "未选择图片");
            return "/user/setting";
        }
        // 完整原始文件名
        String OriginalFilename = headerImage.getOriginalFilename();
        // 获取图片格式
        String suffix = OriginalFilename.substring(OriginalFilename.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("headerMsg", "图片格式错误");
        }
        // 生成随机文件名
        String fileName = UUIDUtils.generateUUID() + suffix;
        File savePath = new File(uploadPath);
        if (!savePath.exists()) {
            savePath.mkdirs();
        }
        File file = new File(uploadPath, fileName);
        try {
            // 存储图片
            headerImage.transferTo(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";

    }

    @GetMapping("/user/header/{fileName}")
    public void getHeader(@PathVariable String fileName, HttpServletResponse response) {
        // 服务器存放路径
        String filePath = uploadPath + "/" + fileName;
        // 获取图片格式
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        response.setContentType("image/" + suffix);
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            OutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/user/setting/password")
    public String updatePassword(String oldPassword, String newPassword, String confirmPassword, Model model) {
        User user = hostHolder.getUser();
        String password = Md5Utils.Md5WithSalt(oldPassword, user.getSalt());
        if (!user.getPassword().equals(password)) {
            model.addAttribute("oldPasswordMsg", "原密码不正确");
            return "site/setting";
        }
        if (newPassword.length() < 6) {
            model.addAttribute("newPasswordMsg", "密码长度不能小于6");
            return "/site/setting";
        }
        if (!confirmPassword.equals(newPassword)) {
            model.addAttribute("confirmPasswordMsg", "两次输入的密码不一致");
            return "/site/setting";
        }
        String dbPassword = Md5Utils.Md5WithSalt(newPassword, user.getSalt());
        userService.updatePassword(user.getId(), dbPassword);

        return "redirect:/index";
    }

    @GetMapping("/user/profile/{userId}")
    public String getProfile(@PathVariable("userId") int userId, Model model) {
        User user = userService.getUserById(userId);
        model.addAttribute("user", user);
        int likeCount = likeService.getUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        long followingCount = followService.getFollowingCount(userId, CommunityConstant.ENTITY_TYPE_USER);
        model.addAttribute("followingCount", followingCount);
        long followerCount = followService.getFollowerCount(CommunityConstant.ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        boolean hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), CommunityConstant.ENTITY_TYPE_USER, userId);
        model.addAttribute("hasFollowed", hasFollowed);
        return "/site/profile";
    }
}
