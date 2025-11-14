package com.grppj.donateblood.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.AppointmentStatus;
import com.grppj.donateblood.model.DonorAppointmentBean;

@Repository
public class DonorAppointmentRepository {

    private static final int DONOR_ROLE_ID = 3; // donors are role 3

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                   DATE_FORMAT(a.date, '%d-%m-%Y') AS date_dmy,
                   a.time,
                   TIME_FORMAT(a.time, '%h:%i%p') AS time12,
                   a.created_at, a.status,
                   a.blood_type_id, a.hospital_id,
                   CASE a.blood_type_id
                     WHEN 1 THEN 'A+' WHEN 2 THEN 'A-' WHEN 3 THEN 'B+' WHEN 4 THEN 'B-'
                     WHEN 5 THEN 'O+' WHEN 6 THEN 'O-' WHEN 7 THEN 'AB+' WHEN 8 THEN 'AB-'
                     ELSE NULL
                   END AS blood_type
              FROM donor_appointment a
              JOIN `user` u
                ON u.id = a.user_id AND u.role_id = a.user_role_id
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

    /** Most recent COMPLETED appointment date for a given user (or null if none). */
    public LocalDate findLastDonationDateByUserId(int userId) {
        String sql = """
            SELECT MAX(date) AS last_date
              FROM donor_appointment
             WHERE user_id = ?
               AND status = 'completed'
        """;
        return jdbcTemplate.query(sql, ps -> ps.setInt(1, userId), rs -> {
            if (rs.next()) {
                Date d = rs.getDate("last_date");
                return (d != null) ? d.toLocalDate() : null;
            }
            return null;
        });
    }
   
    public DonorAppointmentBean findMessageById(int id) {
        String sql = """
            SELECT id, user_id, hospital_id, blood_type_id, date, time, status, created_at
              FROM donor_appointment
             WHERE id = ?
        """;

        List<DonorAppointmentBean> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            DonorAppointmentBean appt = new DonorAppointmentBean();
            appt.setId(rs.getInt("id"));
            appt.setUserId(rs.getInt("user_id"));
            appt.setHospitalId(rs.getInt("hospital_id"));
            appt.setBloodTypeId(rs.getInt("blood_type_id"));
            appt.setDate(rs.getString("date"));
            appt.setTime(rs.getString("time"));
            appt.setCreatedAt(rs.getString("created_at"));
            String statusStr = rs.getString("status");
            appt.setStatus(statusStr != null ? AppointmentStatus.valueOf(statusStr.toLowerCase()) : null);
            return appt;
        }, id);

        return list.isEmpty() ? null : list.get(0);
    }
    
    public int createAppointment(int userId, int roleId, int hospitalId, Integer bloodTypeId,
            LocalDate date, String time, String status) {
String sql = """
INSERT INTO donor_appointment (date, time, created_at, status, user_id, user_role_id, hospital_id, blood_type_id)
VALUES (?, ?, NOW(), ?, ?, ?, ?, ?)
""";
jdbcTemplate.update(sql,
java.sql.Date.valueOf(date),
time,
status,
userId,
roleId,
hospitalId,
bloodTypeId
);
return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
}

}
