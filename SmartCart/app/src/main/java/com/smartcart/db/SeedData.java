package com.smartcart.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.smartcart.util.Security;
import com.smartcart.util.Util;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Sample catalogue plus demo accounts with purchase/return history, so membership tiers and the
 * three risk scenarios (Low, Medium, High) can be demonstrated immediately.
 */
final class SeedData {
    private static final String[] NAME = new String[21], EMOJI = new String[21];
    private static final double[] PRICE = new double[21];

    static void seed(SQLiteDatabase db) {
        Object[][] p = {
            {1, "Wireless Earbuds Pro", "Noise-cancelling Bluetooth earbuds with 24-hour battery life and a charging case.", "Electronics", 2499.0, "\uD83C\uDFA7", 4.5, 25},
            {2, "Smart Watch Active", "Fitness smart watch with heart-rate tracking, GPS and a bright AMOLED display.", "Electronics", 3999.0, "\u231A", 4.3, 15},
            {3, "Bluetooth Speaker Mini", "Pocket-sized waterproof speaker with deep bass and 12 hours of playtime.", "Electronics", 1799.0, "\uD83D\uDD0A", 4.4, 30},
            {4, "Power Bank 20000mAh", "Fast-charging power bank with dual USB ports and a digital battery indicator.", "Electronics", 1599.0, "\uD83D\uDD0B", 4.2, 40},
            {5, "Denim Jacket", "Classic regular-fit denim jacket in washed blue with durable stitching.", "Fashion", 2199.0, "\uD83E\uDDE5", 4.1, 20},
            {6, "Running Sneakers", "Lightweight breathable sneakers with cushioned soles for daily runs.", "Fashion", 2999.0, "\uD83D\uDC5F", 4.6, 18},
            {7, "Cotton T-Shirt Pack", "Pack of three soft combed-cotton t-shirts in assorted colours.", "Fashion", 899.0, "\uD83D\uDC55", 4.0, 50},
            {8, "Leather Backpack", "Spacious faux-leather backpack with a padded 15-inch laptop compartment.", "Fashion", 2599.0, "\uD83C\uDF92", 4.4, 12},
            {9, "Non-stick Cookware Set", "Five-piece induction-friendly non-stick cookware set with glass lids.", "Home & Kitchen", 3299.0, "\uD83C\uDF73", 4.3, 14},
            {10, "Electric Kettle 1.5L", "Stainless-steel electric kettle with auto shut-off and boil-dry protection.", "Home & Kitchen", 1199.0, "\u2615", 4.2, 35},
            {11, "LED Table Lamp", "Dimmable LED desk lamp with three colour modes and a flexible neck.", "Home & Kitchen", 799.0, "\uD83D\uDCA1", 4.0, 28},
            {12, "Memory Foam Pillow", "Ergonomic memory foam pillow that supports your neck and keeps its shape.", "Home & Kitchen", 999.0, "\uD83D\uDECF\uFE0F", 4.5, 22},
            {13, "Atomic Habits (Paperback)", "A practical guide to building good habits and breaking bad ones.", "Books", 499.0, "\uD83D\uDCD8", 4.8, 60},
            {14, "The Alchemist", "A timeless novel about following your dreams and listening to your heart.", "Books", 399.0, "\uD83D\uDCD7", 4.6, 45},
            {15, "Python Programming Guide", "Beginner-friendly guide to Python with exercises and mini projects.", "Books", 699.0, "\uD83D\uDCD9", 4.3, 30},
            {16, "Mindfulness Journal", "Guided daily journal with prompts for gratitude, focus and reflection.", "Books", 349.0, "\uD83D\uDCD3", 4.2, 40},
            {17, "Yoga Mat Premium", "Eco-friendly non-slip yoga mat, 6 mm thick, with a carry strap.", "Sports", 1099.0, "\uD83E\uDDD8", 4.4, 33},
            {18, "Badminton Racket Set", "Two lightweight aluminium rackets with shuttlecocks and a carry bag.", "Sports", 1499.0, "\uD83C\uDFF8", 4.1, 20},
            {19, "Dumbbell Set 10kg", "Adjustable rubber-coated dumbbell pair for home workouts.", "Sports", 2299.0, "\uD83C\uDFCB\uFE0F", 4.5, 16},
            {20, "Football Size 5", "Machine-stitched match football with a durable weather-resistant cover.", "Sports", 799.0, "\u26BD", 4.3, 26},
        };
        for (Object[] r : p) {
            int id = (Integer) r[0];
            NAME[id] = (String) r[1]; PRICE[id] = (Double) r[4]; EMOJI[id] = (String) r[5];
            ContentValues v = new ContentValues();
            v.put("id", id); v.put("name", NAME[id]); v.put("description", (String) r[2]); v.put("category", (String) r[3]);
            v.put("price", PRICE[id]); v.put("emoji", EMOJI[id]); v.put("rating", (Double) r[6]); v.put("stock", (Integer) r[7]);
            db.insert("products", null, v);
        }

        // Customers use password demo123.
        user(db, 1, "Admin", "admin@smartcart.com", "admin123", "admin", "Bronze");
        user(db, 2, "Alice Fernandes", "alice@demo.com", "demo123", "customer", "Silver");
        user(db, 3, "Bob Mehta", "bob@demo.com", "demo123", "customer", "Bronze");
        user(db, 4, "Carl Pereira", "carl@demo.com", "demo123", "customer", "Bronze");
        user(db, 5, "Diana Shah", "diana@demo.com", "demo123", "customer", "Gold");

        // Alice: 4 delivered orders, never returned anything. A return on order 4 scores 0 (Low).
        order(db, 1, 2, 34, 30, new int[][]{{13, 1}, {14, 1}});
        order(db, 2, 2, 24, 20, new int[][]{{9, 1}});
        order(db, 3, 2, 16, 12, new int[][]{{6, 1}});
        order(db, 4, 2, 5, 2, new int[][]{{1, 1}, {17, 1}});

        // Bob: 3 delivered orders and 2 recently rejected requests. A return on order 7 scores 25 (Medium).
        order(db, 5, 3, 44, 40, new int[][]{{2, 1}});
        order(db, 6, 3, 29, 25, new int[][]{{8, 1}});
        order(db, 7, 3, 6, 3, new int[][]{{3, 1}, {4, 1}});
        ret(db, 1, 3, 5, 2, 1, "Changed my mind", 38, 30, "Medium", "Bronze", "Rejected", "Rejected by admin", "Product showed signs of use, so the return policy was not met.", 36);
        ret(db, 2, 3, 6, 8, 1, "Not as described", 23, 30, "Medium", "Bronze", "Rejected", "Rejected by admin", "The item matched its listing description.", 21);

        // Carl: high return rate, repeat product (dumbbells) and frequent requests. A return of p19 on order 11 scores 60 (High).
        order(db, 8, 4, 32, 28, new int[][]{{5, 1}, {6, 1}});
        order(db, 9, 4, 21, 17, new int[][]{{19, 1}});
        order(db, 10, 4, 13, 9, new int[][]{{9, 1}});
        order(db, 11, 4, 6, 2, new int[][]{{19, 1}});
        ret(db, 3, 4, 8, 5, 1, "Quality issue", 26, 0, "Low", "Bronze", "Refund Processed", "Automatically approved (low risk)", null, 24);
        ret(db, 4, 4, 9, 19, 1, "Wrong item delivered", 15, 0, "Low", "Bronze", "Refund Processed", "Automatically approved (low risk)", null, 13);
        ret(db, 5, 4, 10, 9, 1, "Damaged product", 7, 15, "Low", "Bronze", "Pickup Scheduled", "Automatically approved (low risk)", null, -1);

        // Diana: six delivered orders, over ₹40,000 spent, and no rejected returns qualifies for Gold.
        order(db, 12, 5, 50, 48, new int[][]{{2, 1}, {9, 1}});
        order(db, 13, 5, 45, 43, new int[][]{{2, 1}, {8, 1}, {5, 1}});
        order(db, 14, 5, 40, 38, new int[][]{{2, 1}, {6, 1}, {17, 1}});
        order(db, 15, 5, 35, 33, new int[][]{{9, 1}, {6, 1}, {18, 1}});
        order(db, 16, 5, 30, 28, new int[][]{{2, 1}, {5, 1}, {4, 1}});
        order(db, 17, 5, 25, 23, new int[][]{{8, 1}, {6, 1}, {9, 1}});
    }

