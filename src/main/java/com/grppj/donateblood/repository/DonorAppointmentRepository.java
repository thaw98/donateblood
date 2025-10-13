package com.grppj.donateblood.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.DonorAppointmentBean;

@Repository
public class DonorAppointmentRepository {

    private static final int DONOR_ROLE_ID = 3; // donors are role 3

    @Autowired private JdbcTemplate jdbcTemplate;

    /** User flow: create a PENDING appointment. */
    public int createAppointment(DonorAppointmentBean appt) {
        String sql = """
            INSERT INTO donor_appointment
              (date, time, created_at, status,
               user_id, user_role_id, hospital_id, blood_type_id)
            VALUES (?, ?, NOW(), 'pending', ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
            appt.getDate(),
            appt.getTime(),
            appt.getUserId(),
            DONOR_ROLE_ID,
            appt.getHospitalId(),
            appt.getBloodTypeId()
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    /** Admin-created donor: create an APPROVED appointment "now". */
    public int createCompletedAppointmentNow(int userId, int userRoleId,
                                             int hospitalId, int bloodTypeId) {
        String sql = """
            INSERT INTO donor_appointment
                  (date,  time,                               created_at, status,
                   user_id, user_role_id, hospital_id, blood_type_id)
            VALUES (CURDATE(), DATE_FORMAT(NOW(), '%H:%i:%s'), NOW(),      'approved',
                    ?,       ?,            ?,           ?)
        """;
        jdbcTemplate.update(sql, userId, userRoleId, hospitalId, bloodTypeId);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    /** Idempotent approve/reject: only if current status is PENDING. */
    public int updateStatusIfPending(int apptId, String newStatus) {
        String sql = "UPDATE donor_appointment SET status = ? WHERE id = ? AND status = 'pending'";
        return jdbcTemplate.update(sql, newStatus, apptId);
    }

    /** ALL appointments for a hospital (no status filter). */
    public List<Map<String, Object>> listForHospital(int hospitalId) {
        String sql = """
            SELECT a.id, u.username, u.email, u.phone,
                   a.date, a.time, a.created_at, a.status
              FROM donor_appointment a
              JOIN `user` u ON u.id = a.user_id AND u.role_id = a.user_role_id
             WHERE a.hospital_id = ?
             ORDER BY a.created_at DESC
        """;
        return jdbcTemplate.queryForList(sql, hospitalId);
    }

    public Map<String, Object> findById(int id) {
        String sql = """
            SELECT id, user_id, user_role_id, hospital_id, blood_type_id, status
              FROM donor_appointment
             WHERE id = ?
        """;
        return jdbcTemplate.queryForMap(sql, id);
    }
}
