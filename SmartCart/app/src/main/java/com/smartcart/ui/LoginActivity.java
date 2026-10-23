package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.MembershipManager;
import com.smartcart.model.User;
import com.smartcart.util.Session;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Session session = Session.of(this);
        if (session.loggedIn()) { // already signed in: skip the login screen
            startActivity(new Intent(this, session.isAdmin() ? AdminDashboardActivity.class : MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_login);
        TextInputLayout tilE = findViewById(R.id.tilEmail), tilP = findViewById(R.id.tilPassword);
        TextInputEditText etE = findViewById(R.id.etEmail), etP = findViewById(R.id.etPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            tilE.setError(null); tilP.setError(null);
            String email = etE.getText().toString().trim();
            String pw = etP.getText().toString();
            boolean ok = true;
            if (email.isEmpty()) { tilE.setError("Enter your email"); ok = false; }
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilE.setError("Enter a valid email address"); ok = false; }
            if (pw.isEmpty()) { tilP.setError("Enter your password"); ok = false; }
            if (!ok) return;
            User u = DBHelper.get(this).login(email, pw);
            if (u == null) { tilP.setError("Incorrect email or password"); return; }
            if (!u.isAdmin()) u.tier = MembershipManager.recalculate(this, u.id);
            session.save(u);
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        });
        findViewById(R.id.btnGoRegister).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        fill(R.id.btnDemoAdmin, "admin@smartcart.com", "admin123", etE, etP);
        fill(R.id.btnDemoAlice, "alice@demo.com", "demo123", etE, etP);
        fill(R.id.btnDemoBob, "bob@demo.com", "demo123", etE, etP);
        fill(R.id.btnDemoCarl, "carl@demo.com", "demo123", etE, etP);
        fill(R.id.btnDemoDiana, "diana@demo.com", "demo123", etE, etP);
    }

    private void fill(int id, String email, String pw, TextInputEditText etE, TextInputEditText etP) {
        findViewById(id).setOnClickListener(v -> { etE.setText(email); etP.setText(pw); });
    }
}
