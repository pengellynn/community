package com.ripon.community.interceptor;

import com.ripon.community.entity.LoginTicket;
import com.ripon.community.entity.User;
import com.ripon.community.service.UserService;
import com.ripon.community.util.CookieUtils;
import com.ripon.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    UserService userService;

    @Autowired
    HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从cookie中获取ticket
        String ticket = CookieUtils.getValue(request, "ticket");
        if (ticket != null) {
            // 获取登录凭证
            LoginTicket loginTick = userService.getLoginTick(ticket);
            // 检查登录凭证有效性
            if (loginTick != null && loginTick.getStatus() == 0 && loginTick.getExpired().after(new Date())) {
                // 获取凭证用户
                User user = userService.getUserById(loginTick.getUserId());
                // 在本次请求中持有该用户
                hostHolder.setUser(user);
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clean();
    }
}
