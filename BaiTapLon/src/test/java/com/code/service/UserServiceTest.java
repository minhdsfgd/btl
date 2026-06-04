package com.code.service;

import com.code.dao.AuditLogDAO;
import com.code.dao.UserDAO;
import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.Admin;
import com.code.models.RegularUser;
import com.code.models.Role;
import com.code.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt; // IMPORT BCRYPT
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserDAO mockUserDAO;
    private AuditLogDAO mockAuditLogDAO;
    private UserService userService;

    private Admin adminUser;
    private RegularUser regularUser;

    @BeforeEach
    void setUp() {
        // 1. Khởi tạo các Mock DAO
        mockUserDAO = Mockito.mock(UserDAO.class);
        mockAuditLogDAO = Mockito.mock(AuditLogDAO.class);

        // 2. Khởi tạo Service với đầy đủ DAOs
        userService = new UserService(mockUserDAO, mockAuditLogDAO);

        // 3. Chuẩn bị dữ liệu mẫu (PHẢI MÃ HÓA PASSWORD MẪU BẰNG BCRYPT)
        String hashedAdminPass = BCrypt.hashpw("secret123", BCrypt.gensalt());
        adminUser = new Admin(1, "admin", hashedAdminPass, 0);

        String hashedUserPass = BCrypt.hashpw("password", BCrypt.gensalt());
        regularUser = new RegularUser(2, "player1", hashedUserPass, 50000, Role.BIDDER, Role.SELLER);
    }

    // =========================================================================
    // 1. TEST ĐĂNG KÝ & TẠO ADMIN (REGISTER & CREATE ADMIN)
    // =========================================================================
    @Nested
    @DisplayName("Tests Đăng ký tài khoản (register & createAdmin)")
    class RegistrationTests {

        @Test
        @DisplayName("TC01: Đăng ký user thường thành công (có sẵn role BIDDER & SELLER)")
        void testRegister_Success() throws Exception {
            when(mockUserDAO.existsByUsername("newuser")).thenReturn(false);

            RegularUser newUser = userService.register("newuser", "123456", Role.BIDDER);

            assertNotNull(newUser);
            assertEquals("newuser", newUser.getUsername());
            assertTrue(newUser.hasRole(Role.BIDDER));
            assertTrue(newUser.hasRole(Role.SELLER));

            verify(mockUserDAO, times(1)).save(any(RegularUser.class));
        }

        @Test
        @DisplayName("TC02: Đăng ký thất bại do username hoặc password không hợp lệ")
        void testRegister_InvalidInputs() {
            assertThrows(IllegalArgumentException.class, () -> userService.register("", "123456", Role.BIDDER));
            assertThrows(IllegalArgumentException.class, () -> userService.register("user", "123", Role.BIDDER)); // pass < 6 ký tự
        }

        @Test
        @DisplayName("TC03: Ném AuthenticationException khi cố tự đăng ký làm ADMIN")
        void testRegister_Fail_TryToRegisterAdmin() {
            AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
                userService.register("hacker", "123456", Role.ADMIN);
            });
            assertTrue(exception.getMessage().contains("Không thể tự đăng ký tài khoản Admin"));
        }

        @Test
        @DisplayName("TC04: Đăng ký thất bại do username đã tồn tại")
        void testRegister_Fail_UsernameExists() throws SQLException {
            when(mockUserDAO.existsByUsername("player1")).thenReturn(true);

            assertThrows(AuthenticationException.class, () -> userService.register("player1", "123456", Role.BIDDER));
        }

        @Test
        @DisplayName("TC05: Admin tạo Admin mới thành công")
        void testCreateAdmin_Success() throws Exception {
            when(mockUserDAO.existsByUsername("admin2")).thenReturn(false);

            Admin newAdmin = userService.createAdmin(adminUser, "admin2", "123456");

            assertNotNull(newAdmin);
            assertTrue(newAdmin.hasRole(Role.ADMIN));
            verify(mockUserDAO, times(1)).save(any(Admin.class));
        }
    }

    // =========================================================================
    // 2. TEST ĐĂNG NHẬP (LOGIN)
    // =========================================================================
    @Nested
    @DisplayName("Tests Đăng nhập (login)")
    class LoginTests {

        @BeforeEach
        void setUpMockDB() throws SQLException {
            when(mockUserDAO.findByUsername("player1")).thenReturn(regularUser);
        }

        @Test
        @DisplayName("TC06: Đăng nhập thành công")
        void testLogin_Success() throws Exception {
            User loggedInUser = userService.login("player1", "password");
            assertEquals(2, loggedInUser.getUserId());
        }

        @Test
        @DisplayName("TC07: Ném AuthenticationException khi sai mật khẩu hoặc user không tồn tại")
        void testLogin_Fail_WrongCredentials() throws SQLException {
            // Sai pass
            assertThrows(AuthenticationException.class, () -> userService.login("player1", "wrongpass"));

            // Không tồn tại user
            when(mockUserDAO.findByUsername("ghost")).thenReturn(null);
            assertThrows(AuthenticationException.class, () -> userService.login("ghost", "123456"));
        }

        @Test
        @DisplayName("TC08: Ném lỗi khi đăng nhập vào tài khoản bị vô hiệu hóa (inactive)")
        void testLogin_Fail_InactiveUser() {
            regularUser.setActive(false);

            AuthenticationException exception = assertThrows(AuthenticationException.class, () -> userService.login("player1", "password"));
            assertTrue(exception.getMessage().contains("đã bị vô hiệu hóa"));
        }

        @Test
        @DisplayName("TC09: Ném UserBannedException khi đăng nhập vào tài khoản bị ban")
        void testLogin_Fail_BannedUser() {
            regularUser.setBanned(true);
            assertThrows(UserBannedException.class, () -> userService.login("player1", "password"));
        }
    }

    // =========================================================================
    // 3. TEST ADMIN QUẢN LÝ TÀI KHOẢN (BAN, UPDATE, DELETE)
    // =========================================================================
    @Nested
    @DisplayName("Tests Quyền Quản trị viên (Admin Management)")
    class AdminManagementTests {

        @BeforeEach
        void setUpMockDB() throws SQLException {
            when(mockUserDAO.findById(1)).thenReturn(adminUser);
            when(mockUserDAO.findById(2)).thenReturn(regularUser);
        }

        @Test
        @DisplayName("TC10: Ném AuthenticationException nếu User thường cố dùng chức năng của Admin")
        void testRequireAdmin_Fail_NotAdmin() {
            assertThrows(AuthenticationException.class, () -> userService.banUser(regularUser, 2));
            assertThrows(AuthenticationException.class, () -> userService.deleteUser(regularUser, 2));
        }

        @Test
        @DisplayName("TC11: Admin khóa (Ban) và mở khóa (Unban) user thường thành công")
        void testBanAndUnbanUser_Success() throws Exception {
            // Ban
            userService.banUser(adminUser, 2);
            assertFalse(regularUser.isActive()); // isActive sẽ thành false sau khi bị ban
            verify(mockUserDAO, times(1)).update(regularUser);

            // Unban
            userService.unbanUser(adminUser, 2);
            assertTrue(regularUser.isActive()); // Khôi phục lại trạng thái active
            verify(mockUserDAO, times(2)).update(regularUser);
        }

        @Test
        @DisplayName("TC12: Ném lỗi khi Admin cố gắng Ban hoặc Xóa một Admin khác")
        void testBanOrDelete_Fail_TargetIsAdmin() throws SQLException {
            Admin admin2 = new Admin(3, "admin2", "123", 0);
            when(mockUserDAO.findById(3)).thenReturn(admin2);

            assertThrows(AuthenticationException.class, () -> userService.banUser(adminUser, 3));
            assertThrows(AuthenticationException.class, () -> userService.deleteUser(adminUser, 3));
        }

        @Test
        @DisplayName("TC13: Admin không thể tự xóa chính mình")
        void testDeleteUser_Fail_SelfDelete() {
            AuthenticationException exception = assertThrows(AuthenticationException.class, () -> userService.deleteUser(adminUser, 1));
            assertTrue(exception.getMessage().contains("Không thể xóa chính mình"));
        }

        @Test
        @DisplayName("TC14: Admin cập nhật thông tin user thành công")
        void testUpdateUser_Success() throws Exception {
            // Đổi tên thành 'pro_player', đổi pass thành 'newpass', set số dư = 9999
            userService.updateUser(adminUser, 2, "pro_player", "newpass", 9999.0, null, null);

            assertEquals("pro_player", regularUser.getUsername());
            assertEquals(9999.0, regularUser.getBalance());

            // KIỂM TRA MẬT KHẨU ĐÃ ĐƯỢC MÃ HÓA CHUẨN XÁC CHƯA
            assertTrue(BCrypt.checkpw("newpass", regularUser.getPassword()));

            verify(mockUserDAO, times(1)).update(regularUser);
        }

        @Test
        @DisplayName("TC15: Admin lấy danh sách toàn bộ User thành công")
        void testGetAllUsers_Success() throws Exception {
            List<User> mockList = Arrays.asList(adminUser, regularUser);
            when(mockUserDAO.findAll()).thenReturn(mockList);

            List<User> users = userService.getAllUsers(adminUser);
            assertEquals(2, users.size());
        }
    }
}