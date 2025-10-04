package com.grppj.donateblood.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.grppj.donateblood.model.HospitalBean;
import java.util.List;

@Repository
public class HospitalRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<HospitalBean> getAllHospitals() {
        String sql = "SELECT * FROM hospital";
        List<HospitalBean> list = jdbcTemplate.query(
            sql,
            (rs, rowNumber) -> new HospitalBean(
                rs.getInt("id"),
                rs.getString("hospital_name"),
                rs.getString("address"),
                rs.getString("contact")
            )
        );
        return list;
    }

    public HospitalBean getHospitalById(Integer hospitalId) {
        String sql = "SELECT * FROM hospital WHERE id = ?";
        HospitalBean obj = jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new HospitalBean(
                rs.getInt("id"),
                rs.getString("hospital_name"),
                rs.getString("address"),
                rs.getString("contact")
            ),
            hospitalId
        );
        return obj;
    }

    public HospitalBean getDefaultHospital() {
        String sql = "SELECT * FROM hospital ORDER BY id LIMIT 1";
        HospitalBean obj = jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new HospitalBean(
                rs.getInt("id"),
                rs.getString("hospital_name"),
                rs.getString("address"),
                rs.getString("contact")
            )
        );
        return obj;
    }
}