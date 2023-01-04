package com.example.sfl_is.ui.employee;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.sfl_is.Common;
import com.example.sfl_is.R;
import com.example.sfl_is.databinding.ActivityEmployeeBinding;
import com.example.sfl_is.ui.scanner.ScannerActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EmployeeActivity extends AppCompatActivity {

    private TextView userView;
    private ImageView logoView;

    private Button pendingButton;
    private Button completedButton;

    private ScrollView jobsView;
    private LinearLayout jobsLayout;

    private String username;
    private String role;

    private ActivityEmployeeBinding binding;

    private ArrayList<String> pendingJobsData = new ArrayList<String>();
    private ArrayList<String> completedJobsData = new ArrayList<>();
    private ArrayList<String> jobs = new ArrayList<>();

    private RequestQueue requestQueue;
    private String urlJobs = "https://sfl-dev.azurewebsites.net/api/v1/Jobs/{username}";
    private String urlParcels = "https://sfl-dev.azurewebsites.net/api/v1/JobsParcels/{id}";

    private String currentJobID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);

        binding = ActivityEmployeeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getExtras();
        initViews();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::populateData);
        if (!executorService.isTerminated()) {
            executorService.shutdown();
            try {
                if (executorService.awaitTermination(50, TimeUnit.SECONDS)) {
                    Log.i("EXEC_LOG", "Service terminated successfully1.");
                }
                else {
                    Log.i("EXEC_LOG", "Service terminated unsuccessfully.");
                }
            } catch (InterruptedException e) {
                Log.e("EXEC_LOG", e.toString());
            }
        }

        setDisplay(pendingJobsData);
    }

    private void getExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            username = extras.getString("username");
            role = extras.getString("role");
        }
    }

    private void initViews() {
        userView = binding.userTextView;
        logoView = binding.logoImageView;

        pendingButton = binding.pendingButton;
        completedButton = binding.completedButton;

        jobsView = binding.jobsView;
        jobsLayout = binding.jobsLayout;

        setResources();
        initHandlers();
        applyStyling();
    }

    private void initHandlers() {
        pendingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDisplay(pendingJobsData);
            }
        });
        completedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDisplay(completedJobsData);
            }
        });
    }

    private void setResources() {
        userView.setText(username);
    }

    private void applyStyling() {
        userView.setTextSize(15f);
        userView.setTypeface(null, Typeface.BOLD);
        userView.setGravity(Gravity.CENTER);
    }

    private void setDisplay(ArrayList<String> data) {
        jobsLayout.removeAllViews();

        RelativeLayout rl = new RelativeLayout(EmployeeActivity.this);
        LinearLayout ll = new LinearLayout(EmployeeActivity.this);
        TextView id = new TextView(EmployeeActivity.this);
        TextView dm = new TextView(EmployeeActivity.this);
        TextView wg = new TextView(EmployeeActivity.this);
        TextView st = new TextView(EmployeeActivity.this);

        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(40, 0, 0, 0);
        ll.setLayoutParams(llp);

        rl.setBackgroundColor(Color.LTGRAY);

        id.setText("JobID");
        dm.setText("ParcelID");
        wg.setText("JobType");

        id.setPadding(10, 15, 25, 0);
        id.setTextSize(16f);
        id.setTypeface(null, Typeface.BOLD);
        id.setWidth(230);
        id.setHeight(120);
        id.setTextColor(Color.BLACK);
        id.setGravity(Gravity.CENTER);

        dm.setPadding(10, 15, 25, 0);
        dm.setTextSize(16f);
        dm.setTypeface(null, Typeface.BOLD);
        dm.setWidth(270);
        dm.setHeight(120);
        dm.setTextColor(Color.BLACK);
        dm.setGravity(Gravity.CENTER);

        wg.setPadding(10, 15, 25, 0);
        wg.setTextSize(16f);
        wg.setTypeface(null, Typeface.BOLD);
        wg.setWidth(250);
        wg.setHeight(120);
        wg.setTextColor(Color.BLACK);
        wg.setGravity(Gravity.CENTER);

        st.setPadding(10, 15, 25, 0);
        st.setTextSize(16f);
        st.setTypeface(null, Typeface.BOLD);
        st.setTextColor(Color.BLACK);
        st.setGravity(Gravity.CENTER);

        ll.addView(id);
        ll.addView(dm);
        ll.addView(wg);

        rl.addView(ll);

        jobsLayout.addView(rl);

        for (String jobData : data) {
            RelativeLayout relativeLayout = new RelativeLayout(EmployeeActivity.this);
            LinearLayout textLayout = new LinearLayout(EmployeeActivity.this);

            TextView jobIDView = new TextView(EmployeeActivity.this);
            TextView dimensionsView = new TextView(EmployeeActivity.this);
            TextView weightView = new TextView(EmployeeActivity.this);

            Button button = new Button(EmployeeActivity.this);

            // Set text.
            String[] jobAttributes = jobData.split(" ");
            jobIDView.setText(jobAttributes[0]);
            dimensionsView.setText(jobAttributes[1]);
            weightView.setText(Common.typeIDToName.get(jobAttributes[3]));

            // Implement button functionality.
            button.setText("SCAN");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(EmployeeActivity.this, ScannerActivity.class);
                    i.putExtra("username", username);
                    i.putExtra("jobID", jobAttributes[0]);
                    i.putExtra("parcelID", jobAttributes[1]);
                    i.putExtra("role", role);
                    startActivity(i);
                    finish();
                }
            });

            // Style text.
            jobIDView.setPadding(10, 45, 25, 0);
            jobIDView.setTextSize(15f);
            jobIDView.setTypeface(null, Typeface.BOLD);
            jobIDView.setWidth(220);
            jobIDView.setHeight(170);
            jobIDView.setGravity(Gravity.CENTER);

            dimensionsView.setPadding(10, 15, 25, 0);
            dimensionsView.setTextSize(15f);
            dimensionsView.setTypeface(null, Typeface.BOLD);
            dimensionsView.setWidth(250);
            dimensionsView.setHeight(170);
            dimensionsView.setGravity(Gravity.CENTER);

            weightView.setPadding(10, 0, 25, 0);
            weightView.setTextSize(15f);
            weightView.setTypeface(null, Typeface.BOLD);
            weightView.setHeight(180);
            weightView.setWidth(350);
            weightView.setGravity(Gravity.CENTER);

            // Add text views to text layout.
            textLayout.addView(jobIDView);
            textLayout.addView(dimensionsView);
            textLayout.addView(weightView);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(40, 0, 0, 0);
            textLayout.setLayoutParams(lp);

            // Add text layout and button to relative layout.
            relativeLayout.addView(textLayout);
            // Only add button if job is pending.
            if (jobAttributes[2].toLowerCase(Locale.ROOT).equals("1")) {
                relativeLayout.addView(button);
                // Align button to the right side of page.
                RelativeLayout.LayoutParams buttonParams =
                        (RelativeLayout.LayoutParams) button.getLayoutParams();
                buttonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                buttonParams.rightMargin = 40;
                buttonParams.topMargin = 25;
            }

            // Align text to the left side of page.
            RelativeLayout.LayoutParams textViewLayoutParams =
                    (RelativeLayout.LayoutParams) textLayout.getLayoutParams();
            textViewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            jobsLayout.addView(relativeLayout);
        }
    }

    private void populateData() {
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        try {
            String filledUrlJobs = urlJobs.replace("{username}", username);
            JsonArrayRequest request = new JsonArrayRequest(filledUrlJobs, jsonArrayListenerJobs, errorListener);
            requestQueue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Response.Listener<JSONArray> jsonArrayListenerJobs = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(JSONArray response) {
            ArrayList<String> data = new ArrayList<>();

            for (int i = 0; i < response.length(); i++) {
                try {
                    JSONObject object = response.getJSONObject(i);
                    String id = object.getString("id");
                    String status = object.getString("jobStatusID");
                    String type = object.getString("jobTypeID");

                    data.add(id + " " + status + " " + type);
                    Log.i("VOLLEY_LOG", "Data: " + data);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
            }
            jobs.addAll(data);

            try {
                for (int i = 0; i < jobs.size(); i++) {
                    currentJobID = jobs.get(i).split(" ")[0];
                    String filledUrlParcels = urlParcels.replace("{id}", currentJobID);
                    JsonArrayRequest request = new JsonArrayRequest(filledUrlParcels, jsonArrayListenerParcels, errorListener);
                    requestQueue.add(request);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private Response.Listener<JSONArray> jsonArrayListenerParcels = new Response.Listener<JSONArray>() {
        @Override
        public void onResponse(JSONArray response) {
            ArrayList<String> data = new ArrayList<>();

            for (int i = 0; i < response.length(); i++) {
                try {
                    JSONObject object = response.getJSONObject(i);
                    String jobID = object.getString("jobID");
                    String parcelID = object.getString("parcelID");

                    String status = "";
                    String type = "";
                    for (String job : jobs) {
                        if (job.split(" ")[0].equals(jobID)) {
                            status = job.split(" ")[1];
                            type = job.split(" ")[2];
                            break;
                        }
                    }
                    data.add(jobID + " " + parcelID + " " + status + " " + type);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
            }
            for (String singular : data) {
                if (singular.split(" ")[2].equals("1")) {
                    pendingJobsData.add(singular);
                }
                else {
                    completedJobsData.add(singular);
                }
            }
        }
    };

    private Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("LOG_VOLLEY", error.toString());
        }
    };
}