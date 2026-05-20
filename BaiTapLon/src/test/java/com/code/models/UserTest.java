package com.code.models;

import com.code.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    // =========================================================================
    // 1. TEST CORE LOGIC VÀ QUẢN LÝ TÀI CHÍNH (Tiền tệ)
    // =========================================================================
    @Nested
    @DisplayName("Tests Quản lý Tài chính (Balance Logic)")
    class BalanceTests {

        private User user;

        @BeforeEach
        void setUp() {
            // Dùng RegularUser để test logic chung của class User
            user = new RegularUser(1, "tester", "password", 100_000, Role.BIDDER);
        }

        @Test
        @DisplayName("TC01: Khởi tạo User với balance < 0 phải ném IllegalArgumentException")
        void testConstructor_NegativeBalance_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> {
                new RegularUser(2, "bad_user", "pass", -500, Role.BIDDER);
            }, "Không được khởi tạo user với số dư âm");
        }

        @Test
        @DisplayName("TC02: Khởi tạo với username hoặc password null phải ném NullPointerException")
        void testConstructor_NullCredentials() {
            assertThrows(NullPointerException.class, () -> new RegularUser(3, null, "pass", 0, Role.BIDDER));
            assertThrows(NullPointerException.class, () -> new RegularUser(4, "user", null, 0, Role.BIDDER));
        }

        @Test
        @DisplayName("TC03: Nạp tiền (deposit) hợp lệ -> Số dư tăng")
        void testDeposit_ValidAmount() {
            user.deposit(50_000);
            assertEquals(150_000, user.getBalance(), "Số dư phải là 150,000 sau khi nạp");
        }

        @Test
        @DisplayName("TC04: Nạp tiền (deposit) <= 0 -> Ném IllegalArgumentException")
        void testDeposit_InvalidAmount() {
            assertThrows(IllegalArgumentException.class, () -> user.deposit(0));
            assertThrows(IllegalArgumentException.class, () -> user.deposit(-10_000));
        }

        @Test
        @DisplayName("TC05: Trừ tiền (deductBalance) hợp lệ -> Số dư giảm")
        void testDeductBalance_ValidAmount() throws InsufficientBalanceException {
            user.deductBalance(30_000);
            assertEquals(70_000, user.getBalance(), "Số dư phải còn 70,000 sau khi trừ");
        }

        @Test
        @DisplayName("TC06: Trừ tiền (deductBalance) vượt quá số dư -> Ném InsufficientBalanceException")
        void testDeductBalance_InsufficientFunds() {
            InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> {
                user.deductBalance(150_000); // Đang có 100_000, trừ 150_000
            });
            assertTrue(exception.getMessage().contains("Số dư không đủ"));
        }

        @Test
        @DisplayName("TC07: Trừ tiền <= 0 -> Ném IllegalArgumentException")
        void testDeductBalance_InvalidAmount() {
            assertThrows(IllegalArgumentException.class, () -> user.deductBalance(0));
            assertThrows(IllegalArgumentException.class, () -> user.deductBalance(-50_000));
        }
    }

    // =========================================================================
    // 2. TEST QUẢN LÝ QUYỀN (ROLES)
    // =========================================================================
    @Nested
    @DisplayName("Tests Quản lý Quyền (Role Management)")
    class RoleTests {

        @Test
        @DisplayName("TC08: Thêm, xóa và kiểm tra role hoạt động chính xác")
        void testRole_AddRemoveHas() {
            User user = new RegularUser(1, "user", "pass", 0, Role.BIDDER);
            assertTrue(user.hasRole(Role.BIDDER), "Phải có role BIDDER từ lúc khởi tạo");
            assertFalse(user.hasRole(Role.SELLER), "Chưa có role SELLER");

            user.addRole(Role.SELLER);
            assertTrue(user.hasRole(Role.SELLER), "Phải có role SELLER sau khi add");

            user.removeRole(Role.BIDDER);
            assertFalse(user.hasRole(Role.BIDDER), "Role BIDDER phải biến mất sau khi remove");
        }

        @Test
        @DisplayName("TC09: getRoles() trả về tập hợp không thể thay đổi (Unmodifiable)")
        void testGetRoles_IsUnmodifiable() {
            User user = new RegularUser(1, "user", "pass", 0, Role.BIDDER);
            assertThrows(UnsupportedOperationException.class, () -> {
                user.getRoles().add(Role.ADMIN); // Cố tình sửa đổi Set trả về
            }, "Phải ném lỗi khi cố sửa Set role lấy ra từ getRoles()");
        }
    }

    // =========================================================================
    // 3. TEST CÁC CLASS CỤ THỂ (RegularUser & Admin)
    // =========================================================================
    @Nested
    @DisplayName("Tests Các Class con cụ thể (RegularUser, Admin)")
    class SubclassTests {

        @Test
        @DisplayName("TC10: RegularUser nhận đúng quyền canBid và canSell")
        void testRegularUser_Roles() {
            RegularUser bidder = new RegularUser(1, "b1", "p", 0, Role.BIDDER);
            assertTrue(bidder.canBid());
            assertFalse(bidder.canSell());

            RegularUser seller = new RegularUser(2, "s1", "p", 0, Role.SELLER);
            assertFalse(seller.canBid());
            assertTrue(seller.canSell());

            RegularUser both = new RegularUser(3, "bs", "p", 0, Role.BIDDER, Role.SELLER);
            assertTrue(both.canBid());
            assertTrue(both.canSell());
        }

        @Test
        @DisplayName("TC11: Admin luôn tự động có Role.ADMIN")
        void testAdmin_AlwaysHasAdminRole() {
            Admin admin = new Admin(99, "admin", "secret", 0);
            assertTrue(admin.hasRole(Role.ADMIN), "Khởi tạo Admin phải có sẵn Role ADMIN");
        }
    }

    // =========================================================================
    // 4. TEST MULTITHREADING (ĐỒNG BỘ HÓA / CONCURRENCY)
    // =========================================================================
    @Nested
    @DisplayName("Tests Đồng bộ hóa (Thread Safety)")
    class ConcurrencyTests {

        @Test
        @DisplayName("TC12 (Từ TC24 cũ): Nạp tiền đồng thời từ nhiều thread không bị mất dữ liệu (No Lost Update)")
        void testConcurrency_Deposit() throws InterruptedException {
            User user = new RegularUser(1, "tester", "pass", 0, Role.BIDDER);
            int threadCount = 50;
            double depositEach = 10_000;

            CountDownLatch latch = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    try {
                        latch.await();
                        user.deposit(depositEach);
                    } catch (InterruptedException ignored) {}
                });
            }

            latch.countDown(); // Bắn pháo hiệu cho 50 luồng chạy cùng lúc
            pool.shutdown();
            pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

            assertEquals(500_000, user.getBalance(), 0.01, "Tổng số dư phải là 500,000");
        }

        @Test
        @DisplayName("TC13: Trừ tiền đồng thời không làm âm số dư (Race Condition Check)")
        void testConcurrency_Deduct() throws InterruptedException {
            User user = new RegularUser(1, "tester", "pass", 50_000, Role.BIDDER);
            int threadCount = 10;
            double deductEach = 10_000;
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger exceptionCount = new AtomicInteger(0);

            CountDownLatch latch = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);

            // 10 luồng cùng tranh nhau trừ 10k, nhưng tổng tiền chỉ có 50k.
            // Sẽ chỉ có tối đa 5 luồng thành công, 5 luồng văng lỗi.
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    try {
                        latch.await();
                        user.deductBalance(deductEach);
                        successCount.incrementAndGet();
                    } catch (InsufficientBalanceException e) {
                        exceptionCount.incrementAndGet();
                    } catch (InterruptedException ignored) {}
                });
            }

            latch.countDown();
            pool.shutdown();
            pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

            assertEquals(0, user.getBalance(), 0.01, "Số dư cuối cùng không được âm, phải bằng 0");
            assertEquals(5, successCount.get(), "Chỉ được phép có đúng 5 giao dịch trừ tiền thành công");
            assertEquals(5, exceptionCount.get(), "Phải có 5 giao dịch bị văng lỗi InsufficientBalanceException");
        }
    }
}