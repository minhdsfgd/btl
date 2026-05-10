package com.code.network;

import java.io.Serializable;

/**
 * Dùng cho: PLACE_BID.
 *
 * <pre>
 * Request.of(PLACE_BID, new PlaceBidData(auctionId, 1_500_000))
 * </pre>
 */
public class PlaceBidData implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int    auctionId;
    public final double amount;

    public PlaceBidData(int auctionId, double amount) {
        this.auctionId = auctionId;
        this.amount    = amount;
    }
}
