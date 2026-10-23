package com.smartcart.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.util.Session;

/** Hosts the five bottom-navigation sections: Home, Categories, Wishlist, Cart, Profile. */
public class MainActivity extends AppCompatActivity {
    public String pendingCategory = null;
    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Session s = Session.of(this);
        if (!s.loggedIn() || s.isAdmin()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f;
            if (id == R.id.nav_home) f = new HomeFragment();
            else if (id == R.id.nav_categories) f = new CategoriesFragment();
            else if (id == R.id.nav_wishlist) f = new WishlistFragment();
            else if (id == R.id.nav_cart) f = new CartFragment();
            else f = new ProfileFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
            return true;
        });
        if (b == null) nav.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nav != null) refreshBadge();
    }

    public void showHome(String category) {
        pendingCategory = category;
        nav.setSelectedItemId(R.id.nav_home);
    }

    /** Shows the number of items in the cart on the Cart tab. */
    public void refreshBadge() {
        int n = DBHelper.get(this).cartCount(Session.of(this).userId());
        BadgeDrawable badge = nav.getOrCreateBadge(R.id.nav_cart);
        if (n > 0) { badge.setNumber(n); badge.setVisible(true); }
        else badge.setVisible(false);
    }
}
