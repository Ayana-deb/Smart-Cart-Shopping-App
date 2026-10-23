package com.smartcart.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.smartcart.model.*;
import com.smartcart.util.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** All SQLite access: schema, sample data and every query used by the app. */
public class DBHelper extends SQLiteOpenHelper {
    private static final String NAME = "smartcart.db";
    private static final int VERSION = 2;
    private static DBHelper instance;

    public static synchronized DBHelper get(Context c) {
        if (instance == null) instance = new DBHelper(c.getApplicationContext());
        return instance;
    }

    private DBHelper(Context c) { super(c, NAME, null, VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE, password TEXT NOT NULL, role TEXT NOT NULL DEFAULT 'customer', tier TEXT NOT NULL DEFAULT 'Bronze', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY, name TEXT, description TEXT, price REAL, category TEXT, emoji TEXT, rating REAL, stock INTEGER)");
        db.execSQL("CREATE TABLE cart(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, product_id INTEGER, quantity INTEGER, UNIQUE(user_id, product_id))");
        db.execSQL("CREATE TABLE wishlist(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, product_id INTEGER, UNIQUE(user_id, product_id))");
        db.execSQL("CREATE TABLE orders(id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT, user_id INTEGER, total REAL, status TEXT, address TEXT, payment TEXT, placed_at INTEGER, shipped_at INTEGER, delivered_at INTEGER)");
        db.execSQL("CREATE TABLE order_items(id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, product_id INTEGER, name TEXT, emoji TEXT, price REAL, quantity INTEGER)");
        db.execSQL("CREATE TABLE returns(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, order_id INTEGER, product_id INTEGER, product_name TEXT, quantity INTEGER, reason TEXT, description TEXT, photo_path TEXT, request_date INTEGER, risk_score INTEGER, risk_level TEXT, risk_explanation TEXT, tier TEXT, status TEXT, admin_decision TEXT, rejection_reason TEXT, resolution_date INTEGER)");
        db.execSQL("CREATE TABLE return_events(id INTEGER PRIMARY KEY AUTOINCREMENT, return_id INTEGER, status TEXT, note TEXT, created_at INTEGER)");
        db.execSQL("CREATE TABLE notifications(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, title TEXT, message TEXT, created_at INTEGER, is_read INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE settings(skey TEXT PRIMARY KEY, svalue TEXT)");
        SeedData.seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        if (o < 2) {
            db.execSQL("ALTER TABLE returns ADD COLUMN photo_path TEXT");
            return;
        }
        for (String t : new String[]{"users", "products", "cart", "wishlist", "orders", "order_items", "returns", "return_events", "notifications", "settings"})
            db.execSQL("DROP TABLE IF EXISTS " + t);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        SeedData.ensureGoldDemoAccount(db);
    }

    // ---------- cursor helpers ----------
    private static String s(Cursor c, String n) { return c.getString(c.getColumnIndexOrThrow(n)); }
    private static long l(Cursor c, String n) { return c.getLong(c.getColumnIndexOrThrow(n)); }
    private static int i(Cursor c, String n) { return c.getInt(c.getColumnIndexOrThrow(n)); }
    private static double d(Cursor c, String n) { return c.getDouble(c.getColumnIndexOrThrow(n)); }

    private int scalarInt(String sql, String... args) {
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }
    private double scalarDouble(String sql, String... args) {
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try { return c.moveToFirst() ? c.getDouble(0) : 0; } finally { c.close(); }
    }
    private static String[] a(long id) { return new String[]{String.valueOf(id)}; }

    // ---------- users ----------
    public boolean emailExists(String email) {
        return scalarInt("SELECT COUNT(*) FROM users WHERE email=?", email.toLowerCase(Locale.ROOT)) > 0;
    }

