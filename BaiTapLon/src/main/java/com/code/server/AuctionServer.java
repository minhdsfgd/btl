package com.code.server;

import com.code.dao.*;
import com.code.database.DBConnection;
import com.code.service.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Điểm khởi động Server — chạy {@code main()} để bật server.
 *
 * <p><b>Trách nhiệm:</b>
 * <ol>
 *   <li>Khởi tạo tất cả DAO và Service (một lần duy nhất)</li>
 *   <li>Chạy DataSeeder tạo Admin mặc định</li>
 *   <li>Khởi động AuctionService scheduler</li>
 *   <li>Lắng nghe cổng 8888, mỗi client kết nối → tạo thread mới</li>
 *   <li>Đăng ký shutdown hook để dọn dẹp khi server tắt</li>
 * </ol>
 * </p>
 *
 * <p><b>Cách chạy:</b></p>
 * <pre>
 * java -jar auction-server.jar
 * </pre>
 */
public class AuctionServer {

    private static final int PORT = 8888;

    // ── Danh sách client đang kết nối (dùng CopyOnWriteArrayList — thread-safe) ──
    // Dùng để broadcast thông báo toàn hệ thống nếu cần
    static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════");
        System.out.println("   UET Auction System — Server v1.0");
        System.out.println("═══════════════════════════════════════");

        // ── Bước 1: Khởi tạo tất cả DAO ─────────────────────────────────────
        UserDAO        userDAO    = new UserDAO();
        ItemDAO        itemDAO    = new ItemDAO();
        AuctionDAO     auctionDAO = new AuctionDAO();
        BidDAO         bidDAO     = new BidDAO();
        TransactionDAO txDAO      = new TransactionDAO();

        System.out.println("[Server] ✓ DAO khởi tạo xong");

        // ── Bước 2: Khởi tạo tất cả Service (inject DAO vào) ────────────────
        UserService        userService = new UserService(userDAO);
        TransactionService txService   = new TransactionService(txDAO);
        BidService         bidService  = new BidService(bidDAO, auctionDAO, userDAO);
        ItemService        itemService = new ItemService(itemDAO);

        // AuctionService là Singleton — init với đủ dependency
        AuctionService.init(auctionDAO, userDAO, txService);
        AuctionService auctionService = AuctionService.getInstance();

        System.out.println("[Server] ✓ Service khởi tạo xong");

        // ── Bước 3: Seed dữ liệu mặc định ───────────────────────────────────
        try {
            DataSeeder seeder = new DataSeeder(userDAO, itemDAO, auctionDAO);
            seeder.seed();
        } catch (Exception e) {
            System.err.println("[Server] Lỗi seed data: " + e.getMessage());
        }

        // ── Bước 4: Tạo RequestProcessor (truyền tất cả service vào) ────────
        RequestProcessor processor = new RequestProcessor(
                userService, bidService, auctionService,
                itemService, txService,
                userDAO, itemDAO, bidDAO, txDAO
        );

        // ── Bước 5: Đăng ký shutdown hook ────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Server] Đang tắt server...");
            auctionService.shutdown();   // dừng scheduler
            DBConnection.getInstance().close();  // đóng kết nối DB
            System.out.println("[Server] Server đã tắt an toàn.");
        }));

        // ── Bước 6: Bắt đầu lắng nghe kết nối ───────────────────────────────
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] ✓ Đang lắng nghe cổng " + PORT);
            System.out.println("[Server] Nhấn Ctrl+C để dừng server\n");

            while (true) {
                // accept() blocking — chờ client kết nối
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Client mới: "
                        + clientSocket.getInetAddress().getHostAddress()
                        + " (tổng: " + (connectedClients.size() + 1) + ")");

                // Tạo handler cho client này và chạy trên thread riêng
                ClientHandler handler = new ClientHandler(clientSocket, processor);
                connectedClients.add(handler);

                Thread thread = new Thread(handler,
                        "client-" + clientSocket.getInetAddress().getHostAddress());
                thread.setDaemon(true);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("[Server] Lỗi ServerSocket: " + e.getMessage());
        }
    }
}