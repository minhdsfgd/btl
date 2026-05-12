package com.code.service;

import com.code.dao.UserDAO;
import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.RegularUser;
import com.code.models.*;
import com.code.models.Role;
import com.code.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
//một đối tượng duy nhất dùng chung cho cả server
    private   FakeUserDAO fakeUserDAO;
    private UserService userService;

    private RegularUser activeUser;
    private RegularUser bannedUser;
    private Admin adminUser;

    @BeforeEach
    void setUp() {

        fakeUserDAO = new FakeUserDAO();
        userService = new UserService(fakeUserDAO);

        //user bifnh thuwfong
        activeUser = new RegularUser(1, "quynhnhu", "password123", 500000.0, Role.BIDDER);
        activeUser.setActive(true);
        activeUser.setBanned(false);
        fakeUserDAO.saveInMemory(activeUser); // lưu vào cùng 1 đối tợng

        // user bị ban
        RegularUser bannedUser = new RegularUser(2, "bad_user", "123456", 0.0, Role.BIDDER);
        bannedUser.setActive(true);
        bannedUser.setBanned(true); //  khóa tài khoản này
        fakeUserDAO.saveInMemory(activeUser);

        // admin
        adminUser = new Admin(3, "admin", "adminpass", 0.0);
        adminUser.setActive(true);
        adminUser.setBanned(false);
        fakeUserDAO.saveInMemory(adminUser);

    }

    //test logịn

    @Test
    @DisplayName("TC01: Đăng nhập thành công với tài khoản đang hoạt động")
    void testLogin_Success() throws Exception {
        User result = userService.login("quynhnhu", "password123");

        assertNotNull(result, "Đăng nhập thành công phải trả về User");
        assertEquals("quynhnhu", result.getUsername(), "Username trả về phải khớp");
        assertTrue(result.hasRole(Role.BIDDER), "User phải giữ nguyên role BIDDER");
    }

    @Test
    @DisplayName("TC02: Đăng nhập thành công với tài khoản Admin")
    void testLogin_Admin_Success() throws Exception {
        User result = userService.login("admin", "adminpass");

        assertNotNull(result);
        assertTrue(result.hasRole(Role.ADMIN), "Admin phải có role ADMIN sau khi đăng nhập");
    }


    @Test
    @DisplayName("TC03: Đăng nhập thất bại do nhập sai mật khẩu")
    void testLogin_WrongPassword_ThrowsAuthException() {
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> userService.login("quynhnhu", "sai_mat_khau"));

        assertEquals("Sai tên đăng nhập hoặc mật khẩu.", ex.getMessage(),
                "Message phải đúng (tránh lộ thông tin)");
    }

    @Test
    @DisplayName("TC04: Đăng nhập thất bại do tài khoản không tồn tại")
    void testLogin_UserNotFound_ThrowsAuthException() {
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> userService.login("ghost_user", "123456"));

        assertEquals("Sai tên đăng nhập hoặc mật khẩu.", ex.getMessage(),
                "Message sai username và sai password phải giống nhau (chống timing attack)");
    }

    @Test
    @DisplayName("TC05: Đăng nhập thất bại do tài khoản đã bị Admin khóa (Banned)")
    void testLogin_BannedUser_ThrowsUserBannedException() {
        assertThrows(UserBannedException.class,
                () -> userService.login("bad_user", "123456"),
                "User bị ban phải ném UserBannedException dù mật khẩu đúng");
    }

    @Test
    @DisplayName("TC6: Tài khoản không active → ném AuthenticationException")
    void testLogin_InactiveUser_ThrowsAuthException() {
        RegularUser inactiveUser = new RegularUser(4, "inactive_guy", "pass123", 0, Role.BIDDER);
        inactiveUser.setActive(false);  // ← bị vô hiệu hoá
        inactiveUser.setBanned(false);
        fakeUserDAO.saveInMemory(inactiveUser);

        assertThrows(AuthenticationException.class,
                () -> userService.login("inactive_guy", "pass123"),
                "Tài khoản inactive phải ném AuthenticationException");
    }



    // test register

    @Test
    @DisplayName("TC07: Đăng ký thành công với role BIDDER")
    void testRegister_Bidder_Success() throws Exception {
        RegularUser newUser = userService.register("new_bidder", "matkhau123", Role.BIDDER);

        assertNotNull(newUser, "register phải trả về User vừa tạo");
        assertEquals("new_bidder", newUser.getUsername());
        assertTrue(newUser.hasRole(Role.BIDDER), "User mới phải có role BIDDER");
        assertTrue(fakeUserDAO.existsByUsername("new_bidder"),
                "User mới phải được lưu vào 'DB'");
        assertEquals(4, fakeUserDAO.findAll().size(),
                "Tổng số user phải tăng lên 4 (3 seed + 1 mới)");

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
    @DisplayName("TC08: Đăng ký thành công với role SELLER")
    void testRegister_Seller_Success() throws Exception {
        RegularUser newUser = userService.register("new_seller", "matkhau123", Role.SELLER);

        assertNotNull(newUser);
        assertTrue(newUser.hasRole(Role.SELLER), "User mới phải có role SELLER");
        assertTrue(fakeUserDAO.existsByUsername("new_seller"));
    }

    @Test
    @DisplayName("TC09: Username đã tồn tại → ném AuthenticationException")
    void testRegister_DuplicateUsername_ThrowsAuthException() {
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> userService.register("quynhnhu", "newpassword", Role.BIDDER));

        assertEquals("Username 'quynhnhu' đã tồn tại.", ex.getMessage());
    }


    @Test
    @DisplayName("TC010: Đăng ký thất bại do Mật khẩu quá ngắn ( <6 kí tự) ")
    void testRegister_ShortPassword_ThrowsIllegalArg() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register("test_user", "123", Role.BIDDER));
        assertEquals("Password phải ít nhất 6 ký tự.", ex.getMessage());
    }

    @Test
    @DisplayName("TC11: Username null hoặc rỗng → ném IllegalArgumentException")
    void testRegister_BlankUsername_ThrowsIllegalArg() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.register("", "matkhau123", Role.BIDDER),
                "Username rỗng phải ném IllegalArgumentException");

        assertThrows(IllegalArgumentException.class,
                () -> userService.register(null, "matkhau123", Role.BIDDER),
                "Username null phải ném IllegalArgumentException");
    }

    @Test
    @DisplayName("TC12: Tự đăng ký với role ADMIN bị cấm → ném AuthenticationException")
    void testRegister_AdminRole_Forbidden() {
        assertThrows(AuthenticationException.class,
                () -> userService.register("hacker", "matkhau123", Role.ADMIN),
                "Không được tự đăng ký thành Admin");
    }

    @Test
    @DisplayName("TC13: Đăng ký không làm thay đổi số lượng user khác")
    void testRegister_DoesNotAffectExistingUsers() throws Exception {
        int before = fakeUserDAO.findAll().size(); // 3 user seed

        userService.register("totally_new", "matkhau123", Role.BIDDER);

        assertEquals(before + 1, fakeUserDAO.findAll().size(),
                "Chỉ thêm đúng 1 user, không xoá hay thay đổi user cũ");
        // User cũ vẫn còn
        assertNotNull(fakeUserDAO.findByUsername("quynhnhu"), "quynhnhu phải còn nguyên");
        assertNotNull(fakeUserDAO.findByUsername("bad_user"),  "bad_user phải còn nguyên");
        assertNotNull(fakeUserDAO.findByUsername("admin"),     "admin phải còn nguyên");
    }



    //Test quản lý người dùng

    @Test
    @DisplayName("TC14: Admin ban thành công một user bình thường")
    void testBanUser_AdminBansUser_Success() throws Exception {
        assertFalse(activeUser.isBanned(), "Trước khi ban: user phải chưa bị ban");

        userService.banUser(adminUser, activeUser.getUserId());

        assertTrue(fakeUserDAO.findById(activeUser.getUserId()).isBanned(),
                "Sau khi ban: user phải ở trạng thái banned trong 'DB'");
    }

    @Test
    @DisplayName("TC15: Người không phải Admin không được ban user → ném AuthenticationException")
    void testBanUser_NonAdmin_ThrowsAuthException() {
        assertThrows(AuthenticationException.class,
                () -> userService.banUser(activeUser, bannedUser.getUserId()),
                "Người không phải Admin phải ném AuthenticationException");
    }

    @Test
    @DisplayName("TC16: Admin không được ban Admin khác → ném AuthenticationException")
    void testBanUser_CannotBanAdmin_ThrowsAuthException() {
        Admin anotherAdmin = new Admin(5, "admin2", "pass", 0.0);
        anotherAdmin.setActive(true);
        anotherAdmin.setBanned(false);
        fakeUserDAO.saveInMemory(anotherAdmin);

        assertThrows(AuthenticationException.class,
                () -> userService.banUser(adminUser, anotherAdmin.getUserId()),
                "Admin không được ban Admin khác");
    }

    @Test
    @DisplayName("TC17: Admin gỡ ban thành công")
    void testUnbanUser_Success() throws Exception {
        assertTrue(bannedUser.isBanned(), "Trước khi unban: user phải đang bị ban");

        userService.unbanUser(adminUser, bannedUser.getUserId());

        assertFalse(fakeUserDAO.findById(bannedUser.getUserId()).isBanned(),
                "Sau khi unban: user không còn bị ban");
    }

    @Test
    @DisplayName("TC18: Người không phải Admin không được unban ")
    void testUnbanUser_NonAdmin_ThrowsAuthException() {
        assertThrows(AuthenticationException.class,
                () -> userService.unbanUser(activeUser, bannedUser.getUserId()),
                "Người không phải Admin phải ném AuthenticationException");
    }

    @Test
    @DisplayName("TC19: Admin thêm role SELLER cho Bidder thành công")
    void testAddRole_Success() throws Exception {
        assertFalse(activeUser.hasRole(Role.SELLER), "Ban đầu user chưa có role SELLER");

        userService.addRole(adminUser, activeUser.getUserId(), Role.SELLER);

        assertTrue(activeUser.hasRole(Role.SELLER),
                "Sau khi addRole: user phải có thêm role SELLER");
        assertTrue(activeUser.hasRole(Role.BIDDER),
                "Role BIDDER cũ vẫn phải còn");
    }

    @Test
    @DisplayName("TC20: Admin không được addRole ADMIN (phải dùng createAdmin) ")
    void testAddRole_AdminRole_Forbidden() {
        assertThrows(AuthenticationException.class,
                () -> userService.addRole(adminUser, activeUser.getUserId(), Role.ADMIN),
                "Không được addRole ADMIN trực tiếp");
    }

    @Test
    @DisplayName("TC21: Admin xoá role SELLER thành công")
    void testRemoveRole_Success() throws Exception {
        activeUser.addRole(Role.SELLER); // chuẩn bị: thêm role trước

        userService.removeRole(adminUser, activeUser.getUserId(), Role.SELLER);

        assertFalse(activeUser.hasRole(Role.SELLER),
                "Sau khi removeRole: SELLER phải bị xoá");
        assertTrue(activeUser.hasRole(Role.BIDDER),
                "BIDDER ban đầu vẫn phải còn");
    }

    @Test
    @DisplayName("TC22: Admin xem danh sách tất cả user thành công")
    void testGetAllUsers_Admin_Success() throws Exception {
        List<User> users = userService.getAllUsers(adminUser);

        assertNotNull(users);
        assertEquals(3, users.size(), "Phải trả về đúng 3 user đã seed");
    }

    @Test
    @DisplayName("TC23: Người không phải Admin không được xem danh sách user")
    void testGetAllUsers_NonAdmin_ThrowsAuthException() {
        assertThrows(AuthenticationException.class,
                () -> userService.getAllUsers(activeUser),
                "Chỉ Admin mới được xem danh sách tất cả user");
    }

    // test tạo admin

    @Test
    @DisplayName("TC24: Admin tạo Admin mới thành công")
    void testCreateAdmin_Success() throws Exception {
        Admin newAdmin = userService.createAdmin(adminUser, "admin2", "securepass");

        assertNotNull(newAdmin);
        assertEquals("admin2", newAdmin.getUsername());
        assertTrue(newAdmin.hasRole(Role.ADMIN));
        assertTrue(fakeUserDAO.existsByUsername("admin2"),
                "Admin mới phải được lưu vào 'DB'");
    }

    @Test
    @DisplayName("TC25: Người không phải Admin không được tạo Admin → ném AuthenticationException")
    void testCreateAdmin_NonAdmin_ThrowsAuthException() {
        assertThrows(AuthenticationException.class,
                () -> userService.createAdmin(activeUser, "newadmin", "pass123"),
                "Chỉ Admin mới được tạo Admin");
    }

    @Test
    @DisplayName("TC26: Tạo Admin với username đã tồn tại → ném AuthenticationException")
    void testCreateAdmin_DuplicateUsername_ThrowsAuthException() {
        assertThrows(AuthenticationException.class,
                () -> userService.createAdmin(adminUser, "quynhnhu", "pass123"),
                "Username đã tồn tại không được tạo Admin mới");
    }



    //FAKER USE DAO - không connect my sql
    static class FakeUserDAO extends UserDAO {

        // ưsername => User( search theo username)
        private final Map<String, User> byUsername = new LinkedHashMap<>();

        // userId => search theo id
        private final Map<Integer, User> byId = new LinkedHashMap<>();

        // tự tăng id cho người tiếp theo dki
        private int nextId = 10;


        // ọgi từ test để seed dữ liệu ban đầu

        // lưu tt vào memory_ dùng trong setUp(0 để tạo dữ liệu bđ
        void saveInMemory(User user) {
            byUsername.put(user.getUsername(), user);
            byId.put(user.getUserId(), user);
        }

        //override all method của UserDAO-không gọi database
        @Override
        public User findByUsername(String username) throws SQLException {
            return byUsername.get(username);
        }

        @Override
        public boolean existsByUsername(String username) throws SQLException {
            return byUsername.containsKey(username);
        }

        @Override
        public User findById(int id) throws SQLException {
            return byId.get(id);
        }

        @Override
        public List<User> findAll() throws SQLException {
            return new ArrayList<>(byUsername.values());
        }

        // UserService gọi method này khi dki/ tạo admin
        // tự gán id nếu id ban đầu = 0
        @Override
        public void save(User user) throws SQLException {
            if (user.getUserId() == 0) {
                user.setUserId(nextId++);
            }
            byUsername.put(user.getUsername(), user);
            byId.put(user.getUserId(), user);

            // cập nhật cả 2 map nhưng ghi lại để đảm bảo đồng bộ nếu id thay đổi
            byUsername.put(user.getUsername(), user);
            byId.put(user.getUserId(), user);
        }

        @Override
        public void delete(int userId) throws SQLException {
            User u = byId.remove(userId);
            if (u != null) byUsername.remove(u.getUsername());
        }
    }
    }



