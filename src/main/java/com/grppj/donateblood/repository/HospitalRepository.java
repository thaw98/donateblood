package com.grppj.donateblood.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.grppj.donateblood.model.Hospital;

@Repository
public class HospitalRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Hospital> findAll() { 
        String sql = "SELECT * FROM hospital"; 
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToHospital(rs)); 
    }

    public boolean addHospital(Hospital hospital) {
        String sql = "INSERT INTO hospital (hospital_name, address, phoneNo, gmail, profile_picture, admin_id) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            int rows = jdbcTemplate.update(sql,
                    hospital.getHospitalName(),
                    hospital.getAddress(),
                    hospital.getPhoneNo(),
                    hospital.getGmail(),
                    hospital.getProfilePicture(), // Direct string from model
                    hospital.getAdminId()
            );
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Hospital> findAllHospitals() {
        String sql = "SELECT * FROM hospital ORDER BY id DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToHospital(rs));
    }

    public Hospital findById(int id) {
        try {
            String sql = "SELECT * FROM hospital WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRowToHospital(rs), id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteHospital(int id) {
        String sql = "DELETE FROM hospital WHERE id = ?";
        try {
            int rows = jdbcTemplate.update(sql, id);
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String findNameById(int hospitalId) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT hospital_name FROM hospital WHERE id = ?",
                String.class,
                hospitalId
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }
    
 // Count total hospitals (used for Donation Centers stat)
    public int countHospitals() {
        String sql = "SELECT COUNT(*) FROM hospital";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }


    public boolean updateHospital(int id, Hospital hospital) {
        try {
            String sql = "UPDATE hospital SET hospital_name = ?, address = ?, phoneNo = ?, gmail = ?, profile_picture = ?, admin_id = ? WHERE id = ?";
            int rows = jdbcTemplate.update(sql,
                    hospital.getHospitalName(),
                    hospital.getAddress(),
                    hospital.getPhoneNo(),
                    hospital.getGmail(),
                    hospital.getProfilePicture(), // Direct string from model
                    hospital.getAdminId(),
                    id
            );
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Hospital mapRowToHospital(java.sql.ResultSet rs) throws java.sql.SQLException {
        Hospital hospital = new Hospital();
        hospital.setId(rs.getInt("id"));
        hospital.setHospitalName(rs.getString("hospital_name"));
        hospital.setAddress(rs.getString("address"));
        hospital.setPhoneNo(rs.getString("phoneNo"));
        hospital.setGmail(rs.getString("gmail"));
        hospital.setProfilePicture(rs.getString("profile_picture"));
        hospital.setAdminId(rs.getInt("admin_id"));
        return hospital;
    }
}