package com.code.service;

import com.code.dao.AuditLogDAO;
import com.code.models.AuditLog;
import java.sql.SQLException;
import java.util.List;

/**
 * Tập trung quản lý tất cả logging operations.
 *
 * Single Responsibility: chỉ lo việc ghi + đọc logs.
 * Không quan tâm business logic → dễ test, dễ maintain
 */
public class AuditLogService {

    private final AuditLogDAO auditLogDAO;

    public AuditLogService(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    /**
     * Ghi log hành động admin.
     *
     * @param adminId         ID admin thực hiện
     * @param targetUserId    ID user bị tác động
     * @param actionType      loại hành động (ActionType enum)
     * @param oldValue        giá trị cũ (có thể null nếu không áp dụng)
     * @param newValue        giá trị mới (có thể null nếu không áp dụng)
     */
    public void logAction(int adminId, int targetUserId,
                          AuditLog.ActionType actionType, String oldValue, String newValue)
            throws SQLException {
        AuditLog log = new AuditLog(
                adminId,
                targetUserId,
                actionType.name(),      // Sử dụng enum name thay vì magic string
                oldValue,
                newValue
        );
        auditLogDAO.save(log);
    }

    /**
     * Ghi log hành động không có old/new value (vd: DELETE_USER).
     */
    public void logAction(int adminId, int targetUserId, AuditLog.ActionType actionType)
            throws SQLException {
        logAction(adminId, targetUserId, actionType, null, null);
    }

    /**
     * Ghi log thay đổi field đơn giản (vd: balance).
     */
    public void logFieldChange(int adminId, int targetUserId,
                               AuditLog.ActionType actionType, String oldValue, String newValue)
            throws SQLException {
        logAction(adminId, targetUserId, actionType, oldValue, newValue);
    }

    // ────────────────────────────────────────────────────────────────
    // Các method read logs
    // ────────────────────────────────────────────────────────────────

    /**
     * Lấy tất cả logs (Admin xem tất cả thay đổi).
     */
    public List<AuditLog> getAllLogs() throws SQLException {
        return auditLogDAO.findAll();
    }

    /**
     * Lấy logs của một user cụ thể.
     */
    public List<AuditLog> getUserLogs(int targetUserId) throws SQLException {
        return auditLogDAO.findByTargetUserId(targetUserId);
    }

    /**
     * Lấy logs thực hiện bởi admin cụ thể.
     */
    public List<AuditLog> getAdminLogs(int adminId) throws SQLException {
        return auditLogDAO.findByAdminId(adminId);
    }
}