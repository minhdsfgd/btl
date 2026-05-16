package com.code;

import com.code.client.AppConfig;
import com.code.client.SocketClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.net.URL;

public class ClientApp extends Application {
    private static final double WIDTH  = 800;
    private static final double HEIGHT = 600;
    private static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Kết nối server trước khi mở giao diện
        try {
            SocketClient.init(AppConfig.getHost(), AppConfig.getPort());
        } catch (RuntimeException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi kết nối");
            alert.setHeaderText("Không thể kết nối đến server");
            alert.setContentText(
                    "Server: " + AppConfig.getHost() + ":" + AppConfig.getPort()
                            + "\n\nKiểm tra:\n"
                            + " • Server đã khởi động chưa?\n"
                            + " • IP/port trong config.properties có đúng không?");
            alert.showAndWait();
            Platform.exit();
            return;
        }

        // FIX: kiểm tra null trước khi load — tránh NullPointerException dòng 46
        URL fxmlUrl = getClass().getResource("/com/code/views/Login.fxml");
        if (fxmlUrl == null) {
            throw new IllegalStateException(
                    "Không tìm thấy Login.fxml!\n" +
                            "Kiểm tra: src/main/resources/com/code/views/Login.fxml có tồn tại không?\n" +
                            "Và đã chạy 'mvn clean package' chưa?"
            );
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);

        stage.setTitle("UET Auction System");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.centerOnScreen();

        stage.setOnCloseRequest(e -> {
            try { SocketClient.getInstance().disconnect(); } catch (Exception ignored) {}
        });

        stage.show();
    }

    public static Stage getStage() {
        return primaryStage;
    }
}