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

    /** Sum of units ever donated (Available + Used). If you only want “Used”, change WHERE. */
    public int totalBloodDonatedUnits() {
        String sql = """
            SELECT COALESCE(SUM(d.blood_unit),0)
            FROM donation d
            WHERE d.status IN ('Available','Used')
        """;
        return jdbc.queryForObject(sql, Integer.class);
    }

    /** Donors who completed a donation (at least one Completed appointment). */
    public int completedDonors() {
        String sql = """
            SELECT COUNT(DISTINCT da.user_id)
            FROM donor_appointment da
            WHERE da.status = 'Completed'
        """;
        return jdbc.queryForObject(sql, Integer.class);
    }

    /** Donors currently pending (distinct users with a Pending appointment). */
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

    /** Latest donations table for “Recent Donor Activity”. */
    public List<RecentDonorRow> recentDonations(int limit) {
        String sql = """
            SELECT u.username,
                   bt.blood_type       AS bloodType,
                   DATE(d.donation_date) AS date,        -- assumes DATETIME/TIMESTAMP
                   d.status
            FROM donation d
            JOIN donor_appointment da ON da.id = d.donor_appointment_id
            JOIN `user` u             ON u.id = da.user_id
            JOIN blood_type bt        ON bt.id = da.blood_type_id
            ORDER BY d.donation_date DESC
            LIMIT ?
        """;
        return jdbc.query(sql, (rs, i) -> new RecentDonorRow(
                rs.getString("username"),
                rs.getString("bloodType"),
                rs.getString("date"),
                rs.getString("status")
        ), limit);
    }

    // tiny DTO
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
