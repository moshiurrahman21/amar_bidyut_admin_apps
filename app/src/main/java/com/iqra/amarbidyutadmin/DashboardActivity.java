package com.iqra.amarbidyutadmin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    TextView tvAdminName, tvAdminRole, tvRoleBadge, btnLogout;
    TabLayout tabLayout;
    ViewPager2 viewPager;

    String role, name, subId, subName, staffId;

    // ── ট্যাবের তালিকা (label + fragment) ──
    List<String> tabTitles = new ArrayList<>();
    List<Fragment> tabFragments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ── Views ──
        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminRole = findViewById(R.id.tvAdminRole);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        btnLogout   = findViewById(R.id.btnLogout);
        tabLayout   = findViewById(R.id.tabLayout);
        viewPager   = findViewById(R.id.viewPager);

        // ── আগের Activity থেকে আসা ডাটা (অথবা SharedPreferences থেকে) ──
        SharedPreferences prefs = getSharedPreferences("amarbidyut_admin", MODE_PRIVATE);

        staffId = getIntent().getStringExtra("staff_id");
        name    = getIntent().getStringExtra("name");
        role    = getIntent().getStringExtra("role");
        subId   = getIntent().getStringExtra("sub_id");
        subName = getIntent().getStringExtra("sub_name");

        // intent থেকে না পেলে SharedPreferences থেকে নাও (app পুনরায় খুললে)
        if (role == null) {
            staffId = prefs.getString("staff_id", "");
            name    = prefs.getString("name", "—");
            role    = prefs.getString("role", "sba");
            subId   = prefs.getString("sub_id", "");
            subName = prefs.getString("sub_name", "");
        }

        setupTopBar();
        setupTabs();

        // ── Logout ──
        btnLogout.setOnClickListener(v -> doLogout(prefs));
    }

    // ══════════════════════════════════════════
    // ── টপ বার সেট আপ (নাম, রোল, ব্যাজ) ──
    // ══════════════════════════════════════════
    private void setupTopBar() {

        tvAdminName.setText(name != null ? name : "—");

        if ("admin".equals(role)) {
            tvAdminRole.setText("সুপার অ্যাডমিন · WZPDCL Faridpur");
            tvRoleBadge.setText("ADMIN");
            tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_admin);
        } else {
            String subDisplay = (subName != null && !subName.isEmpty()) ? subName : "—";
            tvAdminRole.setText("SBA Operator · " + subDisplay);
            tvRoleBadge.setText("SBA");
            tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_sba);
        }
    }

    // ══════════════════════════════════════════
    // ── ট্যাব সেট আপ (role অনুযায়ী আলাদা ট্যাব) ──
    // ══════════════════════════════════════════
    private void setupTabs() {

        tabTitles.clear();
        tabFragments.clear();

        // ── সবার জন্য (Admin + SBA) ──
        tabTitles.add("📊 সারসংক্ষেপ");
        tabFragments.add(OverviewFragment.newInstance(role, subId, subName));

        tabTitles.add("📢 নোটিশ");
        tabFragments.add(NoticeFragment.newInstance(role, subId, subName, name));

        tabTitles.add("⚡ ফিডার");
        tabFragments.add(PlaceholderFragment.newInstance("⚡ ফিডার কন্ট্রোল — পরের ধাপে যুক্ত হবে"));

        tabTitles.add("📋 অভিযোগ");
        tabFragments.add(PlaceholderFragment.newInstance("📋 অভিযোগ তালিকা — পরের ধাপে যুক্ত হবে"));

        // ── শুধু Super Admin এর জন্য এক্সট্রা ট্যাব ──
        if ("admin".equals(role)) {

            tabTitles.add("👥 ব্যবহারকারী");
            tabFragments.add(PlaceholderFragment.newInstance("👥 SBA ব্যবহারকারী ব্যবস্থাপনা — পরের ধাপে যুক্ত হবে"));

            tabTitles.add("🏗️ সাব-স্টেশন");
            tabFragments.add(PlaceholderFragment.newInstance("🏗️ সাব-স্টেশন ও ফিডার ব্যবস্থাপনা — পরের ধাপে যুক্ত হবে"));
        }

        DashboardPagerAdapter adapter = new DashboardPagerAdapter(this, tabFragments);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(tabTitles.get(position))
        ).attach();
    }


    // ══════════════════════════════════════════
    // ── লগআউট ──
    // ══════════════════════════════════════════
    private void doLogout(SharedPreferences prefs) {
        prefs.edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
