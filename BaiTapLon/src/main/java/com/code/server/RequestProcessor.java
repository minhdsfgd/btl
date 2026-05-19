package com.code.server;

import com.code.dao.*;
import com.code.exception.*;
import com.code.models.*;
import com.code.network.*;
import com.code.service.*;

import java.util.List;

/**
 * Bộ định tuyến request — nhận Request, gọi Service phù hợp, trả Response.
 *
 * <p><b>Vai trò:</b> Là trung gian giữa ClientHandler (mạng) và Services (nghiệp vụ).
 * ClientHandler không biết gọi Service nào — RequestProcessor biết tất cả.</p>
 *
 * <p><b>Xử lý lỗi:</b> Mọi exception từ Service đều được bắt và chuyển thành
 * {@code Response.fail(message)} — client KHÔNG bao giờ nhận exception thô.</p>
 */
public class RequestProcessor {

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final UserService        userService;
    private final BidService         bidService;
    private final AuctionService     auctionService;
    private final ItemService        itemService;
    private final TransactionService txService;

    // DAO dùng trực tiếp khi chỉ cần truy vấn, không cần logic nghiệp vụ
    private final UserDAO        userDAO;
    private final ItemDAO        itemDAO;
    private final BidDAO         bidDAO;
    private final TransactionDAO txDAO;

    public RequestProcessor(UserService userService, BidService bidService,
                            AuctionService auctionService, ItemService itemService,
                            TransactionService txService,
                            UserDAO userDAO, ItemDAO itemDAO,
                            BidDAO bidDAO, TransactionDAO txDAO) {
        this.userService    = userService;
        this.bidService     = bidService;
        this.auctionService = auctionService;
        this.itemService    = itemService;
        this.txService      = txService;
        this.userDAO        = userDAO;
        this.itemDAO        = itemDAO;
        this.bidDAO         = bidDAO;
        this.txDAO          = txDAO;
    }

    // ── Điểm vào chính ───────────────────────────────────────────────────────

