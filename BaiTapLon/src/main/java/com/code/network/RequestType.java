package com.code.network;

/**
 * Tất cả loại request Client có thể gửi lên Server.
 *
 * <p>Nhóm theo chức năng:</p>
 * <ul>
 *   <li>AUTH — đăng ký, đăng nhập, đăng xuất</li>
 *   <li>USER — quản lý tài khoản (Admin)</li>
 *   <li>ITEM — quản lý sản phẩm (Seller)</li>
 *   <li>AUCTION — quản lý phiên đấu giá</li>
 *   <li>BID — đặt giá</li>
 *   <li>TRANSACTION — giao dịch tài chính</li>
 * </ul>
 */
public enum RequestType {

    // ── User Info ─────────────────────────────────────────────────────────────
    /** Lấy thông tin user hiện tại (refresh balance) — data: null */
    GET_MY_INFO,

    // ── Auth ──────────────────────────────────────────────────────────────────
    /** Đăng ký tài khoản mới — data: {@code LoginData(username, password, role)} */
    REGISTER,

    /** Đăng nhập — data: {@code LoginData(username, password)} */
    LOGIN,

    /** Đăng xuất — data: null */
    LOGOUT,

    // ── User (Admin) ──────────────────────────────────────────────────────────
    /** Admin lấy danh sách tất cả user — data: null */
    GET_ALL_USERS,

    /** Admin ban tài khoản — data: {@code Integer userId} */
    BAN_USER,

    /** Admin gỡ ban — data: {@code Integer userId} */
    UNBAN_USER,

    /** Admin thêm role — data: {@code RoleChangeData(userId, role)} */
    ADD_ROLE,

    /** Admin xóa role — data: {@code RoleChangeData(userId, role)} */
    REMOVE_ROLE,

    /** Admin tạo tài khoản Admin mới — data: {@code LoginData(username, password)} */
    CREATE_ADMIN,

    /** Admin sửa thông tin user — data: {@code UpdateUserData} */
    UPDATE_USER,

    /** Admin xóa user — data: {@code Integer userId} */
    DELETE_USER,

    // ── Balance ───────────────────────────────────────────────────────────────
    /** Nạp tiền vào tài khoản — data: {@code Double amount} */
    DEPOSIT,

    // ── Item (Seller) ─────────────────────────────────────────────────────────
    /** Seller lấy sản phẩm của mình — data: null */
    GET_MY_ITEMS,

    /** Admin lấy tất cả sản phẩm — data: null */
    GET_ALL_ITEMS,

    /** Seller tạo sản phẩm mới — data: {@code Item} (Electronics/Art/Vehicle) */
    CREATE_ITEM,

    /** Seller sửa sản phẩm — data: {@code Item} */
    UPDATE_ITEM,

    /** Seller/Admin xóa sản phẩm — data: {@code Integer itemId} */
    DELETE_ITEM,

    // ── Auction ───────────────────────────────────────────────────────────────
    /** Bidder lấy danh sách phiên đang hoạt động (OPEN+RUNNING) — data: null */
    GET_ACTIVE_AUCTIONS,

    /** Admin lấy tất cả phiên — data: null */
    GET_ALL_AUCTIONS,

    /** Seller lấy phiên của mình — data: null */
    GET_MY_AUCTIONS,

    /** Lấy chi tiết một phiên — data: {@code Integer auctionId} */
    GET_AUCTION_DETAIL,

    /** Seller tạo phiên đấu giá mới — data: {@code CreateAuctionData} */
    CREATE_AUCTION,

    /** Seller/Admin bắt đầu phiên sớm — data: {@code Integer auctionId} */
    START_AUCTION,

    /** Seller/Admin hủy phiên — data: {@code Integer auctionId} */
    CANCEL_AUCTION,

    /** Admin ban phiên vi phạm — data: {@code Integer auctionId} */
    BAN_AUCTION,

    /** Admin xác nhận thanh toán (nếu dùng cách thủ công) — data: {@code Integer auctionId} */
    MARK_AS_PAID,

    // ── Bid ───────────────────────────────────────────────────────────────────
    /** Bidder đặt giá — data: {@code PlaceBidData(auctionId, amount)} */
    PLACE_BID,

    /** Lấy lịch sử bid của một phiên — data: {@code Integer auctionId} */
    GET_BIDS_BY_AUCTION,

    /** Bidder xem lịch sử bid của mình — data: null */
    GET_MY_BIDS,

    // ── Transaction ───────────────────────────────────────────────────────────
    /** Admin xem tất cả giao dịch — data: null */
    GET_ALL_TRANSACTIONS,

    /** User xem giao dịch của mình — data: null */
    GET_MY_TRANSACTIONS,
    // ── Watching / Observer realtime ─────────────────────────────────────────
    /** Theo dõi realtime một phiên đấu giá — data: {@code Integer auctionId} */
    WATCH_AUCTION,

    /** Dừng theo dõi phiên đấu giá hiện tại — data: null */
    UNWATCH_AUCTION,




}