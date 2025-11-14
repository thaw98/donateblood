// src/main/java/com/grppj/donateblood/repository/FulfillmentRepository.java
package com.grppj.donateblood.repository;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.grppj.donateblood.model.FulfillmentBean;
import com.grppj.donateblood.model.FulfillmentSummary;
import com.grppj.donateblood.model.RequestDonorUsage;

@Repository
public class FulfillmentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------- Row-level (legacy) ----------
    private static final String BASE_SELECT = """
        SELECT
            rf.fulfillment_id,
            DATE_FORMAT(rf.fulfillment_date, '%d-%m-%Y %h:%i%p') AS fulfillment_date,
            rf.quantity_used,
            rf.donation_donation_id     AS donation_id,
            rf.blood_request_id         AS request_id,

            bt_req.blood_type           AS blood_type,
            u_req.username              AS recipient_name,
            u_donor.username            AS donor_name,
            br.status                   AS request_status
        FROM request_fulfillment rf
        JOIN blood_request br   ON br.id = rf.blood_request_id
        JOIN blood_type bt_req  ON bt_req.id = br.blood_type_id
        JOIN `user` u_req       ON u_req.id = br.user_id
        LEFT JOIN donation d    ON d.donation_id = rf.donation_donation_id
        LEFT JOIN donor_appointment da ON da.id = d.donor_appointment_id
        LEFT JOIN `user` u_donor ON u_donor.id = da.user_id
        """;

    /** Row-level list for one hospital (kept for completeness). */
    public List<FulfillmentBean> findAllByHospital(Integer hospitalId) {
        final String sql = BASE_SELECT + """
            WHERE br.hospital_id = ?
            ORDER BY rf.fulfillment_date DESC, rf.fulfillment_id DESC
        """;
        return jdbcTemplate.query(sql, (rs, rn) ->
            new FulfillmentBean(
                rs.getInt("fulfillment_id"),
                rs.getString("fulfillment_date"),
                rs.getInt("quantity_used"),
                rs.getInt("donation_id"),
                rs.getInt("request_id"),
                rs.getString("blood_type"),
                rs.getString("recipient_name"),
                rs.getString("donor_name"),
                rs.getString("request_status")
            ),
            hospitalId
        );
    }

    // ---------- “View details” (donors used for a specific request) ----------
    public List<RequestDonorUsage> findDonorsForRequest(int bloodRequestId) {
        final String sql = """
            SELECT 
                u.username                              AS donor_name,
                u.email                                 AS donor_email,
                bt.blood_type                           AS blood_type,
                u.phone                                 AS donor_phone,
                u.address                               AS donor_address,
                DATE_FORMAT(rf.fulfillment_date, '%d-%m-%Y %h:%i%p') AS fulfillment_time
            FROM request_fulfillment rf
            JOIN donation d             ON d.donation_id = rf.donation_donation_id
            JOIN donor_appointment da   ON da.id = d.donor_appointment_id
            JOIN `user` u               ON u.id = da.user_id
            JOIN blood_type bt          ON bt.id = da.blood_type_id
            WHERE rf.blood_request_id = ?
            ORDER BY rf.fulfillment_date DESC, d.donation_id DESC
        """;
        return jdbcTemplate.query(sql, (rs, rn) ->
            new RequestDonorUsage(
                rs.getString("donor_name"),
                rs.getString("donor_email"),
                rs.getString("blood_type"),
                rs.getString("donor_phone"),
                rs.getString("donor_address"),
                rs.getString("fulfillment_time")
            ),
            bloodRequestId
        );
    }
    // ---------- Grouped summaries used by /admin/fulfillment ----------
    /** Grouped by request for the current hospital. */
    public List<FulfillmentSummary> findGroupedByHospital(Integer hospitalId) {
	    	final String sql = """
	    		    SELECT
	    		        br.id                                   AS request_id,
	    		        u.username                              AS recipient_name,
	    		        u.email                                 AS recipient_email,
	    		        u.phone                                 AS recipient_phone,
	    		        u.address                               AS recipient_address,
	    		        bt.blood_type                           AS blood_type,
	    		        COALESCE(SUM(rf.quantity_used), 0)      AS total_used,
	    		        DATE_FORMAT(MAX(rf.fulfillment_date), '%d-%m-%Y %l:%i%p') AS completed_at,
	    		        GROUP_CONCAT(rf.donation_donation_id ORDER BY rf.donation_donation_id) AS donation_ids_csv,
	    		        br.status                               AS request_status
	    		    FROM request_fulfillment rf
	    		    JOIN blood_request br ON br.id = rf.blood_request_id
	    		    JOIN blood_type bt    ON bt.id = br.blood_type_id
	    		    JOIN `user` u         ON u.id = br.user_id
	    		    WHERE br.hospital_id = ?
	    		    GROUP BY br.id, u.username, u.email, u.phone, u.address, bt.blood_type, br.status
	    		    ORDER BY MAX(rf.fulfillment_date) DESC, br.id DESC
	    		""";

        return jdbcTemplate.query(sql, (rs, rn) ->
            new FulfillmentSummary(
                rs.getInt("request_id"),
                rs.getString("recipient_name"),
                rs.getString("recipient_email"),
                rs.getString("recipient_phone"),
                rs.getString("recipient_address"),
                rs.getString("blood_type"),
                rs.getInt("total_used"),
                rs.getString("completed_at"),
                rs.getString("donation_ids_csv"),
                rs.getString("request_status")
            ),
            hospitalId
        );
    }

    /** Grouped across ALL hospitals (fallback). */
    public List<FulfillmentSummary> findGroupedAll() {
	    	final String sql = """
	    		    SELECT
	    		        br.id                                   AS request_id,
	    		        u.username                              AS recipient_name,
	    		        u.email                                 AS recipient_email,
	    		        u.phone                                 AS recipient_phone,
	    		        u.address                               AS recipient_address,
	    		        bt.blood_type                           AS blood_type,
	    		        COALESCE(SUM(rf.quantity_used), 0)      AS total_used,
	    		        DATE_FORMAT(MAX(rf.fulfillment_date), '%d-%m-%Y %l:%i%p') AS completed_at,
	    		        GROUP_CONCAT(rf.donation_donation_id ORDER BY rf.donation_donation_id) AS donation_ids_csv,
	    		        br.status                               AS request_status
	    		    FROM request_fulfillment rf
	    		    JOIN blood_request br ON br.id = rf.blood_request_id
	    		    JOIN blood_type bt    ON bt.id = br.blood_type_id
	    		    JOIN `user` u         ON u.id = br.user_id
	    		    GROUP BY br.id, u.username, u.email, u.phone, u.address, bt.blood_type, br.status
	    		    ORDER BY MAX(rf.fulfillment_date) DESC, br.id DESC
	    		""";

        return jdbcTemplate.query(sql, (rs, rn) ->
            new FulfillmentSummary(
                rs.getInt("request_id"),
                rs.getString("recipient_name"),
                rs.getString("recipient_email"),
                rs.getString("recipient_phone"),
                rs.getString("recipient_address"),
                rs.getString("blood_type"),
                rs.getInt("total_used"),
                rs.getString("completed_at"),
                rs.getString("donation_ids_csv"),
                rs.getString("request_status")
            )
        );
    }
    
    public String findRecipientNameForRequest(int bloodRequestId) {
        final String sql = """
            SELECT u.username
            FROM blood_request br
            JOIN `user` u ON u.id = br.user_id
            WHERE br.id = ?
            """;
        // Returns null if not found
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null, bloodRequestId);
    }
    
 // Total lives saved (sum of quantity_used from request_fulfillment + units_used from blood_usage)
    public int getTotalUnitsUsed() {
        String sql = """
            SELECT 
                COALESCE(SUM(rf.quantity_used),0) + COALESCE((SELECT SUM(units_used) FROM blood_usage),0) AS total_used
            FROM request_fulfillment rf
        """;
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

}
