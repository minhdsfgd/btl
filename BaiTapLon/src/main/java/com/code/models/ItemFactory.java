package com.code.models;
import java.util.Map;

public class ItemFactory {


    /*
     * General-purpose factory method that dispatches based on type string.
     * @param type     one of "electronics", "art", "vehicle"
     * @param params   key-value map of item fields

    public static Item createItem(String type, Map<String, Object> params) {
        return switch (type.toLowerCase()) {
            case "electronics" -> createElectronics(params);
            case "art"         -> createArt(params);
            case "vehicle"     -> createVehicle(params);
            default            -> throw new IllegalArgumentException("Unknown item type: " + type);
        };
    }


     * Creates an Electronics item.
     * Required params: name, description, startingPrice, owner, brand, model, warranty

    public static Electronics createElectronics(Map<String, Object> params) {
        return new Electronics(
                (String)  params.get("name"),
                (String)  params.get("description"),
                (double)  params.get("startingPrice"),
                (Seller)  params.get("owner"),
                (String)  params.get("brand"),
                (String)  params.get("model"),
                (String)  params.get("warranty")
        );
    }

    public static Art createArt(Map<String, Object> params) {
        return new Art(
                (String)  params.get("name"),
                (String)  params.get("description"),
                (double)  params.get("startingPrice"),
                (Seller)  params.get("owner"),
                (String)  params.get("artist"),
                (int)     params.get("yearCreated"),
                (String)  params.get("medium")
        );
    }

    public static Vehicle createVehicle(Map<String, Object> params) {
        return new Vehicle(
                (String)  params.get("name"),
                (String)  params.get("description"),
                (double)  params.get("startingPrice"),
                (Seller)  params.get("owner"),
                (String)  params.get("make"),
                (int)     params.get("year"),
                (int)     params.get("mileage")
        );
    }
    */
}