package com.smartcart.model;

public class ReturnRequest {
    public long id, userId, orderId, productId, requestDate, resolutionDate;
    public int quantity, riskScore, userScore;
    public String productName, reason, description, riskLevel, riskExplanation, tier, status,
            adminDecision, rejectionReason, userName, photoPath, userTier;

    /** Text shown to the customer. It never exposes the risk score or explanation. */
    public String customerMessage() {
        switch (status) {
            case Constants.APPROVED: return "Approved. Your pickup will be arranged shortly.";
            case Constants.UNDER_REVIEW: return "Additional verification is required. Our team will review your request.";
            case Constants.PICKUP: return "Pickup scheduled. Please keep the item ready with its original packaging.";
            case Constants.REFUNDED: return "Refund processed. The amount will be returned to you.";
            case Constants.REJECTED: return "Your request was not approved."
                    + (rejectionReason == null || rejectionReason.isEmpty() ? "" : " Reason: " + rejectionReason);
            default: return "Request received. Our team is reviewing it.";
        }
    }

    /** Simple text tracker: Requested - Approved - Pickup - Refunded. */
    public String progress() {
        if (Constants.REJECTED.equals(status)) return "\u2716 Rejected";
        int stage = 0;
        if (Constants.APPROVED.equals(status)) stage = 1;
        else if (Constants.PICKUP.equals(status)) stage = 2;
        else if (Constants.REFUNDED.equals(status)) stage = 3;
        String[] steps = {"Requested", "Approved", "Pickup", "Refunded"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.length; i++) {
            sb.append(i <= stage ? "\u25CF " : "\u25CB ").append(steps[i]);
            if (i < steps.length - 1) sb.append("  \u2500  ");
        }
        return sb.toString();
    }
}
