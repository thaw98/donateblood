package com.grppj.donateblood.repository;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.grppj.donateblood.model.BloodRequestBean;

@Repository
public class BloodRequestRepository {

    @Autowired private JdbcTemplate jdbcTemplate;

    public int create(BloodRequestBean r) {
        String sql = """
            INSERT INTO blood_request
              (quantity, required_date, urgency, status, user_id, user_role_id, hospital_id, blood_type_id)
            VALUES (?, ?, ?, 'pending', ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql, r.getQuantity(), r.getRequiredDate(), r.getUrgency(),
                r.getUserId(), r.getUserRoleId(), r.getHospitalId(), r.getBloodTypeId());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public List<Map<String,Object>> listPendingByHospital(int hospitalId) {
        String sql = """
          SELECT br.id, br.quantity, br.required_date, br.urgency, br.status,
                 br.blood_type_id, bt.blood_type, br.request_date
            FROM blood_request br
            JOIN blood_type bt ON bt.id = br.blood_type_id
           WHERE br.hospital_id = ? AND br.status='pending'
           ORDER BY br.request_date DESC
        """;
        return jdbcTemplate.queryForList(sql, hospitalId);
    }

    public Map<String,Object> findById(int id) {
        String sql = "SELECT * FROM blood_request WHERE id=?";
        List<Map<String,Object>> list = jdbcTemplate.queryForList(sql, id);
        return list.isEmpty()? null : list.get(0);
    }

    public int markFulfilled(int id) {
        return jdbcTemplate.update("UPDATE blood_request SET status='fulfilled' WHERE id=?", id);
    }

    public int insertFulfillmentRow(int donationId, int requestId, int qtyUsed) {
        String sql = """
            INSERT INTO request_fulfillment (donation_donation_id, blood_request_id, quantity_used)
            VALUES (?, ?, ?)
        """;
        return jdbcTemplate.update(sql, donationId, requestId, qtyUsed);
    }
    
 // latest AVAILABLE donations for a hospital & blood type (FIFO by date)
    public List<Map<String,Object>> findAvailableDonationsForHospital(int hospitalId, int bloodTypeId, int limit) {
        String sql = """
          SELECT d.donation_id, d.donation_date, d.status, u.blood_type_id
            FROM donation d
            JOIN user u ON u.id = d.user_id AND u.role_id = d.user_role_id
           WHERE d.hospital_id = ?
             AND u.blood_type_id = ?
             AND d.status = 'Available'
           ORDER BY d.donation_date ASC
           LIMIT ?
        """;
        return jdbcTemplate.queryForList(sql, hospitalId, bloodTypeId, limit);
    }

    // Only change if it is currently Available (idempotent)
    public int markDonationUsed(int donationId) {
        String sql = "UPDATE donation SET status='Used' WHERE donation_id=? AND status='Available'";
        return jdbcTemplate.update(sql, donationId);
    }

    public int decreaseStock(Integer hospitalId, Integer bloodTypeId, Integer units, Integer userId, Integer userRoleId) {
        // create row if missing (amount defaults 0) then decrement
        // Try update first:
        int updated = jdbcTemplate.update(
            "UPDATE blood_stock SET amount = GREATEST(amount - ?, 0), updated_date = CURRENT_DATE " +
            " WHERE hospital_id=? AND blood_type_id=?", units, hospitalId, bloodTypeId);

        if (updated == 0) {
            // create then update once more
            jdbcTemplate.update(
              "INSERT IGNORE INTO blood_stock (amount, updated_date, hospital_id, blood_type_id, user_id, user_role_id) " +
              "VALUES (0, CURRENT_DATE, ?, ?, ?, ?)",
              hospitalId, bloodTypeId, userId, userRoleId);
            updated = jdbcTemplate.update(
              "UPDATE blood_stock SET amount = GREATEST(amount - ?, 0), updated_date = CURRENT_DATE " +
              " WHERE hospital_id=? AND blood_type_id=?", units, hospitalId, bloodTypeId);
        }
        return updated;
    }

}
