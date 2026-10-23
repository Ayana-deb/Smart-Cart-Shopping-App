package com.smartcart.logic;

import android.content.Context;
import com.smartcart.db.DBHelper;
import com.smartcart.model.Constants;
import com.smartcart.model.CustomerHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bronze / Silver / Gold tiers from purchase points and repeated flagged return reasons. */
public final class MembershipManager {
    private MembershipManager() {}

    /** Recalculates the tier from current history and stores it in SQLite. */
    public static String recalculate(Context c, long userId) {
        DBHelper db = DBHelper.get(c);
        String tier = tierForScore(c, score(c, db.loadHistory(userId)));
        db.setTier(userId, tier);
        return tier;
    }

    private static int cfg(Context c, String key) { return AppConfig.get(c, key); }

    public static int score(Context c, CustomerHistory h) {
        return h.purchaseOrders * cfg(c, AppConfig.MEMBERSHIP_PURCHASE_POINTS)
                - h.repeatedFlaggedReasons * cfg(c, AppConfig.MEMBERSHIP_REPEAT_REASON_PENALTY);
    }

    public static String tierForScore(Context c, int score) {
        if (score >= cfg(c, AppConfig.GOLD_MIN_SCORE)) return Constants.GOLD;
        if (score >= cfg(c, AppConfig.SILVER_MIN_SCORE)) return Constants.SILVER;
        return Constants.BRONZE;
    }

    public static String nextTier(String tier) {
        if (Constants.BRONZE.equals(tier)) return Constants.SILVER;
        if (Constants.SILVER.equals(tier)) return Constants.GOLD;
        return null;
    }

    public static String benefits(String tier) {
        switch (tier) {
            case Constants.GOLD: return "\u2022 Priority return pickup within 24 hours\n\u2022 Fastest processing of approved returns\n\u2022 Dedicated support queue";
            case Constants.SILVER: return "\u2022 Faster return pickup within 48 hours\n\u2022 Priority processing of approved returns";
            default: return "\u2022 Standard return pickup within 3 to 5 days\n\u2022 Earn Silver by shopping regularly";
        }
    }

    public static String pickupEta(String tier) {
        if (Constants.GOLD.equals(tier)) return "Priority pickup within 24 hours.";
        if (Constants.SILVER.equals(tier)) return "Pickup within 48 hours.";
        return "Pickup within 3 to 5 days.";
    }

    public static int scoreProgress(Context c, int score, String next) {
        int need = cfg(c, next.toLowerCase(Locale.ROOT) + "_min_score");
        return need <= 0 ? 100 : Math.min(100, Math.max(0, score) * 100 / need);
    }

    /** Human readable score threshold for the next tier. */
    public static List<String> requirements(Context c, int score, String next) {
        int need = cfg(c, next.toLowerCase(Locale.ROOT) + "_min_score");
        List<String> out = new ArrayList<>();
        out.add(mark(score >= need) + "Membership score: " + score + " of " + need);
        out.add("Each purchase adds " + cfg(c, AppConfig.MEMBERSHIP_PURCHASE_POINTS) + " points.");
        out.add("Repeated flagged reasons subtract " + cfg(c, AppConfig.MEMBERSHIP_REPEAT_REASON_PENALTY) + " points each.");
        return out;
    }

    private static String mark(boolean ok) { return ok ? "\u2713 " : "\u2022 "; }
}
