package com.code.client;

import java.io.*;
import java.util.Properties;

/**
 * Đọc cấu hình kết nối server từ file {@code config.properties}.
 *
 * <p>File đặt cạnh file JAR — người dùng tự sửa IP và port:</p>
 * <pre>
 * # config.properties
 * server.host=192.168.1.105
 * server.port=8888
 * </pre>
 *
 * <p>Nếu không tìm thấy file → dùng giá trị mặc định (localhost:8888).</p>
 *
 * <p><b>Dùng trong ClientApp:</b></p>
 * <pre>
 * SocketClient.init(AppConfig.getHost(), AppConfig.getPort());
 * </pre>
 */
public class AppConfig {

    private static final Properties props = new Properties();
    private static boolean loaded = false;

    static {
        load();
    }

    private static void load() {
        // Tìm file cạnh JAR trước
        File externalFile = new File("config.properties");
        if (externalFile.exists()) {
            try (InputStream in = new FileInputStream(externalFile)) {
                props.load(in);
                loaded = true;
                System.out.println("[Config] Đọc config từ: " + externalFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("[Config] Lỗi đọc config.properties: " + e.getMessage());
            }
        } else {
            // Fallback: tìm trong classpath (resources/)
            try (InputStream in = AppConfig.class
                    .getClassLoader().getResourceAsStream("config.properties")) {
                if (in != null) {
                    props.load(in);
                    loaded = true;
                    System.out.println("[Config] Dùng config mặc định từ classpath.");
                }
            } catch (IOException e) {
                System.err.println("[Config] Không đọc được config: " + e.getMessage());
            }
        }

        if (!loaded) {
            System.out.println("[Config] Không tìm thấy config.properties → dùng localhost:8888");
        }
    }

    public static String getHost() {
        return props.getProperty("server.host", "localhost");
    }

    public static int getPort() {
        try {
            return Integer.parseInt(props.getProperty("server.port", "8888"));
        } catch (NumberFormatException e) {
            return 8888;
        }
    }
}