package com.ripon.community.util;

import com.ripon.community.entity.User;
import org.springframework.stereotype.Component;

@Component
public class HostHolder {
    private  ThreadLocal<User> users = new ThreadLocal<>();
    public User getUser() {
        return users.get();
    }
    public void setUser(User user) {
        users.set(user);
    }
    public void clean() {
        users.remove();
    }
}
