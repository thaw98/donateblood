package com.grppj.donateblood.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.BloodTypeBean;
import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.model.HospitalBean;
import com.grppj.donateblood.model.UserBean;

@Repository
public class DonorRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Add a new user (donor)
    public int addDonor(UserBean user) {
    	   	String sql = "INSERT INTO user (username, email, password, gender, dateofbirth, address, phone, role_id, donate_again) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    	    jdbcTemplate.update(sql, user.getUsername(), user.getEmail(), user.getPassword(), user.getGender(), user.getDateOfBirth(), user.getAddress(), user.getPhone(), user.getRoleId(), user.getDonateAgain());
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
        String sql = "SELECT * FROM user WHERE role_id = 2"; // assumes 2 is donor role
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserBean user = new UserBean();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPhone(rs.getString("phone"));
            user.setGender(rs.getString("gender"));
            user.setDateOfBirth(rs.getString("dateofbirth"));
            user.setAddress(rs.getString("address"));
            user.setDonateAgain(rs.getInt("donate_again"));
            // ... add any other needed fields
            return user;
        });
    // More methods for get/update can be added as needed using same pattern
    }
    public List<UserBean> getAllDonorsWithStatus() {
        String sql = "SELECT u.*, d.status FROM user u " +
                     "LEFT JOIN donation d ON u.id = d.user_id " +
                     "WHERE u.role_id = 2";
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
            user.setStatus(rs.getString("status")); // Add this setter in UserBean
            user.setDonateAgain(rs.getInt("donate_again"));
            return user;
        });
    }
}
