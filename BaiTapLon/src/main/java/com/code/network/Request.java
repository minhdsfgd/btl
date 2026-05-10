package com.code.network;

import java.io.Serializable;

/**
 * Đối tượng Client gửi lên Server qua ObjectOutputStream.
 *
 * <p><b>Cấu trúc:</b>
 * <ul>
 *   <li>{@code type} — loại yêu cầu (từ {@link RequestType})</li>
 *   <li>{@code data} — payload, kiểu tuỳ theo type (xem Javadoc của từng RequestType)</li>
 * </ul>
 * </p>
 *
 * <p><b>Cách dùng ở Controller (Client):</b></p>
 * <pre>
 * // Đăng nhập
 * Request req = Request.of(RequestType.LOGIN,
 *                          new LoginData("alice", "pass123"));
 * Response.java res = SocketClient.getInstance().sendRequest(req);
 *
 * // Đặt giá — không cần truyền user vì server đã biết qua session
 * Request req = Request.of(RequestType.PLACE_BID,
 *                          new PlaceBidData(auctionId, 1_500_000));
 * </pre>
 *
 * <p><b>Lưu ý:</b> Mọi object dùng làm {@code data} phải {@code implements Serializable}.</p>
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private final RequestType type;
    private final Object      data;  // Serializable payload

    // ── Constructor ───────────────────────────────────────────────────────────

    private Request(RequestType type, Object data) {
        this.type = type;
        this.data = data;
    }

    /** Tạo Request với data. */
    public static Request of(RequestType type, Object data) {
        return new Request(type, data);
    }

    /** Tạo Request không có data (vd: GET_ACTIVE_AUCTIONS, LOGOUT). */
    public static Request of(RequestType type) {
        return new Request(type, null);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public RequestType getType() { return type; }
    public Object      getData() { return data; }

    /** Tiện ích — cast data về kiểu mong muốn. Ném ClassCastException nếu sai kiểu. */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> clazz) {
        return clazz.cast(data);
    }

    @Override
    public String toString() {
        return "Request{type=" + type + ", data=" + (data != null ? data.getClass().getSimpleName() : "null") + "}";
    }
}