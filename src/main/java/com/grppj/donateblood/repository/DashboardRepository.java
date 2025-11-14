package com.grppj.donateblood.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbc;
    public DashboardRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /* ===================== KPIS (global + hospital) ===================== */

    public int totalBloodDonatedUnits() {
        String sql = """
            SELECT COALESCE(SUM(d.blood_unit),0)
              FROM donation d
             WHERE d.status IN ('Available','Used')
        """;
        return jdbc.queryForObject(sql, Integer.class);
    }

    public int totalBloodDonatedUnitsByHospital(int hospitalId) {
        String sql = """
            SELECT COALESCE(SUM(d.blood_unit),0)
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
             WHERE d.status IN ('Available','Used') AND da.hospital_id = ?
        """;
        return jdbc.queryForObject(sql, Integer.class, hospitalId);
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
        return jdbc.queryForObject("""
            SELECT COUNT(DISTINCT user_id) FROM donor_appointment WHERE status='Pending'
        """, Integer.class);
    }
       

    public int pendingDonorsByHospital(int hospitalId) {
        String sql = """
            SELECT COUNT(DISTINCT user_id)
              FROM donor_appointment
             WHERE status='Pending' AND hospital_id=?
        """;
        return jdbc.queryForObject(sql, Integer.class, hospitalId);
    }

    public int totalAppointments() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM donor_appointment", Integer.class);
    }

    public int totalAppointmentsByHospital(int hospitalId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM donor_appointment WHERE hospital_id = ?",
                Integer.class, hospitalId);
    }

    public int pendingBloodRequests() {
        String sql = """
            SELECT COUNT(*) FROM blood_request WHERE COALESCE(LOWER(TRIM(status)),'')='pending'
        """;
        return jdbc.queryForObject(sql, Integer.class);
    }

    public int pendingBloodRequestsByHospital(int hospitalId) {
        String sql = """
            SELECT COUNT(*) FROM blood_request
             WHERE COALESCE(LOWER(TRIM(status)),'')='pending' AND hospital_id = ?
        """;
        return jdbc.queryForObject(sql, Integer.class, hospitalId);
    }

    /* ===================== DONATION OVERVIEW (stacked) ===================== */
    /** Daily buckets (inclusive) for a hospital: bucket = 'YYYY-MM-DD' */
    public List<Map<String,Object>> donationsDailyByBloodTypeForHospital(
            String startDateIso /* yyyy-MM-dd */, String endDateIso /* yyyy-MM-dd */, int hospitalId) {

        String sql = """
            SELECT DATE_FORMAT(d.donation_date, '%Y-%m-%d') AS bucket,
                   bt.blood_type AS bt,
                   COALESCE(SUM(d.blood_unit),0) AS units
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
              JOIN blood_type bt        ON bt.id = da.blood_type_id
             WHERE d.status IN ('Available','Used')
               AND DATE(d.donation_date) BETWEEN ? AND ?
               AND da.hospital_id = ?
             GROUP BY DATE_FORMAT(d.donation_date, '%Y-%m-%d'), bt.blood_type
             ORDER BY bucket
        """;
        return jdbc.queryForList(sql, startDateIso, endDateIso, hospitalId);
    }

    /** Monthly buckets for a hospital across a date span: bucket = 'YYYY-MM' */
    public List<Map<String,Object>> donationsMonthlyByBloodTypeForHospital(
            String startDateIso /* first day yyyy-MM-dd */,
            String endDateIso   /* last day yyyy-MM-dd  */,
            int hospitalId) {

        String sql = """
            SELECT DATE_FORMAT(d.donation_date, '%Y-%m') AS bucket,
                   bt.blood_type AS bt,
                   COALESCE(SUM(d.blood_unit),0) AS units
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
              JOIN blood_type bt        ON bt.id = da.blood_type_id
             WHERE d.status IN ('Available','Used')
               AND DATE(d.donation_date) BETWEEN ? AND ?
               AND da.hospital_id = ?
             GROUP BY DATE_FORMAT(d.donation_date, '%Y-%m'), bt.blood_type
             ORDER BY bucket
        """;
        return jdbc.queryForList(sql, startDateIso, endDateIso, hospitalId);
    }

    /* ===================== RECENT APPOINTMENTS (hospital) ===================== */
    public List<RecentApptRow> recentAppointmentsByHospital(int limit, int hospitalId) {
        String sql = """
            SELECT a.id,
                   u.username,
                   bt.blood_type AS bloodType,
                   DATE_FORMAT(a.date, '%d-%m-%Y') AS date_dmy,
                   TIME_FORMAT(a.time, '%h:%i %p') AS time12,
                   a.status
              FROM donor_appointment a
              JOIN `user` u   ON u.id = a.user_id AND u.role_id = a.user_role_id
              JOIN blood_type bt ON bt.id = a.blood_type_id
             WHERE a.hospital_id = ?
             ORDER BY a.created_at DESC
             LIMIT ?
        """;
        return jdbc.query(sql, (rs, i) -> new RecentApptRow(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("bloodType"),
                rs.getString("date_dmy"),
                rs.getString("time12"),
                rs.getString("status")
        ), hospitalId, limit);
    }

    /* ===================== DTO ===================== */
    public static class RecentApptRow {
        public final int id;
        public final String username;
        public final String bloodType;
        public final String dateDmy;
        public final String time12;
        public final String status;
        public RecentApptRow(int id, String u, String bt, String d, String t, String s) {
            this.id = id; this.username = u; this.bloodType = bt; this.dateDmy = d; this.time12 = t; this.status = s;
        }
    }
}
