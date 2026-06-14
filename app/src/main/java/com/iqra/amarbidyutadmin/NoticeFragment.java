package com.iqra.amarbidyutadmin;

import static android.view.View.VISIBLE;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class NoticeFragment extends Fragment {

    Spinner spNoticeType,spSelectArea,spTitle;
    EditText edDate,edTime,edTime2,edComplain,edTitle;
    TextView tvNotifyStatus, tvNotifyBtn;
    ArrayAdapter<String> adapter,adapter2,adapter3;
    String noticeType,selectArea,date,title;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notice, container, false);

        spNoticeType = view.findViewById(R.id.spNoticeType);
        spSelectArea = view.findViewById(R.id.spSelectArea);
        edDate = view.findViewById(R.id.edDate);
        edTime = view.findViewById(R.id.edTime);
        edTime2 = view.findViewById(R.id.edTime2);
        edComplain = view.findViewById(R.id.edComplain);
        tvNotifyStatus = view.findViewById(R.id.tvNotifyStatus);
        tvNotifyBtn = view.findViewById(R.id.tvNotifyBtn);
        spTitle = view.findViewById(R.id.spTitle);
        edTitle = view.findViewById(R.id.edTitle);


        spinnerData();
        spNoticeType.setAdapter(adapter);
        spSelectArea.setAdapter(adapter2);
        spTitle.setAdapter(adapter3);



        spTitle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {


                title = parent.getSelectedItem().toString();

                if (title.equals("অন্যান্য (নিজে লিখুন)")){

                    edTitle.setVisibility(VISIBLE);
                    title = edTitle.getText().toString();

                }else{

                    edTitle.setVisibility(View.GONE);

                }


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spNoticeType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                noticeType = parent.getSelectedItem().toString();
                Toast.makeText(getContext(), "Item type: "+noticeType, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        spSelectArea.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectArea = parent.getSelectedItem().toString();
                Toast.makeText(getContext(), "Select Area: "+selectArea, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        edDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Button click করলে এটা চালাবে

                Calendar calendar = Calendar.getInstance();

                DatePickerDialog datePicker = new DatePickerDialog(
                        getContext(),
                        (view, year, month, dayOfMonth) -> {
                            // Select করলে এখানে আসবে
                            // month 0 থেকে শুরু তাই +1 করতে হয়
                            date = dayOfMonth + "/"
                                    + (month + 1) + "/" + year;

                            // EditText-এ বসিয়ে দাও
                            edDate.setText(date);
                            Toast.makeText(getContext(), "date: "+date, Toast.LENGTH_SHORT).show();
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

                datePicker.show();

            }
        });

        edTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    TimePickerDialog picker = new TimePickerDialog(
                            getContext(),
                            (view, hour, minute) -> {
                                String time = formatTime(hour, minute);
                                edTime.setText(time);
                            },
                            8, 0, false // default সকাল ৮টা
                    );
                    picker.show();


            }
        });

        edTime2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                    TimePickerDialog picker = new TimePickerDialog(
                            getContext(),
                            (view, hour, minute) -> {
                                String time = formatTime(hour, minute);
                                edTime2.setText(time);
                            },
                            17, 0, false // default বিকাল ৫টা
                    );
                    picker.show();

                }

        });

        tvNotifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String startTime = edTime.getText().toString();
                String endTime   = edTime2.getText().toString();


                String fullTime  = startTime + " — " + endTime;

                Toast.makeText(getContext(), "fullTIme:"+ fullTime + edDate.getText().toString()+"complain: "+ edComplain.getText().toString(), Toast.LENGTH_SHORT).show();

                String url = "https://dainikbhorerbarta.com/bidyut_apps/insert_data.php?t="+title+"&n="+noticeType+"&a="+selectArea+"&d="+date+"&ft="+fullTime+"&des="+edComplain.getText().toString();

                StringRequest stringRequest = new StringRequest(url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {

                        tvNotifyStatus.setVisibility(VISIBLE);
                        tvNotifyStatus.setText(selectArea + " এর সকল গ্রাহক push notification পেয়েছেন।");

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                        Toast.makeText(getContext(), "Error: "+volleyError.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
                );

                RequestQueue requestQueue = Volley.newRequestQueue(getContext());
                requestQueue.add(stringRequest);

// এটা API-তে পাঠাবে

            }
        });


        return view;
    }

    private void spinnerData(){


        List<String> list = new ArrayList<>();
        list.add("শিডিউল লোডশেডিং");
        list.add("রক্ষণাবেক্ষণ কাজ");
        list.add("জরুরি মেরামত");
        list.add("সাধারণ বিজ্ঞপ্তি");
        list.add("অন্যান্য");


        List<String> arealist = new ArrayList<>();
        arealist.add("সব এলাকা");
        arealist.add("শুধু F-1 এলাকা");
        arealist.add("শুধু F-2 এলাকা");
        arealist.add("শুধু F-3 এলাকা");
        arealist.add("শুধু F-4 এলাকা");

        List<String> noticeList = new ArrayList<>();
        noticeList.add("আগামীকাল বিদ্যুৎ বন্ধ থাকবে");
        noticeList.add("আজ বিদ্যুৎ বন্ধ থাকবে");
        noticeList.add("জরুরি মেরামত চলছে");
        noticeList.add("লোডশেডিং চলছে");
        noticeList.add("বিদ্যুৎ সরবরাহ স্বাভাবিক হয়েছে");
        noticeList.add("অন্যান্য (নিজে লিখুন)");



        adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                list
        );

        adapter2 = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                arealist
        );
        adapter3 = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                noticeList
        );



        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        adapter2.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        adapter3.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


    }

    private String formatTime(int hour, int minute) {
        String period;
        int displayHour = hour;

        if (hour == 0) {
            period = "রাত";
            displayHour = 12;
        } else if (hour < 6) {
            period = "রাত";
        } else if (hour < 12) {
            period = "সকাল";
        } else if (hour == 12) {
            period = "দুপুর";
            displayHour = 12;
        } else if (hour < 17) {
            period = "দুপুর";
            displayHour = hour - 12;
        } else if (hour < 18) {
            period = "বিকাল";
            displayHour = hour - 12;
        } else if (hour < 20) {
            period = "সন্ধ্যা";
            displayHour = hour - 12;
        } else {
            period = "রাত";
            displayHour = hour - 12;
        }

        return period + " " + displayHour + ":"
                + String.format("%02d", minute);
    }

}