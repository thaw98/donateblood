package com.grppj.donateblood.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.BloodTypeBean;

@Repository
public class BloodTypeRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Get all blood types
    public List<BloodTypeBean> getAllBloodTypes() {
        String sql = "SELECT * FROM blood_type";
        return jdbcTemplate.query(sql, 
            (rs, rowNum) -> {
                BloodTypeBean b = new BloodTypeBean();
                b.setId(rs.getInt("id"));
                b.setBloodType(rs.getString("blood_type"));
                return b;
            });
    }
    
    public BloodTypeBean getBloodTypeById(int id) {
        String sql = "SELECT * FROM blood_type WHERE id=?";
        return jdbcTemplate.queryForObject(sql, 
            (rs, rowNum) -> {
                BloodTypeBean bt = new BloodTypeBean();
                bt.setId(rs.getInt("id"));
                bt.setBloodType(rs.getString("blood_type"));
                return bt;
            }, id);
    }

}
