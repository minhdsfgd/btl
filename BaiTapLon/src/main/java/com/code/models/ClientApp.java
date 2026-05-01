package com.code.models;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class ClientApp extends Application {
    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;

    public static void main() {

    }

    @Override // Override from Application
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("/com/code/views/Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WIDTH, HEIGHT);

        stage.setTitle("UET Auction System");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }
}
