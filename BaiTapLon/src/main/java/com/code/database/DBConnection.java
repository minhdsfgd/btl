package com.code.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Quản lý kết nối MySQL — Singleton.
 *
 * <p>Đọc config từ {@code db.properties} trong classpath.
 * Tự động kết nối lại nếu connection bị đóng hoặc timeout.</p>
 *
 * <pre>
 * // Lấy connection để dùng trong DAO:
 * Connection conn = DBConnection.getInstance().getConnection();
 * </pre>
 *
 * <p><b>Lưu ý:</b> Đây là single-connection đơn giản phù hợp cho BTL.
 * Production nên dùng Connection Pool (HikariCP).</p>
 */
public class DBConnection {

    // ── Singleton ──────────────────────────────────────────────────────────

    private static volatile DBConnection instance;

    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    // ── Fields ─────────────────────────────────────────────────────────────

    private final String url;
    private final String username;
    private final String password;
    private Connection   connection;

    // ── Constructor ────────────────────────────────────────────────────────

    private DBConnection() {
        Properties props = loadProperties();
        this.url      = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");
        this.connection = createConnection();
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Lấy Connection hiện tại.
     * Tự động kết nối lại nếu connection đã bị đóng hoặc invalid.
     */
    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("[DB] Kết nối lại MySQL...");
                connection = createConnection();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Lỗi kiểm tra connection: " + e.getMessage());
            connection = createConnection();
        }
        return connection;
    }

    /** Đóng kết nối khi server shutdown. */
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Đã đóng kết nối MySQL.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Lỗi khi đóng connection: " + e.getMessage());
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private Connection createConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("[DB] ✓ Kết nối MySQL thành công: " + url);
            return conn;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "[DB] Không tìm thấy MySQL JDBC Driver. Kiểm tra pom.xml.", e);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "[DB] Không thể kết nối MySQL: " + e.getMessage(), e);
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        // Tìm file trong classpath (src/main/resources/db.properties)
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null)
                throw new RuntimeException(
                        "Không tìm thấy db.properties trong classpath!");
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc db.properties: " + e.getMessage(), e);
        }
        return props;
    }
}