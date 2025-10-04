package com.grppj.donateblood.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.grppj.donateblood.model.DonationBean;
import java.time.LocalDateTime;

@Repository
public class DonationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int saveDonation(DonationBean donation) {
        String sql = "INSERT INTO donation (blood_unit, donation_date, status, user_id, user_role_id, hospital_id) VALUES (?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql, donation.getBloodUnit(), 
            LocalDateTime.now(), "Used", donation.getUserId(), 
            donation.getUserRoleId(), donation.getHospitalId());
    }

    public DonationBean getDonationById(Integer donationId) {
        String sql = "SELECT * FROM donation WHERE donation_id = ?";
        DonationBean obj = jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new DonationBean(
                rs.getInt("donation_id"),
                rs.getInt("blood_unit"),
                rs.getTimestamp("donation_date").toLocalDateTime(),
                rs.getString("status"),
                rs.getInt("user_id"),
                rs.getInt("user_role_id"),
                rs.getInt("hospital_id")
            ),
            donationId
        );
        return obj;
    }
}