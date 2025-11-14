package com.grppj.donateblood.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.Appointment;
import com.grppj.donateblood.model.AppointmentStatus;

@Repository
public class AppointmentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Insert or update appointment and update user's blood_type_id and hospital_id */
    public int doAppointment(Appointment ap) {
        // Get user's role_id from user table
        String getRoleSql = "SELECT role_id FROM user WHERE id = ?";
        Integer userRoleId = jdbcTemplate.queryForObject(getRoleSql, Integer.class, ap.getUserId());

        int rowsAffected = 0;

        if (ap.getId() == null || ap.getId() == 0) {
            // Insert appointment
            String sql = "INSERT INTO donor_appointment " +
                         "(date, time, created_at, status, user_id, user_role_id, hospital_id, blood_type_id) " +
                         "VALUES (?, ?, NOW(), ?, ?, ?, ?, ?)";
            rowsAffected = jdbcTemplate.update(sql,
                    ap.getDate(),
                    ap.getTime(),
                    ap.getStatus().name(), // lowercase enum
                    ap.getUserId(),
                    userRoleId,
                    ap.getHospitalId(),
                    ap.getBloodTypeId()
            );
        } else {
            // Update appointment
            String sql = "UPDATE donor_appointment SET date=?, time=?, created_at=NOW(), status=?, " +
                         "user_id=?, user_role_id=?, hospital_id=?, blood_type_id=? WHERE id=?";
            rowsAffected = jdbcTemplate.update(sql,
                    ap.getDate(),
                    ap.getTime(),
                    ap.getStatus().name(),
                    ap.getUserId(),
                    userRoleId,
                    ap.getHospitalId(),
                    ap.getBloodTypeId(),
                    ap.getId()
            );
        }

        // Update user's blood_type_id and hospital_id
        String updateUserSql = "UPDATE user SET hospital_id = ? WHERE id = ?";
        jdbcTemplate.update(updateUserSql, ap.getHospitalId(), ap.getUserId());

        return rowsAffected;
    }

    /** Get all appointments for a user */
    public List<Appointment> findByUserId(Integer userId) {
        String sql = "SELECT da.id, da.date, da.time, da.created_at, da.status, da.user_role_id, " +
                     "h.hospital_name, b.blood_type " +
                     "FROM donor_appointment da " +
                     "JOIN hospital h ON da.hospital_id = h.id " +
                     "JOIN blood_type b ON da.blood_type_id = b.id " +
                     "WHERE da.user_id = ? " +
                     "ORDER BY da.created_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId);
    }

    /** Get appointment by ID */
    public Appointment findById(Integer id) {
        String sql = "SELECT da.id, da.date, da.time, da.created_at, da.status, da.user_id, da.user_role_id, " +
                     "da.hospital_id, da.blood_type_id, h.hospital_name, b.blood_type " +
                     "FROM donor_appointment da " +
                     "JOIN hospital h ON da.hospital_id = h.id " +
                     "JOIN blood_type b ON da.blood_type_id = b.id " +
                     "WHERE da.id = ?";

        List<Appointment> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Appointment a = mapRow(rs);
            a.setUserId(rs.getInt("user_id"));
            a.setHospitalId(rs.getInt("hospital_id"));
            a.setBloodTypeId(rs.getInt("blood_type_id"));
            return a;
        }, id);

        return list.isEmpty() ? null : list.get(0);
    }

    /** Delete appointment by ID */
    public int deleteById(Integer id) {
        String sql = "DELETE FROM donor_appointment WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    /** Update date, time, blood type, hospital only (also update user) */
    public int update(Appointment a) {
        String sql = "UPDATE donor_appointment SET date=?, time=?, blood_type_id=?, hospital_id=? WHERE id=?";
        int rowsAffected = jdbcTemplate.update(sql,
            Date.valueOf(a.getDate()),
            a.getTime(),
            a.getBloodTypeId(),
            a.getHospitalId(),
            a.getId()
        );

        // Update user's blood_type_id and hospital_id
        String updateUserSql = "UPDATE user SET blood_type_id = ?, hospital_id = ? WHERE id = ?";
        jdbcTemplate.update(updateUserSql, a.getBloodTypeId(), a.getHospitalId(), a.getUserId());

        return rowsAffected;
    }

    /** Helper: Map ResultSet to Appointment object */
    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getInt("id"));
        a.setDate(rs.getDate("date").toLocalDate());
        a.setTime(rs.getString("time"));

        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            a.setCreatedAt(createdAtTimestamp.toLocalDateTime());
        }

        // ✅ Normalize DB status to lowercase to match enum
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            a.setStatus(AppointmentStatus.valueOf(statusStr.trim().toLowerCase()));
        }

        if (columnExists(rs, "user_role_id")) {
            a.setUserRoleId(rs.getInt("user_role_id"));
        }

        a.setHospitalName(rs.getString("hospital_name"));
        a.setBloodTypeName(rs.getString("blood_type"));
        return a;
    }

    /** Check if column exists in ResultSet (optional utility) */
    private boolean columnExists(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public LocalDate findLastDonationDateByUserId(Integer userId) {
 	   String sql = "SELECT MAX(date) As last_donation " + 
                      "FROM donor_appointment " + 
                    "WHERE user_id = ? AND status = 'completed'";
 	   return jdbcTemplate.query(sql, rs->{
 		   if(rs.next()) {
 			   Date date = rs.getDate("last_donation");
 			   return (date != null) ? date.toLocalDate():null;
 		   }
 		   return null;
 	   },userId);
 	   
    }

}
