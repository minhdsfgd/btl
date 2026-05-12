package com.code.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

import com.code.network.Request;
import com.code.network.Response;

/**
 * Quản lý kết nối Socket từ phía Client — Singleton.
 *
 * <p><b>2 chế độ giao tiếp:</b>
 * <ol>
 *   <li><b>sendRequest()</b> — gửi Request, CHỜ Response đồng bộ.
 *       Dùng khi cần kết quả ngay (đăng nhập, đặt giá, v.v.).</li>
 *   <li><b>startListening()</b> — lắng nghe Server PUSH bất đồng bộ.
 *       Dùng nhận AuctionEvent realtime khi đang xem phiên đấu giá.</li>
 * </ol>
 * </p>
 *
 * <p><b>Khởi động:</b></p>
 * <pre>
 * // Trong ClientApp.start() hoặc trước khi mở màn hình đầu tiên:
 * SocketClient.init("192.168.1.105", 8888);
 * SocketClient client = SocketClient.getInstance();
 * </pre>
 *
 * <p><b>Gửi request từ Controller:</b></p>
 * <pre>
 * Response res = SocketClient.getInstance()
 *                            .sendRequest(Request.of(RequestType.LOGIN, loginData));
 * if (res.isSuccess()) {
 *     User user = res.getDataAs(User.class);
 * }
 * </pre>
 */
public class SocketClient {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile SocketClient instance;

    /** Khởi tạo lần đầu — gọi một lần duy nhất khi app khởi động. */
    public static synchronized void init(String host, int port) {
        if (instance == null) {
            instance = new SocketClient(host, port);
        }
    }

    public static SocketClient getInstance() {
        if (instance == null)
            throw new IllegalStateException("SocketClient chưa được khởi tạo. Gọi init() trước.");
        return instance;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final String host;
    private final int    port;
    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private Thread listenerThread;
    private volatile boolean listening = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    private SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
        connect();
    }

    // ── Kết nối ───────────────────────────────────────────────────────────────

    private void connect() {
        try {
            socket = new Socket(host, port);
            // QUAN TRỌNG: khởi tạo out TRƯỚC in — tránh deadlock
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream (socket.getInputStream());
            System.out.println("[Client] ✓ Đã kết nối server " + host + ":" + port);
        } catch (IOException e) {
            throw new RuntimeException(
                    "[Client] Không thể kết nối server " + host + ":" + port
                            + " — " + e.getMessage(), e);
        }
    }

    // ── Gửi request đồng bộ ──────────────────────────────────────────────────

    /**
     * Gửi Request lên Server và CHỜ Response.
     *
     * <p>Phương thức này BLOCKING — UI thread không nên gọi trực tiếp.
     * Dùng {@code Task<Response>} của JavaFX hoặc gọi trong thread riêng:</p>
     *
     * <pre>
     * // Trong Controller — chạy trong background thread:
     * new Thread(() -> {
     *     try {
     *         Response res = SocketClient.getInstance().sendRequest(req);
     *         Platform.runLater(() -> handleResponse(res));
     *     } catch (IOException | ClassNotFoundException e) {
     *         Platform.runLater(() -> showError(e.getMessage()));
     *     }
     * }).start();
     * </pre>
     */
    public synchronized Response sendRequest(Request request)
            throws IOException, ClassNotFoundException {
        try {
            out.writeObject(request);
            out.flush();
            out.reset(); // tránh cache object cũ
            return (Response) in.readObject();
        } catch (IOException e) {
            // Thử reconnect nếu mất kết nối
            if (socket == null || socket.isClosed()) {
                System.out.println("[Client] Socket closed, attempting reconnect...");
                connect();
            }
            throw e;
        }
    }

    // ── Gửi request KHÔNG chờ response (dùng khi đang startListening) ────────

    /**
     * Gửi Request lên Server mà KHÔNG đọc Response.
     *
     * <p>Dùng khi đã gọi {@link #startListening(Consumer)} — tức là đang trong
     * màn hình LiveBidding. Response trả về từ server sẽ được listener bắt,
     * không cần đọc lại ở đây.</p>
     *
     * <pre>
     * // Trong LiveBiddingController — gửi bid, listener sẽ nhận Response:
     * SocketClient.getInstance().sendAsync(
     *     Request.of(RequestType.PLACE_BID, new PlaceBidData(auctionId, amount)));
     * </pre>
     */
    public synchronized void sendAsync(Request request) throws IOException {
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
        } catch (IOException e) {
            // Thử reconnect nếu mất kết nối
            if (socket == null || socket.isClosed()) {
                System.out.println("[Client] Socket closed, attempting reconnect...");
                connect();
            }
            throw e;
        }
    }

    // ── Lắng nghe push từ Server (realtime) ───────────────────────────────────

    /**
     * Bắt đầu lắng nghe sự kiện Server PUSH bất đồng bộ.
     *
     * <p>Server chủ động gửi {@link com.code.models.AuctionEvent} xuống client
     * khi có bid mới hoặc phiên kết thúc — không cần client hỏi trước.</p>
     *
     * <p>Callback {@code onEvent} được gọi trên background thread.
     * Mọi cập nhật UI phải qua {@code Platform.runLater()}:</p>
     *
     * <pre>
     * // Trong LiveBiddingController khi vào màn hình đấu giá:
     * SocketClient.getInstance().startListening(event -> {
     *     Platform.runLater(() -> {
     *         switch (event.getType()) {
     *             case BID_PLACED ->
     *                 priceLabel.setText(String.format("%,.0f VNĐ",
     *                     event.getBid().getAmount()));
     *             case AUCTION_FINISHED ->
     *                 showWinner(event.getWinnerBidderId());
     *             case AUCTION_CANCELED ->
     *                 showCanceledMessage();
     *         }
     *     });
     * });
     * </pre>
     *
     * @param onEvent callback nhận {@link com.code.models.AuctionEvent}
     */
    public synchronized void startListening(Consumer<Object> onEvent) {

        stopListening();

        listening = true;

        listenerThread = new Thread(() -> {
            try {
                while (listening && socket != null && !socket.isClosed()) {

                    Object event = in.readObject();
                    onEvent.accept(event);
                }

            } catch (EOFException | java.net.SocketException e) {

                if (listening) {
                    System.out.println("[Client] Mất kết nối server.");
                    onEvent.accept(null);
                }

            } catch (Exception e) {

                if (listening) {
                    System.err.println("[Client] Lỗi listener: " + e.getMessage());
                }
            }

        }, "auction-listener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    public synchronized void stopListening() {

        listening = false;

        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
    }

    // ── Đóng kết nối ─────────────────────────────────────────────────────────

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[Client] Đã ngắt kết nối server.");
        } catch (IOException e) {
            System.err.println("[Client] Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }
}