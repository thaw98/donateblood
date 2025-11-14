package com.grppj.donateblood.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.RoleBean;
import com.grppj.donateblood.model.User;

@Repository
public class LoginRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<User> loginUser(String email, String password) {
        String sql = "SELECT u.id, u.username, u.email, u.password, u.gender, u.dateofbirth, u.address, u.image, " +
                     "r.id AS role_id, r.role, u.blood_type_id, u.is_verified " +
                     "FROM user u JOIN role r ON u.role_id = r.id " +
                     "WHERE u.email = ? AND u.password = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            RoleBean role = new RoleBean();
            role.setId(rs.getInt("role_id"));
            role.setRole(rs.getString("role"));

            User user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setGender(rs.getString("gender"));

            var dob = rs.getDate("dateofbirth");
            user.setDateofbirth(dob != null ? dob.toLocalDate() : null);

            user.setAddress(rs.getString("address"));
            user.setRole(role);
            user.setBloodTypeId(rs.getInt("blood_type_id"));
            
            user.setVerified(rs.getBoolean("is_verified"));

            // ✅ Load profile image bytes
            user.setImageBytes(rs.getBytes("image"));

            return user;
        }, email, password);
    }


    /** Returns this admin user's hospital_id, or null if not mapped. */
    public Integer findHospitalIdForAdmin(int userId) {
        // 1) Try direct column on user
        try {
            Integer hid = jdbcTemplate.queryForObject(
                "SELECT hospital_id FROM `user` WHERE id = ?",
                Integer.class,
                userId
            );
            if (hid != null) return hid;
        } catch (Exception ignore) {
            // continue to mapping
        }

        // 2) Try mapping table (rename if yours differs)
        try {
            return jdbcTemplate.queryForObject(
                "SELECT hospital_id FROM hospital_admin WHERE user_id = ? ORDER BY id DESC LIMIT 1",
                Integer.class,
                userId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }
}
