package com.grppj.donateblood.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.HospitalBean;

@Repository
public class HospitalsRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Get all hospitals
    public List<HospitalBean> getAllHospitals() {
        String sql = "SELECT * FROM hospital";
        return jdbcTemplate.query(sql, 
            (rs, rowNum) -> {
                HospitalBean h = new HospitalBean();
                h.setId(rs.getInt("id"));
                h.setHospitalName(rs.getString("hospital_name"));
                h.setAddress(rs.getString("address"));
                h.setPhone(rs.getString("phoneNo")); // map 'contact' to 'phone'
                return h;
            });
    }

}
