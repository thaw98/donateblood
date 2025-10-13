package com.grppj.donateblood.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.model.UserBean;

@Repository
public class DonorRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------- CREATE ----------
    public int addDonor(UserBean user) {
        String sql = "INSERT INTO user (username, email, password, gender, dateofbirth, address, phone, blood_type_id, role_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getPhone(),
                user.getBloodTypeId(),
                user.getRoleId());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public int addDonation(DonationBean d) {
        String ins = """
            INSERT INTO donation (blood_unit, donation_date, status, donor_appointment_id)
            VALUES (?, ?, ?, ?)
        """;
        jdbcTemplate.update(ins, d.getBloodUnit(), d.getDonationDate(), d.getStatus(), d.getDonorAppointmentId());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    // ---------- READ (LIST) ----------
    /**
     * List donors who have at least one APPROVED (or legacy COMPLETED) appointment.
     * Also returns the latest donation status for those appointments.
     */
    public List<UserBean> getAllDonorsWithStatus(int roleId) {
        String sql = """
            SELECT
                u.id,
                u.username,
                u.email,
                u.phone,
                u.gender,
                u.address,
                u.dateofbirth,
                u.blood_type_id,
                u.role_id,

                /* Next eligible = 4 months after the latest APPROVED/COMPLETED appointment */
                (
                    SELECT DATE_ADD(
                               MAX(STR_TO_DATE(CONCAT(da2.date, ' ', IFNULL(da2.`time`,'00:00:00')),
                                                 '%Y-%m-%d %H:%i:%s')),
                               INTERVAL 4 MONTH
                           )
                      FROM donor_appointment da2
                     WHERE da2.user_id      = u.id
                       AND da2.user_role_id = u.role_id
                       AND da2.status IN ('approved','completed')
                ) AS valid_of_donation,

                /* Latest donation status among APPROVED/COMPLETED appointments */
                (
                    SELECT d.status
                      FROM donation d
                     WHERE d.donor_appointment_id IN (
                               SELECT da3.id
                                 FROM donor_appointment da3
                                WHERE da3.user_id      = u.id
                                  AND da3.user_role_id = u.role_id
                                  AND da3.status IN ('approved','completed')
                           )
                     ORDER BY d.donation_date DESC, d.donation_id DESC
                     LIMIT 1
                ) AS last_status

            FROM `user` u
            WHERE u.role_id = ?
              AND EXISTS (
                    SELECT 1
                      FROM donor_appointment da4
                     WHERE da4.user_id      = u.id
                       AND da4.user_role_id = u.role_id
                       AND da4.status IN ('approved','completed')
              )
            ORDER BY u.id
            """;

        return jdbcTemplate.query(sql, (rs, rn) -> {
            UserBean u = new UserBean();
            u.setId(rs.getInt("id"));
            u.setUsername(rs.getString("username"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setGender(rs.getString("gender"));
            u.setAddress(rs.getString("address"));
            u.setDateOfBirth(rs.getString("dateofbirth"));
            u.setBloodTypeId((Integer) rs.getObject("blood_type_id"));
            u.setRoleId(rs.getInt("role_id"));
            u.setStatus(rs.getString("last_status")); // may be null if no donation yet

            Timestamp ts = rs.getTimestamp("valid_of_donation");
            u.setValidOfDonation(ts == null ? null :
                ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return u;
        }, roleId);
    }

    // ---------- READ (ONE) ----------
    public UserBean getDonorById(int id) {
        String sql = """
            SELECT u.id, u.username, u.email, u.password, u.gender, u.dateofbirth, u.address,
                   u.phone, u.role_id, u.blood_type_id,
                   d.status,
                   h.hospital_name,
                   DATE_ADD(d.donation_date, INTERVAL 4 MONTH) AS next_eligible
              FROM user u
              LEFT JOIN donor_appointment da
                     ON da.user_id = u.id AND da.user_role_id = u.role_id
              LEFT JOIN donation d
                     ON d.donor_appointment_id = da.id
              LEFT JOIN hospital h
                     ON h.id = da.hospital_id
             WHERE u.id = ?
             ORDER BY d.donation_date DESC, d.donation_id DESC
             LIMIT 1
        """;

        return jdbcTemplate.queryForObject(sql, new Object[]{id}, (rs, rowNum) -> {
            UserBean user = new UserBean();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPhone(rs.getString("phone"));
            user.setGender(rs.getString("gender"));
            user.setDateOfBirth(rs.getString("dateofbirth"));
            user.setAddress(rs.getString("address"));
            user.setStatus(rs.getString("status"));
            user.setHospitalName(rs.getString("hospital_name"));
            user.setValidOfDonation(rs.getString("next_eligible"));
            Object bt = rs.getObject("blood_type_id");
            user.setBloodTypeId(bt == null ? null : rs.getInt("blood_type_id"));
            return user;
        });
    }

    // ---------- UPDATE ----------
    public int updateDonor(UserBean donor) {
        String sqlUser = "UPDATE user SET phone = ? WHERE id = ?";
        int rowsUser = jdbcTemplate.update(sqlUser, donor.getPhone(), donor.getId());

        String sqlDonation = """
            UPDATE donation d
            JOIN donor_appointment da ON da.id = d.donor_appointment_id
               SET d.status = ?
             WHERE da.user_id = ?
        """;
        int rowsDonation = jdbcTemplate.update(sqlDonation, donor.getStatus(), donor.getId());
        return rowsUser + rowsDonation;
    }

    // ---------- HELPERS ----------
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE LOWER(email) = LOWER(?)";
        Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return cnt != null && cnt > 0;
    }

    public LocalDateTime getNextValidDonation(int userId, int roleId) {
        String sql = """
            SELECT DATE_ADD(MAX(d.donation_date), INTERVAL 4 MONTH) AS next_eligible
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
             WHERE da.user_id = ? AND da.user_role_id = ?
        """;
        Timestamp ts = jdbcTemplate.queryForObject(sql, java.sql.Timestamp.class, userId, roleId);
        return (ts == null) ? null : ts.toLocalDateTime();
    }

    public Map<String, Object> findLatestDonation(int userId, int roleId) {
        String sql = """
            SELECT d.donation_id,
                   da.hospital_id,
                   da.user_id,
                   da.user_role_id,
                   d.blood_unit,
                   d.status
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
             WHERE da.user_id = ? AND da.user_role_id = ?
             ORDER BY d.donation_date DESC, d.donation_id DESC
             LIMIT 1
        """;
        var list = jdbcTemplate.queryForList(sql, userId, roleId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Integer findLatestAvailableDonationId(int userId, int roleId) {
        String sql = """
            SELECT d.donation_id
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
             WHERE da.user_id = ? AND da.user_role_id = ?
               AND d.status = 'Available'
             ORDER BY d.donation_date DESC, d.donation_id DESC
             LIMIT 1
        """;
        List<Integer> ids = jdbcTemplate.query(sql, (rs, rn) -> rs.getInt(1), userId, roleId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public int markDonationUsed(int donationId) {
        String sql = "UPDATE donation SET status='Used' WHERE donation_id=? AND status='Available'";
        return jdbcTemplate.update(sql, donationId);
    }

    public List<StockRow> getStockFromDonationsByHospital(int hospitalId) {
        String sql = """
            SELECT bt.id AS blood_type_id,
                   bt.blood_type,
                   COALESCE(SUM(CASE WHEN d.status='Available' THEN d.blood_unit ELSE 0 END), 0) AS units,
                   MAX(d.donation_date) AS last_donation
              FROM blood_type bt
              LEFT JOIN user u
                     ON u.blood_type_id = bt.id
              LEFT JOIN donor_appointment da
                     ON da.user_id = u.id
                    AND da.user_role_id = u.role_id
                    AND da.hospital_id = ?
              LEFT JOIN donation d
                     ON d.donor_appointment_id = da.id
             GROUP BY bt.id, bt.blood_type
             ORDER BY bt.id
        """;
        return jdbcTemplate.query(sql, (rs, rn) -> {
            StockRow r = new StockRow();
            r.bloodTypeId = rs.getInt("blood_type_id");
            r.bloodType   = rs.getString("blood_type");
            r.units       = rs.getInt("units");
            r.lastDonation= rs.getTimestamp("last_donation");
            return r;
        }, hospitalId);
    }

    public Integer findUserRoleId(int userId) {
        String sql = "SELECT role_id FROM `user` WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }

    // DTO
    public static class StockRow {
        public Integer bloodTypeId;
        public String  bloodType;
        public Integer units;
        public java.sql.Timestamp lastDonation;
    }
}
