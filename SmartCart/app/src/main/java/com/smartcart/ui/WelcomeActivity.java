package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.model.User;
import com.smartcart.util.Session;
import com.smartcart.util.Util;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_welcome);
        Session s = Session.of(this);
        User u = DBHelper.get(this).getUser(s.userId());
        if (u == null) { s.logout(); startActivity(new Intent(this, LoginActivity.class)); finish(); return; }
        ((TextView) findViewById(R.id.tvWelcome)).setText("Welcome, " + u.name.split(" ")[0] + "!");
        TextView tier = findViewById(R.id.tvTier), tips = findViewById(R.id.tvTips);
        if (u.isAdmin()) {
            Util.badge(tier, "Administrator", Util.color("Pending"));
            tips.setText("Review return requests, inspect risk explanations\nand record your decisions.");
            ((TextView) findViewById(R.id.btnContinue)).setText("Open admin dashboard");
        } else {
            Util.badge(tier, u.tier + " member", Util.color(u.tier));
            tips.setText("Browse products, add items to your cart,\nplace a Cash on Delivery order and\nreturn delivered items easily.");
        }
        findViewById(R.id.btnContinue).setOnClickListener(v -> {
            Intent i = new Intent(this, u.isAdmin() ? AdminDashboardActivity.class : MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
    }
}
