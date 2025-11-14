package com.grppj.donateblood.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.grppj.donateblood.model.RoleBean;
import com.grppj.donateblood.model.User;
import com.grppj.donateblood.model.UserAppointmentView;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // =================== GENERAL USER METHODS ===================

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM user WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    public int doRegister(User user) {
        String sql = "INSERT INTO user (username, email, password, gender, dateofbirth, address, phone, role_id,blood_type_id,image) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Integer roleId = user.getRole() != null ? user.getRole().getId() : 1;

        byte[] imageBytes = null;
        try {
            if (user.getFilePart() != null && !user.getFilePart().isEmpty()) {
                imageBytes = user.getFilePart().getBytes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jdbcTemplate.update(sql,
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getGender(),
                user.getDateofbirth(),
                user.getAddress(),
                user.getPhone(),
                roleId,
                user.getBloodTypeId(),
                imageBytes
        );
    }

    public List<RoleBean> getAllRoles() {
        String sql = "SELECT id, role FROM role";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            RoleBean role = new RoleBean();
            role.setId(rs.getInt("id"));
            role.setRole(rs.getString("role"));
            return role;
        });
    }

    public RoleBean getRoleById(int id) {
        try {
            String sql = "SELECT id, role FROM role WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                RoleBean role = new RoleBean();
                role.setId(rs.getInt("id"));
                role.setRole(rs.getString("role"));
                return role;
            }, id);
        } catch (Exception e) {
            return null;
        }
    }

    public User getUserById(int id) {
        String sql = "SELECT u.id, u.username, u.email, u.password, u.gender, u.dateofbirth, u.address, u.phone, u.image, r.id as role_id, r.role, u.blood_type_id " +
                     "FROM user u JOIN role r ON u.role_id = r.id WHERE u.id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setGender(rs.getString("gender"));
            if (rs.getDate("dateofbirth") != null)
                user.setDateofbirth(rs.getDate("dateofbirth").toLocalDate());
            user.setAddress(rs.getString("address"));
            user.setPhone(rs.getString("phone"));
            user.setRole(new RoleBean(rs.getInt("role_id"), rs.getString("role")));
            user.setBloodTypeId(rs.getInt("blood_type_id"));
            user.setImageBytes(rs.getBytes("image"));
            return user;
        }, id);
    }

    public User getUserByEmail(String email) {
        try {
            String sql = "SELECT u.id, u.username, u.email, u.password, u.gender, u.dateofbirth, u.address, u.phone, u.image, r.id as role_id, r.role, u.hospital_id, u.auto_generated_code " +
                         "FROM user u JOIN role r ON u.role_id = r.id WHERE u.email = ?";
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setGender(rs.getString("gender"));
                if (rs.getDate("dateofbirth") != null)
                    user.setDateofbirth(rs.getDate("dateofbirth").toLocalDate());
                user.setAddress(rs.getString("address"));
                user.setPhone(rs.getString("phone"));
                user.setRole(new RoleBean(rs.getInt("role_id"), rs.getString("role")));
                user.setHospitalId(rs.getInt("hospital_id"));
                user.setImageBytes(rs.getBytes("image"));
                user.setAutoGeneratedCode(rs.getString("auto_generated_code"));
                return user;
            }, email);
        } catch (Exception e) {
            return null;
        }
    }
    
    

    public int updateUser(User user) {
        String sql = "UPDATE user SET username = ?, email = ?, password = ?, address = ?, phone = ?, image = ? WHERE id = ?";
        return jdbcTemplate.update(sql,
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.getAddress(),
            user.getPhone(),
            user.getImageBytes(),
            user.getId()
        );
    }

    public int updateUserPassword(int userId, String newPassword) {
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        return jdbcTemplate.update(sql, newPassword, userId);
    }
    
    public int updateUserName(int userId, String username) {
        String sql = "UPDATE user SET username = ? WHERE id = ?";
        return jdbcTemplate.update(sql, username, userId);
    }

    /** NEW: change only the display name */
    public int updateAdminName(int userId, String newName) {
        String sql = "UPDATE user SET username = ? WHERE id = ?";
        return jdbcTemplate.update(sql, newName, userId);
    }

    public int updateUserImage(int userId, byte[] imageBytes) {
        if (imageBytes == null) return 0;
        String sql = "UPDATE user SET image = ? WHERE id = ?";
        return jdbcTemplate.update(sql, imageBytes, userId);
    }
    
    public long countAllUsers() {
        try {
            String sql = "SELECT COUNT(*) FROM user";
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // =================== ADMIN-SPECIFIC METHODS ===================

    public User findAdminByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ? AND role_id = 2";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                User admin = new User();
                admin.setId(rs.getInt("id"));
                admin.setUsername(rs.getString("username"));
                admin.setEmail(rs.getString("email"));
                admin.setPassword(rs.getString("password"));
                admin.setHospitalId(rs.getInt("hospital_id"));
                admin.setAutoGeneratedCode(rs.getString("auto_generated_code"));
                return admin;
            }, email);
        } catch (Exception e) {
            return null;
        }
    }

    public List<User> findAllAdmins() {
        String sql = "SELECT * FROM user WHERE role_id = 2";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User admin = new User();
            admin.setId(rs.getInt("id"));
            admin.setUsername(rs.getString("username"));
            admin.setEmail(rs.getString("email"));
            admin.setHospitalId(rs.getInt("hospital_id"));
            admin.setVerified(rs.getBoolean("is_verified"));
            return admin;
        });
    }

    public boolean createAdmin(String username, String email, String gender, String dateOfBirth,
            String address, int bloodTypeId, String phone,
            Integer hospitalId, String otpCode) {
    	String sql = "INSERT INTO user (username, email, gender, dateofbirth, address, blood_type_id, phone, role_id, hospital_id, auto_generated_code) " +
  "VALUES (?, ?, ?, ?, ?, ?, ?, 2, ?, ?)";
return jdbcTemplate.update(sql, username, email, gender, dateOfBirth, address,
                bloodTypeId, phone, hospitalId, otpCode) > 0;
}


    public boolean saveOTP(String email, String code) {
        String sql = "UPDATE user SET auto_generated_code = ? WHERE email = ? AND role_id = 2";
        return jdbcTemplate.update(sql, code, email) > 0;
    }

    public boolean verifyAdminOTP(String email, String otp) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ? AND auto_generated_code = ? AND role_id = 2";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, otp);
        return count != null && count > 0;
    }

    public boolean setAdminPassword(int adminId, String newPassword) {
        String sql = "UPDATE user SET password = ?, auto_generated_code = NULL, is_verified = 1 WHERE id = ? AND role_id = 2";
        return jdbcTemplate.update(sql, newPassword, adminId) > 0;
    }

    public User adminLogin(String email, String password) {
        String sql = "SELECT * FROM user WHERE email = ? AND password = ? AND role_id = 2";
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                User admin = new User();
                admin.setId(rs.getInt("id"));
                admin.setUsername(rs.getString("username"));
                admin.setEmail(rs.getString("email"));
                admin.setPassword(rs.getString("password"));
                admin.setHospitalId(rs.getInt("hospital_id"));
                return admin;
            }, email, password);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean assignAdminToHospital(int adminId, int hospitalId) {
        String sql = "UPDATE user SET hospital_id = ? WHERE id = ? AND role_id = 2";
        return jdbcTemplate.update(sql, hospitalId, adminId) > 0;
    }

    public boolean unassignAdminFromHospital(int adminId) {
        String sql = "UPDATE user SET hospital_id = NULL WHERE id = ? AND role_id = 2";
        return jdbcTemplate.update(sql, adminId) > 0;
    }

    public boolean deleteAdmin(int adminId) {
        String sql = "DELETE FROM user WHERE id = ? AND role_id = 2";
        return jdbcTemplate.update(sql, adminId) > 0;
    }
    
    public List<User> findAdminsByHospital(Integer hospitalId) {
        String sql = "SELECT u.* FROM user u WHERE u.hospital_id = ? AND u.role_id = 2";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setHospitalId(rs.getInt("hospital_id"));
                return user;
            }, hospitalId);
        } catch (Exception e) {
            return List.of();
        }
    }
    
    public boolean updateUserVerificationStatus(int userId, boolean isVerified) {
        String sql = "UPDATE user SET is_verified = ? WHERE id = ? AND role_id = 2";
        try {
            int rowsAffected = jdbcTemplate.update(sql, isVerified, userId);
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public List<User> findAllAdmin() {
        String sql = "SELECT id, username, email, hospital_id, is_verified FROM user WHERE role_id = 2";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User admin = new User();
            admin.setId(rs.getInt("id"));
            admin.setUsername(rs.getString("username"));
            admin.setEmail(rs.getString("email"));
            
            // Handle NULL hospital_id properly
            int hospitalId = rs.getInt("hospital_id");
            admin.setHospitalId(rs.wasNull() ? null : hospitalId);
            
            admin.setVerified(rs.getBoolean("is_verified")); // Use setVerified
            return admin;
        });
    }

    /**
     * Gets a list of all Donors and Receivers for the Super Admin Dashboard.
     */
    public List<UserAppointmentView> findAllUserAppointments() {
        String sql = "SELECT u.id as userId, u.username, u.email, u.phone, u.gender, " +
                     "u.dateofbirth, u.address, r.role as userType, bt.blood_type as bloodType, " +
                     "h.hospital_name, u.image " +
                     "FROM user u " +
                     "JOIN role r ON u.role_id = r.id " +
                     "LEFT JOIN blood_type bt ON u.blood_type_id = bt.id " +
                     "LEFT JOIN hospital h ON u.hospital_id = h.id " +
                     "WHERE r.role = 'DONOR' OR r.role = 'RECEIVER' " +
                     "ORDER BY u.id DESC " +
                     "LIMIT 10";

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapToUserAppointmentView(rs));
    }

    /**
     * Gets a list of Donors and Receivers for a specific hospital.
     */
    public List<UserAppointmentView> findUserAppointmentsByHospitalId(Integer hospitalId) {
        String sql = "SELECT u.id as userId, u.username, u.email, u.phone, u.gender, " +
                     "u.dateofbirth, u.address, r.role as userType, bt.blood_type as bloodType, " +
                     "h.hospital_name, u.image " +
                     "FROM user u " +
                     "JOIN role r ON u.role_id = r.id " +
                     "LEFT JOIN blood_type bt ON u.blood_type_id = bt.id " +
                     "LEFT JOIN hospital h ON u.hospital_id = h.id " +
                     "WHERE u.hospital_id = ? AND (r.role = 'DONOR' OR r.role = 'RECEIVER') " +
                     "ORDER BY u.id DESC " +
                     "LIMIT 10";

        return jdbcTemplate.query(sql, new Object[]{hospitalId}, (rs, rowNum) -> mapToUserAppointmentView(rs));
    }

    // Helper method to map a row to UserAppointmentView object
    private UserAppointmentView mapToUserAppointmentView(ResultSet rs) throws SQLException {
        UserAppointmentView view = new UserAppointmentView();
        view.setUserId(rs.getInt("userId"));
        view.setUsername(rs.getString("username"));
        view.setEmail(rs.getString("email"));
        view.setPhone(rs.getString("phone"));
        view.setGender(rs.getString("gender"));

        if (rs.getDate("dateofbirth") != null) {
            view.setDateofbirth(rs.getDate("dateofbirth").toLocalDate());
        }

        view.setAddress(rs.getString("address"));
        view.setUserType(rs.getString("userType"));
        view.setBloodType(rs.getString("bloodType"));
        view.setHospitalName(rs.getString("hospital_name"));
        view.setImageBytes(rs.getBytes("image"));

        // These are not part of this specific query, so we set them to null/default
        view.setAppointmentId(null);
        view.setAppointmentDate(null);
        view.setAppointmentStatus("NO APPOINTMENT");

        return view;
    }
    
    public User findAdminByEmails(String email) {
        String sql = """
            SELECT 
                id, username, email, hospital_id, is_verified,
                gender, date_of_birth, address, blood_type_id, phone
            FROM user
            WHERE role_id = 2 AND email = ?
        """;

        return jdbcTemplate.queryForObject(sql, new Object[]{email}, (rs, rowNum) -> {
            User admin = new User();
            admin.setId(rs.getInt("id"));
            admin.setUsername(rs.getString("username"));
            admin.setEmail(rs.getString("email"));

            int hospitalId = rs.getInt("hospital_id");
            admin.setHospitalId(rs.wasNull() ? null : hospitalId);

            admin.setVerified(rs.getBoolean("is_verified"));
            admin.setGender(rs.getString("gender"));

            java.sql.Date dob = rs.getDate("date_of_birth");
            if (dob != null) {
                admin.setDateofbirth(dob.toLocalDate());
            }

            admin.setAddress(rs.getString("address"));
            admin.setBloodTypeId(rs.getInt("blood_type_id"));
            admin.setPhone(rs.getString("phone"));

            return admin;
        });
    }

}
