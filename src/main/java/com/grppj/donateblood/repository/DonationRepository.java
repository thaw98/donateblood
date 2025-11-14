package com.grppj.donateblood.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.grppj.donateblood.model.DonationBean;

@Repository
public class DonationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<DonationBean> findByUserId(Integer userId) {
        String sql = """
            SELECT 
                d.donation_id, 
                d.blood_unit, 
                DATE_FORMAT(d.donation_date, '%W, %d-%m-%Y, %h:%i %p') AS donation_date,
                d.status,
                da.id AS appointment_id, 
                DATE_FORMAT(da.date, '%d-%m-%Y') AS appointment_date,
                h.hospital_name, 
                b.blood_type
            FROM donation d
            JOIN donor_appointment da ON d.donor_appointment_id = da.id
            JOIN hospital h ON da.hospital_id = h.id
            JOIN blood_type b ON da.blood_type_id = b.id
            WHERE da.user_id = ?
            ORDER BY d.donation_date DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DonationBean d = new DonationBean();
            d.setDonationId(rs.getInt("donation_id"));
            d.setBloodUnit(rs.getInt("blood_unit"));
            d.setDonationDate(rs.getString("donation_date")); // Already formatted: "Friday, 22-11-2024, 03:45 PM"
            d.setStatus(rs.getString("status"));
            d.setDonorAppointmentId(rs.getInt("appointment_id"));
            d.setAppointmentDate(rs.getString("appointment_date")); // Already formatted: "Friday, 22-11-2024"
            d.setHospitalName(rs.getString("hospital_name"));
            d.setBloodTypeName(rs.getString("blood_type"));
            return d;
        }, userId);
    }

    public DonationBean findDonationInfoById(Integer donationId) {
        String sql = """
            SELECT 
                DATE_FORMAT(d.donation_date, '%W, %d-%m-%Y, %h:%i %p') AS donation_date,
                h.hospital_name
            FROM donation d
            JOIN donor_appointment da ON d.donor_appointment_id = da.id
            JOIN hospital h ON da.hospital_id = h.id
            WHERE d.donation_id = ?;
        """;

        List<DonationBean> result = jdbcTemplate.query(sql, (rs, rowNum) -> {
            DonationBean d = new DonationBean();
            d.setDonationDate(rs.getString("donation_date")); // Already formatted
            d.setHospitalName(rs.getString("hospital_name"));
            return d;
        }, donationId);

        return result.isEmpty() ? null : result.get(0);
    }
    
    public int countRegularDonors() {
        String sql = "SELECT COUNT(DISTINCT donor_appointment_id) FROM donation";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

}
