package com.code.network;

import java.io.Serializable;

/**
 * Đối tượng Server trả về Client qua ObjectOutputStream.
 *
 * <p><b>Cấu trúc:</b>
 * <ul>
 *   <li>{@code success} — true nếu xử lý thành công, false nếu có lỗi</li>
 *   <li>{@code message} — thông báo hiển thị cho user (lỗi hoặc xác nhận)</li>
 *   <li>{@code data}    — payload trả về (null nếu không cần)</li>
 * </ul>
 * </p>
 *
 * <p><b>Cách dùng ở RequestProcessor (Server):</b></p>
 * <pre>
 * // Thành công có data
 * return Response.ok("Đăng nhập thành công", user);
 *
 * // Thành công không cần data
 * return Response.ok("Đặt giá thành công");
 *
 * // Thất bại
 * return Response.fail("Sai mật khẩu");
 * </pre>
 *
 * <p><b>Cách dùng ở Controller (Client):</b></p>
 * <pre>
 * Response res = SocketClient.getInstance().sendRequest(req);
 * if (res.isSuccess()) {
 *     User user = res.getDataAs(User.class);
 *     // chuyển màn hình
 * } else {
 *     errorLabel.setText(res.getMessage());
 * }
 * </pre>
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String  message;
    private final Object  data;

    // ── Constructor ───────────────────────────────────────────────────────────

    private Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data    = data;
    }

    // ── Static factories (dùng thay vì new Response()) ───────────────────────

    /** Phản hồi thành công kèm data. */
    public static Response ok(String message, Object data) {
        return new Response(true, message, data);
    }

    /** Phản hồi thành công không cần data. */
    public static Response ok(String message) {
        return new Response(true, message, null);
    }

    /** Phản hồi thất bại. */
    public static Response fail(String message) {
        return new Response(false, message, null);
    }

    /** Phản hồi lỗi server nội bộ — bắt exception và trả về message an toàn. */
    public static Response error(Exception e) {
        return new Response(false, "Lỗi server: " + e.getMessage(), null);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isSuccess() { return success; }
    public String  getMessage(){ return message;  }
    public Object  getData()   { return data;     }

    /** Tiện ích — cast data về kiểu mong muốn. */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> clazz) {
        return clazz.cast(data);
    }

    @Override
    public String toString() {
        return "Response{success=" + success + ", message='" + message + "'}";
    }
}