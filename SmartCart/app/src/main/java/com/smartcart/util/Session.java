package com.smartcart.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.smartcart.model.User;

/** Login session kept in SharedPreferences. */
public class Session {
    private final SharedPreferences sp;

    private Session(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("smartcart_session", Context.MODE_PRIVATE);
    }

    public static Session of(Context c) { return new Session(c); }

    public void save(User u) {
        sp.edit().putLong("uid", u.id).putString("name", u.name).putString("role", u.role).apply();
    }
    public boolean loggedIn() { return sp.getLong("uid", 0) > 0; }
    public long userId() { return sp.getLong("uid", 0); }
    public String name() { return sp.getString("name", ""); }
    public boolean isAdmin() { return "admin".equals(sp.getString("role", "")); }
    public void logout() { sp.edit().clear().apply(); }
}
