package com.code.exception;

public class UserBannedException extends Exception {
    public UserBannedException(String username) {
        super("Tài khoản '" + username + "' đã bị cấm. Liên hệ Admin để được hỗ trợ.");
    }
}
