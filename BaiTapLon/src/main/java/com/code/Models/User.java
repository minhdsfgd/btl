package com.code.Models;
public abstract class User {
    protected String name,id,email,password;
    protected boolean isActive;



    public User(String name, String email,String id,String password) {
        this.name = name;
        this.email = email;
        this.id = id;
        this.password = password;
        this.isActive = true;
    }

    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public String getId() {
        return id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void checkPassword(String password) {
        if (this.password.equals(password)) {
            System.out.println("Login successful!");
        } else {
            System.out.println("Invalid password.");
        }
    }
}