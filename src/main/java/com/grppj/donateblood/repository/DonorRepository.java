package com.grppj.donateblood.repository;

import java.util.List;

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
    public int addDonation(DonationBean donation) {
	    	String sql = "INSERT INTO donation (blood_unit, donation_date, status, user_id, user_role_id, hospital_id) VALUES (?, ?, ?, ?, ?, ?)";
	    	return jdbcTemplate.update(sql,
	    	    donation.getBloodUnit(),
	    	    donation.getDonationDate(),
	    	    donation.getStatus(),
	    	    donation.getUserId(),
	    	    donation.getUserRoleId(),
	    	    donation.getHospitalId()
	    	);

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
    public List<UserBean> getAllDonorsWithStatus() {
        String sql = """
        	      SELECT u.id, u.username, u.email, u.password, u.gender, u.dateofbirth, u.address,
        	             u.phone, u.role_id, u.blood_type_id,
        	             d.status,
        	             CASE
        	               WHEN x.last_date IS NOT NULL THEN DATE_ADD(x.last_date, INTERVAL 4 MONTH)
        	               ELSE NULL
        	             END AS next_eligible
        	        FROM user u
        	        LEFT JOIN (
        	          SELECT user_id, user_role_id, MAX(donation_date) AS last_date
        	          FROM donation
        	          GROUP BY user_id, user_role_id
        	        ) x
        	          ON x.user_id = u.id AND x.user_role_id = u.role_id
        	        LEFT JOIN donation d
        	          ON d.user_id = x.user_id
        	         AND d.user_role_id = x.user_role_id
        	         AND d.donation_date = x.last_date
        	       WHERE u.role_id = 2
        	         AND x.last_date IS NOT NULL      -- ⬅️ exclude users with no donation
        	       ORDER BY u.id DESC
        	    """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserBean user = new UserBean();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setGender(rs.getString("gender"));
            user.setDateOfBirth(rs.getString("dateofbirth"));
            user.setAddress(rs.getString("address"));
            user.setPhone(rs.getString("phone"));
            user.setRoleId(rs.getInt("role_id"));
            user.setStatus(rs.getString("status"));
            user.setValidOfDonation(rs.getString("next_eligible")); 
            Object bt = rs.getObject("blood_type_id");
            user.setBloodTypeId(bt == null ? null : rs.getInt("blood_type_id")); // <-- add
            user.setValidOfDonation(rs.getString("next_eligible"));
            return user;
        });
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
    
 // latest donation for this user (most recent by donation_date)
    public java.util.Map<String, Object> findLatestDonation(int userId, int roleId) {
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

    // Only change if it's currently Available
    public int markDonationUsed(int donationId) {
      String sql = "UPDATE donation SET status='Used' WHERE donation_id=? AND status='Available'";
      return jdbcTemplate.update(sql, donationId);
    }


}
