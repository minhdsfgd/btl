package com.code.controllers.seller;

import com.code.client.SocketClient;
import com.code.models.Auction;
import com.code.models.Item;
import com.code.models.User;
import com.code.network.CreateAuctionData;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

//Tập trung toàn bộ network call liên quan đến Seller

public class SellerApiService {

    private final SocketClient socket;

    public SellerApiService() {
        this(SocketClient.getInstance());
    }

    public SellerApiService(SocketClient socket) {
        this.socket = socket;
    }

    // ── User ──────────────────────────────────────────────────────────────────

    /**
     * Lấy thông tin user hiện tại (làm mới balance).
     * @return Response chứa {@link User} nếu thành công.
     */
    public Response fetchMyInfo() throws Exception {
        return socket.sendRequest(Request.of(RequestType.GET_MY_INFO, null));
    }

    // ── Auction ───────────────────────────────────────────────────────────────


    //Lấy danh sách phiên đấu giá của seller hiện tại

    public Response fetchMyAuctions() throws Exception {
        return socket.sendRequest(Request.of(RequestType.GET_MY_AUCTIONS));
    }

    //Tạo phiên đấu giá mới.
    public Response createAuction(int itemId, double bidIncrement,
                                  LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        CreateAuctionData data = new CreateAuctionData(itemId, bidIncrement, startTime, endTime);
        return socket.sendRequest(Request.of(RequestType.CREATE_AUCTION, data));
    }

    //Hủy phiên đấu giá.

    public Response cancelAuction(int auctionId) throws Exception {
        return socket.sendRequest(Request.of(RequestType.CANCEL_AUCTION, auctionId));
    }

    // ── Item ──────────────────────────────────────────────────────────────────

    //Lấy danh sách sản phẩm của seller hiện tại.

    public Response fetchMyItems() throws Exception {
        return socket.sendRequest(Request.of(RequestType.GET_MY_ITEMS));
    }

    //Tạo sản phẩm mới.

    public Response createItem(Item item) throws Exception {
        return socket.sendRequest(Request.of(RequestType.CREATE_ITEM, item));
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    //Đăng xuất, thông báo server dọn session.

    public void logout() {
        try {
            socket.sendRequest(Request.of(RequestType.LOGOUT));
        } catch (Exception e) {
            System.err.println("[SellerApiService] logout error: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Auction> extractAuctions(Response res) {
        if (!res.isSuccess()) return Collections.emptyList();
        List<Auction> list = res.getDataAs(List.class);
        return list != null ? list : Collections.emptyList();
    }


    @SuppressWarnings("unchecked")
    public List<Item> extractItems(Response res) {
        if (!res.isSuccess()) return Collections.emptyList();
        List<Item> list = res.getDataAs(List.class);
        return list != null ? list : Collections.emptyList();
    }
}