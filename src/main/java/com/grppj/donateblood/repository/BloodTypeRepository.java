package com.grppj.donateblood.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.grppj.donateblood.model.BloodType;

@Repository
public class BloodTypeRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<BloodType> findAll() {
        String sql = "SELECT * FROM blood_type";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BloodType b = new BloodType();
            b.setId(rs.getInt("id"));
            b.setBloodType(rs.getString("blood_type"));
            return b;
        });
    }
    
    public BloodType getBloodTypeById(int id) {
        String sql = "SELECT * FROM blood_type WHERE id=?";
        return jdbcTemplate.queryForObject(sql, 
            (rs, rowNum) -> {
                BloodType bt = new BloodType();
                bt.setId(rs.getInt("id"));
                bt.setBloodType(rs.getString("blood_type"));
                return bt;
            }, id);
    }


}
