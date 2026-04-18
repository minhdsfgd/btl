package com.code.Models;
public class ItemFactory{
    public static Item createItem(String type){
        if (type.equals("electronics")){
            return new Electronics();
        }
        if (type.equals("art")){
            return new Art();
        }
        if (type.equals("vehicle")){
            return new Vehicle();
        }
        throw new IllegalArgumentException("Invalid type");
    }
}