package com.ripon.community.util;

import org.springframework.util.DigestUtils;

public class Md5Utils {
    public static String Md5WithSalt(String formPassword, String salt) {
        String str = formPassword +salt;
//        String str = formPassword + salt.charAt(0) + salt.charAt(2) + salt.charAt(4) + salt.charAt(5);
        return DigestUtils.md5DigestAsHex(str.getBytes());
    }
}
