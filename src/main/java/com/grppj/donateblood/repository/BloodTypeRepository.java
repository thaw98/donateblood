package com.grppj.donateblood.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.grppj.donateblood.model.BloodTypeBean;
import java.util.List;

@Repository
public class BloodTypeRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<BloodTypeBean> getAllBloodTypes() {
        String sql = "SELECT * FROM blood_type";
        List<BloodTypeBean> list = jdbcTemplate.query(
            sql,
            (rs, rowNumber) -> new BloodTypeBean(
                rs.getInt("id"),
                rs.getString("blood_type")
            )
        );
        return list;
    }

    public BloodTypeBean getBloodTypeById(Integer bloodTypeId) {
        String sql = "SELECT * FROM blood_type WHERE id = ?";
        BloodTypeBean obj = jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new BloodTypeBean(
                rs.getInt("id"),
                rs.getString("blood_type")
            ),
            bloodTypeId
        );
        return obj;
    }
}