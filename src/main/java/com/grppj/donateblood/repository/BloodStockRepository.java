package com.grppj.donateblood.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BloodStockRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // (used by /admin/stock page)
    public List<StockViewRow> getViewForHospital(Integer hospitalId) {
        String sql =
            """
				SELECT
				  bt.id          AS blood_type_id,
				  bt.blood_type  AS bloodType,
				  COALESCE(SUM(d.blood_unit), 0) AS units,
				  DATE_FORMAT(MAX(d.donation_date), '%d-%m-%Y %l:%i%p') AS lastDonationFmt
				FROM donation d
				JOIN donor_appointment a ON a.id = d.donor_appointment_id
				JOIN blood_type bt       ON bt.id = a.blood_type_id
				WHERE a.hospital_id = ?
				GROUP BY bt.id, bt.blood_type
				ORDER BY bt.id;
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rn) -> new StockViewRow(
                rs.getInt("blood_type_id"),
                rs.getString("blood_type"),
                rs.getInt("amount"),
                rs.getDate("updated_date"),
                rs.getString("last_donation_fmt") // may be null
            ),
              // for subquery
            hospitalId   // for left join
        );
    }

    // ---- stock mutation helpers ----

    // find most recent stock row for this (hospital, bloodType)
    public Integer findStockId(int hospitalId, int bloodTypeId) {
        String sql = "SELECT stock_id FROM blood_stock " +
                     "WHERE hospital_id=? AND blood_type_id=? " +
                     "ORDER BY stock_id DESC LIMIT 1";
        List<Integer> ids = jdbcTemplate.query(sql, (rs, rn) -> rs.getInt(1), hospitalId, bloodTypeId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    // +units (called on approve/create donation)
    public int increaseStock(int hospitalId, int bloodTypeId, int units, int adminUserId, int adminRoleId) {
        Integer stockId = findStockId(hospitalId, bloodTypeId);
        if (stockId == null) {
            String ins = "INSERT INTO blood_stock(amount, updated_date, hospital_id, blood_type_id, user_id, user_role_id) " +
                         "VALUES(?, CURRENT_DATE(), ?, ?, ?, ?)";
            return jdbcTemplate.update(ins, units, hospitalId, bloodTypeId, adminUserId, adminRoleId);
        } else {
            String upd = "UPDATE blood_stock SET amount = amount + ?, updated_date=CURRENT_DATE() WHERE stock_id=?";
            return jdbcTemplate.update(upd, units, stockId);
        }
    }

    // -units (called when a donation is actually consumed)
    public int decreaseStock(int hospitalId, int bloodTypeId, int units, int adminUserId, int adminRoleId) {
        Integer stockId = findStockId(hospitalId, bloodTypeId);
        if (stockId == null) {
            String ins = "INSERT INTO blood_stock(amount, updated_date, hospital_id, blood_type_id, user_id, user_role_id) " +
                         "VALUES(0, CURRENT_DATE(), ?, ?, ?, ?)";
            jdbcTemplate.update(ins, hospitalId, bloodTypeId, adminUserId, adminRoleId);
            stockId = findStockId(hospitalId, bloodTypeId);
        }
        String upd = "UPDATE blood_stock SET amount = GREATEST(amount - ?, 0), updated_date=CURRENT_DATE() WHERE stock_id=?";
        return jdbcTemplate.update(upd, units, stockId);
    }

    // simple DTO for the view
    public static class StockViewRow {
        public final Integer bloodTypeId;
        public final String  bloodType;
        public final Integer amount;
        public final java.sql.Date updatedDate;

        /** Already formatted like "17-10-2025 2:45PM" (or null if none yet). */
        public final String lastDonationFmt;

        // Backward-compatible ctor (if old code expects 4 args)
        public StockViewRow(Integer btId, String bt, Integer amt, java.sql.Date upd) {
            this(btId, bt, amt, upd, null);
        }

        public StockViewRow(Integer btId, String bt, Integer amt, java.sql.Date upd, String lastDonationFmt) {
            this.bloodTypeId = btId;
            this.bloodType   = bt;
            this.amount      = amt;
            this.updatedDate = upd;
            this.lastDonationFmt = lastDonationFmt;
        }
    }
    
    public List<StockViewRow> getStockViewForSuperAdminDashboard(Integer hospitalId) {
        String sql =
            """
                SELECT
                  bt.id          AS blood_type_id,
                  bt.blood_type  AS blood_type,
                  COALESCE(SUM(d.blood_unit), 0) AS amount,
                  CURRENT_DATE() AS updated_date,
                  DATE_FORMAT(MAX(d.donation_date), '%d-%m-%Y %l:%i%p') AS last_donation_fmt
                FROM blood_type bt
                LEFT JOIN (
                    SELECT a.blood_type_id, d.blood_unit, d.donation_date
                    FROM donation d
                    JOIN donor_appointment a ON a.id = d.donor_appointment_id
                    WHERE a.hospital_id = ?
                ) d ON bt.id = d.blood_type_id
                GROUP BY bt.id, bt.blood_type
                ORDER BY bt.id;
            """;

        return jdbcTemplate.query(sql, new Object[]{hospitalId}, (rs, rowNum) -> new StockViewRow(
            rs.getInt("blood_type_id"),
            rs.getString("blood_type"),
            rs.getInt("amount"),
            rs.getDate("updated_date"),
            rs.getString("last_donation_fmt")
        ));
    }

    
}
