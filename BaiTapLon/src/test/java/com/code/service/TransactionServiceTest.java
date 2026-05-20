package com.code.service;

import com.code.dao.TransactionDAO;
import com.code.models.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private TransactionDAO mockTxDAO;
    private TransactionService txService;

    @BeforeEach
    void setUp() {
        // 1. Tạo mock cho DAO
        mockTxDAO = Mockito.mock(TransactionDAO.class);

        // 2. Khởi tạo Service
        txService = new TransactionService(mockTxDAO);
    }

    // =========================================================================
    // 1. TEST CÁC LUỒNG GHI NHẬN GIAO DỊCH (LOGGING)
    // =========================================================================
    @Nested
    @DisplayName("Tests Ghi nhận Giao dịch (Logging Transactions)")
    class LogTransactionTests {

        @Test
        @DisplayName("TC01: Ghi nhận nạp tiền (logDeposit) chuẩn xác")
        void testLogDeposit() throws SQLException {
            txService.logDeposit(10, 50_000.0);

            // Tóm lấy object Transaction mà txService chuẩn bị gửi cho DAO
            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(mockTxDAO, times(1)).save(captor.capture());

            Transaction savedTx = captor.getValue();
            assertEquals(10, savedTx.getToUserId(), "Tiền nạp phải vào ví User ID 10");
            assertEquals(50_000.0, savedTx.getAmount());
            assertEquals(Transaction.Type.DEPOSIT, savedTx.getType(), "Loại giao dịch phải là DEPOSIT");
        }

        @Test
        @DisplayName("TC02: Ghi nhận giam tiền đặt giá (logBidHold) chuẩn xác")
        void testLogBidHold() throws SQLException {
            txService.logBidHold(20, 15_000.0, 100);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(mockTxDAO, times(1)).save(captor.capture());

            Transaction savedTx = captor.getValue();
            assertEquals(20, savedTx.getFromUserId(), "Tiền phải đi từ User ID 20");
            assertEquals(-1, savedTx.getToUserId(), "Tiền phải chuyển cho hệ thống (-1)");
            assertEquals(15_000.0, savedTx.getAmount());
            assertEquals(100, savedTx.getAuctionId());
            assertEquals(Transaction.Type.ADJUSTMENT, savedTx.getType());
        }

        @Test
        @DisplayName("TC03: Ghi nhận hoàn tiền (logRefund) chuẩn xác")
        void testLogRefund() throws SQLException {
            txService.logRefund(30, 25_000.0, 100);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(mockTxDAO, times(1)).save(captor.capture());

            Transaction savedTx = captor.getValue();
            assertEquals(-1, savedTx.getFromUserId(), "Tiền hoàn phải từ hệ thống (-1)");
            assertEquals(30, savedTx.getToUserId(), "Tiền hoàn phải về User ID 30");
            assertEquals(25_000.0, savedTx.getAmount());
            assertEquals(100, savedTx.getAuctionId());
            assertEquals(Transaction.Type.REFUND, savedTx.getType());
        }

        @Test
        @DisplayName("TC04: Ghi nhận thanh toán cho người bán (logPaymentToSeller) chuẩn xác")
        void testLogPaymentToSeller() throws SQLException {
            // Buyer ID = 40, Seller ID = 50
            txService.logPaymentToSeller(40, 50, 200_000.0, 200);

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(mockTxDAO, times(1)).save(captor.capture());

            Transaction savedTx = captor.getValue();
            assertEquals(40, savedTx.getFromUserId(), "Tiền từ người mua (40)");
            assertEquals(50, savedTx.getToUserId(), "Tiền tới người bán (50)");
            assertEquals(200_000.0, savedTx.getAmount());
            assertEquals(200, savedTx.getAuctionId());
            assertEquals(Transaction.Type.AUCTION_PAYMENT, savedTx.getType());
        }
    }

    // =========================================================================
    // 2. TEST TRUY VẤN LỊCH SỬ GIAO DỊCH
    // =========================================================================
    @Nested
    @DisplayName("Tests Lấy lịch sử giao dịch (Transaction History)")
    class GetHistoryTests {

        @Test
        @DisplayName("TC05: Lấy sao kê lịch sử (getTransactionHistory) thành công")
        void testGetTransactionHistory_Success() throws SQLException {
            // Giả lập Database trả về 2 dòng giao dịch cho User ID 10
            List<Transaction> mockList = Arrays.asList(
                    Transaction.deposit(1, 10, 50000),
                    new Transaction(2, 10, -1, 10000, 1, Transaction.Type.ADJUSTMENT)
            );
            when(mockTxDAO.findByUserId(10)).thenReturn(mockList);

            List<Transaction> result = txService.getTransactionHistory(10);

            assertEquals(2, result.size());
            verify(mockTxDAO, times(1)).findByUserId(10);
        }
    }

    // =========================================================================
    // 3. TEST CƠ CHẾ XỬ LÝ LỖI (EXCEPTION HANDLING)
    // =========================================================================
    @Nested
    @DisplayName("Tests Ngoại lệ Database (DB Exception Handling)")
    class ExceptionTests {

        @Test
        @DisplayName("TC06: Ném RuntimeException khi save gặp SQLException")
        void testSave_ThrowsRuntimeException() throws SQLException {
            // Ép DAO quăng ra SQLException khi gọi save
            doThrow(new SQLException("Lỗi mạng Database")).when(mockTxDAO).save(any(Transaction.class));

            // Đảm bảo Service bọc lỗi đó lại bằng RuntimeException
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                txService.logDeposit(1, 50000);
            });

            assertTrue(exception.getMessage().contains("Lỗi DB"));
            assertTrue(exception.getCause() instanceof SQLException);
        }

        @Test
        @DisplayName("TC07: Ném RuntimeException khi getHistory gặp SQLException")
        void testGetHistory_ThrowsRuntimeException() throws SQLException {
            doThrow(new SQLException("Lỗi truy vấn")).when(mockTxDAO).findByUserId(99);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                txService.getTransactionHistory(99);
            });

            assertTrue(exception.getMessage().contains("Lỗi DB"));
            assertTrue(exception.getCause() instanceof SQLException);
        }
    }
}