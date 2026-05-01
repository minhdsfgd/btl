package com.code.models;
public abstract class Item{
    private final int itemId;
    private String name;
    private String description;

    Item(int itemId, String name, String description){
        this.itemId = itemId;
        this.name = name;
        this.description = description;
    }
    // Setters
    public void setName(String name){this.name = name;}
    public void setDescription(String description){this.description = description;}

    // Getters
    public int getItemId(){return this.itemId;}
    public String getName(){return this.name;}
    public String getDescription(){return this.description;}
}