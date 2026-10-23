package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.logic.MembershipManager;
import com.smartcart.model.User;
import com.smartcart.util.Session;
import com.smartcart.util.Util;

public class ProfileFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_profile, c, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v == null) return;
        DBHelper db = DBHelper.get(requireContext());
        Session s = Session.of(requireContext());
        MembershipManager.recalculate(requireContext(), s.userId());
        User u = db.getUser(s.userId());
        if (u == null) return;
        ((TextView) v.findViewById(R.id.tvAvatar)).setText(u.name.substring(0, 1).toUpperCase());
        ((TextView) v.findViewById(R.id.tvName)).setText(u.name);
        ((TextView) v.findViewById(R.id.tvEmail)).setText(u.email);
        Util.badge((TextView) v.findViewById(R.id.tvTier), u.tier + " member", Util.color(u.tier));

        LinearLayout rows = v.findViewById(R.id.llRows);
        rows.removeAllViews();
        int unread = db.unreadCount(u.id);
        addRow(rows, "\uD83D\uDCE6  My orders", "", OrderHistoryActivity.class);
        addRow(rows, "\u21A9\uFE0F  Request a return", "", ReturnRequestActivity.class);
        addRow(rows, "\uD83D\uDCCB  Return history", "", ReturnHistoryActivity.class);
        addRow(rows, "\uD83C\uDFC5  Membership", u.tier, MembershipActivity.class);
        addRow(rows, "\uD83D\uDD14  Notifications", unread > 0 ? unread + " new" : "", NotificationsActivity.class);

        View logout = LayoutInflater.from(getContext()).inflate(R.layout.item_row, rows, false);
        ((TextView) logout.findViewById(R.id.tvLabel)).setText("\uD83D\uDEAA  Logout");
        logout.setOnClickListener(x -> {
            s.logout();
            Intent i = new Intent(getContext(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
        rows.addView(logout);
    }

    private void addRow(LinearLayout parent, String label, String extra, Class<?> target) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.item_row, parent, false);
        ((TextView) row.findViewById(R.id.tvLabel)).setText(label);
        TextView ex = row.findViewById(R.id.tvExtra);
        if (!extra.isEmpty()) Util.badge(ex, extra, Util.color(extra.endsWith("new") ? "High" : extra));
        row.setOnClickListener(x -> startActivity(new Intent(getContext(), target)));
        parent.addView(row);
    }
}