    static void ensureGoldDemoAccount(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            Cursor existing = db.rawQuery("SELECT id FROM users WHERE email=?", new String[]{"diana@demo.com"});
            try {
                if (existing.moveToFirst()) {
                    db.setTransactionSuccessful();
                    return;
                }
            } finally {
                existing.close();
            }

            ContentValues user = new ContentValues();
            user.put("name", "Diana Shah");
            user.put("email", "diana@demo.com");
            user.put("password", Security.hash("demo123"));
            user.put("role", "customer");
            user.put("tier", "Gold");
            user.put("created_at", Util.daysAgo(60));
            long userId = db.insertOrThrow("users", null, user);

            addGoldOrder(db, userId, 50, 48, new int[][]{{2, 1}, {9, 1}});
            addGoldOrder(db, userId, 45, 43, new int[][]{{2, 1}, {8, 1}, {5, 1}});
            addGoldOrder(db, userId, 40, 38, new int[][]{{2, 1}, {6, 1}, {17, 1}});
            addGoldOrder(db, userId, 35, 33, new int[][]{{9, 1}, {6, 1}, {18, 1}});
            addGoldOrder(db, userId, 30, 28, new int[][]{{2, 1}, {5, 1}, {4, 1}});
            addGoldOrder(db, userId, 25, 23, new int[][]{{8, 1}, {6, 1}, {9, 1}});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void addGoldOrder(SQLiteDatabase db, long userId, int placedAgo, int deliveredAgo, int[][] items) {
        long placed = Util.daysAgo(placedAgo);
        double total = 0;
        for (int[] item : items) {
            Cursor product = db.rawQuery("SELECT price FROM products WHERE id=?", new String[]{String.valueOf(item[0])});
            try {
                if (!product.moveToFirst()) throw new IllegalStateException("Missing demo product " + item[0]);
                total += product.getDouble(0) * item[1];
            } finally {
                product.close();
            }
        }

        ContentValues order = new ContentValues();
        order.put("code", "");
        order.put("user_id", userId);
        order.put("total", total);
        order.put("status", "Delivered");
        order.put("address", "12 Demo Street, Navi Mumbai 400703");
        order.put("payment", "Cash on Delivery (simulated)");
        order.put("placed_at", placed);
        order.put("shipped_at", placed + Util.DAY);
        order.put("delivered_at", Util.daysAgo(deliveredAgo));
        long orderId = db.insertOrThrow("orders", null, order);

        ContentValues code = new ContentValues();
        code.put("code", String.format(Locale.US, "SC%s%04d",
                new SimpleDateFormat("yyMMdd", Locale.US).format(new Date(placed)), orderId));
        db.update("orders", code, "id=?", new String[]{String.valueOf(orderId)});

        for (int[] item : items) {
            Cursor product = db.rawQuery("SELECT name, price, emoji FROM products WHERE id=?",
                    new String[]{String.valueOf(item[0])});
            try {
                if (!product.moveToFirst()) throw new IllegalStateException("Missing demo product " + item[0]);
                ContentValues orderItem = new ContentValues();
                orderItem.put("order_id", orderId);
                orderItem.put("product_id", item[0]);
                orderItem.put("name", product.getString(0));
                orderItem.put("emoji", product.getString(2));
                orderItem.put("price", product.getDouble(1));
                orderItem.put("quantity", item[1]);
                db.insertOrThrow("order_items", null, orderItem);
            } finally {
                product.close();
            }
        }
    }

    private static void user(SQLiteDatabase db, long id, String name, String email, String pw, String role, String tier) {
        ContentValues v = new ContentValues();
        v.put("id", id); v.put("name", name); v.put("email", email); v.put("password", Security.hash(pw));
        v.put("role", role); v.put("tier", tier); v.put("created_at", Util.daysAgo(60));
        db.insert("users", null, v);
    }

    private static void order(SQLiteDatabase db, long id, long uid, int placedAgo, int deliveredAgo, int[][] items) {
        long placed = Util.daysAgo(placedAgo);
        double total = 0;
        for (int[] it : items) total += PRICE[it[0]] * it[1];
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("code", String.format(Locale.US, "SC%s%04d", new SimpleDateFormat("yyMMdd", Locale.US).format(new Date(placed)), id));
        v.put("user_id", uid); v.put("total", total); v.put("status", "Delivered");
        v.put("address", "12 Demo Street, Navi Mumbai 400703"); v.put("payment", "Cash on Delivery (simulated)");
        v.put("placed_at", placed); v.put("shipped_at", placed + Util.DAY); v.put("delivered_at", Util.daysAgo(deliveredAgo));
        db.insert("orders", null, v);
        for (int[] it : items) {
            ContentValues oi = new ContentValues();
            oi.put("order_id", id); oi.put("product_id", it[0]); oi.put("name", NAME[it[0]]); oi.put("emoji", EMOJI[it[0]]);
            oi.put("price", PRICE[it[0]]); oi.put("quantity", it[1]);
            db.insert("order_items", null, oi);
        }
    }

    private static void ret(SQLiteDatabase db, long id, long uid, long oid, int pid, int qty, String reason, int daysAgo,
                            int score, String level, String tier, String status, String decision, String rejection, int resolvedAgo) {
        ContentValues v = new ContentValues();
        v.put("id", id); v.put("user_id", uid); v.put("order_id", oid); v.put("product_id", pid); v.put("product_name", NAME[pid]);
        v.put("quantity", qty); v.put("reason", reason); v.put("description", "");
        v.put("request_date", Util.daysAgo(daysAgo)); v.put("risk_score", score); v.put("risk_level", level);
        v.put("risk_explanation", "Seeded demo record (historical request).");
        v.put("tier", tier); v.put("status", status); v.put("admin_decision", decision);
        if (rejection != null) v.put("rejection_reason", rejection);
        if (resolvedAgo >= 0) v.put("resolution_date", Util.daysAgo(resolvedAgo));
        db.insert("returns", null, v);
        ContentValues e = new ContentValues();
        e.put("return_id", id); e.put("status", status); e.put("note", decision); e.put("created_at", Util.daysAgo(daysAgo));
        db.insert("return_events", null, e);
    }
}
