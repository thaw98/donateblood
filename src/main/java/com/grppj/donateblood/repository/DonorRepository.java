package com.grppj.donateblood.repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
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

    private static final int ROLE_DONOR = 3;
    private static final int ROLE_RECIPIENT = 4;

    // ---------- CREATE ----------
    public int addDonor(UserBean user) {
        String sql = "INSERT INTO user (username, email, password, gender, dateofbirth, address, phone, blood_type_id, role_id,is_verified) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getPhone(),
                user.getBloodTypeId(),
                user.getRoleId(),
                true  );
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public int addDonation(DonationBean d) {
        String ins = """
            INSERT INTO donation (blood_unit, donation_date, status, donor_appointment_id)
            VALUES (?, ?, ?, ?)
        """;
        jdbcTemplate.update(ins, d.getBloodUnit(), d.getDonationDate(), d.getStatus(), d.getDonorAppointmentId());
        int donationId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

        String infoSql = """
            SELECT da.hospital_id, u.blood_type_id, u.id AS user_id, u.role_id AS user_role_id
            FROM donor_appointment da
            JOIN user u ON u.id = da.user_id
            WHERE da.id = ?
        """;
        Map<String,Object> info = jdbcTemplate.queryForMap(infoSql, d.getDonorAppointmentId());

        int hospitalId = (Integer) info.get("hospital_id");
        int bloodTypeId = (Integer) info.get("blood_type_id");
        int userId = (Integer) info.get("user_id");
        int userRoleId = (Integer) info.get("user_role_id");

        String checkSql = "SELECT COUNT(*) FROM blood_stock WHERE hospital_id=? AND blood_type_id=? AND user_id=? AND user_role_id=?";
        Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class, hospitalId, bloodTypeId, userId, userRoleId);

        if (exists != null && exists > 0) {
            String updateStock = """
                UPDATE blood_stock
                SET amount = amount + ?, updated_date = NOW()
                WHERE hospital_id=? AND blood_type_id=? AND user_id=? AND user_role_id=?
            """;
            jdbcTemplate.update(updateStock, d.getBloodUnit(), hospitalId, bloodTypeId, userId, userRoleId);
        } else {
            String insertStock = """
                INSERT INTO blood_stock (amount, updated_date, hospital_id, blood_type_id, user_id, user_role_id)
                VALUES (?, NOW(), ?, ?, ?, ?)
            """;
            jdbcTemplate.update(insertStock, d.getBloodUnit(), hospitalId, bloodTypeId, userId, userRoleId);
        }

        return donationId;
    }

    // ---------- READ (LIST) ----------
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
                (
                    SELECT DATE_ADD(MAX(d.donation_date), INTERVAL 4 MONTH)
                    FROM donation d
                    JOIN donor_appointment da2 ON da2.id = d.donor_appointment_id
                    WHERE da2.user_id = u.id
                      AND da2.user_role_id = u.role_id
                ) AS valid_of_donation,
                (
                    SELECT d.status
                    FROM donation d
                    JOIN donor_appointment da3 ON da3.id = d.donor_appointment_id
                    WHERE da3.user_id = u.id
                      AND da3.user_role_id = u.role_id
                    ORDER BY d.donation_date DESC, d.donation_id DESC
                    LIMIT 1
                ) AS last_status
            FROM `user` u
            WHERE u.role_id = ?
            ORDER BY u.id DESC
            """;

        return jdbcTemplate.query(sql, (rs, rn) -> {
            UserBean u = new UserBean();
            u.setId(rs.getInt("id"));
            u.setUsername(rs.getString("username"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setGender(rs.getString("gender"));
            u.setAddress(rs.getString("address"));
            u.setDateOfBirth(rs.getDate("dateofbirth") != null
                    ? rs.getDate("dateofbirth").toLocalDate()
                    : null);
            u.setBloodTypeId((Integer) rs.getObject("blood_type_id"));
            u.setRoleId(rs.getInt("role_id"));
            u.setStatus(rs.getString("last_status"));

            Timestamp ts = rs.getTimestamp("valid_of_donation");
            u.setValidOfDonation(ts == null ? null
                    : ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
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

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            UserBean user = new UserBean();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPhone(rs.getString("phone"));
            user.setGender(rs.getString("gender"));
            user.setDateOfBirth(rs.getDate("dateofbirth") != null ? rs.getDate("dateofbirth").toLocalDate() : null);
            user.setAddress(rs.getString("address"));
            user.setStatus(rs.getString("status"));
            user.setHospitalName(rs.getString("hospital_name"));
            user.setValidOfDonation(rs.getString("next_eligible"));

            Object bt = rs.getObject("blood_type_id");
            user.setBloodTypeId(bt == null ? null : rs.getInt("blood_type_id"));

            Object roleObj = rs.getObject("role_id");
            user.setRoleId(roleObj != null ? ((Number) roleObj).intValue() : null);

            return user;
        }, id);  // <-- pass parameters after RowMapper (modern style)
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

    // --------- TYPE-SAFE Donation History DTO + Mapper ---------
    public static class DonationHistoryRow {
        private Integer donationId;
        private LocalDateTime donationDate; // type-safe
        private String  status;
        private Integer bloodUnit;
        private Integer appointmentId;
        private LocalDate appointmentDate;  // type-safe
        private LocalTime appointmentTime;  // type-safe (parsed from String)
        private Integer hospitalId;
        private String  hospitalName;

        public Integer getDonationId() { return donationId; }
        public void setDonationId(Integer donationId) { this.donationId = donationId; }

        public LocalDateTime getDonationDate() { return donationDate; }
        public void setDonationDate(LocalDateTime donationDate) { this.donationDate = donationDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getBloodUnit() { return bloodUnit; }
        public void setBloodUnit(Integer bloodUnit) { this.bloodUnit = bloodUnit; }

        public Integer getAppointmentId() { return appointmentId; }
        public void setAppointmentId(Integer appointmentId) { this.appointmentId = appointmentId; }

        public LocalDate getAppointmentDate() { return appointmentDate; }
        public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

        public LocalTime getAppointmentTime() { return appointmentTime; }
        public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }

        public Integer getHospitalId() { return hospitalId; }
        public void setHospitalId(Integer hospitalId) { this.hospitalId = hospitalId; }

        public String getHospitalName() { return hospitalName; }
        public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    }

    public List<DonationHistoryRow> getDonationHistory(int userId, int roleId) {
        String sql = """
            SELECT
                d.donation_id,
                d.donation_date,
                d.status,
                d.blood_unit,
                da.id   AS appt_id,
                da.date AS appt_date,
                da.time AS appt_time,   -- may be TIME or VARCHAR like '6:00 PM'
                h.id    AS hospital_id,
                h.hospital_name
            FROM donation d
            JOIN donor_appointment da
              ON da.id = d.donor_appointment_id
            JOIN hospital h
              ON h.id = da.hospital_id
            WHERE da.user_id = ? AND da.user_role_id = ?
            ORDER BY d.donation_date DESC, d.donation_id DESC
        """;

        return jdbcTemplate.query(sql, (rs, rn) -> {
            DonationHistoryRow r = new DonationHistoryRow();
            r.setDonationId(rs.getInt("donation_id"));

            Timestamp ts = rs.getTimestamp("donation_date");
            r.setDonationDate(ts == null ? null : ts.toLocalDateTime());

            r.setStatus(rs.getString("status"));
            r.setBloodUnit((Integer) rs.getObject("blood_unit"));
            r.setAppointmentId((Integer) rs.getObject("appt_id"));

            java.sql.Date apptDate = rs.getDate("appt_date");
            r.setAppointmentDate(apptDate == null ? null : apptDate.toLocalDate());

            // Robust: read time as String (works for TIME and VARCHAR) and parse
            String rawTime = rs.getString("appt_time");
            r.setAppointmentTime(parseApptTime(rawTime));

            r.setHospitalId((Integer) rs.getObject("hospital_id"));
            r.setHospitalName(rs.getString("hospital_name"));
            return r;
        }, userId, roleId);
    }

    // ---- Robust time parser (handles 'HH:mm[:ss]', 'H:mm', '6:00 PM', '6:00PM', '06:00 PM', etc.) ----
    private static LocalTime parseApptTime(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        String[] patterns = {
            "HH:mm:ss", "H:mm:ss",
            "HH:mm", "H:mm",
            "h:mm a", "hh:mm a",
            "h:mma", "hh:mma"
        };
        for (String p : patterns) {
            try {
                return LocalTime.parse(s, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignore) { }
        }
        // Case-insensitive fallback with optional seconds and optional space before AM/PM
        DateTimeFormatter fallback = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("h:mm")
                .optionalStart().appendPattern(":ss").optionalEnd()
                .optionalStart().appendLiteral(' ').optionalEnd()
                .appendPattern("a")
                .toFormatter();
        try {
            return LocalTime.parse(s, fallback);
        } catch (Exception ignore) {
            return null; // treat unparseable times as missing
        }
    }

    public static class StockRow {
        public Integer bloodTypeId;
        public String  bloodType;
        public Integer units;
        public java.sql.Timestamp lastDonation;
    }

    // 🔎 lookup by email (case-insensitive) — allow donor(3) OR recipient(4)
    public UserBean findByEmail(String email) {
        String sql = """
            SELECT id, username, email, phone, gender, dateofbirth, address, blood_type_id, role_id
              FROM `user`
             WHERE LOWER(email) = LOWER(?)
               AND role_id IN (?, ?)
             LIMIT 1
        """;
        var list = jdbcTemplate.query(sql, (rs, rn) -> {
            UserBean u = new UserBean();
            u.setId(rs.getInt("id"));
            u.setUsername(rs.getString("username"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setGender(rs.getString("gender"));
            u.setDateOfBirth(rs.getDate("dateofbirth") != null ? rs.getDate("dateofbirth").toLocalDate() : null);
            u.setAddress(rs.getString("address"));
            u.setBloodTypeId((Integer) rs.getObject("blood_type_id"));
            u.setRoleId((Integer) rs.getObject("role_id"));
            return u;
        }, email, ROLE_DONOR, ROLE_RECIPIENT);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<UserBean> getDonorsForHospital(int roleId, int hospitalId) {
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
                (
                    SELECT DATE_ADD(MAX(d.donation_date), INTERVAL 4 MONTH)
                    FROM donation d
                    JOIN donor_appointment da2 ON da2.id = d.donor_appointment_id
                    WHERE da2.user_id = u.id
                      AND da2.user_role_id = u.role_id
                      AND da2.hospital_id = ?
                ) AS valid_of_donation,
                (
                    SELECT d.status
                    FROM donation d
                    JOIN donor_appointment da3 ON da3.id = d.donor_appointment_id
                    WHERE da3.user_id = u.id
                      AND da3.user_role_id = u.role_id
                      AND da3.hospital_id = ?
                    ORDER BY d.donation_date DESC, d.donation_id DESC
                    LIMIT 1
                ) AS last_status
            FROM `user` u
            WHERE u.role_id = ?
              AND EXISTS (
                  SELECT 1
                  FROM donor_appointment da
                  WHERE da.user_id = u.id
                    AND da.user_role_id = u.role_id
                    AND da.hospital_id = ?
              )
            ORDER BY u.id DESC
            """;

        return jdbcTemplate.query(sql, (rs, rn) -> {
            UserBean u = new UserBean();
            u.setId(rs.getInt("id"));
            u.setUsername(rs.getString("username"));
            u.setEmail(rs.getString("email"));
            u.setPhone(rs.getString("phone"));
            u.setGender(rs.getString("gender"));
            u.setAddress(rs.getString("address"));
            u.setDateOfBirth(rs.getDate("dateofbirth") != null
                    ? rs.getDate("dateofbirth").toLocalDate()
                    : null);
            u.setBloodTypeId((Integer) rs.getObject("blood_type_id"));
            u.setRoleId(rs.getInt("role_id"));
            u.setStatus(rs.getString("last_status"));

            java.sql.Timestamp ts = rs.getTimestamp("valid_of_donation");
            u.setValidOfDonation(ts == null ? null
                    : ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return u;
        }, hospitalId, hospitalId, roleId, hospitalId);
    }
    
    // Count ALL donations made by a user (across all hospitals)
    public int countDonations(int userId) {
        String sql = """
            SELECT COUNT(*)
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
             WHERE da.user_id = ?
        """;
        Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return (cnt == null) ? 0 : cnt;
    }

    /** (Optional) Count donations by user limited to a hospital. */
    public int countDonationsByHospital(int userId, int hospitalId) {
        String sql = """
            SELECT COUNT(*)
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
             WHERE da.user_id = ?
               AND da.hospital_id = ?
        """;
        Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class, userId, hospitalId);
        return (cnt == null) ? 0 : cnt;
    }
    
    // Reduce blood units (used quantity) for a blood type at a hospital
    public int useBloodUnits(int bloodTypeId, int hospitalId, int usedUnits) {
        // First, calculate current available units
        String selectSql = """
            SELECT COALESCE(SUM(d.blood_unit), 0) 
            FROM donation d
            JOIN donor_appointment da ON da.id = d.donor_appointment_id
            WHERE d.status='Available' 
              AND da.hospital_id=? 
              AND da.user_id IN (
                  SELECT u.id FROM user u WHERE u.blood_type_id=?
              )
        """;
        Integer currentUnits = jdbcTemplate.queryForObject(selectSql, Integer.class, hospitalId, bloodTypeId);
        if (currentUnits == null || currentUnits <= 0) return 0;

        int toUse = Math.min(currentUnits, usedUnits);

        // Mark donations as used one by one until `toUse` units are consumed
        String selectDonations = """
                SELECT d.donation_id, d.blood_unit 
                FROM donation d
                JOIN donor_appointment da ON da.id = d.donor_appointment_id
                JOIN user u ON u.id = da.user_id
                WHERE d.status='Available' AND da.hospital_id=? AND u.blood_type_id=?
                ORDER BY d.donation_date ASC
            """;

        List<Map<String,Object>> donations = jdbcTemplate.queryForList(selectDonations, hospitalId, bloodTypeId);

        for (Map<String,Object> row : donations) {
            int donationId = (Integer) row.get("donation_id");
            int unit = (Integer) row.get("blood_unit");

            if (toUse <= 0) break;

            int reduce = Math.min(unit, toUse);

            // Update donation unit
            if (unit - reduce <= 0) {
                // All used → mark as Used
                jdbcTemplate.update("UPDATE donation SET status='Used', blood_unit=0 WHERE donation_id=?", donationId);
            } else {
                // Partial use → subtract units
                jdbcTemplate.update("UPDATE donation SET blood_unit=? WHERE donation_id=?", unit - reduce, donationId);
            }

            toUse -= reduce;
        }

        return usedUnits - toUse; // actual units reduced

    }
    
    public LocalDateTime getLastDonationDate(int userId, int roleId, int hospitalId) {
        String sql = """
            SELECT MAX(d.donation_date)
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
             WHERE da.user_id = ?
               AND da.user_role_id = ?
               AND da.hospital_id = ?
        """;
        Timestamp ts = jdbcTemplate.queryForObject(sql, Timestamp.class, userId, roleId, hospitalId);
        return (ts == null) ? null : ts.toLocalDateTime();
    }
    
    public static class UseResult {
          public int actualUsed;
          public java.util.List<Integer> donationIds = new java.util.ArrayList<>();
        }

        public UseResult useBloodUnitsDetailed(int bloodTypeId, int hospitalId, int usedUnits) {
          UseResult out = new UseResult();
          // (same selection you already have, ordered oldest-first)
          var rows = jdbcTemplate.queryForList("""
              SELECT d.donation_id, d.blood_unit
              FROM donation d
              JOIN donor_appointment da ON da.id=d.donor_appointment_id
              JOIN user u ON u.id=da.user_id
              WHERE d.status='Available' AND da.hospital_id=? AND u.blood_type_id=?
              ORDER BY d.donation_date ASC
          """, hospitalId, bloodTypeId);

          int remaining = usedUnits;
          for (var r : rows) {
            if (remaining <= 0) break;
            int donationId = (Integer) r.get("donation_id");
            int unit       = (Integer) r.get("blood_unit");
            int reduce     = Math.min(unit, remaining);

            if (unit - reduce <= 0) {
              jdbcTemplate.update("UPDATE donation SET status='Used', blood_unit=0 WHERE donation_id=?", donationId);
            } else {
              jdbcTemplate.update("UPDATE donation SET blood_unit=? WHERE donation_id=?", unit - reduce, donationId);
            }
            out.donationIds.add(donationId);
            out.actualUsed += reduce;
            remaining -= reduce;
          }
          return out;
        }
    
}
