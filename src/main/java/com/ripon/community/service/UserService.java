package com.ripon.community.service;

import com.ripon.community.entity.LoginTicket;
import com.ripon.community.entity.User;

import java.util.Map;

public interface UserService {
    User getUserById(int id);
    User getUserByName(String username);
    User getUserByEmail(String email);
    Map<String,Object> insertUser(User user);
    int updateStatus(int id, int status);
    int updateHeader(int id, String headerUrl);
    int updatePassword(int id, String password);
    int activation(int userId, String code);
    Map<String, Object> login(String username, String password, int expireSeconds);
    void logout(String ticket);
    LoginTicket getLoginTick(String ticket);
}
