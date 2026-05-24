package com.code.dao;

import com.code.database.DBConnection;
import com.code.models.AuditLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    private Connection conn() {
        return DBConnection.getInstance().getConnection();
    }

    public void save(AuditLog log) throws SQLException {

        String sql = """
                INSERT INTO audit_logs (
                    admin_id,
                    target_user_id,
                    action,
                    old_value,
                    new_value
                                        
             
                )
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, log.getAdminId());
            ps.setInt(2, log.getTargetUserId());
            ps.setString(3, log.getAction());
            ps.setString(4, log.getOldValue());
            ps.setString(5, log.getNewValue());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getInt(1));
                }
            }
        }
    }

    public List<AuditLog> findAll() throws SQLException {

        String sql = """
                SELECT *
                FROM audit_logs
                ORDER BY created_at DESC
                """;

        List<AuditLog> logs = new ArrayList<>();

        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        }
        return logs;
    }
    public List<AuditLog> findByTargetUserId(int userId)
            throws SQLException {

        String sql = """
                SELECT *
                FROM audit_logs
                WHERE target_user_id = ?
                ORDER BY created_at DESC
                """;

        List<AuditLog> logs = new ArrayList<>();

        try (PreparedStatement ps = conn().prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }
        return logs;
    }
    public List<AuditLog> findByAdminId(int adminId)
            throws SQLException {

        String sql = """
                SELECT *
                FROM audit_logs
                WHERE admin_id = ?
                ORDER BY created_at DESC
                """;

        List<AuditLog> logs = new ArrayList<>();

        try (PreparedStatement ps = conn().prepareStatement(sql)) {

            ps.setInt(1, adminId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }
        return logs;
    }
    private AuditLog mapRow(ResultSet rs) throws SQLException {

        AuditLog log = new AuditLog();

        log.setId(rs.getInt("id"));
        log.setAdminId(rs.getInt("admin_id"));
        log.setTargetUserId(rs.getInt("target_user_id"));

        log.setAction(rs.getString("action"));
        log.setOldValue(rs.getString("old_value"));
        log.setNewValue(rs.getString("new_value"));

        Timestamp timestamp = rs.getTimestamp("created_at");

        if (timestamp != null) {
            log.setCreatedAt(timestamp.toLocalDateTime());
        }

        return log;
    }
}