    /** Returns the new user id, or -1 if the email is already registered. */
    public long registerUser(String name, String email, String password) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("email", email.toLowerCase(Locale.ROOT));
        v.put("password", Security.hash(password));
        v.put("role", "customer");
        v.put("tier", Constants.BRONZE);
        v.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("users", null, v);
    }

    private User user(Cursor c) {
        User u = new User();
        u.id = l(c, "id"); u.name = s(c, "name"); u.email = s(c, "email"); u.role = s(c, "role"); u.tier = s(c, "tier");
        return u;
    }

    public User login(String email, String password) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM users WHERE email=?", new String[]{email.toLowerCase(Locale.ROOT)});
        try {
            if (c.moveToFirst() && Security.verify(password, s(c, "password"))) return user(c);
        } finally { c.close(); }
        return null;
    }

    public User getUser(long id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM users WHERE id=?", a(id));
        try { return c.moveToFirst() ? user(c) : null; } finally { c.close(); }
    }

    public void setTier(long userId, String tier) {
        ContentValues v = new ContentValues();
        v.put("tier", tier);
        getWritableDatabase().update("users", v, "id=?", a(userId));
    }

    // ---------- products ----------
    private Product product(Cursor c) {
        Product p = new Product();
        p.id = l(c, "id"); p.name = s(c, "name"); p.description = s(c, "description"); p.category = s(c, "category");
        p.emoji = s(c, "emoji"); p.price = d(c, "price"); p.rating = d(c, "rating"); p.stock = i(c, "stock");
        return p;
    }

    public List<Product> getProducts(String category, String query) {
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (category != null && !category.isEmpty()) { sql.append(" AND category=?"); args.add(category); }
        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (name LIKE ? OR description LIKE ? OR category LIKE ?)");
            String q = "%" + query.trim() + "%";
            args.add(q); args.add(q); args.add(q);
        }
        sql.append(" ORDER BY rating DESC, name");
        List<Product> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        try { while (c.moveToNext()) out.add(product(c)); } finally { c.close(); }
        return out;
    }

    public Product getProduct(long id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM products WHERE id=?", a(id));
        try { return c.moveToFirst() ? product(c) : null; } finally { c.close(); }
    }

    public List<String> getCategories() {
        List<String> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT DISTINCT category FROM products ORDER BY category", null);
        try { while (c.moveToNext()) out.add(c.getString(0)); } finally { c.close(); }
        return out;
    }

    public void addStock(long productId, int qty) {
        getWritableDatabase().execSQL("UPDATE products SET stock=stock+? WHERE id=?", new Object[]{qty, productId});
    }

    // ---------- cart ----------
    private int cartQty(long uid, long pid) {
        return scalarInt("SELECT COALESCE(SUM(quantity),0) FROM cart WHERE user_id=? AND product_id=?", String.valueOf(uid), String.valueOf(pid));
    }

    /** Adds to cart with stock validation. Returns an error message, or null on success. */
    public String addToCart(long uid, long pid, int qty) {
        Product p = getProduct(pid);
        if (p == null) return "Product not found";
        if (p.stock <= 0) return "Sorry, this product is out of stock";
        int cur = cartQty(uid, pid);
        if (cur + qty > p.stock) return "Only " + p.stock + " in stock" + (cur > 0 ? " (" + cur + " already in your cart)" : "");
        SQLiteDatabase db = getWritableDatabase();
        if (cur == 0) {
            ContentValues v = new ContentValues();
            v.put("user_id", uid); v.put("product_id", pid); v.put("quantity", qty);
            db.insert("cart", null, v);
        } else {
            db.execSQL("UPDATE cart SET quantity=quantity+? WHERE user_id=? AND product_id=?", new Object[]{qty, uid, pid});
        }
        return null;
    }

    public List<CartItem> getCart(long uid) {
        List<CartItem> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id, product_id, quantity FROM cart WHERE user_id=? ORDER BY id", a(uid));
        try {
            while (c.moveToNext()) {
                CartItem ci = new CartItem();
                ci.id = c.getLong(0); ci.quantity = c.getInt(2);
                ci.product = getProduct(c.getLong(1));
                if (ci.product != null) out.add(ci);
            }
        } finally { c.close(); }
        return out;
    }

    public int cartCount(long uid) {
        return scalarInt("SELECT COALESCE(SUM(quantity),0) FROM cart WHERE user_id=?", String.valueOf(uid));
    }
    public void setCartQty(long cartId, int qty) {
        ContentValues v = new ContentValues();
        v.put("quantity", qty);
        getWritableDatabase().update("cart", v, "id=?", a(cartId));
    }
    public void removeCart(long cartId) { getWritableDatabase().delete("cart", "id=?", a(cartId)); }

    // ---------- wishlist ----------
    public boolean isWishlisted(long uid, long pid) {
        return scalarInt("SELECT COUNT(*) FROM wishlist WHERE user_id=? AND product_id=?", String.valueOf(uid), String.valueOf(pid)) > 0;
    }

    /** Toggles wishlist membership. Returns true if the product is now wishlisted. */
    public boolean toggleWishlist(long uid, long pid) {
        SQLiteDatabase db = getWritableDatabase();
        if (isWishlisted(uid, pid)) {
            db.delete("wishlist", "user_id=? AND product_id=?", new String[]{String.valueOf(uid), String.valueOf(pid)});
            return false;
        }
        ContentValues v = new ContentValues();
        v.put("user_id", uid); v.put("product_id", pid);
        db.insert("wishlist", null, v);
        return true;
    }

    public List<Product> getWishlist(long uid) {
        List<Product> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT p.* FROM products p JOIN wishlist w ON w.product_id=p.id WHERE w.user_id=? ORDER BY w.id DESC", a(uid));
        try { while (c.moveToNext()) out.add(product(c)); } finally { c.close(); }
        return out;
    }

    // ---------- orders ----------
    /** Places a Cash-on-Delivery order from the cart in one transaction. Throws IllegalStateException with a user-friendly message. */
    public long placeOrder(long uid, String address) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<CartItem> cart = getCart(uid);
            if (cart.isEmpty()) throw new IllegalStateException("Your cart is empty");
            double total = 0;
            for (CartItem ci : cart) {
                if (ci.quantity > ci.product.stock)
                    throw new IllegalStateException(ci.product.name + " has only " + ci.product.stock + " left in stock");
                total += ci.product.price * ci.quantity;
            }
            long now = System.currentTimeMillis();
            ContentValues o = new ContentValues();
            o.put("code", "TMP"); o.put("user_id", uid); o.put("total", total); o.put("status", Constants.PLACED);
            o.put("address", address); o.put("payment", "Cash on Delivery (simulated)"); o.put("placed_at", now);
            long id = db.insert("orders", null, o);
            ContentValues cv = new ContentValues();
            cv.put("code", String.format(Locale.US, "SC%s%04d", new SimpleDateFormat("yyMMdd", Locale.US).format(new Date(now)), id));
            db.update("orders", cv, "id=?", a(id));
            for (CartItem ci : cart) {
                ContentValues oi = new ContentValues();
                oi.put("order_id", id); oi.put("product_id", ci.product.id); oi.put("name", ci.product.name);
                oi.put("emoji", ci.product.emoji); oi.put("price", ci.product.price); oi.put("quantity", ci.quantity);
                db.insert("order_items", null, oi);
                db.execSQL("UPDATE products SET stock=stock-? WHERE id=?", new Object[]{ci.quantity, ci.product.id});
            }
            db.delete("cart", "user_id=?", a(uid));
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    private Order order(Cursor c) {
        Order o = new Order();
        o.id = l(c, "id"); o.code = s(c, "code"); o.userId = l(c, "user_id"); o.total = d(c, "total"); o.status = s(c, "status");
        o.address = s(c, "address"); o.payment = s(c, "payment"); o.placedAt = l(c, "placed_at");
        o.shippedAt = l(c, "shipped_at"); o.deliveredAt = l(c, "delivered_at");
        return o;
    }

    public List<Order> getOrders(long uid) {
        List<Order> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM orders WHERE user_id=? ORDER BY placed_at DESC", a(uid));
        try { while (c.moveToNext()) out.add(order(c)); } finally { c.close(); }
        return out;
    }

    public Order getOrder(long id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM orders WHERE id=?", a(id));
        try { return c.moveToFirst() ? order(c) : null; } finally { c.close(); }
    }

    public List<OrderItem> getOrderItems(long orderId) {
        List<OrderItem> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM order_items WHERE order_id=? ORDER BY id", a(orderId));
        try {
            while (c.moveToNext()) {
                OrderItem it = new OrderItem();
                it.id = l(c, "id"); it.orderId = l(c, "order_id"); it.productId = l(c, "product_id");
                it.name = s(c, "name"); it.emoji = s(c, "emoji"); it.price = d(c, "price"); it.quantity = i(c, "quantity");
                out.add(it);
            }
        } finally { c.close(); }
        return out;
    }

    /** Demo helper: Placed -> Shipped -> Delivered. Returns the new status, or null if already delivered. */
    public String advanceOrder(long orderId) {
        Order o = getOrder(orderId);
        if (o == null) return null;
        ContentValues v = new ContentValues();
        if (Constants.PLACED.equals(o.status)) { v.put("status", Constants.SHIPPED); v.put("shipped_at", System.currentTimeMillis()); }
        else if (Constants.SHIPPED.equals(o.status)) { v.put("status", Constants.DELIVERED); v.put("delivered_at", System.currentTimeMillis()); }
        else return null;
        getWritableDatabase().update("orders", v, "id=?", a(orderId));
        return v.getAsString("status");
    }

    public void setDeliveredAt(long orderId, long time) {
        ContentValues v = new ContentValues();
        v.put("delivered_at", time);
        getWritableDatabase().update("orders", v, "id=?", a(orderId));
    }

    // ---------- returns ----------
    private ReturnRequest ret(Cursor c) {
        ReturnRequest r = new ReturnRequest();
        r.id = l(c, "id"); r.userId = l(c, "user_id"); r.orderId = l(c, "order_id"); r.productId = l(c, "product_id");
        r.productName = s(c, "product_name"); r.quantity = i(c, "quantity"); r.reason = s(c, "reason"); r.description = s(c, "description");
        r.requestDate = l(c, "request_date"); r.riskScore = i(c, "risk_score"); r.riskLevel = s(c, "risk_level");
        r.riskExplanation = s(c, "risk_explanation"); r.tier = s(c, "tier"); r.status = s(c, "status");
        r.adminDecision = s(c, "admin_decision"); r.rejectionReason = s(c, "rejection_reason"); r.resolutionDate = l(c, "resolution_date");
        int photoIx = c.getColumnIndex("photo_path");
        r.photoPath = photoIx >= 0 ? c.getString(photoIx) : null;
        int ix = c.getColumnIndex("user_name");
        if (ix >= 0) r.userName = c.getString(ix);
        return r;
    }

    public long insertReturn(ReturnRequest r) {
        ContentValues v = new ContentValues();
        v.put("user_id", r.userId); v.put("order_id", r.orderId); v.put("product_id", r.productId); v.put("product_name", r.productName);
        v.put("quantity", r.quantity); v.put("reason", r.reason); v.put("description", r.description);
        v.put("photo_path", r.photoPath); v.put("request_date", r.requestDate);
        v.put("risk_score", r.riskScore); v.put("risk_level", r.riskLevel); v.put("risk_explanation", r.riskExplanation);
        v.put("tier", r.tier); v.put("status", r.status); v.put("admin_decision", r.adminDecision);
        return getWritableDatabase().insert("returns", null, v);
    }

    public void updateReturn(long id, String status, String decision, String rejection, long resolution) {
        ContentValues v = new ContentValues();
        v.put("status", status);
        if (decision != null) v.put("admin_decision", decision);
        if (rejection != null) v.put("rejection_reason", rejection);
        if (resolution > 0) v.put("resolution_date", resolution);
        getWritableDatabase().update("returns", v, "id=?", a(id));
    }

    public ReturnRequest getReturn(long id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT r.*, u.name AS user_name FROM returns r JOIN users u ON u.id=r.user_id WHERE r.id=?", a(id));
        try { return c.moveToFirst() ? ret(c) : null; } finally { c.close(); }
    }

    public List<ReturnRequest> getReturns(long uid) {
        List<ReturnRequest> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT r.*, u.name AS user_name FROM returns r JOIN users u ON u.id=r.user_id WHERE r.user_id=? ORDER BY r.request_date DESC, r.id DESC", a(uid));
        try { while (c.moveToNext()) out.add(ret(c)); } finally { c.close(); }
        return out;
    }

    /** filter: All, High Risk, or any return status. */
    public List<ReturnRequest> getAllReturns(String filter) {
        String where = "";
        String[] args = null;
        if ("High Risk".equals(filter)) where = " WHERE r.risk_level='High'";
        else if (filter != null && !"All".equals(filter)) { where = " WHERE r.status=?"; args = new String[]{filter}; }
        List<ReturnRequest> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT r.*, u.name AS user_name FROM returns r JOIN users u ON u.id=r.user_id" + where + " ORDER BY r.request_date DESC, r.id DESC", args);
        try { while (c.moveToNext()) out.add(ret(c)); } finally { c.close(); }
        return out;
    }

    public int countReturnsWhere(String where) { return scalarInt("SELECT COUNT(*) FROM returns WHERE " + where); }

    /** Counts a customer's requests. statusSql: list like 'A','B' (null = any); since/productId/orderId are ignored when 0. */
    public int countReturns(long uid, String statusSql, long since, long productId, long orderId) {
        StringBuilder q = new StringBuilder("SELECT COUNT(*) FROM returns WHERE user_id=" + uid);
        if (statusSql != null) q.append(" AND status IN (").append(statusSql).append(")");
        if (since > 0) q.append(" AND request_date>=").append(since);
        if (productId > 0) q.append(" AND product_id=").append(productId);
        if (orderId > 0) q.append(" AND order_id=").append(orderId);
        return scalarInt(q.toString());
    }

    /** Units of one order line already returned or requested (rejected requests do not count). */
    public int returnedQty(long orderId, long productId) {
        return scalarInt("SELECT COALESCE(SUM(quantity),0) FROM returns WHERE order_id=? AND product_id=? AND status<>'Rejected'", String.valueOf(orderId), String.valueOf(productId));
    }

    public boolean hasActive(long orderId, long productId) {
        return scalarInt("SELECT COUNT(*) FROM returns WHERE order_id=? AND product_id=? AND status IN (" + Constants.ACTIVE_SQL + ")", String.valueOf(orderId), String.valueOf(productId)) > 0;
    }

    public void addEvent(long returnId, String status, String note) {
        ContentValues v = new ContentValues();
        v.put("return_id", returnId); v.put("status", status); v.put("note", note); v.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert("return_events", null, v);
    }

    /** Each row: {status, note, timestamp}. */
    public List<String[]> getEvents(long returnId) {
        List<String[]> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT status, note, created_at FROM return_events WHERE return_id=? ORDER BY id", a(returnId));
        try { while (c.moveToNext()) out.add(new String[]{c.getString(0), c.getString(1), String.valueOf(c.getLong(2))}); } finally { c.close(); }
        return out;
    }

    // ---------- customer history (shared by fraud + membership logic) ----------
    public CustomerHistory loadHistory(long uid) {
        String u = String.valueOf(uid);
        CustomerHistory h = new CustomerHistory();
        h.purchaseOrders = scalarInt("SELECT COUNT(*) FROM orders WHERE user_id=?", u);
        h.deliveredOrders = scalarInt("SELECT COUNT(*) FROM orders WHERE user_id=? AND status='Delivered'", u);
        h.deliveredUnits = scalarInt("SELECT COALESCE(SUM(oi.quantity),0) FROM order_items oi JOIN orders o ON o.id=oi.order_id WHERE o.user_id=? AND o.status='Delivered'", u);
        h.totalSpent = scalarDouble("SELECT COALESCE(SUM(total),0) FROM orders WHERE user_id=? AND status='Delivered'", u);
        h.completedReturnUnits = scalarInt("SELECT COALESCE(SUM(quantity),0) FROM returns WHERE user_id=? AND status IN (" + Constants.COMPLETED_SQL + ")", u);
        h.rejectedReturns = scalarInt("SELECT COUNT(*) FROM returns WHERE user_id=? AND status='Rejected'", u);
        h.totalRequests = scalarInt("SELECT COUNT(*) FROM returns WHERE user_id=?", u);
        h.repeatedFlaggedReasons = scalarInt(
                "SELECT COALESCE(SUM(reason_count - 1),0) FROM (" +
                        "SELECT COUNT(*) AS reason_count FROM returns WHERE user_id=? " +
                        "AND lower(trim(reason)) IN ('other','changed my mind','not as described') " +
                        "GROUP BY lower(trim(reason)))", u);
        return h;
    }

    // ---------- notifications ----------
    public void addNotification(long uid, String title, String message) {
        ContentValues v = new ContentValues();
        v.put("user_id", uid); v.put("title", title); v.put("message", message);
        v.put("created_at", System.currentTimeMillis()); v.put("is_read", 0);
        getWritableDatabase().insert("notifications", null, v);
    }

    public List<AppNotification> getNotifications(long uid) {
        List<AppNotification> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM notifications WHERE user_id=? ORDER BY id DESC", a(uid));
        try {
            while (c.moveToNext()) {
                AppNotification n = new AppNotification();
                n.id = l(c, "id"); n.title = s(c, "title"); n.message = s(c, "message"); n.createdAt = l(c, "created_at"); n.read = i(c, "is_read") == 1;
                out.add(n);
            }
        } finally { c.close(); }
        return out;
    }

    public int unreadCount(long uid) {
        return scalarInt("SELECT COUNT(*) FROM notifications WHERE user_id=? AND is_read=0", String.valueOf(uid));
    }

    public void markAllRead(long uid) {
        getWritableDatabase().execSQL("UPDATE notifications SET is_read=1 WHERE user_id=?", new Object[]{uid});
    }

    // ---------- settings (configurable rules) ----------
    public int getSettingInt(String key, int def) {
        Cursor c = getReadableDatabase().rawQuery("SELECT svalue FROM settings WHERE skey=?", new String[]{key});
        try {
            if (c.moveToFirst()) return Integer.parseInt(c.getString(0));
        } catch (NumberFormatException ignored) {
        } finally { c.close(); }
        return def;
    }

    public void setSetting(String key, int value) {
        ContentValues v = new ContentValues();
        v.put("skey", key); v.put("svalue", String.valueOf(value));
        getWritableDatabase().insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void resetSettings() { getWritableDatabase().delete("settings", null, null); }
}
