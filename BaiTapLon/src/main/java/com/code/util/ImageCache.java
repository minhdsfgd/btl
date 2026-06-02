package com.code.util;

import com.code.client.SocketClient;
import com.code.network.Request;
import com.code.network.RequestType;
import com.code.network.Response;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache ảnh phía client — tải ảnh từ server qua socket một lần, lưu lại để
 * dùng lại mà không cần gửi request lần sau.

   <li>Server lưu ảnh dưới dạng file vật lý — client ở máy khác không đọc được.</li>
 *   <li>Giải pháp: client gửi {@code GET_IMAGE(fileName)} lên server,
 *       server đọc file và trả bytes (encode Base64).</li>
 *   <li>Cache trong memory để không gửi request lặp lại cho cùng file.</li>
 * </ul>
 *
 * <p><b>Cách dùng:</b></p>
 * <pre>{@code
 * // Trong controller, truyền imageUrl từ Item (ví dụ "images/item_123.jpg")
 * ImageCache.loadInto(imageUrl, myImageView, 226, 130);
 * }</pre>
 */
public class ImageCache {

    // Key = tên file (chỉ tên, không có đường dẫn), Value = Image đã tải
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    /**
     * Tải ảnh từ server (nếu chưa có trong cache) và set vào ImageView.
     * Chạy trong background thread, update UI trên JavaFX thread.
     *
     * @param imageUrl  đường dẫn file từ server (ví dụ "images/item_1234.jpg")
     * @param view      ImageView cần set ảnh
     * @param width     chiều rộng hiển thị (px)
     * @param height    chiều cao hiển thị (px)
     */
    public static void loadInto(String imageUrl, ImageView view, double width, double height) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        // Chỉ lấy tên file từ path (bất kể Windows hay Unix)
        String fileName = extractFileName(imageUrl);

        // Kiểm tra cache trước — nếu có rồi thì set ngay trên UI thread
        Image cached = cache.get(fileName);
        if (cached != null) {
            Platform.runLater(() -> view.setImage(cached));
            return;
        }

        // Chưa có → tải từ server trong background thread
        new Thread(() -> {
            try {
                Response res = SocketClient.getInstance()
                        .sendRequest(Request.of(RequestType.GET_IMAGE, fileName));

                if (!res.isSuccess()) {
                    System.err.println("[ImageCache] Server báo lỗi: " + res.getMessage());
                    return;
                }

                // Server trả về Base64 string
                String base64 = res.getDataAs(String.class);
                if (base64 == null || base64.isBlank()) return;

                byte[] bytes = Base64.getDecoder().decode(base64);

                // Tạo Image từ bytes (ByteArrayInputStream)
                Image img = new Image(
                        new ByteArrayInputStream(bytes),
                        width, height,
                        false,  // preserveRatio
                        true    // smooth
                );

                if (img.isError()) {
                    System.err.println("[ImageCache] Lỗi tạo Image: " + img.getException());
                    return;
                }

                // Lưu vào cache
                cache.put(fileName, img);

                // Cập nhật UI trên JavaFX Application Thread
                Platform.runLater(() -> view.setImage(img));

            } catch (Exception e) {
                System.err.println("[ImageCache] Lỗi tải ảnh '" + fileName + "': " + e.getMessage());
            }
        }, "img-load-" + fileName).start();
    }

    /**
     * Xóa toàn bộ cache — gọi khi logout để giải phóng memory.
     */
    public static void clear() {
        cache.clear();
    }

    /**
     * Xóa 1 ảnh khỏi cache theo tên file.
     */
    public static void evict(String fileName) {
        cache.remove(extractFileName(fileName));
    }

    // Lấy tên file từ path bất kể hệ điều hành
    private static String extractFileName(String path) {
        // Thay cả \ (Windows) và / (Unix) để xử lý nhất quán
        String normalized = path.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
}