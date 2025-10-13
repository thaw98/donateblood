package com.grppj.donateblood.repository;

import java.sql.Timestamp;
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

    // Add a new user (donor)
    public int addDonor(UserBean user) {
    	   	String sql = "INSERT INTO user (username, email, password, gender, dateofbirth, address, phone, blood_type_id, role_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
        // Get the new user's id for use in donation
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }


    // Add a donation
    public int addDonation(DonationBean d) {
        // 1) insert donation (your current code)
        String ins = """
            INSERT INTO donation (blood_unit, donation_date, status, user_id, user_role_id, hospital_id)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
            ins,
            d.getBloodUnit(),
            d.getDonationDate(),   // "yyyy-MM-dd HH:mm:ss"
            d.getStatus(),
            d.getUserId(),
            d.getUserRoleId(),
            d.getHospitalId()
        );

        // 2) persist Next Eligible -> user.valid_of_donation
        String upd = """
            UPDATE `user`
               SET valid_of_donation =
                   DATE_ADD(STR_TO_DATE(?, '%Y-%m-%d %H:%i:%s'), INTERVAL 120 DAY)
             WHERE id = ?
               AND role_id IN (2,3)   -- donors (use your role ids)
        """;
        jdbcTemplate.update(upd, d.getDonationDate(), d.getUserId());

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public List<UserBean> getAllDonors() {
        String sql = "SELECT * FROM user WHERE role_id = 2"; // Donor role is 2 (this is from my side, just notes for code combining)
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserBean user = new UserBean();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPhone(rs.getString("phone"));
            user.setGender(rs.getString("gender"));
            user.setDateOfBirth(rs.getString("dateofbirth"));
            user.setAddress(rs.getString("address"));
            user.setValidOfDonation(rs.getString("next_eligible")); 
            Object bt = rs.getObject("blood_type_id");
            user.setBloodTypeId(bt == null ? null : rs.getInt("blood_type_id"));
            user.setValidOfDonation(rs.getString("next_eligible"));        // <- use computed value
            return user;
        });
    }
    public List<UserBean> getAllDonorsWithStatus(int roleId) {
        String sql = """
            SELECT u.id, u.username, u.email, u.phone, u.gender, u.address, u.dateofbirth,
                   u.blood_type_id, u.role_id,
                   u.valid_of_donation,
                   (
                       SELECT d.status
                         FROM donation d
                        WHERE d.user_id = u.id
                          AND d.user_role_id = u.role_id
                        ORDER BY d.donation_date DESC, d.donation_id DESC
                        LIMIT 1
                   ) AS last_status
              FROM `user` u
             WHERE u.role_id = ?
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
            u.setStatus(rs.getString("last_status")); // may be null

            Timestamp ts = rs.getTimestamp("valid_of_donation");
            u.setValidOfDonation(ts == null ? null :
                ts.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return u;
        }, roleId);
    }

    
    public UserBean getDonorById(int id) {
        String sql = """
          SELECT u.id, u.username, u.email, u.password, u.gender, u.dateofbirth, u.address,
                 u.phone, u.role_id, u.blood_type_id,
                 d.status,
                 h.hospital_name,
                 DATE_ADD(d.donation_date, INTERVAL 4 MONTH) AS next_eligible
            FROM user u
            LEFT JOIN (
                SELECT d1.*
                  FROM donation d1
                  JOIN (
                    SELECT user_id, user_role_id, MAX(donation_date) AS last_date
                      FROM donation
                     GROUP BY user_id, user_role_id
                  ) x ON x.user_id = d1.user_id
                     AND x.user_role_id = d1.user_role_id
                     AND x.last_date = d1.donation_date
            ) d ON d.user_id = u.id AND d.user_role_id = u.role_id
            LEFT JOIN hospital h ON d.hospital_id = h.id
           WHERE u.id = ?
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
            user.setBloodTypeId(bt == null ? null : rs.getInt("blood_type_id")); // <-- add
            user.setValidOfDonation(rs.getString("next_eligible")); // ⬅️ use computed value
            return user;
        });
    }
    
    public int updateDonor(UserBean donor) {
        // Update phone and donate_again in user table
        String sqlUser = "UPDATE user SET phone = ? WHERE id = ?";
        int rowsUser = jdbcTemplate.update(sqlUser,
            donor.getPhone(),
            donor.getId()
        );

        // Update status in donation table
        String sqlDonation = "UPDATE donation SET status = ? WHERE user_id = ?";
        int rowsDonation = jdbcTemplate.update(sqlDonation,
            donor.getStatus(),
            donor.getId()
        );

        // return total number of rows affected
        return rowsUser + rowsDonation;
    }
    
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE LOWER(email) = LOWER(?)";
        Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return cnt != null && cnt > 0;
    }
    
    public java.time.LocalDateTime getNextValidDonation(int userId, int roleId) {
        String sql = """
            SELECT DATE_ADD(MAX(donation_date), INTERVAL 4 MONTH) AS next_eligible
              FROM donation
             WHERE user_id = ? AND user_role_id = ?
        """;
        java.sql.Timestamp ts = jdbcTemplate.queryForObject(sql, java.sql.Timestamp.class, userId, roleId);
        return (ts == null) ? null : ts.toLocalDateTime();
    }


    public boolean canDonateNow(int userId, int roleId) {
        java.time.LocalDateTime next = getNextValidDonation(userId, roleId);
        return next == null || !next.isAfter(java.time.LocalDateTime.now());
    }

    public int updateNextValidDonation(int userId, int roleId, String donationDate) {
        String sql = "UPDATE user SET valid_of_donation = DATE_ADD(?, INTERVAL 4 MONTH) WHERE id=? AND role_id=?";
        return jdbcTemplate.update(sql, donationDate, userId, roleId);
    }
    

    public Map<String, Object> findLatestDonation(int userId, int roleId) {
        String sql = """
            SELECT donation_id, hospital_id, user_id, user_role_id, blood_unit, status
              FROM donation
             WHERE user_id=? AND user_role_id=?
             ORDER BY donation_date DESC
             LIMIT 1
        """;
        var list = jdbcTemplate.queryForList(sql, userId, roleId);
        return list.isEmpty() ? null : list.get(0);
    }
    
 // Latest AVAILABLE donation id for this user (if any)
    public Integer findLatestAvailableDonationId(int userId, int roleId) {
        String sql = """
            SELECT donation_id
              FROM donation
             WHERE user_id=? AND user_role_id=? AND status='Available'
             ORDER BY donation_date DESC
             LIMIT 1
        """;
        java.util.List<Integer> ids = jdbcTemplate.query(sql, (rs, rn) -> rs.getInt(1), userId, roleId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    // Flip one donation from Available -> Used (idempotent)
    public int markDonationUsed(int donationId) {
        String sql = "UPDATE donation SET status='Used' WHERE donation_id=? AND status='Available'";
        return jdbcTemplate.update(sql, donationId);
    }
    
 // Stock derived from donation table for one hospital
    public java.util.List<StockRow> getStockFromDonationsByHospital(int hospitalId) {
        String sql = """
            SELECT bt.id AS blood_type_id,
                   bt.blood_type,
                   COALESCE(SUM(CASE WHEN d.status='Available' THEN d.blood_unit ELSE 0 END), 0) AS units,
                   MAX(d.donation_date) AS last_donation
              FROM blood_type bt
              LEFT JOIN user u
                ON u.blood_type_id = bt.id
              LEFT JOIN donation d
                ON d.user_id = u.id
               AND d.user_role_id = u.role_id
               AND d.hospital_id = ?
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
    
 // inside DonorRepository
    public Integer findUserRoleId(int userId) {
        // `user` is a reserved word in MySQL → keep the backticks
        String sql = "SELECT role_id FROM `user` WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId);
    }


    // simple DTO for the stock view
    public static class StockRow {
        public Integer bloodTypeId;
        public String  bloodType;
        public Integer units;
        public java.sql.Timestamp lastDonation;
    }

}
