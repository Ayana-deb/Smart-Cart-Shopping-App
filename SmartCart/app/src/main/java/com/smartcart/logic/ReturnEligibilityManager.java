package com.smartcart.logic;

import android.content.Context;
import com.smartcart.db.DBHelper;
import com.smartcart.model.*;
import com.smartcart.util.Util;
import java.io.File;

/**
 * Coordinates the whole return workflow:
 * validate eligibility -> load history -> calculate risk -> choose processing path ->
 * save request + decision factors -> update history/membership -> notify the customer.
 * Also contains the admin actions (approve, reject, change status) and the local pickup/refund simulation.
 */
public class ReturnEligibilityManager {
    public static class Result {
        public boolean ok;
        public String message;
        public long requestId;
    }

    private final Context ctx;
    private final DBHelper db;

    public ReturnEligibilityManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.db = DBHelper.get(ctx);
    }

    private Result fail(String msg) {
        Result r = new Result();
        r.ok = false; r.message = msg;
        return r;
    }

    public Result submit(long userId, long orderId, long productId, int qty, String reason, String description, String photoPath) {
        File photo = photoPath == null || photoPath.trim().isEmpty() ? null : new File(photoPath);
        if (photo == null || !photo.isFile() || photo.length() == 0)
            return fail("A camera photo of the item is required to submit a return request.");

        // 1. Validate eligibility (policy rules; membership is never consulted here)
        Order o = db.getOrder(orderId);
        if (o == null || o.userId != userId) return fail("This order does not belong to your account.");
        if (!Constants.DELIVERED.equals(o.status) || o.deliveredAt == 0) return fail("Returns are only allowed for delivered orders.");
        int window = AppConfig.get(ctx, AppConfig.RETURN_WINDOW_DAYS);
        int days = Util.daysSince(o.deliveredAt);
        if (days > window) return fail("The return window of " + window + " days has expired. This order was delivered " + days + " days ago.");
        OrderItem item = null;
        for (OrderItem it : db.getOrderItems(orderId)) if (it.productId == productId) item = it;
        if (item == null) return fail("This product is not part of the selected order.");
        if (reason == null || reason.trim().isEmpty()) return fail("Please select a return reason.");
        if (qty < 1) return fail("Quantity must be at least 1.");
        if (db.hasActive(orderId, productId)) return fail("An active return request already exists for this product.");
        int remaining = item.quantity - db.returnedQty(orderId, productId);
        if (remaining <= 0) return fail("All units of this product were already returned or requested for return.");
        if (qty > remaining) return fail("You can return at most " + remaining + " unit(s) of this product.");

        // 2-3. Retrieve history and calculate risk
        FraudDetectionManager.RiskResult risk = new FraudDetectionManager(ctx).evaluate(userId, orderId, productId);
        User u = db.getUser(userId);
        String tier = u == null ? Constants.BRONZE : u.tier;

        // 4. Determine processing path (high risk is never auto-rejected)
        String status, decision, message;
        switch (risk.level) {
            case Constants.LOW:
                status = Constants.APPROVED; decision = "Automatically approved (low risk)";
                message = "Your return was approved. " + MembershipManager.pickupEta(tier);
                break;
            case Constants.MEDIUM:
                status = Constants.UNDER_REVIEW; decision = "Sent for additional verification (medium risk)";
                message = "Your request was received. Additional verification is required before it can be approved.";
                break;
            default:
                status = Constants.PENDING; decision = "Flagged for admin review (high risk)";
                message = "Your request was received and is being reviewed by our team.";
        }

        // 5. Save request and decision factors
        ReturnRequest r = new ReturnRequest();
        r.userId = userId; r.orderId = orderId; r.productId = productId; r.productName = item.name; r.quantity = qty;
        r.reason = reason; r.description = description == null ? "" : description.trim();
        r.photoPath = photoPath;
        r.requestDate = System.currentTimeMillis(); r.riskScore = risk.score; r.riskLevel = risk.level;
        r.riskExplanation = risk.explanation(); r.tier = tier; r.status = status; r.adminDecision = decision;
        long id = db.insertReturn(r);
        db.addEvent(id, status, decision);

        // 6. Update history / membership
        MembershipManager.recalculate(ctx, userId);

        // 7. Notify the customer (no risk details)
        db.addNotification(userId, "Return request #" + id + " submitted", "Your return request for " + item.name + " was submitted.");
        if (Constants.APPROVED.equals(status))
            db.addNotification(userId, "Return #" + id + " approved", "Your return for " + item.name + " was approved. " + MembershipManager.pickupEta(tier));
        else if (Constants.UNDER_REVIEW.equals(status))
            db.addNotification(userId, "Verification required for return #" + id, "Additional verification is required for your return of " + item.name + ". Our team will review it.");

        Result res = new Result();
        res.ok = true; res.requestId = id; res.message = message;
        return res;
    }

    // ---------- admin actions ----------
    public void approve(long id) { setStatus(db.getReturn(id), Constants.APPROVED, "Approved by admin", null); }

    public void reject(long id, String reason) { setStatus(db.getReturn(id), Constants.REJECTED, "Rejected by admin", reason); }

    public void updateStatus(long id, String status) { setStatus(db.getReturn(id), status, "Status updated by admin to " + status, null); }

    /** Local simulation of the pickup and refund steps. Returns the new status or null. */
    public String advance(long id) {
        ReturnRequest r = db.getReturn(id);
        if (r == null) return null;
        String next;
        if (Constants.APPROVED.equals(r.status)) next = Constants.PICKUP;
        else if (Constants.PICKUP.equals(r.status)) next = Constants.REFUNDED;
        else return null;
        setStatus(r, next, "Simulated: " + next, null);
        return next;
    }

    private void setStatus(ReturnRequest r, String status, String decision, String rejection) {
        if (r == null) return;
        boolean fin = Constants.REJECTED.equals(status) || Constants.REFUNDED.equals(status);
        db.updateReturn(r.id, status, decision, rejection, fin ? System.currentTimeMillis() : 0);
        db.addEvent(r.id, status, rejection == null ? decision : decision + ": " + rejection);
        if (Constants.REFUNDED.equals(status) && !Constants.REFUNDED.equals(r.status)) db.addStock(r.productId, r.quantity);
        String title, msg;
        switch (status) {
            case Constants.APPROVED: title = "Return #" + r.id + " approved"; msg = "Your return for " + r.productName + " was approved. " + MembershipManager.pickupEta(r.tier); break;
            case Constants.REJECTED: title = "Return #" + r.id + " rejected"; msg = "Your return for " + r.productName + " was not approved." + (rejection == null ? "" : " Reason: " + rejection); break;
            case Constants.UNDER_REVIEW: title = "Verification required for return #" + r.id; msg = "Additional verification is required for your return of " + r.productName + "."; break;
            case Constants.PICKUP: title = "Pickup scheduled for return #" + r.id; msg = "A pickup for " + r.productName + " has been scheduled."; break;
            case Constants.REFUNDED: title = "Refund processed for return #" + r.id; msg = "Your refund for " + r.productName + " has been processed."; break;
            default: title = "Return #" + r.id + " updated"; msg = "Your return for " + r.productName + " is now: " + status + ".";
        }
        db.addNotification(r.userId, title, msg);
        MembershipManager.recalculate(ctx, r.userId);
    }
}
