package com.smartcart.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.model.AppNotification;
import com.smartcart.util.Session;
import com.smartcart.util.Util;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_list);
        Util.toolbar(this, "Notifications", true);
        DBHelper db = DBHelper.get(this);
        long uid = Session.of(this).userId();
        List<AppNotification> list = db.getNotifications(uid);
        TextView empty = findViewById(R.id.tvEmpty);
        empty.setText("You are all caught up \uD83D\uDD14\nOrder and return updates will show up here.");
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new SimpleAdapter<>(new ArrayList<>(list), R.layout.item_notification, (v, n) -> {
            ((TextView) v.findViewById(R.id.tvTitle)).setText((n.read ? "" : "\u25CF ") + n.title);
            ((TextView) v.findViewById(R.id.tvMessage)).setText(n.message);
            ((TextView) v.findViewById(R.id.tvTime)).setText(Util.dateTime(n.createdAt));
        }));
        db.markAllRead(uid); // list above keeps the unread dots for this visit
    }
}
