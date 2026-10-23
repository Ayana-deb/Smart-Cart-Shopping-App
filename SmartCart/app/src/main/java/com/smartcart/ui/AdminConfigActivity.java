package com.smartcart.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.AppConfig;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lets the admin edit every scoring rule, membership threshold and the return window. */
public class AdminConfigActivity extends AppCompatActivity {
    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private DBHelper db;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Session s = Session.of(this);
        if (!s.loggedIn() || !s.isAdmin()) { finish(); return; }
        setContentView(R.layout.activity_admin_config);
        Util.toolbar(this, "Rules and settings", true);
        db = DBHelper.get(this);
        build();
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            for (Map.Entry<String, EditText> e : fields.entrySet()) {
                String t = e.getValue().getText().toString().trim();
                if (t.isEmpty() || t.length() > 9) { e.getValue().setError("Enter a number"); return; }
            }
            for (Map.Entry<String, EditText> e : fields.entrySet())
                db.setSetting(e.getKey(), Integer.parseInt(e.getValue().getText().toString().trim()));
            Util.toast(this, "Settings saved");
        });
        findViewById(R.id.btnReset).setOnClickListener(v -> { db.resetSettings(); build(); Util.toast(this, "Defaults restored"); });
    }

    private void build() {
        LinearLayout ll = findViewById(R.id.llConfig);
        ll.removeAllViews();
        fields.clear();
        for (Map.Entry<String, Integer> e : AppConfig.DEFAULTS.entrySet()) {
            TextView label = new TextView(this);
            label.setText(AppConfig.LABELS.get(e.getKey()));
            label.setTextColor(0xFF374151);
            label.setTextSize(13);
            label.setPadding(0, Util.dp(this, 10), 0, 0);
            EditText et = new EditText(this);
            et.setInputType(InputType.TYPE_CLASS_NUMBER);
            et.setText(String.valueOf(AppConfig.get(this, e.getKey())));
            et.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ll.addView(label);
            ll.addView(et);
            fields.put(e.getKey(), et);
        }
    }
}
