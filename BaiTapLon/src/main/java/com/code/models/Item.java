package com.code.models;
public abstract class Item{
    private final int itemId;
    private String name;
    private String description;
    private double price;

    Item(int itemId, String name, String description, double price){
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.price = price;
    }
}