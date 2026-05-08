package com.code.service;

import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.RegularUser;
import com.code.models.Role;
import com.code.models.User;
import com.code.repository.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private FakeUserRepository fakeRepo;
    private UserService userService;

    @BeforeEach
    void setUp() {
        // 1. Khởi tạo kho chứa dữ liệu giả lập (chạy trên RAM)
        fakeRepo = new FakeUserRepository();

        userService = new UserService(fakeRepo);

        RegularUser activeUser = new RegularUser(1, "quynhnhu", "password123", 500000.0, Role.BIDDER);
        activeUser.setActive(true);
        activeUser.setBanned(false);
        fakeRepo.save(activeUser);

        RegularUser bannedUser = new RegularUser(2, "bad_user", "123456", 0.0, Role.BIDDER);
        bannedUser.setActive(true);
        bannedUser.setBanned(true); // Cố tình khóa tài khoản này
        fakeRepo.save(bannedUser);
    }

    //test logịn

    @Test
    @DisplayName("TC01: Đăng nhập thành công với tài khoản đang hoạt động")
    void testLogin_Success() {
        assertDoesNotThrow(() -> {
            User result = userService.login("quynhnhu", "password123");
            assertNotNull(result, "Đăng nhập thành công phải trả về thông tin User");
            assertEquals("quynhnhu", result.getUsername(), "Username trả về phải khớp");
        });
    }

    @Test
    @DisplayName("TC02: Đăng nhập thất bại do nhập sai mật khẩu")
    void testLogin_ThrowsAuthenticationException_WrongPassword() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userService.login("quynhnhu", "mat_khau_sai_bét");
        });
        assertEquals("Sai tên đăng nhập hoặc mật khẩu.", exception.getMessage());
    }

    @Test
    @DisplayName("TC03: Đăng nhập thất bại do tài khoản không tồn tại")
    void testLogin_ThrowsAuthenticationException_UserNotFound() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            userService.login("ghost_user", "123456");
        });
        assertEquals("Sai tên đăng nhập hoặc mật khẩu.", exception.getMessage());
    }

    @Test
    @DisplayName("TC04: Đăng nhập thất bại do tài khoản đã bị Admin khóa (Banned)")
    void testLogin_ThrowsUserBannedException() {
        assertThrows(UserBannedException.class, () -> {
            userService.login("bad_user", "123456");
        });
    }

   // test register

    @Test
    @DisplayName("TC05: Đăng ký thành công và lưu vào Fake DB")
    void testRegister_Success() {
        assertDoesNotThrow(() -> {
            RegularUser newUser = userService.register("new_bidder", "matkhau123", Role.BIDDER);

            // Kiểm tra kết quả trả về
            assertNotNull(newUser);
            assertEquals("new_bidder", newUser.getUsername());

            // Kiểm tra xem FakeRepo đã thực sự lưu User này vào List chưa
            assertTrue(fakeRepo.existsByUsername("new_bidder"), "User mới phải được lưu vào Database");
            assertEquals(3, fakeRepo.findAll().size(), "Tổng số User trong hệ thống phải tăng lên 3");
        });
    }

    @Test
    @DisplayName("TC06: Đăng ký thất bại do Username đã tồn tại")
    void testRegister_ThrowsAuthenticationException_DuplicateUsername() {
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            // Cố tình đăng ký lại tên quynhnhu đã có sẵn trong hàm setUp()
            userService.register("quynhnhu", "newpassword", Role.BIDDER);
        });
        assertEquals("Username 'quynhnhu' đã tồn tại.", exception.getMessage());
    }

    @Test
    @DisplayName("TC07: Đăng ký thất bại do Mật khẩu quá ngắn")
    void testRegister_ThrowsIllegalArgumentException_ShortPassword() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.register("test_user", "123", Role.BIDDER); // Pass có 3 ký tự
        });
        assertEquals("Password phải ít nhất 6 ký tự.", exception.getMessage());
    }
}