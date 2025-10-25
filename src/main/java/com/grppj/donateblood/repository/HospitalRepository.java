package com.grppj.donateblood.repository;

import java.util.List;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grppj.donateblood.model.Hospital;

@Repository
public class HospitalRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Hospital> findAll() {
        String sql = "SELECT * FROM hospital";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Hospital h = new Hospital();
            h.setId(rs.getInt("id"));
            h.setHospitalName(rs.getString("hospital_name"));
            h.setAddress(rs.getString("address"));
            h.setPhoneNo(rs.getString("phoneNo"));
            h.setGmail(rs.getString("gmail"));
            
            // ✅ FIXED: Added profile pictures handling
            String profilePicturesJson = rs.getString("profile_picture");
            if (profilePicturesJson != null) {
                try {
                    List<String> pictures = objectMapper.readValue(profilePicturesJson, 
                        new TypeReference<List<String>>() {});
                    h.setProfilePictures(pictures);
                } catch (Exception e) {
                    h.setProfilePictures(List.of());
                }
            } else {
                h.setProfilePictures(List.of());
            }
            
            // ✅ FIXED: Added admin_id
            h.setCreatedBy(rs.getLong("admin_id"));
            
            return h;
        });
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
    
    public boolean addHospital(Hospital hospital) {
        String sql = "INSERT INTO hospital (hospital_name, address, phoneNo, gmail, profile_picture, admin_id) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            String profilePictures = hospital.getProfilePictures() != null ? 
                objectMapper.writeValueAsString(hospital.getProfilePictures()) : null;
            
            int rowsAffected = jdbcTemplate.update(sql, 
                hospital.getHospitalName(), 
                hospital.getAddress(), 
                hospital.getPhoneNo(),
                hospital.getGmail(),
                profilePictures,
                hospital.getCreatedBy() != null ? hospital.getCreatedBy() : 0L);
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Hospital> findAllHospitals() {
        String sql = "SELECT * FROM hospital ORDER BY id DESC";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Hospital hospital = new Hospital();
                hospital.setId(rs.getInt("id"));
                hospital.setHospitalName(rs.getString("hospital_name"));
                hospital.setAddress(rs.getString("address"));
                hospital.setPhoneNo(rs.getString("phoneNo"));
                hospital.setGmail(rs.getString("gmail"));
                
                String profilePicturesJson = rs.getString("profile_picture");
                if (profilePicturesJson != null) {
                    try {
                        List<String> pictures = objectMapper.readValue(profilePicturesJson, 
                            new TypeReference<List<String>>() {});
                        hospital.setProfilePictures(pictures);
                    } catch (Exception e) {
                        hospital.setProfilePictures(List.of());
                    }
                } else {
                    hospital.setProfilePictures(List.of());
                }
                
                hospital.setCreatedBy(rs.getLong("admin_id"));
                return hospital;
            });
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
    
    public boolean deleteHospital(int id) {
        String sql = "DELETE FROM hospital WHERE id = ?";
        try {
            int rowsAffected = jdbcTemplate.update(sql, id);
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public Hospital findById(int id) {
        try {
            String sql = "SELECT * FROM hospital WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Hospital hospital = new Hospital();
                hospital.setId(rs.getInt("id"));
                hospital.setHospitalName(rs.getString("hospital_name"));
                hospital.setAddress(rs.getString("address"));
                hospital.setPhoneNo(rs.getString("phoneNo"));
                hospital.setGmail(rs.getString("gmail"));
                
                String profilePicturesJson = rs.getString("profile_picture");
                if (profilePicturesJson != null) {
                    try {
                        List<String> pictures = objectMapper.readValue(profilePicturesJson, 
                            new TypeReference<List<String>>() {});
                        hospital.setProfilePictures(pictures);
                    } catch (Exception e) {
                        hospital.setProfilePictures(List.of());
                    }
                } else {
                    hospital.setProfilePictures(List.of());
                }
                
                hospital.setCreatedBy(rs.getLong("admin_id"));
                return hospital;
            }, id);
        } catch (Exception e) {
            return null;
        }
    }
}