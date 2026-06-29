package com.iqra.amarbidyutadmin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ComplaintFragment extends Fragment {

    String role, subId;
    LinearLayout complaintContainer;
    SwipeRefreshLayout swipeRefresh;
    TextView tvEmpty, tvTotal;
    Spinner spinnerStatus, spinnerFeeder;
    RequestQueue requestQueue;

    // Filter state
    String selectedStatus = "all";
    String selectedFeederId = "all";

    // Feeder list for spinner
    ArrayList<String> feederNames = new ArrayList<>();
    ArrayList<String> feederIds = new ArrayList<>();

    public static ComplaintFragment newInstance(String role, String subId) {
        ComplaintFragment f = new ComplaintFragment();
        Bundle args = new Bundle();
        args.putString("role", role);
        args.putString("sub_id", subId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_complaint, container, false);

        role  = getArguments() != null ? getArguments().getString("role", "") : "";
        subId = getArguments() != null ? getArguments().getString("sub_id", "0") : "0";

        complaintContainer = view.findViewById(R.id.complaintContainer);
        swipeRefresh       = view.findViewById(R.id.swipeRefresh);
        tvEmpty            = view.findViewById(R.id.tvEmpty);
        tvTotal            = view.findViewById(R.id.tvTotal);
        spinnerStatus      = view.findViewById(R.id.spinnerStatus);
        spinnerFeeder      = view.findViewById(R.id.spinnerFeeder);

        requestQueue = Volley.newRequestQueue(requireContext());

        setupStatusFilter();
        loadFeeders();

        swipeRefresh.setOnRefreshListener(this::loadComplaints);

        return view;
    }

    // ══════════════════════════════════════════
    // Status Filter Spinner
    // ══════════════════════════════════════════
    private void setupStatusFilter() {
        String[] statusItems = {"সব অভিযোগ", "🔴 নতুন", "👁 দেখা হয়েছে", "✓ সমাধান"};
        String[] statusValues = {"all", "pending", "seen", "resolved"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, statusItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedStatus = statusValues[pos];
                loadComplaints();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ══════════════════════════════════════════
    // Feeder list লোড করো Spinner-এর জন্য
    // ══════════════════════════════════════════
    private void loadFeeders() {
        String url = "https://dainikbhorerbarta.com/bidyut_super_admin/get_feeders.php"
                + "?role=" + role + "&sub_id=" + subId;

        com.android.volley.toolbox.JsonArrayRequest req = new com.android.volley.toolbox.JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        feederNames.clear();
                        feederIds.clear();

                        feederNames.add("সব ফিডার");
                        feederIds.add("all");

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject f = response.getJSONObject(i);
                            feederNames.add(f.optString("name", ""));
                            feederIds.add(f.optString("id", ""));
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_item,
                                feederNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerFeeder.setAdapter(adapter);

                        spinnerFeeder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                                selectedFeederId = feederIds.get(pos);
                                loadComplaints();
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> loadComplaints()
        );

        req.setShouldCache(false);
        requestQueue.add(req);
    }

    // ══════════════════════════════════════════
    // Complaint লোড করো
    // ══════════════════════════════════════════
    private void loadComplaints() {
        String url = "https://dainikbhorerbarta.com/bidyut_super_admin/get_complaints.php"
                + "?role=" + role
                + "&sub_id=" + subId
                + "&status=" + selectedStatus
                + "&feeder_id=" + selectedFeederId;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    swipeRefresh.setRefreshing(false);
                    try {
                        complaintContainer.removeAllViews();
                        JSONArray list = response.getJSONArray("complaints");
                        int total = response.optInt("total", list.length());

                        tvTotal.setText("মোট: " + total + "টি অভিযোগ");

                        if (list.length() == 0) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }

                        tvEmpty.setVisibility(View.GONE);
                        for (int i = 0; i < list.length(); i++) {
                            addComplaintCard(list.getJSONObject(i));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> swipeRefresh.setRefreshing(false)
        );

        req.setShouldCache(false);
        requestQueue.add(req);
    }

    // ══════════════════════════════════════════
    // Complaint Card
    // ══════════════════════════════════════════
    private void addComplaintCard(JSONObject c) {
        try {
            String id            = c.optString("id", "");
            String ticket        = c.optString("ticket", "#" + id);
            String complaintType = c.optString("complaint_type", "");
            String description   = c.optString("description", "");
            String phone         = c.optString("phone", "");
            String areaName      = c.optString("area_name", "");
            String feederName    = c.optString("feeder_name", "");
            String status        = c.optString("status", "pending");
            String createdAt     = c.optString("created_at", "");

            CardView card = new CardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 12);
            card.setLayoutParams(cardParams);
            card.setRadius(16f);
            card.setCardElevation(4f);

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(32, 28, 32, 28);
            inner.setBackgroundColor(Color.WHITE);

            // Status color bar
            View statusBar = new View(requireContext());
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 8);
            statusBar.setLayoutParams(barParams);
            switch (status) {
                case "resolved": statusBar.setBackgroundColor(Color.parseColor("#16A34A")); break;
                case "seen":     statusBar.setBackgroundColor(Color.parseColor("#D97706")); break;
                default:         statusBar.setBackgroundColor(Color.parseColor("#E03B3B")); break;
            }

            // Ticket + Badge row
            LinearLayout topRow = new LinearLayout(requireContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvTicket = new TextView(requireContext());
            LinearLayout.LayoutParams ticketParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvTicket.setLayoutParams(ticketParams);
            tvTicket.setText("🎫 " + ticket);
            tvTicket.setTextSize(11f);
            tvTicket.setTextColor(Color.parseColor("#64748B"));

            String badgeText;
            int badgeColor, badgeTextColor;
            switch (status) {
                case "resolved":
                    badgeText = "✓ সমাধান";
                    badgeColor = Color.parseColor("#DCFCE7");
                    badgeTextColor = Color.parseColor("#166534");
                    break;
                case "seen":
                    badgeText = "👁 দেখা হয়েছে";
                    badgeColor = Color.parseColor("#FEF9C3");
                    badgeTextColor = Color.parseColor("#92400E");
                    break;
                default:
                    badgeText = "🔴 নতুন";
                    badgeColor = Color.parseColor("#FEE2E2");
                    badgeTextColor = Color.parseColor("#991B1B");
                    break;
            }
            TextView tvBadge = new TextView(requireContext());
            tvBadge.setText(badgeText);
            tvBadge.setTextSize(10f);
            tvBadge.setTextColor(badgeTextColor);
            tvBadge.setPadding(16, 6, 16, 6);
            tvBadge.setBackgroundColor(badgeColor);

            topRow.addView(tvTicket);
            topRow.addView(tvBadge);

            // Complaint type
            TextView tvType = new TextView(requireContext());
            tvType.setText("📋 " + complaintType);
            tvType.setTextSize(13f);
            tvType.setTextColor(Color.parseColor("#0F1923"));
            tvType.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            typeParams.setMargins(0, 10, 0, 0);
            tvType.setLayoutParams(typeParams);

            // এলাকা ও ফিডার
            TextView tvArea = new TextView(requireContext());
            tvArea.setText("📍 " + areaName + " · " + feederName);
            tvArea.setTextSize(11f);
            tvArea.setTextColor(Color.parseColor("#64748B"));
            LinearLayout.LayoutParams areaParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            areaParams.setMargins(0, 6, 0, 0);
            tvArea.setLayoutParams(areaParams);

            // বিস্তারিত
            TextView tvDesc = new TextView(requireContext());
            tvDesc.setText(description.isEmpty() ? "বিস্তারিত দেওয়া হয়নি" : description);
            tvDesc.setTextSize(12f);
            tvDesc.setTextColor(Color.parseColor("#475569"));
            LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            descParams.setMargins(0, 6, 0, 0);
            tvDesc.setLayoutParams(descParams);

            // ফোন
            TextView tvPhone = new TextView(requireContext());
            tvPhone.setText("📞 " + (phone.isEmpty() ? "নম্বর নেই" : phone));
            tvPhone.setTextSize(12f);
            tvPhone.setTextColor(Color.parseColor("#1D4ED8"));
            LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            phoneParams.setMargins(0, 6, 0, 0);
            tvPhone.setLayoutParams(phoneParams);

            // সময়
            TextView tvTime = new TextView(requireContext());
            tvTime.setText("🕐 " + createdAt);
            tvTime.setTextSize(10f);
            tvTime.setTextColor(Color.parseColor("#94A3B8"));
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            timeParams.setMargins(0, 6, 0, 14);
            tvTime.setLayoutParams(timeParams);

            // Status বাটন row
            LinearLayout btnRow = new LinearLayout(requireContext());
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            if (!status.equals("seen") && !status.equals("resolved")) {
                TextView btnSeen = makeBtn("👁 দেখা হয়েছে", "#FEF9C3", "#92400E");
                btnSeen.setOnClickListener(v -> updateStatus(id, "seen"));
                btnRow.addView(btnSeen);
            }

            if (!status.equals("resolved")) {
                TextView btnResolved = makeBtn("✓ সমাধান হয়েছে", "#DCFCE7", "#166534");
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rp.setMargins(12, 0, 0, 0);
                btnResolved.setLayoutParams(rp);
                btnResolved.setOnClickListener(v -> updateStatus(id, "resolved"));
                btnRow.addView(btnResolved);
            }

            card.addView(statusBar);
            inner.addView(topRow);
            inner.addView(tvType);
            inner.addView(tvArea);
            inner.addView(tvDesc);
            inner.addView(tvPhone);
            inner.addView(tvTime);
            inner.addView(btnRow);
            card.addView(inner);

            complaintContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TextView makeBtn(String text, String bgColor, String textColor) {
        TextView btn = new TextView(requireContext());
        btn.setText(text);
        btn.setTextSize(11f);
        btn.setTextColor(Color.parseColor(textColor));
        btn.setBackgroundColor(Color.parseColor(bgColor));
        btn.setPadding(20, 10, 20, 10);
        return btn;
    }

    private void updateStatus(String complaintId, String newStatus) {
        String url = "https://dainikbhorerbarta.com/bidyut_super_admin/update_complaint_status.php";

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            Toast.makeText(requireContext(), "আপডেট হয়েছে ✓", Toast.LENGTH_SHORT).show();
                            loadComplaints();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(requireContext(), "সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("id", complaintId);
                params.put("status", newStatus);
                return params;
            }
        };

        req.setShouldCache(false);
        requestQueue.add(req);
    }
}