package com.code.network;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Dùng cho: CREATE_AUCTION.
 * Chứa itemId và thông số phiên — server tìm Item trong DB theo itemId.
 *
 * <pre>
 * Request.of(CREATE_AUCTION, new CreateAuctionData(
 *     itemId, 100_000,
 *     LocalDateTime.now().plusHours(1),
 *     LocalDateTime.now().plusHours(3)
 * ))
 * </pre>
 */
public class CreateAuctionData implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int           itemId;
    public final double        bidIncrement;
    public final LocalDateTime startTime;
    public final LocalDateTime endTime;

    public CreateAuctionData(int itemId, double bidIncrement,
                             LocalDateTime startTime, LocalDateTime endTime) {
        this.itemId       = itemId;
        this.bidIncrement = bidIncrement;
        this.startTime    = startTime;
        this.endTime      = endTime;
    }
}