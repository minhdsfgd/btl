package com.code.network;

import java.io.Serializable;

public class AutoBidData implements Serializable {

    public final int auctionId;
    public final double maxAmount;
    public final double step;

    public AutoBidData(int auctionId,
                       double maxAmount,
                       double step) {
        this.auctionId = auctionId;
        this.maxAmount = maxAmount;
        this.step = step;
    }
}