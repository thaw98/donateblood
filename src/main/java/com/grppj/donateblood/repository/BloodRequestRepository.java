package com.grppj.donateblood.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.BloodRequest;
import com.grppj.donateblood.model.AppointmentStatus;
import com.grppj.donateblood.model.Urgency;

@Repository
public class BloodRequestRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 🩸 Save or update a blood request (and update user info after) */
    public int saveBloodRequest(BloodRequest br) {
        String status = (br.getStatus() != null) ? br.getStatus().name() : "pending";

        int rowsAffected;

        if (br.getId() == null || br.getId() == 0) {
            // ✅ Insert new request
            String sql = "INSERT INTO blood_request (quantity, request_date, required_date, urgency, status, user_id, hospital_id, blood_type_id) " +
                         "VALUES (?, NOW(), ?, ?, ?, ?, ?, ?)";
            rowsAffected = jdbcTemplate.update(sql,
                    br.getQuantity(),
                    br.getRequiredDate(),
                    br.getUrgency().name(),
                    status,
                    br.getUserId(),
                    br.getHospitalId(),
                    br.getBloodTypeId()
            );
        } else {
            // ✅ Update existing request
            String sql = "UPDATE blood_request SET quantity=?, request_date=NOW(), required_date=?, urgency=?, status=?, user_id=?, hospital_id=?, blood_type_id=? " +
                         "WHERE id=?";
            rowsAffected = jdbcTemplate.update(sql,
                    br.getQuantity(),
                    br.getRequiredDate(),
                    br.getUrgency().name(),
                    status,
                    br.getUserId(),
                    br.getHospitalId(),
                    br.getBloodTypeId(),
                    br.getId()
            );
        }

        // 🧩 After save/update → update user blood type and hospital
        if (rowsAffected > 0) {
            updateUserBloodAndHospital(br.getUserId(), br.getBloodTypeId(), br.getHospitalId());
        }

        return rowsAffected;
    }

    /** 🧠 Reusable method to update user blood type & hospital */
    private void updateUserBloodAndHospital(Integer userId, Integer bloodTypeId, Integer hospitalId) {
        String updateUserSql = "UPDATE user SET blood_type_id = ?, hospital_id = ? WHERE id = ?";
        jdbcTemplate.update(updateUserSql, bloodTypeId, hospitalId, userId);
    }

    /** Fetch all pending blood requests (for home page) */
    public List<BloodRequest> findAll() {
        String sql = "SELECT br.id, br.quantity, br.required_date, br.urgency, br.status, " +
                     "h.hospital_name, b.blood_type " +
                     "FROM blood_request br " +
                     "JOIN hospital h ON br.hospital_id = h.id " +
                     "JOIN blood_type b ON br.blood_type_id = b.id " +
                     "WHERE br.status = 'pending' " +
                     "ORDER BY br.required_date ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs));
    }

    /** Fetch requests by user ID */
    public List<BloodRequest> findByUserId(int userId) {
        String sql = "SELECT br.id, br.quantity, br.required_date, br.urgency, br.status, " +
                     "h.hospital_name, b.blood_type " +
                     "FROM blood_request br " +
                     "JOIN hospital h ON br.hospital_id = h.id " +
                     "JOIN blood_type b ON br.blood_type_id = b.id " +
                     "WHERE br.user_id = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);
    }

    /** Fetch request by ID */
    public BloodRequest findById(int id) {
        String sql = "SELECT br.id, br.quantity, br.required_date, br.urgency, br.status, " +
                     "br.user_id, br.hospital_id, br.blood_type_id, " +
                     "h.hospital_name, b.blood_type " +
                     "FROM blood_request br " +
                     "JOIN hospital h ON br.hospital_id = h.id " +
                     "JOIN blood_type b ON br.blood_type_id = b.id " +
                     "WHERE br.id = ?";

        List<BloodRequest> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            BloodRequest br = new BloodRequest();
            br.setId(rs.getInt("id"));
            br.setQuantity(rs.getInt("quantity"));
            br.setRequiredDate(rs.getDate("required_date").toLocalDate());
            br.setUrgency(Urgency.valueOf(rs.getString("urgency").toUpperCase()));
            br.setStatus(AppointmentStatus.valueOf(rs.getString("status")));
            br.setUserId(rs.getInt("user_id"));
            br.setHospitalId(rs.getInt("hospital_id"));
            br.setBloodTypeId(rs.getInt("blood_type_id"));
            br.setHospitalName(rs.getString("hospital_name"));
            br.setBloodTypeName(rs.getString("blood_type"));
            return br;
        }, id);

        return list.isEmpty() ? null : list.get(0);
    }

    /** Delete blood request by ID */
    public int deleteById(Integer id) {
        String sql = "DELETE FROM blood_request WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
    
    /** Update blood request */
    public int update(BloodRequest br) {
        String sql = "UPDATE blood_request SET blood_type_id=?, hospital_id=?, quantity=?, required_date=?, urgency=? WHERE id=?";
        int rows = jdbcTemplate.update(sql,
            br.getBloodTypeId(),
            br.getHospitalId(),
            br.getQuantity(),
            Date.valueOf(br.getRequiredDate()),
            br.getUrgency().name(),
            br.getId()
        );

        // ✅ Also update user data here if needed
        if (rows > 0) {
            updateUserBloodAndHospital(br.getUserId(), br.getBloodTypeId(), br.getHospitalId());
        }

        return rows;
    }

    /** Map ResultSet to BloodRequest (for lists) */
    private BloodRequest mapRow(ResultSet rs) throws SQLException {
        BloodRequest br = new BloodRequest();
        br.setId(rs.getInt("id"));
        br.setQuantity(rs.getInt("quantity"));
        br.setRequiredDate(rs.getDate("required_date").toLocalDate());
        br.setUrgency(Urgency.valueOf(rs.getString("urgency").toUpperCase()));
        br.setStatus(AppointmentStatus.valueOf(rs.getString("status")));
        br.setHospitalName(rs.getString("hospital_name"));
        br.setBloodTypeName(rs.getString("blood_type"));
        return br;
    }
}
