package com.code.service;

import com.code.dao.ItemDAO;
import com.code.exception.AuthenticationException;
import com.code.exception.UserBannedException;
import com.code.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    private ItemDAO mockItemDAO;
    private ItemService itemService;

    private RegularUser seller;
    private RegularUser bidder;
    private Admin admin;
    private Electronics dummyItem;

    @BeforeEach
    void setUp() {
        mockItemDAO = Mockito.mock(ItemDAO.class);
        itemService = new ItemService(mockItemDAO);

        seller = new RegularUser(1, "seller", "pass", 0, Role.SELLER);
        bidder = new RegularUser(2, "bidder", "pass", 0, Role.BIDDER);
        admin = new Admin(99, "admin", "pass", 0);

        dummyItem = new Electronics(10, seller.getUserId(), "Laptop Dell", "Core i7", 15000000, "Dell", 12);
    }

    // =========================================================================
    // 1. TEST TẠO SẢN PHẨM (CREATE ITEM)
    // =========================================================================
    @Nested
    @DisplayName("Tests Tạo sản phẩm (createItem)")
    class CreateItemTests {

        @Test
        @DisplayName("Tạo sản phẩm thành công khi User là SELLER")
        void testCreateItem_Success_Seller() throws Exception {
            itemService.createItem(dummyItem, seller);
            verify(mockItemDAO, times(1)).save(dummyItem);
        }

        @Test
        @DisplayName("Tạo sản phẩm thành công khi User là ADMIN")
        void testCreateItem_Success_Admin() throws Exception {
            itemService.createItem(dummyItem, admin);
            verify(mockItemDAO, times(1)).save(dummyItem);
        }

        @Test
        @DisplayName("Ném lỗi IllegalArgumentException nếu item = null")
        void testCreateItem_NullItem() {
            assertThrows(IllegalArgumentException.class, () -> itemService.createItem(null, seller));
        }

        @Test
        @DisplayName("Ném lỗi AuthenticationException nếu user = null")
        void testCreateItem_NullUser() {
            assertThrows(AuthenticationException.class, () -> itemService.createItem(dummyItem, null));
        }

        @Test
        @DisplayName("Ném lỗi UserBannedException nếu user bị ban")
        void testCreateItem_BannedUser() {
            // Đã sửa: setActive(false)
            seller.setActive(false);
            assertThrows(UserBannedException.class, () -> itemService.createItem(dummyItem, seller));
        }

        @Test
        @DisplayName("Ném lỗi AuthenticationException nếu user không có quyền SELLER/ADMIN")
        void testCreateItem_InvalidRole() {
            assertThrows(AuthenticationException.class, () -> itemService.createItem(dummyItem, bidder));
        }

        @Test
        @DisplayName("Ném RuntimeException nếu Database lỗi (SQLException)")
        void testCreateItem_SQLException() throws Exception {
            doThrow(new SQLException("DB Error")).when(mockItemDAO).save(any(Item.class));
            assertThrows(RuntimeException.class, () -> itemService.createItem(dummyItem, seller));
        }
    }

    // =========================================================================
    // 2. TEST LẤY DANH SÁCH & CHI TIẾT SẢN PHẨM (GET ITEM)
    // =========================================================================
    @Nested
    @DisplayName("Tests Truy vấn sản phẩm (getItem, getAllItems)")
    class GetItemTests {

        @Test
        @DisplayName("getItem thành công")
        void testGetItem_Success() throws Exception {
            when(mockItemDAO.findById(10)).thenReturn(dummyItem);
            Item result = itemService.getItem(10);
            assertNotNull(result);
            assertEquals("Laptop Dell", result.getName());
        }

        @Test
        @DisplayName("getItem ném IllegalArgumentException khi không tìm thấy ID")
        void testGetItem_NotFound() throws Exception {
            when(mockItemDAO.findById(999)).thenReturn(null);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> itemService.getItem(999));
            assertTrue(exception.getMessage().contains("Không tìm thấy mặt hàng"));
        }

        @Test
        @DisplayName("getItem ném RuntimeException nếu có lỗi DB")
        void testGetItem_SQLException() throws Exception {
            when(mockItemDAO.findById(10)).thenThrow(new SQLException("DB Lỗi"));
            assertThrows(RuntimeException.class, () -> itemService.getItem(10));
        }

        @Test
        @DisplayName("getAllItems trả về danh sách chính xác")
        void testGetAllItems() throws Exception {
            List<Item> mockList = Arrays.asList(dummyItem, new Art(11, 1, "Tranh", "Sơn dầu", 50000));
            when(mockItemDAO.findAll()).thenReturn(mockList);

            List<Item> result = itemService.getAllItems();
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("getAllItems ném RuntimeException nếu có lỗi DB")
        void testGetAllItems_SQLException() throws Exception {
            when(mockItemDAO.findAll()).thenThrow(new SQLException("DB Lỗi"));
            assertThrows(RuntimeException.class, () -> itemService.getAllItems());
        }
    }

    // =========================================================================
    // 3. TEST CẬP NHẬT SẢN PHẨM (UPDATE ITEM)
    // =========================================================================
    @Nested
    @DisplayName("Tests Cập nhật sản phẩm (updateItem)")
    class UpdateItemTests {

        @BeforeEach
        void setupMockDB() throws Exception {
            when(mockItemDAO.findById(10)).thenReturn(dummyItem);
        }

        @Test
        @DisplayName("Chủ sở hữu update sản phẩm thành công")
        void testUpdateItem_Success_Owner() throws Exception {
            Item updateData = new Electronics(10, seller.getUserId(), "Laptop Mới", "Ngon hơn", 20000000);
            itemService.updateItem(updateData, seller);

            assertEquals("Laptop Mới", dummyItem.getName());
            assertEquals("Ngon hơn", dummyItem.getDescription());
            assertEquals(20000000, dummyItem.getStartingPrice());
            verify(mockItemDAO, times(1)).save(dummyItem);
        }

        @Test
        @DisplayName("Admin update sản phẩm của người khác thành công")
        void testUpdateItem_Success_Admin() throws Exception {
            Item updateData = new Electronics(10, admin.getUserId(), "Tên Bị Sửa Bởi Admin", "Ngon", 200);
            itemService.updateItem(updateData, admin);

            assertEquals("Tên Bị Sửa Bởi Admin", dummyItem.getName());
            verify(mockItemDAO, times(1)).save(dummyItem);
        }

        @Test
        @DisplayName("Ném lỗi AuthenticationException nếu user = null")
        void testUpdateItem_NullUser() {
            assertThrows(AuthenticationException.class, () -> itemService.updateItem(dummyItem, null));
        }

        @Test
        @DisplayName("Ném lỗi UserBannedException nếu user bị ban")
        void testUpdateItem_BannedUser() {
            // Đã sửa: setActive(false)
            seller.setActive(false);
            assertThrows(UserBannedException.class, () -> itemService.updateItem(dummyItem, seller));
        }

        @Test
        @DisplayName("Ném lỗi IllegalArgumentException nếu item không tồn tại")
        void testUpdateItem_ItemNotFound() throws Exception {
            when(mockItemDAO.findById(10)).thenReturn(null);
            assertThrows(IllegalArgumentException.class, () -> itemService.updateItem(dummyItem, seller));
        }

        @Test
        @DisplayName("Ném AuthenticationException khi user cố sửa sản phẩm của người khác")
        void testUpdateItem_Fail_NotOwner() {
            Item updateData = new Electronics(10, bidder.getUserId(), "Hack", "Hack", 0);
            AuthenticationException exception = assertThrows(AuthenticationException.class, () -> itemService.updateItem(updateData, bidder));
            assertTrue(exception.getMessage().contains("Không có quyền sửa item"));
        }

        @Test
        @DisplayName("Ném RuntimeException nếu có lỗi DB khi update")
        void testUpdateItem_SQLException() throws Exception {
            doThrow(new SQLException("Lỗi ghi DB")).when(mockItemDAO).save(any(Item.class));
            assertThrows(RuntimeException.class, () -> itemService.updateItem(dummyItem, seller));
        }
    }

    // =========================================================================
    // 4. TEST XÓA SẢN PHẨM (DELETE ITEM)
    // =========================================================================
    @Nested
    @DisplayName("Tests Xóa sản phẩm (deleteItem)")
    class DeleteItemTests {

        @BeforeEach
        void setupMockDB() throws Exception {
            when(mockItemDAO.findById(10)).thenReturn(dummyItem);
        }

        @Test
        @DisplayName("Chủ sở hữu xóa sản phẩm thành công")
        void testDeleteItem_Success_Owner() throws Exception {
            itemService.deleteItem(10, seller);
            verify(mockItemDAO, times(1)).delete(10);
        }

        @Test
        @DisplayName("Admin xóa sản phẩm của người khác thành công")
        void testDeleteItem_Success_Admin() throws Exception {
            itemService.deleteItem(10, admin);
            verify(mockItemDAO, times(1)).delete(10);
        }

        @Test
        @DisplayName("Ném lỗi AuthenticationException nếu user = null")
        void testDeleteItem_NullUser() {
            assertThrows(AuthenticationException.class, () -> itemService.deleteItem(10, null));
        }

        @Test
        @DisplayName("Ném lỗi UserBannedException nếu user bị ban")
        void testDeleteItem_BannedUser() {
            // Đã sửa: setActive(false)
            seller.setActive(false);
            assertThrows(UserBannedException.class, () -> itemService.deleteItem(10, seller));
        }

        @Test
        @DisplayName("Ném AuthenticationException khi user cố xóa sản phẩm của người khác")
        void testDeleteItem_Fail_NotOwner() {
            AuthenticationException exception = assertThrows(AuthenticationException.class, () -> itemService.deleteItem(10, bidder));
            assertTrue(exception.getMessage().contains("Không có quyền xóa item"));
        }

        @Test
        @DisplayName("Ném IllegalArgumentException khi item cần xóa không tồn tại")
        void testDeleteItem_Fail_NotFound() throws Exception {
            when(mockItemDAO.findById(999)).thenReturn(null);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> itemService.deleteItem(999, seller));
            assertTrue(exception.getMessage().contains("Item không tồn tại"));
        }

        @Test
        @DisplayName("Ném RuntimeException nếu có lỗi DB khi delete")
        void testDeleteItem_SQLException() throws Exception {
            doThrow(new SQLException("Lỗi xóa DB")).when(mockItemDAO).delete(10);
            assertThrows(RuntimeException.class, () -> itemService.deleteItem(10, seller));
        }
    }
}