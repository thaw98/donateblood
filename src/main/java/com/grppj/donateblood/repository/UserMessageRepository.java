package com.grppj.donateblood.repository;

import com.grppj.donateblood.model.UserMessageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserMessageRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Insert new message
    public void sendMessage(int senderId, int receiverId, String message) {
        String sql = "INSERT INTO user_messages (sender_id, receiver_id, message, created_at, is_read) " +
                     "VALUES (?, ?, ?, NOW(), 0)";
        jdbcTemplate.update(sql, senderId, receiverId, message);
    }

    public List<UserMessageBean> getMessagesByReceiver(int receiverId) {
        String sql = "SELECT um.*, h.hospital_name AS sender_name " +
                     "FROM user_messages um " +
                     "JOIN hospital h ON um.sender_id = h.id " +
                     "WHERE um.receiver_id = ? " +
                     "ORDER BY um.created_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserMessageBean msg = new UserMessageBean();
            msg.setId(rs.getInt("id"));
            msg.setSenderId(rs.getInt("sender_id"));
            msg.setReceiverId(rs.getInt("receiver_id"));
            msg.setMessage(rs.getString("message"));
            msg.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            msg.setRead(rs.getBoolean("is_read"));
            msg.setSenderName(rs.getString("sender_name")); // hospital_name from hospital table
            return msg;
        }, receiverId);
    }



    // Mark message as read
    public void markAsRead(int messageId) {
        String sql = "UPDATE user_messages SET is_read = 1 WHERE id = ?";
        jdbcTemplate.update(sql, messageId);
    }
    
    // NEW: insert with appointment info
    public void sendMessageWithAppt(int senderId, int receiverId, String message,
                                    java.time.LocalDate apptDate, String apptTime) {
        String sql = """
            INSERT INTO user_messages
              (sender_id, receiver_id, message, appointment_date, appointment_time, created_at, is_read)
            VALUES (?, ?, ?, ?, ?, NOW(), 0)
        """;
        jdbcTemplate.update(sql, senderId, receiverId, message,
                java.sql.Date.valueOf(apptDate), apptTime);
    }


}
