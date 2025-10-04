package com.grppj.donateblood.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import  com.grppj.donateblood.model.UserBean;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<UserBean> getAllUsers() {
        String sql = "SELECT * FROM user";
        List<UserBean> list = jdbcTemplate.query(
            sql,
            (rs, rowNumber) -> new UserBean(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("gender"),
                rs.getString("dateofbirth"),
                rs.getString("address"),
                rs.getInt("role_id"),
                rs.getTimestamp("created_at") != null ? 
                    rs.getTimestamp("created_at").toLocalDateTime() : null
            )
        );
        return list;
    }

    public UserBean getUserById(Integer userId) {
        String sql = "SELECT * FROM user WHERE id = ?";
        UserBean obj = jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new UserBean(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("gender"),
                rs.getString("dateofbirth"),
                rs.getString("address"),
                rs.getInt("role_id"),
                rs.getTimestamp("created_at") != null ? 
                    rs.getTimestamp("created_at").toLocalDateTime() : null
            ),
            userId
        );
        return obj;
    }

    public UserBean getUserByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        UserBean obj = jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new UserBean(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("gender"),
                rs.getString("dateofbirth"),
                rs.getString("address"),
                rs.getInt("role_id"),
                rs.getTimestamp("created_at") != null ? 
                    rs.getTimestamp("created_at").toLocalDateTime() : null
            ),
            email
        );
        return obj;
    }

    public int saveUser(UserBean user) {
        String sql = "INSERT INTO user (username, email, password, gender, dateofbirth, address, role_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql, user.getUsername(), user.getEmail(), 
            user.getPassword(), user.getGender(), user.getDateofbirth(), 
            user.getAddress(), user.getRoleId(), LocalDateTime.now());
    }

    public Integer getDonorRoleId() {
        String sql = "SELECT id FROM role WHERE role = 'Donor'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}