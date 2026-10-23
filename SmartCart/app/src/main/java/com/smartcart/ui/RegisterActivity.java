package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.model.User;
import com.smartcart.util.Session;
import com.smartcart.util.Util;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_register);
        Util.toolbar(this, "Register", true);
        TextInputLayout tilN = findViewById(R.id.tilName), tilE = findViewById(R.id.tilEmail), tilP = findViewById(R.id.tilPassword), tilC = findViewById(R.id.tilConfirm);
        TextInputEditText etN = findViewById(R.id.etName), etE = findViewById(R.id.etEmail), etP = findViewById(R.id.etPassword), etC = findViewById(R.id.etConfirm);

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            tilN.setError(null); tilE.setError(null); tilP.setError(null); tilC.setError(null);
            String name = etN.getText().toString().trim(), email = etE.getText().toString().trim();
            String pw = etP.getText().toString(), cf = etC.getText().toString();
            boolean ok = true;
            if (name.length() < 2) { tilN.setError("Enter your full name"); ok = false; }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilE.setError("Enter a valid email address"); ok = false; }
            if (pw.length() < 6) { tilP.setError("Password must be at least 6 characters"); ok = false; }
            if (!pw.equals(cf)) { tilC.setError("Passwords do not match"); ok = false; }
            if (!ok) return;
            DBHelper db = DBHelper.get(this);
            if (db.emailExists(email)) { tilE.setError("An account with this email already exists"); return; }
            long id = db.registerUser(name, email, pw);
            if (id < 0) { Util.toast(this, "Could not create the account. Please try again."); return; }
            User u = db.getUser(id);
            Session.of(this).save(u);
            Intent i = new Intent(this, WelcomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
    }
}
