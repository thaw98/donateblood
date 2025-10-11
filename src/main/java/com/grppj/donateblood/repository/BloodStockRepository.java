package com.grppj.donateblood.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.BloodStockBean;

@Repository
public class BloodStockRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // (used by /admin/stock page)
    public List<StockViewRow> getViewForHospital(Integer hospitalId) {
        String sql =
            "SELECT bt.id AS blood_type_id, bt.blood_type, " +
            "       COALESCE(bs.amount, 0) AS amount, bs.updated_date " +
            "  FROM blood_type bt " +
            "  LEFT JOIN blood_stock bs " +
            "    ON bs.blood_type_id = bt.id AND bs.hospital_id = ? " +
            " ORDER BY bt.id";
        return jdbcTemplate.query(sql, (rs, rn) ->
            new StockViewRow(
                rs.getInt("blood_type_id"),
                rs.getString("blood_type"),
                rs.getInt("amount"),
                rs.getDate("updated_date")
            ), hospitalId);
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
        public StockViewRow(Integer btId, String bt, Integer amt, java.sql.Date upd) {
            this.bloodTypeId = btId; this.bloodType = bt; this.amount = amt; this.updatedDate = upd;
        }
    }
}
