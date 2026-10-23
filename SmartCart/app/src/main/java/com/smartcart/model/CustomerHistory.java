package com.smartcart.model;

/** Aggregated purchase and return history of one customer (pending requests are never counted as completed). */
public class CustomerHistory {
    public int deliveredOrders, purchaseOrders, deliveredUnits, completedReturnUnits, rejectedReturns,
            totalRequests, repeatedFlaggedReasons;
    public double totalSpent;

    /** Share of delivered units that ended in a completed (approved) return, in percent. */
    public double returnRatePct() {
        return deliveredUnits == 0 ? 0 : completedReturnUnits * 100.0 / deliveredUnits;
    }
}
