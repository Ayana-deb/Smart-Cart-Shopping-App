package com.smartcart.model;

/** Shared status names and SQL status groups. */
public final class Constants {
    private Constants() {}
    public static final String PLACED = "Placed", SHIPPED = "Shipped", DELIVERED = "Delivered";
    public static final String PENDING = "Pending", UNDER_REVIEW = "Under Review", APPROVED = "Approved",
            PICKUP = "Pickup Scheduled", REFUNDED = "Refund Processed", REJECTED = "Rejected";
    public static final String LOW = "Low", MEDIUM = "Medium", HIGH = "High";
    public static final String BRONZE = "Bronze", SILVER = "Silver", GOLD = "Gold";

    public static final String[] RETURN_STATUSES = {PENDING, UNDER_REVIEW, APPROVED, PICKUP, REFUNDED, REJECTED};
    public static final String[] REASONS = {"Damaged product", "Wrong item delivered", "Not as described",
            "Quality issue", "Size or fit issue", "Changed my mind", "Other"};

    /** Returns that count as "completed" (approved outcome). Pending / Under Review are excluded on purpose. */
    public static final String COMPLETED_SQL = "'Approved','Pickup Scheduled','Refund Processed'";
    /** Requests still in progress. */
    public static final String ACTIVE_SQL = "'Pending','Under Review','Approved','Pickup Scheduled'";
    /** Everything except rejected requests. */
    public static final String NON_REJECTED_SQL = "'Pending','Under Review','Approved','Pickup Scheduled','Refund Processed'";
}
