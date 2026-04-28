package com.code.models;
public class Vehicle extends Item {
    /*
    private String make;
    private int year;
    private int mileage;

    public Vehicle(String name, String description, double startingPrice, Seller owner,
                   String make, int year, int mileage) {
        super(name, description, startingPrice, owner);
        this.make = make;
        this.year = year;
        this.mileage = mileage;
    }

    @Override
    public void printInfo() {
        System.out.println("=== Vehicle ===");
        System.out.println("Name: " + name);
        System.out.println("Make: " + make + " | Year: " + year);
        System.out.println("Mileage: " + mileage + " km");
        System.out.println("Starting Price: $" + startingPrice);
        System.out.println("Description: " + description);
    }

    @Override
    public String getCategory() {
        return "Vehicle";
    }

    @Override
    public boolean validate() {
        return make != null && !make.isBlank()
                && year > 1886   // first automobile invented 1886
                && mileage >= 0
                && startingPrice > 0;
    }

    public String getMake() { return make; }
    public int getYear() { return year; }
    public int getMileage() { return mileage; }

     */
}