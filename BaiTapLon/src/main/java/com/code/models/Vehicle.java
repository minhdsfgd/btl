
package com.code.models;

import com.code.util.ItemType;
import java.io.Serializable;

/** Phương tiện. Field riêng: licensePlate (biển số), yearMade (năm sản xuất). */
public class Vehicle extends Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private String licensePlate;
    private int yearMade;

    public Vehicle(int itemId, int sellerId, String name,
                   String description, double startingPrice,
                   String licensePlate, int yearMade) {
        super(itemId, sellerId, name, description, startingPrice);
        this.licensePlate = licensePlate != null ? licensePlate : "";
        this.yearMade     = yearMade;
    }

    /** Constructor tương thích cũ. */
    public Vehicle(int itemId, int sellerId, String name,
                   String description, double startingPrice) {
        this(itemId, sellerId, name, description, startingPrice, "", 0);
    }

    @Override public ItemType getType() { return ItemType.VEHICLE; }

    public String getLicensePlate()          { return licensePlate; }
    public int    getYearMade()              { return yearMade; }
    public void   setLicensePlate(String lp) { this.licensePlate = lp != null ? lp : ""; }
    public void   setYearMade(int y)         { this.yearMade = y; }

    @Override
    public String toString() {
        return String.format("Vehicle{name='%s', plate='%s', year=%d, price=%,.0f}",
                getName(), licensePlate, yearMade, getStartingPrice());
    }
}