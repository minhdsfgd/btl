package com.code;
public class Admin extends User {
    public Admin(String name, String email,String id, String password) {
        super(name, email, id, password);
    }
    public void banUser(User user) {
        user.isActive = false;
    }
    public void unbanUser(User user) {
        user.isActive = true;
    }   
}