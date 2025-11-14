// src/main/java/com/grppj/donateblood/repository/BloodUsageRepository.java
package com.grppj.donateblood.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository  // ✅ must be present
public class BloodUsageRepository {
    private final JdbcTemplate jdbc;
    public BloodUsageRepository(JdbcTemplate jdbc){ this.jdbc = jdbc; }

    public int insertUsage(int hospitalId, int bloodTypeId, int unitsUsed,
            int adminUserId, int adminRoleId, String remarks) {
			String sql = """
			INSERT INTO blood_usage(hospital_id, blood_type_id, units_used, admin_user_id, admin_role_id, remarks)
			VALUES (?,?,?,?,?,?)
			""";
			jdbc.update(sql, hospitalId, bloodTypeId, unitsUsed, adminUserId, adminRoleId, remarks);
			return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
			}


    public void insertUsageDetails(int usageId, java.util.List<Integer> donationIds) {
        if (donationIds == null || donationIds.isEmpty()) return;
        String sql = "INSERT INTO blood_usage_detail(usage_id, donation_id) VALUES (?,?)";
        for (Integer id : donationIds) jdbc.update(sql, usageId, id);
    }

    public List<Map<String,Object>> findRecent(int hospitalId, int limit) {
        return jdbc.queryForList("""
            SELECT bu.used_at, u.username AS admin_name, bt.blood_type, bu.units_used, bu.remarks
            FROM blood_usage bu
            JOIN user u        ON u.id=bu.admin_user_id AND u.role_id=bu.admin_role_id
            JOIN blood_type bt ON bt.id=bu.blood_type_id
            WHERE bu.hospital_id=?
            ORDER BY bu.used_at DESC
            LIMIT ?
        """, hospitalId, limit);
    }
    public List<Map<String,Object>> findByHospital(int hospitalId) {
        String sql = """
          SELECT bu.used_at,
                 bt.blood_type,
                 bu.units_used,
                 u.username AS admin_name,
                 bu.remarks
            FROM blood_usage bu
            JOIN blood_type bt ON bt.id = bu.blood_type_id
            JOIN user u ON u.id = bu.admin_user_id AND u.role_id = bu.admin_role_id
           WHERE bu.hospital_id = ?
           ORDER BY bu.used_at DESC, bu.id DESC
        """;
        return jdbc.queryForList(sql, hospitalId);
    }
    
    

}
