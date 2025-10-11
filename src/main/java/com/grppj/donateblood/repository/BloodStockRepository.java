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

  // All stock rows for one hospital (ordered by blood_type_id)
  public List<BloodStockBean> getStockForHospital(Integer hospitalId) {
    String sql = "SELECT stock_id, amount, updated_date, hospital_id, blood_type_id, user_id, user_role_id " +
                 "FROM blood_stock WHERE hospital_id = ? ORDER BY blood_type_id";
    List<BloodStockBean> list = jdbcTemplate.query(
        sql,
        (rs, rowNum) ->
            new BloodStockBean(
                rs.getInt("stock_id"),
                rs.getInt("amount"),
                rs.getDate("updated_date"),
                rs.getInt("hospital_id"),
                rs.getInt("blood_type_id"),
                rs.getInt("user_id"),
                rs.getInt("user_role_id")
            ),
        hospitalId
    );
    return list;
  }

  // Single row (may throw if missing — call ensureRow(..) first if needed)
  public BloodStockBean getStock(Integer hospitalId, Integer bloodTypeId) {
    String sql = "SELECT stock_id, amount, updated_date, hospital_id, blood_type_id, user_id, user_role_id " +
                 "FROM blood_stock WHERE hospital_id = ? AND blood_type_id = ?";
    BloodStockBean obj = jdbcTemplate.queryForObject(
        sql,
        (rs, rowNum) ->
            new BloodStockBean(
                rs.getInt("stock_id"),
                rs.getInt("amount"),
                rs.getDate("updated_date"),
                rs.getInt("hospital_id"),
                rs.getInt("blood_type_id"),
                rs.getInt("user_id"),
                rs.getInt("user_role_id")
            ),
        hospitalId, bloodTypeId
    );
    return obj;
  }

  // Increase stock by 'units' (creates row if missing). Tracks updater in user_id/user_role_id.
  public int increaseStock(Integer hospitalId, Integer bloodTypeId, Integer units,
                           Integer updaterUserId, Integer updaterRoleId) {
    ensureRow(hospitalId, bloodTypeId, updaterUserId, updaterRoleId);
    String sql = "UPDATE blood_stock " +
                 "SET amount = amount + ?, updated_date = CURRENT_DATE(), " +
                 "    user_id = ?, user_role_id = ? " +
                 "WHERE hospital_id = ? AND blood_type_id = ?";
    return jdbcTemplate.update(sql, units, updaterUserId, updaterRoleId, hospitalId, bloodTypeId);
  }

  // Decrease stock by 'units' (never below 0). Creates row if missing (treated as 0).
  public int decreaseStock(Integer hospitalId, Integer bloodTypeId, Integer units,
                           Integer updaterUserId, Integer updaterRoleId) {
    ensureRow(hospitalId, bloodTypeId, updaterUserId, updaterRoleId);
    String sql = "UPDATE blood_stock " +
                 "SET amount = GREATEST(0, amount - ?), updated_date = CURRENT_DATE(), " +
                 "    user_id = ?, user_role_id = ? " +
                 "WHERE hospital_id = ? AND blood_type_id = ?";
    return jdbcTemplate.update(sql, units, updaterUserId, updaterRoleId, hospitalId, bloodTypeId);
  }

  // Optional: set an absolute amount (clamped >= 0)
  public int setAmount(Integer hospitalId, Integer bloodTypeId, Integer amount,
                       Integer updaterUserId, Integer updaterRoleId) {
    ensureRow(hospitalId, bloodTypeId, updaterUserId, updaterRoleId);
    String sql = "UPDATE blood_stock " +
                 "SET amount = GREATEST(0, ?), updated_date = CURRENT_DATE(), " +
                 "    user_id = ?, user_role_id = ? " +
                 "WHERE hospital_id = ? AND blood_type_id = ?";
    return jdbcTemplate.update(sql, amount, updaterUserId, updaterRoleId, hospitalId, bloodTypeId);
  }

  // --- helpers ---

  // Make sure a row exists for (hospital_id, blood_type_id). If not, create it with amount=0.
  private void ensureRow(Integer hospitalId, Integer bloodTypeId,
                         Integer userId, Integer roleId) {
    String sql = ""
        + "INSERT INTO blood_stock (amount, updated_date, hospital_id, blood_type_id, user_id, user_role_id) "
        + "SELECT 0, CURRENT_DATE(), ?, ?, ?, ? "
        + "FROM DUAL "
        + "WHERE NOT EXISTS ("
        + "  SELECT 1 FROM blood_stock WHERE hospital_id = ? AND blood_type_id = ?"
        + ")";
    jdbcTemplate.update(sql, hospitalId, bloodTypeId, userId, roleId, hospitalId, bloodTypeId);
  }
  
  // shows all blood types for a hospital; amount defaults to 0 if missing
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
