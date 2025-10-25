package com.grppj.donateblood.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class RecipientRepository {
	
	@Autowired
	private BloodStockRepository bloodStockRepository;


    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<RecipientRow> listRecipientRequestsForHospital(Integer hospitalId) {
        String sql = """
            SELECT
                br.id             AS request_id,
                br.quantity       AS quantity,
                br.status         AS status,
                br.required_date  AS required_date,
                br.request_date   AS request_date,
                br.urgency        AS urgency,
                br.hospital_id    AS hospital_id,
                br.blood_type_id  AS blood_type_id,

                u.username        AS username,
                u.email           AS email,
                u.phone           AS phone,
                u.gender          AS gender,
                u.dateofbirth     AS date_of_birth,
                u.address         AS address,

                bt.blood_type     AS blood_type,
                h.hospital_name   AS hospital_name
            FROM blood_request br
            LEFT JOIN `user` u   ON u.id = br.user_id
            JOIN blood_type bt   ON bt.id = br.blood_type_id
            JOIN hospital h      ON h.id = br.hospital_id
            WHERE (? IS NULL OR br.hospital_id = ?)
            ORDER BY br.required_date DESC, br.id DESC
        """;

        return jdbcTemplate.query(sql, (rs, rn) -> {
            RecipientRow row = mapRow(rs);

            // compute canComplete by comparing current stock
            Integer availableUnits = jdbcTemplate.queryForObject("""
            	    SELECT COALESCE(SUM(d.blood_unit), 0)
            	      FROM donation d
            	      JOIN donor_appointment da ON da.id = d.donor_appointment_id
            	     WHERE da.hospital_id   = ?
            	       AND da.blood_type_id = ?
            	       AND d.status = 'Available'
            	    """,
            	    Integer.class,
            	    row.getHospitalId(),
            	    row.getBloodTypeId()
            	);

            	// enable Complete only when there are enough available units
            	row.setCanComplete(availableUnits != null && availableUnits >= row.getQuantity());
            return row;
        }, hospitalId, hospitalId);
    }

    private RecipientRow mapRow(ResultSet rs) throws SQLException {
        RecipientRow r = new RecipientRow();
        r.setRequestId(rs.getInt("request_id"));
        r.setQuantity(rs.getInt("quantity"));
        r.setStatus(rs.getString("status"));

        // request/required dates (DATETIME/DATE safe handling)
        try {
            var ts = rs.getTimestamp("request_date");
            r.setRequestDate(ts != null ? ts.toLocalDateTime() : null);
        } catch (Exception ignore) {
            var d = rs.getDate("request_date");
            r.setRequestDate(d != null ? d.toLocalDate().atStartOfDay() : null);
        }
        try {
            var ts = rs.getTimestamp("required_date");
            r.setRequiredDate(ts != null ? ts.toLocalDateTime() : null);
        } catch (Exception ignore) {
            var d = rs.getDate("required_date");
            r.setRequiredDate(d != null ? d.toLocalDate().atStartOfDay() : null);
        }

        r.setUrgency(rs.getString("urgency"));
        r.setHospitalId(rs.getInt("hospital_id"));
        r.setBloodTypeId(rs.getInt("blood_type_id"));

        r.setUsername(rs.getString("username"));
        r.setEmail(rs.getString("email"));
        r.setPhone(rs.getString("phone"));
        r.setGender(rs.getString("gender"));
        r.setAddress(rs.getString("address"));

        // DOB stored as VARCHAR(45) in column `dateofbirth`
        r.setDateOfBirth(rs.getString("date_of_birth"));

        r.setBloodType(rs.getString("blood_type"));
        r.setHospitalName(rs.getString("hospital_name"));
        return r;
    }
    
    /** Returns the hospital_id that owns a given blood_request id. */
    /** Returns hospital_id that owns a given blood_request id. */
    public Integer findHospitalIdForRequest(int requestId) {
        return jdbcTemplate.queryForObject(
            "SELECT hospital_id FROM blood_request WHERE id = ?",
            Integer.class,
            requestId
        );
    }

    /**
     * Fulfill a blood request by consuming N available donations of the same blood type
     * from the same hospital. Marks donations as Used, logs request_fulfillment rows,
     * and sets the request status to completed.
     */

    public void updateStatusAndInsertFulfillment(int requestId,
                                                 int hospitalId,
                                                 int adminUserId,
                                                 int units) {

        // 1) Get the blood_type_id for the request
        Integer bloodTypeId = jdbcTemplate.queryForObject(
            "SELECT blood_type_id FROM blood_request WHERE id = ?",
            Integer.class, requestId
        );
        if (bloodTypeId == null || units <= 0) {
            // nothing to consume; just mark completed
            jdbcTemplate.update("UPDATE blood_request SET status = 'completed' WHERE id = ?", requestId);
            return;
        }

        // 2) Find N available donations that match hospital & blood type
        List<Integer> donationIds = jdbcTemplate.query(
            """
            SELECT d.donation_id
              FROM donation d
              JOIN donor_appointment da ON da.id = d.donor_appointment_id
             WHERE da.hospital_id   = ?
               AND da.blood_type_id = ?
               AND d.status         = 'Available'
             ORDER BY d.donation_date ASC, d.donation_id ASC
             LIMIT ?
            """,
            (rs, rn) -> rs.getInt(1),
            hospitalId, bloodTypeId, units
        );

        // 3) Mark donations as Used and log fulfillment
        for (Integer did : donationIds) {
            jdbcTemplate.update(
                "UPDATE donation SET status = 'Used' WHERE donation_id = ? AND status = 'Available'",
                did
            );

            jdbcTemplate.update(
                """
                INSERT INTO request_fulfillment(fulfillment_date, quantity_used, donation_donation_id, blood_request_id)
                VALUES (NOW(), 1, ?, ?)
                """,
                did, requestId
            );
        }

        // 4) Mark the blood request completed
        jdbcTemplate.update("UPDATE blood_request SET status = 'completed' WHERE id = ?", requestId);

        // 5) Reduce blood stock using BloodStockRepository
        int consumed = donationIds.size();
        if (consumed > 0) {
            bloodStockRepository.decreaseStock(hospitalId, bloodTypeId, consumed, adminUserId, 0);
        }
    }

    
    public void transferAllUnitsNoTx(int requestId, int targetHospitalId) {
        // 1) current qty
        Integer qty = jdbcTemplate.query(
            "SELECT quantity FROM blood_request WHERE id = ?",
            rs -> rs.next() ? rs.getInt(1) : null,
            requestId
        );
        if (qty == null) throw new IllegalArgumentException("Request not found.");
        if (qty <= 0) throw new IllegalArgumentException("Nothing to transfer (quantity is 0).");

        // 2) insert destination row with the same fields, status = 'pending'
        int ins = jdbcTemplate.update("""
            INSERT INTO blood_request
                (quantity, request_date, required_date, urgency, status,
                 user_id, hospital_id, blood_type_id)
            SELECT
                ?, NOW(), br.required_date, br.urgency, 'pending',
                br.user_id, ?, br.blood_type_id
            FROM blood_request br
            WHERE br.id = ?
            """, qty, targetHospitalId, requestId);

        if (ins != 1) {
            throw new IllegalStateException("Could not create target request.");
        }

        // 3) zero-out source and mark transferred (also keep pointer for audit)
        int upd = jdbcTemplate.update("""
            UPDATE blood_request
               SET quantity = 0,
                   status = 'transferred',
                   target_hospital_id = ?
             WHERE id = ? AND quantity = ?
            """, targetHospitalId, requestId, qty);

        if (upd != 1) {
            // 4) rollback the target insert if the source update didn’t go through
            jdbcTemplate.update("""
                DELETE FROM blood_request
                 WHERE hospital_id = ?
                   AND quantity = ?
                   AND status = 'pending'
                 ORDER BY id DESC
                 LIMIT 1
            """, targetHospitalId, qty);
            throw new IllegalStateException("Transfer failed while updating the source request. No changes kept.");
        }
    }
    // Simple DTO used by the Thymeleaf view
    public static class RecipientRow {
        private Integer requestId;
        private Integer quantity;
        private String  status;
        private LocalDateTime requiredDate;
        private LocalDateTime requestDate;
        private String  urgency;

        private Integer hospitalId;
        private Integer bloodTypeId;

        private String  username;
        private String  email;
        private String  phone;
        private String  gender;
        private String  dateOfBirth;   // <-- String, matches VARCHAR(45)
        private String  address;

        private String  bloodType;
        private String  hospitalName;

        private boolean canComplete;

        // getters/setters
        public Integer getRequestId() { return requestId; }
        public void setRequestId(Integer v) { requestId = v; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer v) { quantity = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { status = v; }
        public LocalDateTime getRequiredDate() { return requiredDate; }
        public void setRequiredDate(LocalDateTime v) { requiredDate = v; }
        public LocalDateTime getRequestDate() { return requestDate; }
        public void setRequestDate(LocalDateTime v) { requestDate = v; }
        public String getUrgency() { return urgency; }
        public void setUrgency(String v) { urgency = v; }
        public Integer getHospitalId() { return hospitalId; }
        public void setHospitalId(Integer v) { hospitalId = v; }
        public Integer getBloodTypeId() { return bloodTypeId; }
        public void setBloodTypeId(Integer v) { bloodTypeId = v; }
        public String getUsername() { return username; }
        public void setUsername(String v) { username = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { email = v; }
        public String getPhone() { return phone; }
        public void setPhone(String v) { phone = v; }
        public String getGender() { return gender; }
        public void setGender(String v) { gender = v; }
        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String v) { dateOfBirth = v; }
        public String getAddress() { return address; }
        public void setAddress(String v) { address = v; }
        public String getBloodType() { return bloodType; }
        public void setBloodType(String v) { bloodType = v; }
        public String getHospitalName() { return hospitalName; }
        public void setHospitalName(String v) { hospitalName = v; }
        public boolean isCanComplete() { return canComplete; }
        public void setCanComplete(boolean v) { canComplete = v; }
    }
}
