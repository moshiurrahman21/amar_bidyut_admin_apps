package com.iqra.amarbidyutadmin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * যেসব ট্যাবের ডিজাইন এখনো করা হয়নি, তাদের জন্য সাময়িক placeholder।
 * ব্যবহার: PlaceholderFragment.newInstance("📢 নোটিশ — শীঘ্রই আসছে")
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_TEXT = "arg_text";

    public static PlaceholderFragment newInstance(String text) {
        PlaceholderFragment f = new PlaceholderFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TEXT, text);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_placeholder, container, false);
        TextView tv = v.findViewById(R.id.tvPlaceholderTitle);
        if (getArguments() != null) {
            tv.setText(getArguments().getString(ARG_TEXT, "🚧 এই অংশ পরে যুক্ত হবে"));
        }
        return v;
    }
}
