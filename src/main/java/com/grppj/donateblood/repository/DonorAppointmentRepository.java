package com.grppj.donateblood.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.DonorAppointmentBean;
import com.grppj.donateblood.model.AppointmentStatus;

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
            VALUES (?, ?, NOW(), ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
            appt.getDate(),
            appt.getTime(),
            AppointmentStatus.pending.name().toLowerCase(),
            appt.getUserId(),
            DONOR_ROLE_ID,
            appt.getHospitalId(),
            appt.getBloodTypeId()
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    /** Admin-created donor: create a COMPLETED appointment "now". */
    public int createCompletedAppointmentNow(int userId, int userRoleId,
                                             int hospitalId, int bloodTypeId) {
        String sql = """
            INSERT INTO donor_appointment
                  (date,  time,                               created_at, status,
                   user_id, user_role_id, hospital_id, blood_type_id)
            VALUES (CURDATE(), DATE_FORMAT(NOW(), '%H:%i:%s'), NOW(),      ?,
                    ?,       ?,            ?,           ?)
        """;
        jdbcTemplate.update(sql,
            AppointmentStatus.completed.name().toLowerCase(),
            userId, userRoleId, hospitalId, bloodTypeId
        );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    /** Transition to newStatus only if current status is PENDING. Idempotent. */
    public int updateStatusIfPending(int apptId, AppointmentStatus newStatus) {
        String sql = "UPDATE donor_appointment SET status = ? WHERE id = ? AND status = ?";
        return jdbcTemplate.update(
            sql,
            newStatus.name().toLowerCase(),
            apptId,
            AppointmentStatus.pending.name().toLowerCase()
        );
    }

    /** ALL appointments for a hospital (add blood_type_id for UI convenience). */
    public List<Map<String, Object>> listForHospital(int hospitalId) {
        String sql = """
						SELECT a.id,
						       u.username, u.email, u.phone,
						       a.date,
						       DATE_FORMAT(a.date, '%d-%m-%Y') AS date_dmy,   -- <-- add this
						       a.time,
						       TIME_FORMAT(a.time, '%h:%i%p') AS time12,      -- (from earlier)
						       a.created_at, a.status,
						       a.blood_type_id, a.hospital_id
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
    
    public int expirePendingOlderThanHours(int hours) {
        String sql = """
            UPDATE donor_appointment
               SET status = ?
             WHERE status = ?
               AND created_at < TIMESTAMPADD(HOUR, -?, NOW())
        """;
        return jdbcTemplate.update(
            sql,
            AppointmentStatus.expired.name().toLowerCase(),
            AppointmentStatus.pending.name().toLowerCase(),
            hours
        );
    }
}
