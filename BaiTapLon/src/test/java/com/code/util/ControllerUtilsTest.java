package com.code.util;

import com.code.ClientApp;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ControllerUtilsTest {

    @BeforeAll
    static void initJavaFX() {
        // Cực kỳ quan trọng: Khởi tạo JavaFX Toolkit ảo ở chế độ ngầm.
        // Nếu không có hàm này, mọi lệnh liên quan đến JavaFX (Alert, Scene) sẽ văng lỗi IllegalStateException.
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit đã được khởi tạo trước đó rồi thì bỏ qua
        }
    }

    // =========================================================================
    // 1. TEST HÀM SHOW ALERT
    // =========================================================================
    @Test
    @DisplayName("TC01: Hàm showAlert tạo đúng thông báo và hiển thị mà không bị treo")
    void testShowAlert() {
        // Dùng mockConstruction để "đánh chặn" lệnh khởi tạo new Alert(...)
        try (MockedConstruction<Alert> mockedAlert = mockConstruction(Alert.class,
                (mock, context) -> {
                    // Yêu cầu Mockito: Khi ai đó gọi showAndWait() trên mock này, hãy KHÔNG LÀM GÌ CẢ (tránh treo test)
                    doNothing().when(mock).showAndWait();
                })) {

            // Chạy hàm thực tế
            ControllerUtils.showAlert(Alert.AlertType.INFORMATION, "Test Title", "Test Message");

            // Xác nhận rằng có đúng 1 đối tượng Alert vừa được sinh ra
            assertEquals(1, mockedAlert.constructed().size());
            Alert createdAlert = mockedAlert.constructed().get(0);

            // Xác nhận các thuộc tính được truyền vào Alert chính xác
            verify(createdAlert, times(1)).setTitle("Test Title");
            verify(createdAlert, times(1)).setContentText("Test Message");

            // Xác nhận hàm hiển thị đã thực sự được kích hoạt
            verify(createdAlert, times(1)).showAndWait();
        }
    }

    // =========================================================================
    // 2. TEST HÀM NAVIGATE TO
    // =========================================================================
    @Test
    @DisplayName("TC02: navigateTo bắt lỗi IOException và gọi showAlert khi sai đường dẫn FXML")
    void testNavigateTo_InvalidPath() {
        // Tạo một Stage giả
        Stage mockStage = mock(Stage.class);
        when(mockStage.getWidth()).thenReturn(800.0);
        when(mockStage.getHeight()).thenReturn(600.0);
        when(mockStage.isMaximized()).thenReturn(false);

        // Bật 2 cơ chế đánh chặn cùng lúc: 1 cho ClientApp (chứa Stage) và 1 cho Alert
        try (MockedStatic<ClientApp> mockedClientApp = mockStatic(ClientApp.class);
             MockedConstruction<Alert> mockedAlert = mockConstruction(Alert.class,
                     (mock, context) -> doNothing().when(mock).showAndWait())) {

            // Giả mạo: Khi code gọi ClientApp.getStage(), hãy trả về mockStage của chúng ta
            mockedClientApp.when(ClientApp::getStage).thenReturn(mockStage);

            // THỰC THI: Cố tình truyền đường dẫn tào lao để FXMLLoader.load() quăng lỗi IOException
            ControllerUtils.navigateTo("/invalid_path.fxml");

            // KIỂM TRA: Code của bạn đã hứng lỗi (catch) và sinh ra 1 hộp thoại Alert báo lỗi chưa?
            assertEquals(1, mockedAlert.constructed().size(), "Phải có 1 Alert báo lỗi được sinh ra khi không tìm thấy file");

            Alert errorAlert = mockedAlert.constructed().get(0);

            // Xác nhận Alert sinh ra mang đúng dòng chữ bạn đã code
            verify(errorAlert).setTitle("Lỗi");
            verify(errorAlert).setContentText(argThat(msg -> msg.contains("Không tìm thấy màn hình")));
        }
    }
}