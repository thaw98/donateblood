package com.grppj.donateblood.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.DonorAppointmentBean;

@Repository
public class DonorAppointmentRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int createAppointment(DonorAppointmentBean appt) {
        String sql = """
            INSERT INTO donor_appointment
              (date, time, created_at, status, user_id, user_role_id, hospital_id, blood_type_id, admin_id)
            VALUES
              (?, ?, NOW(), 'pending', ?, ?, ?, ?, NULL)
        """;
        jdbcTemplate.update(sql,
            appt.getDate(),
            appt.getTime(),
            appt.getUserId(),
            2,                       // donor role
            appt.getHospitalId(),
            appt.getBloodTypeId()
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public List<Map<String,Object>> listAllForAdmin() {
        // simple list; join to show names in dashboard
        String sql = """
          SELECT da.id, da.date, da.time, da.created_at, da.status,
                 u.username, u.email, u.phone,
                 h.hospital_name
          FROM donor_appointment da
          JOIN user u ON u.id = da.user_id AND u.role_id = da.user_role_id
          JOIN hospital h ON h.id = da.hospital_id
          ORDER BY da.created_at DESC
        """;
        return jdbcTemplate.queryForList(sql);
    }

    public int updateStatus(int apptId, String status) {
        String sql = "UPDATE donor_appointment SET status = ? WHERE id = ?";
        return jdbcTemplate.update(sql, status, apptId);
    }
    /** Auto reject any pending older than given hours (3). */
    public int autoRejectStalePending(int hours) {
        String sql = """
            UPDATE donor_appointment
               SET status = 'rejected'
             WHERE status = 'pending'
               AND created_at <= DATE_SUB(NOW(), INTERVAL ? HOUR)
        """;
        return jdbcTemplate.update(sql, hours);
    }
    
    public List<Map<String, Object>> listForHospital(int hospitalId) {
        String sql = """
            SELECT a.id, u.username, u.email, u.phone,
                   a.date, a.time, a.created_at, a.status
              FROM donor_appointment a
              JOIN user u ON u.id = a.user_id AND u.role_id = a.user_role_id
             WHERE a.hospital_id = ?
             ORDER BY a.created_at DESC
        """;
        return jdbcTemplate.queryForList(sql, hospitalId);
    }
    
    public Map<String, Object> findById(int id) {
        String sql = "SELECT id, user_id, user_role_id, hospital_id, blood_type_id " +
                     "FROM donor_appointment WHERE id = ?";
        return jdbcTemplate.queryForMap(sql, id);
    }
}