    /**
     * Xử lý request và trả về Response.
     *
     * @param request  request từ client
     * @param handler  ClientHandler của client này (để đọc/ghi currentUser, watching)
     */
    public Response process(Request request, ClientHandler handler) {
        try {
            return switch (request.getType()) {

                // ── Auth — KHÔNG cần đăng nhập ────────────────────────────────
                case LOGIN    -> handleLogin(request, handler);
                case REGISTER -> handleRegister(request);

                // ── Auth — cần đăng nhập ──────────────────────────────────────
                case LOGOUT   -> handleLogout(handler);
                case CREATE_ADMIN -> handleCreateAdmin(request, handler);

                // ── User (Admin) ───────────────────────────────────────────────
                case GET_ALL_USERS -> handleGetAllUsers(handler);
                case BAN_USER      -> handleBanUser(request, handler);
                case UNBAN_USER    -> handleUnbanUser(request, handler);
                case ADD_ROLE      -> handleAddRole(request, handler);
                case REMOVE_ROLE   -> handleRemoveRole(request, handler);
                case UPDATE_USER   -> handleUpdateUser(request, handler);
                case DELETE_USER   -> handleDeleteUser(request, handler);

                // ── Balance ────────────────────────────────────────────────────
                case DEPOSIT -> handleDeposit(request, handler);
                case GET_MY_INFO -> handleGetMyInfo(handler);

                // ── Item ───────────────────────────────────────────────────────
                case GET_MY_ITEMS  -> handleGetMyItems(handler);
                case GET_ALL_ITEMS -> handleGetAllItems(handler);
                case CREATE_ITEM   -> handleCreateItem(request, handler);
                case UPDATE_ITEM   -> handleUpdateItem(request, handler);
                case DELETE_ITEM   -> handleDeleteItem(request, handler);

                // ── Auction ────────────────────────────────────────────────────
                case GET_ACTIVE_AUCTIONS -> handleGetActiveAuctions();
                case GET_ALL_AUCTIONS    -> handleGetAllAuctions(handler);
                case GET_MY_AUCTIONS     -> handleGetMyAuctions(handler);
                case GET_AUCTION_DETAIL  -> handleGetAuctionDetail(request);
                case CREATE_AUCTION      -> handleCreateAuction(request, handler);
                case START_AUCTION       -> handleStartAuction(request, handler);
                case CANCEL_AUCTION      -> handleCancelAuction(request, handler);
                case BAN_AUCTION         -> handleBanAuction(request, handler);
                case MARK_AS_PAID        -> handleMarkAsPaid(request, handler);

                // ── Watching (realtime Observer) ───────────────────────────────
                case WATCH_AUCTION   -> handleWatchAuction(request, handler);
                case UNWATCH_AUCTION -> handleUnwatchAuction(handler);

                // ── Bid ────────────────────────────────────────────────────────
                case PLACE_BID           -> handlePlaceBid(request, handler);
                case GET_BIDS_BY_AUCTION -> handleGetBidsByAuction(request);
                case GET_MY_BIDS         -> handleGetMyBids(handler);

                // ── Transaction ────────────────────────────────────────────────
                case GET_ALL_TRANSACTIONS -> handleGetAllTransactions(handler);
                case GET_MY_TRANSACTIONS -> handleGetMyTransactions(handler);



            };

        } catch (Exception e) {
            System.err.println("[Processor] Lỗi xử lý " + request.getType()
                    + ": " + e.getMessage());
            return Response.error(e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  HELPER — dùng chung trong tất cả handler
    // ────────────────────────────────────────────────────────────────────────

    /**
     * FIX #5: Kiểm tra user đã đăng nhập chưa.
     * Trả về Response.fail nếu chưa, null nếu đã đăng nhập.
     * Gọi đầu tiên trong mọi handler cần xác thực.
     *
     * <pre>
     * Response check = requireLogin(handler);
     * if (check != null) return check;
     * </pre>
     */
    private Response requireLogin(ClientHandler handler) {
        if (handler.currentUser == null)
            return Response.fail("Bạn chưa đăng nhập. Vui lòng đăng nhập trước.");
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  AUTH HANDLERS
    // ════════════════════════════════════════════════════════════════════════

    // LOGIN và REGISTER không cần requireLogin()

    private Response handleLogin(Request req, ClientHandler handler) {
        try {
            LoginData data = req.getDataAs(LoginData.class);
            User user = userService.login(data.username, data.password);
            handler.currentUser = user;
            System.out.println("[Auth] Đăng nhập: " + user.getUsername()
                    + " roles=" + user.getRoles());
            return Response.ok("Đăng nhập thành công!", user);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleRegister(Request req) {
        try {
            LoginData data = req.getDataAs(LoginData.class);
            RegularUser user = userService.register(
                    data.username, data.password, data.primaryRole);
            return Response.ok("Đăng ký thành công!", user);
        } catch (AuthenticationException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleLogout(ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        handler.stopWatching();
        String username = handler.currentUser.getUsername();
        handler.currentUser = null;
        System.out.println("[Auth] Đăng xuất: " + username);
        return Response.ok("Đăng xuất thành công.");
    }

    private Response handleCreateAdmin(Request req, ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            LoginData data = req.getDataAs(LoginData.class);
            Admin admin = userService.createAdmin(
                    handler.currentUser, data.username, data.password);
            return Response.ok("Tạo Admin thành công!", admin);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  USER HANDLERS (Admin)
    // ════════════════════════════════════════════════════════════════════════

    private Response handleGetAllUsers(ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            List<User> users = userService.getAllUsers(handler.currentUser);
            return Response.ok("OK", users);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleBanUser(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            int targetId = req.getDataAs(Integer.class);
            userService.banUser(handler.currentUser, targetId);
            return Response.ok("Đã ban user #" + targetId);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleUnbanUser(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            int targetId = req.getDataAs(Integer.class);
            userService.unbanUser(handler.currentUser, targetId);
            return Response.ok("Đã gỡ ban user #" + targetId);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleAddRole(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            RoleChangeData data = req.getDataAs(RoleChangeData.class);
            userService.addRole(handler.currentUser, data.userId, data.role);
            return Response.ok("Đã thêm quyền " + data.role
                    + " cho user #" + data.userId);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleRemoveRole(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            RoleChangeData data = req.getDataAs(RoleChangeData.class);
            userService.removeRole(handler.currentUser, data.userId, data.role);
            return Response.ok("Đã xóa quyền " + data.role
                    + " của user #" + data.userId);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleUpdateUser(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            com.code.network.UpdateUserData data = req.getDataAs(com.code.network.UpdateUserData.class);
            userService.updateUser(handler.currentUser, data.userId, 
                    data.username, data.password, data.balance, data.active, data.roles);
            return Response.ok("Đã cập nhật user #" + data.userId);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleDeleteUser(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            int userId = req.getDataAs(Integer.class);
            userService.deleteUser(handler.currentUser, userId);
            return Response.ok("Đã xóa user #" + userId);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BALANCE
    // ════════════════════════════════════════════════════════════════════════

    private Response handleDeposit(Request req, ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            double amount = req.getDataAs(Double.class);

            // FIX #1: bidService.deposit() cập nhật balance + tạo Transaction
            //         txService.save(tx) lưu Transaction vào DB — đã thêm method save()
            Transaction tx = bidService.deposit(handler.currentUser, amount);
            txService.save(tx);

            // Cập nhật balance mới vào DB
            userDAO.update(handler.currentUser);

            return Response.ok(
                    String.format("Nạp %,.0f VNĐ thành công! Số dư: %,.0f VNĐ",
                            amount, handler.currentUser.getBalance()),
                    handler.currentUser);
        } catch (UserBannedException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Lỗi nạp tiền: " + e.getMessage());
        }
    }

    private Response handleGetMyInfo(ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            // Luôn lấy bản mới nhất từ DB để client có balance chính xác
            User freshUser = userDAO.findById(handler.currentUser.getUserId());
            if (freshUser == null) return Response.fail("Không tìm thấy user.");
            handler.currentUser = freshUser;
            return Response.ok("OK", freshUser);
        } catch (Exception e) {
            return Response.fail("Lỗi lấy thông tin user: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ITEM HANDLERS
    // ════════════════════════════════════════════════════════════════════════

    private Response handleGetMyItems(ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            List<Item> items = itemDAO.findBySellerId(handler.currentUser.getUserId());
            return Response.ok("OK", items);
        } catch (Exception e) {
            return Response.fail("Lỗi lấy danh sách sản phẩm: " + e.getMessage());
        }
    }

    private Response handleGetAllItems(ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            AuthGuard.requireAdmin(handler.currentUser);
            List<Item> items = itemDAO.findAll();
            return Response.ok("OK", items);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }

    private Response handleCreateItem(Request req, ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            Item item = req.getDataAs(Item.class);
            // FIX #2: ItemService.createItem() chỉ nhận 1 tham số Item
            //         truyền thêm currentUser để validate quyền SELLER bên trong
            itemService.createItem(item, handler.currentUser);
            return Response.ok("Tạo sản phẩm thành công!", item);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Lỗi tạo sản phẩm: " + e.getMessage());
        }
    }

    private Response handleUpdateItem(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            Item item = req.getDataAs(Item.class);
            itemService.updateItem(item, handler.currentUser);
            return Response.ok("Cập nhật sản phẩm thành công!");
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Lỗi cập nhật: " + e.getMessage());
        }
    }

    private Response handleDeleteItem(Request req, ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            int itemId = req.getDataAs(Integer.class);
            // FIX #3: ItemService.deleteItem() không tồn tại → đã thêm vào ItemService
            itemService.deleteItem(itemId, handler.currentUser);
            return Response.ok("Đã xóa sản phẩm #" + itemId);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Lỗi xóa: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  AUCTION HANDLERS
    // ════════════════════════════════════════════════════════════════════════

    // GET_ACTIVE_AUCTIONS: không cần đăng nhập (ai cũng xem được)
    private Response handleGetActiveAuctions() {
        try {
            List<Auction> auctions = auctionService.getActiveAuctions();
            return Response.ok("OK", auctions);
        } catch (Exception e) {
            return Response.fail("Lỗi lấy danh sách phiên: " + e.getMessage());
        }
    }

    private Response handleGetAllAuctions(ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            AuthGuard.requireAdmin(handler.currentUser);
            List<Auction> auctions = auctionService.getAllAuctions();
            return Response.ok("OK", auctions);
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }

    private Response handleGetMyAuctions(ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            List<Auction> auctions = auctionService.getAuctionsBySeller(
                    handler.currentUser.getUserId());
            return Response.ok("OK", auctions);
        } catch (Exception e) {
            return Response.fail("Lỗi lấy phiên của bạn: " + e.getMessage());
        }
    }

    // GET_AUCTION_DETAIL: không cần đăng nhập
    private Response handleGetAuctionDetail(Request req) {
        try {
            int auctionId = req.getDataAs(Integer.class);
            Auction auction = auctionService.getAuction(auctionId);
            return Response.ok("OK", auction);
        } catch (AuctionClosedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleCreateAuction(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            CreateAuctionData data = req.getDataAs(CreateAuctionData.class);
            Item item = itemDAO.findById(data.itemId);
            if (item == null)
                return Response.fail("Không tìm thấy sản phẩm #" + data.itemId);

            Auction auction = auctionService.createAuction(
                    item, handler.currentUser,
                    data.bidIncrement, data.startTime, data.endTime);
            return Response.ok("Tạo phiên đấu giá thành công!", auction);
        } catch (UserBannedException | AuctionClosedException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Lỗi tạo phiên: " + e.getMessage());
        }
    }

    private Response handleStartAuction(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            int auctionId = req.getDataAs(Integer.class);
            auctionService.startAuction(auctionId, handler.currentUser);
            return Response.ok("Phiên #" + auctionId + " đã bắt đầu!");
        } catch (UserBannedException | AuctionClosedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleCancelAuction(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            int auctionId = req.getDataAs(Integer.class);
            auctionService.cancelAuction(auctionId, handler.currentUser);
            return Response.ok("Đã hủy phiên #" + auctionId);
        } catch (UserBannedException | AuctionClosedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleBanAuction(Request req, ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            int auctionId = req.getDataAs(Integer.class);
            auctionService.banAuction(auctionId, handler.currentUser);
            return Response.ok("Đã ban phiên #" + auctionId);
        } catch (UserBannedException | AuctionClosedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleMarkAsPaid(Request req, ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            // FIX #4: AuctionService.markAsPaid() chỉ nhận 1 tham số (int auctionId)
            //         kiểm tra quyền Admin ở đây trước khi gọi
            AuthGuard.requireAdmin(handler.currentUser);
            int auctionId = req.getDataAs(Integer.class);
            auctionService.markAsPaid(auctionId);
            return Response.ok("Phiên #" + auctionId + " đã được xác nhận thanh toán.");
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        } catch (AuctionClosedException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Lỗi xác nhận thanh toán: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  WATCH / UNWATCH — Đăng ký nhận AuctionEvent realtime
    // ════════════════════════════════════════════════════════════════════════

    private Response handleWatchAuction(Request req, ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            int auctionId = req.getDataAs(Integer.class);
            Auction auction = auctionService.getAuction(auctionId);
            handler.startWatching(auction);
            return Response.ok("Đang theo dõi phiên #" + auctionId, auction);
        } catch (AuctionClosedException e) {
            return Response.fail(e.getMessage());
        }
    }

    private Response handleUnwatchAuction(ClientHandler handler) {
        handler.stopWatching();
        return Response.ok("Đã thoát khỏi phiên đấu giá.");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BID HANDLERS
    // ════════════════════════════════════════════════════════════════════════

    private Response handlePlaceBid(Request req, ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            PlaceBidData data = req.getDataAs(PlaceBidData.class);
            Auction auction = auctionService.getAuction(data.auctionId);
            bidService.placeBid(handler.currentUser, auction, data.amount);

            // Refresh session user từ DB
            handler.currentUser = userDAO.findById(handler.currentUser.getUserId());

            // Trả về user mới nhất (bid đã broadcast qua BID_PLACED event rồi)
            return Response.ok(
                    String.format("Dat gia %,.0f VND thanh cong!", data.amount),
                    handler.currentUser);

        } catch (UserBannedException | InvalidBidException |
                 SelfBidException | AuctionClosedException |
                 InsufficientBalanceException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Loi dat gia: " + e.getMessage());
        }
    }

    // GET_BIDS_BY_AUCTION: không cần đăng nhập (ai cũng xem lịch sử bid)
    private Response handleGetBidsByAuction(Request req) {
        try {
            int auctionId = req.getDataAs(Integer.class);
            return Response.ok("OK", bidDAO.findByAuctionId(auctionId));
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }

    private Response handleGetMyBids(ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            return Response.ok("OK",
                    bidDAO.findByUserId(handler.currentUser.getUserId()));
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TRANSACTION HANDLERS
    // ════════════════════════════════════════════════════════════════════════

    private Response handleGetAllTransactions(ClientHandler handler) {
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            AuthGuard.requireAdmin(handler.currentUser);
            return Response.ok("OK", txDAO.findAll());
        } catch (AuthenticationException | UserBannedException e) {
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }

    private Response handleGetMyTransactions(ClientHandler handler) {
        // FIX #5: kiểm tra login
        Response check = requireLogin(handler);
        if (check != null) return check;

        try {
            return Response.ok("OK",
                    txDAO.findByUserId(handler.currentUser.getUserId()));
        } catch (Exception e) {
            return Response.fail("Lỗi: " + e.getMessage());
        }
    }
}
