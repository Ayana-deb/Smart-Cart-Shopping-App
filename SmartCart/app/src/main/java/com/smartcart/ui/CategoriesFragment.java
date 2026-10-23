package com.smartcart.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.smartcart.R;
import com.smartcart.db.DBHelper;
import com.smartcart.util.Util;
import java.util.List;

public class CategoriesFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        return inf.inflate(R.layout.fragment_categories, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        DBHelper db = DBHelper.get(requireContext());
        List<String> cats = db.getCategories();
        RecyclerView rv = v.findViewById(R.id.rvCategories);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rv.setAdapter(new SimpleAdapter<>(cats, R.layout.item_category, (row, cat) -> {
            Util.emoji((TextView) row.findViewById(R.id.tvEmoji), Util.categoryEmoji(cat), cat);
            ((TextView) row.findViewById(R.id.tvName)).setText(cat);
            ((TextView) row.findViewById(R.id.tvCount)).setText(db.getProducts(cat, null).size() + " products");
            row.setOnClickListener(x -> {
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).showHome(cat);
            });
        }));
    }
}
