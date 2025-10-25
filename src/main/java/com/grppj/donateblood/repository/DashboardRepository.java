// src/main/java/com/grppj/donateblood/repository/DashboardRepository.java
package com.grppj.donateblood.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /* ---------- Existing global metrics (unchanged) ---------- */
    public int totalBloodDonatedUnits() {
        String sql = """
            SELECT COALESCE(SUM(d.blood_unit),0)
            FROM donation d
            WHERE d.status IN ('Available','Used')
        """;
        return jdbc.queryForObject(sql, Integer.class);
    }

    public int completedDonors() {
        String sql = """
            SELECT COUNT(DISTINCT da.user_id)
            FROM donor_appointment da
            WHERE da.status = 'Completed'
        """;
        return jdbc.queryForObject(sql, Integer.class);
    }

    public int pendingDonors() {
        String sql = """
            SELECT COUNT(DISTINCT da.user_id)
            FROM donor_appointment da
            WHERE da.status = 'Pending'
        """;
        return jdbc.queryForObject(sql, Integer.class);
    }

    public int totalAppointments() {
        String sql = "SELECT COUNT(*) FROM donor_appointment";
        return jdbc.queryForObject(sql, Integer.class);
    }

    public int pendingBloodRequests() {
        String sql = """
            SELECT COUNT(*)
            FROM recipient r
            WHERE COALESCE(LOWER(TRIM(r.status)),'') = 'pending'
        """;
        return jdbc.queryForObject(sql, Integer.class);
    }

    public List<RecentDonorRow> recentDonations(int limit) {
        String sql = """
            SELECT u.username,
                   bt.blood_type AS bloodType,
                   DATE(d.donation_date) AS date,
                   d.status
            FROM donation d
            JOIN donor_appointment da ON da.id = d.donor_appointment_id
            JOIN `user` u             ON u.id = da.user_id
            JOIN blood_type bt        ON bt.id = da.blood_type_id
            ORDER BY d.donation_date DESC, d.donation_id DESC
            LIMIT ?
        """;
        return jdbc.query(sql, (rs, i) -> new RecentDonorRow(
            rs.getString("username"),
            rs.getString("bloodType"),
            rs.getString("date"),
            rs.getString("status")
        ), limit);
    }

    /* ---------- New: hospital-scoped variants ---------- */
    public int totalBloodDonatedUnitsByHospital(int hospitalId) {
        String sql = """
            SELECT COALESCE(SUM(d.blood_unit),0)
            FROM donation d
            JOIN donor_appointment da ON da.id = d.donor_appointment_id
            WHERE d.status IN ('Available','Used')
              AND da.hospital_id = ?
        """;
        return jdbc.queryForObject(sql, Integer.class, hospitalId);
    }

    public int completedDonorsByHospital(int hospitalId) {
        String sql = """
            SELECT COUNT(DISTINCT da.user_id)
            FROM donor_appointment da
            WHERE da.status = 'Completed'
              AND da.hospital_id = ?
        """;
        return jdbc.queryForObject(sql, Integer.class, hospitalId);
    }

    public int pendingDonorsByHospital(int hospitalId) {
        String sql = """
            SELECT COUNT(DISTINCT da.user_id)
            FROM donor_appointment da
            WHERE da.status = 'Pending'
              AND da.hospital_id = ?
        """;
        return jdbc.queryForObject(sql, Integer.class, hospitalId);
    }

    public int totalAppointmentsByHospital(int hospitalId) {
        String sql = "SELECT COUNT(*) FROM donor_appointment WHERE hospital_id = ?";
        return jdbc.queryForObject(sql, Integer.class, hospitalId);
    }

    /** If your `recipient` table has a hospital_id column, use this; otherwise keep the global one. */
    public Integer pendingBloodRequestsByHospital(Integer hospitalId) {
        String sql = """
            SELECT COUNT(*)
            FROM recipient r
            WHERE COALESCE(LOWER(TRIM(r.status)),'') = 'pending'
              AND r.hospital_id = ?
        """;
        return jdbc.queryForObject(sql, Integer.class, hospitalId);
    }

    public List<RecentDonorRow> recentDonationsByHospital(int limit, int hospitalId) {
        String sql = """
            SELECT u.username,
                   bt.blood_type AS bloodType,
                   DATE(d.donation_date) AS date,
                   d.status
            FROM donation d
            JOIN donor_appointment da ON da.id = d.donor_appointment_id
            JOIN `user` u             ON u.id = da.user_id
            JOIN blood_type bt        ON bt.id = da.blood_type_id
            WHERE da.hospital_id = ?
            ORDER BY d.donation_date DESC, d.donation_id DESC
            LIMIT ?
        """;
        return jdbc.query(sql, (rs, i) -> new RecentDonorRow(
            rs.getString("username"),
            rs.getString("bloodType"),
            rs.getString("date"),
            rs.getString("status")
        ), hospitalId, limit);
    }

    /* DTO */
    public static class RecentDonorRow {
        public final String username;
        public final String bloodType;
        public final String date;
        public final String status;
        public RecentDonorRow(String u, String b, String d, String s) {
            this.username = u; this.bloodType = b; this.date = d; this.status = s;
        }
    }
}
