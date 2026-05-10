package com.code.server;

import com.code.models.*;
import com.code.network.Request;
import com.code.network.Response;
import com.code.service.AuthGuard;
import com.code.exception.*;

import java.io.*;
import java.net.Socket;

/**
 * Xử lý một client — mỗi client kết nối tạo ra một ClientHandler.
 *
 * <p><b>Hai vai trò:</b>
 * <ol>
 *   <li><b>Runnable</b> — vòng lặp đọc Request → gọi processor → ghi Response</li>
 *   <li><b>AuctionObserver</b> — khi đang xem phiên đấu giá,
 *       nhận AuctionEvent từ server và push ngay xuống client</li>
 * </ol>
 * </p>
 *
 * <p><b>Trạng thái của ClientHandler:</b>
 * <ul>
 *   <li>{@code currentUser} — null trước khi đăng nhập, User object sau khi login</li>
 *   <li>{@code watchingAuction} — null nếu không xem phiên nào,
 *       Auction object khi đang ở màn hình LiveBidding</li>
 * </ul>
 * </p>
 */
public class ClientHandler implements Runnable, Auction.AuctionObserver {

    private final Socket           socket;
    private final RequestProcessor processor;

    private ObjectOutputStream out;
    private ObjectInputStream  in;

    // Trạng thái của client này
    User    currentUser;      // null = chưa đăng nhập
    Auction watchingAuction;  // null = không xem phiên nào

    // ── Constructor ───────────────────────────────────────────────────────────

    public ClientHandler(Socket socket, RequestProcessor processor) {
        this.socket    = socket;
        this.processor = processor;
        try {
            // QUAN TRỌNG: khởi tạo out TRƯỚC in — tránh deadlock
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in  = new ObjectInputStream (socket.getInputStream());
        } catch (IOException e) {
            System.err.println("[Handler] Lỗi khởi tạo stream: " + e.getMessage());
        }
    }

    // ── Runnable: vòng lặp xử lý request ────────────────────────────────────

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {

                // 1. Đọc request từ client (blocking)
                Request request = (Request) in.readObject();
                System.out.println("[Handler] Nhận: " + request
                        + (currentUser != null ? " từ " + currentUser.getUsername() : " (chưa login)"));

                // 2. Kiểm tra ban trước mọi request (trừ LOGIN và REGISTER)
                if (request.getType() != com.code.network.RequestType.LOGIN
                        && request.getType() != com.code.network.RequestType.REGISTER) {
                    try {
                        AuthGuard.requireNotBanned(currentUser);
                    } catch (UserBannedException e) {
                        sendResponse(Response.fail("Tài khoản bị cấm: " + e.getMessage()));
                        break; // đóng kết nối nếu bị ban
                    }
                }

                // 3. Xử lý request
                Response response = processor.process(request, this);

                // 4. Gửi response về client
                sendResponse(response);
            }

        } catch (EOFException | java.net.SocketException e) {
            // Client ngắt kết nối bình thường
            System.out.println("[Handler] Client ngắt kết nối: "
                    + (currentUser != null ? currentUser.getUsername() : "unknown"));
        } catch (Exception e) {
            System.err.println("[Handler] Lỗi: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // ── AuctionObserver: nhận event và push xuống client ────────────────────

    /**
     * Server gọi method này khi có AuctionEvent mới (bid mới, phiên kết thúc...).
     * Push ngay xuống client đang xem phiên này.
     */
    @Override
    public synchronized void onAuctionEvent(AuctionEvent event) {
        try {
            out.writeObject(event);
            out.flush();
            out.reset();
        } catch (IOException e) {
            // Client đã ngắt kết nối — gỡ khỏi observer
            System.err.println("[Handler] Không thể push event, gỡ observer: " + e.getMessage());
            stopWatching();
        }
    }

    // ── Quản lý watching auction ──────────────────────────────────────────────

    /**
     * Đăng ký xem một phiên đấu giá — nhận AuctionEvent realtime.
     * Gọi khi client mở màn hình LiveBidding.
     */
    void startWatching(Auction auction) {
        stopWatching(); // gỡ phiên cũ nếu có
        this.watchingAuction = auction;
        auction.addObserver(this);
        System.out.println("[Handler] " + getUsername() + " bắt đầu xem phiên #" + auction.getAuctionId());
    }

    /**
     * Hủy đăng ký xem phiên — khi client thoát màn hình LiveBidding.
     */
    void stopWatching() {
        if (watchingAuction != null) {
            watchingAuction.removeObserver(this);
            System.out.println("[Handler] " + getUsername() + " thoát phiên #" + watchingAuction.getAuctionId());
            watchingAuction = null;
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Gửi Response về client — synchronized để tránh race condition khi push event đồng thời. */
    synchronized void sendResponse(Response response) {
        try {
            out.writeObject(response);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("[Handler] Lỗi gửi response: " + e.getMessage());
        }
    }

    private void cleanup() {
        stopWatching();
        AuctionServer.connectedClients.remove(this);
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private String getUsername() {
        return currentUser != null ? currentUser.getUsername() : "unknown";
    }

    public User getCurrentUser() { return currentUser; }
}