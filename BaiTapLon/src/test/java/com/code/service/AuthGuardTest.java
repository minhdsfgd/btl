package com.code.service;

import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.Role;
import com.code.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AuthGuardTest {

    private User mockUser;

    @BeforeEach
    void setUp() {
        // Tạo một User giả bằng Mockito để không cần bận tâm đến class Admin hay RegularUser
        mockUser = Mockito.mock(User.class);

        // Mặc định cho User giả có tên là "testUser" để khi ném lỗi có message cho đẹp
        when(mockUser.getUsername()).thenReturn("testUser");
    }

    // =========================================================================
    // 1. TEST HÀM requireNotBanned
    // =========================================================================

    @Test
    @DisplayName("TC01: Ném NullPointerException khi user chưa đăng nhập (user = null)")
    void testRequireNotBanned_NullUser() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            AuthGuard.requireNotBanned(null);
        });
        assertTrue(exception.getMessage().contains("User chưa đăng nhập"));
    }

    @Test
    @DisplayName("TC02: Ném UserBannedException khi user đã bị ban")
    void testRequireNotBanned_BannedUser() {
        // Giả lập user này đã bị khóa
        when(mockUser.isBanned()).thenReturn(true);

        UserBannedException exception = assertThrows(UserBannedException.class, () -> {
            AuthGuard.requireNotBanned(mockUser);
        });
        // Lỗi ném ra phải chứa username của người bị ban
        assertTrue(exception.getMessage().contains("testUser"));
    }

    @Test
    @DisplayName("TC03: Không ném lỗi (vượt qua) khi user hợp lệ và không bị ban")
    void testRequireNotBanned_ValidUser() {
        when(mockUser.isBanned()).thenReturn(false);

        // assertDoesNotThrow đảm bảo khối lệnh chạy trơn tru mà không văng ra Exception nào
        assertDoesNotThrow(() -> {
            AuthGuard.requireNotBanned(mockUser);
        });
    }

    // =========================================================================
    // 2. TEST HÀM requireRole CHUNG
    // =========================================================================

    @Test
    @DisplayName("TC04: Hợp lệ (vượt qua) khi user có Role được yêu cầu")
    void testRequireRole_HasRole() {
        when(mockUser.isBanned()).thenReturn(false);
        when(mockUser.hasRole(Role.ADMIN)).thenReturn(true);

        assertDoesNotThrow(() -> {
            AuthGuard.requireRole(mockUser, Role.ADMIN);
        });
    }

    @Test
    @DisplayName("TC05: Ném AuthenticationException khi user thiếu Role yêu cầu")
    void testRequireRole_MissingRole() {
        when(mockUser.isBanned()).thenReturn(false);
        when(mockUser.hasRole(Role.ADMIN)).thenReturn(false); // Cố tình không cho quyền ADMIN

        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            AuthGuard.requireRole(mockUser, Role.ADMIN);
        });

        assertTrue(exception.getMessage().contains("Không có quyền thực hiện thao tác này"));
        assertTrue(exception.getMessage().contains("Cần quyền: ADMIN"));
    }

    // =========================================================================
    // 3. TEST CÁC HÀM TIỆN ÍCH (requireAdmin, requireBidder, requireSeller)
    // =========================================================================

    @Test
    @DisplayName("TC06: requireAdmin hoạt động đúng")
    void testRequireAdmin() {
        when(mockUser.isBanned()).thenReturn(false);

        when(mockUser.hasRole(Role.ADMIN)).thenReturn(true);
        assertDoesNotThrow(() -> AuthGuard.requireAdmin(mockUser));

        when(mockUser.hasRole(Role.ADMIN)).thenReturn(false);
        assertThrows(AuthenticationException.class, () -> AuthGuard.requireAdmin(mockUser));
    }

    @Test
    @DisplayName("TC07: requireBidder hoạt động đúng")
    void testRequireBidder() {
        when(mockUser.isBanned()).thenReturn(false);

        when(mockUser.hasRole(Role.BIDDER)).thenReturn(true);
        assertDoesNotThrow(() -> AuthGuard.requireBidder(mockUser));

        when(mockUser.hasRole(Role.BIDDER)).thenReturn(false);
        assertThrows(AuthenticationException.class, () -> AuthGuard.requireBidder(mockUser));
    }

    @Test
    @DisplayName("TC08: requireSeller hoạt động đúng")
    void testRequireSeller() {
        when(mockUser.isBanned()).thenReturn(false);

        when(mockUser.hasRole(Role.SELLER)).thenReturn(true);
        assertDoesNotThrow(() -> AuthGuard.requireSeller(mockUser));

        when(mockUser.hasRole(Role.SELLER)).thenReturn(false);
        assertThrows(AuthenticationException.class, () -> AuthGuard.requireSeller(mockUser));
    }
}