package com.iqra.amarbidyutadmin;

import static android.view.View.VISIBLE;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class FeederFragment extends Fragment {

    RecyclerView recyclerView;
    Spinner spReasonTitle;
    EditText edDate;
    TextView sumbitBtn, cancleBtn, tvSuccess;
    CardView offPanel;

    // যে feeder toggle করেছে সেটা মনে রাখব
    MyAdapter.MyViewHolder pendingHolder = null;
    ScrollView scrollView;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_feeder, container, false);

        recyclerView   = view.findViewById(R.id.recyclerView);
        spReasonTitle  = view.findViewById(R.id.spReasonTitle);
        edDate         = view.findViewById(R.id.edDate);
        sumbitBtn      = view.findViewById(R.id.sumbitBtn);
        cancleBtn      = view.findViewById(R.id.cancleBtn);
        offPanel       = view.findViewById(R.id.offPanel);
        tvSuccess      = view.findViewById(R.id.tvSuccess);
        scrollView      = view.findViewById(R.id.scrollView);

        // শুরুতে off panel লুকানো

        scrollView.post(() ->
                scrollView.smoothScrollTo(0, offPanel.getTop())
        );

        offPanel.setVisibility(View.GONE);
        tvSuccess.setVisibility(View.GONE);

        // Spinner data
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{
                        "লোডশেডিং",
                        "যান্ত্রিক ত্রুটি",
                        "লাইনের কাজ চলছে",
                        "ঝড়-বৃষ্টি / দুর্যোগ",
                        "দুর্ঘটনা",
                        "অন্যান্য"
                }
        );
        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spReasonTitle.setAdapter(spinnerAdapter);

        // TimePicker
        edDate.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(
                    getContext(),
                    (view1, hour, minute) -> {
                        String time = String.format(
                                "%02d:%02d", hour, minute);
                        edDate.setText(time);
                    },
                    9, 0, false
            );
            timePicker.show();
        });

        // ✅ Submit — off panel তথ্য নিয়ে feeder বন্ধ করো
        sumbitBtn.setOnClickListener(v -> {
            if (pendingHolder == null) return;

            String reason = spReasonTitle
                    .getSelectedItem().toString();
            String eta = edDate.getText().toString();

            if (eta.isEmpty()) {
                Toast.makeText(getContext(),
                        "সময় দিন", Toast.LENGTH_SHORT).show();
                return;
            }

            // Toggle বন্ধ করো
            pendingHolder.toggleContainer
                    .setBackgroundResource(R.drawable.toggle_off);
            pendingHolder.toggleContainer
                    .setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

            // Status বদলাও
            pendingHolder.tvStatus.setText("বন্ধ");
            pendingHolder.tvStatus.setTextColor(
                    Color.parseColor("#E03B3B"));
            pendingHolder.tvStatus.setBackgroundColor(
                    Color.parseColor("#FCEBEB"));
            pendingHolder.statusDot.setBackgroundResource(
                    R.drawable.circle_red);

            // isOn false করো
            pendingHolder.isOn[0] = false;

            // Off panel লুকাও
            offPanel.setVisibility(View.GONE);

            // Success দেখাও
            tvSuccess.setText("✓  ফিডার বন্ধ হয়েছে।" +
                    " কারণ: " + reason +
                    " | সময়: " + eta +
                    " | গ্রাহকদের notify করা হয়েছে।");
            tvSuccess.setVisibility(View.VISIBLE);

            // Reset
            pendingHolder = null;
            edDate.setText("");
        });

        // ✅ Cancel — toggle আবার চালু করো
        cancleBtn.setOnClickListener(v -> {
            if (pendingHolder != null) {
                // Toggle আবার চালু করো
                pendingHolder.toggleContainer
                        .setBackgroundResource(R.drawable.toggle_on);
                pendingHolder.toggleContainer
                        .setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

                pendingHolder.tvStatus.setText("চালু");
                pendingHolder.tvStatus.setTextColor(
                        Color.parseColor("#18A55A"));
                pendingHolder.tvStatus.setBackgroundColor(
                        Color.parseColor("#EDFAF3"));
                pendingHolder.statusDot.setBackgroundResource(
                        R.drawable.circle_green);

                pendingHolder.isOn[0] = true;
                pendingHolder = null;
            }

            offPanel.setVisibility(View.GONE);
            tvSuccess.setVisibility(View.GONE);
        });

        MyAdapter adapter = new MyAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext()));

        return view;
    }

    class MyAdapter extends RecyclerView
            .Adapter<MyAdapter.MyViewHolder> {

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext())
                    .inflate(R.layout.feeder_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull MyViewHolder holder, int position) {

            holder.tvFeeder.setText(
                    "ফিডার F-" + (position + 1));
            holder.tvFeederArea.setText(
                    "সদরপুর বাজার ও আশেপাশের এলাকা");

            // শুরুতে চালু দেখাও
            setOn(holder);

            holder.toggleContainer.setOnClickListener(v -> {

                if (holder.isOn[0]) {
                    // চালু → বন্ধ করতে চাইছে
                    // আগে off panel দেখাও
                    pendingHolder = holder;
                    offPanel.setVisibility(View.VISIBLE);
                    tvSuccess.setVisibility(View.GONE);

                } else {
                    // বন্ধ → চালু করছে
                    holder.isOn[0] = true;
                    setOn(holder);
                    offPanel.setVisibility(View.GONE);
                    tvSuccess.setText(
                            "✓  ফিডার F-" + (position+1) +
                                    " চালু হয়েছে। গ্রাহকদের notify করা হয়েছে।");
                    tvSuccess.setVisibility(View.VISIBLE);
                }
            });
        }

        // চালু state set করো
        private void setOn(MyViewHolder holder) {
            holder.toggleContainer.setBackgroundResource(
                    R.drawable.toggle_on);
            holder.toggleContainer.setGravity(
                    Gravity.END | Gravity.CENTER_VERTICAL);
            holder.tvStatus.setText("চালু");
            holder.tvStatus.setTextColor(
                    Color.parseColor("#18A55A"));
            holder.tvStatus.setBackgroundColor(
                    Color.parseColor("#EDFAF3"));
            holder.statusDot.setBackgroundResource(
                    R.drawable.circle_green);
        }

        @Override
        public int getItemCount() { return 6; }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView tvFeeder, tvFeederArea, tvStatus;
            View statusDot;
            LinearLayout toggleContainer;
            boolean[] isOn = {true}; // ← ViewHolder এ রাখো

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFeeder       = itemView.findViewById(R.id.tvFeederName);
                tvFeederArea   = itemView.findViewById(R.id.tvFeederArea);
                tvStatus       = itemView.findViewById(R.id.tvStatus);
                statusDot      = itemView.findViewById(R.id.statusDot);
                toggleContainer= itemView.findViewById(R.id.toggleContainer);
            }
        }
    }
}