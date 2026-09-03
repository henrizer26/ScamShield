package com.example.scamshield;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.scamshield.database.DatabaseHelper;
import com.example.scamshield.models.ScamModel;
import com.example.scamshield.utils.AppPrefs;
import com.example.scamshield.utils.Nav;
import com.example.scamshield.utils.RiskUi;
import com.example.scamshield.utils.ScamDetector;
import com.example.scamshield.utils.ScanArgs;
import com.example.scamshield.views.RiskDonutView;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

/** Dashboard: totals, the risk ring and the newest alerts. */
public class HomeFragment extends Fragment {

    private static final int MAX_ALERTS = 3;

    private TextView tvScanned, tvDetected, tvReportsSent;
    private TextView tvHighRisk, tvMediumRisk, tvLowRisk, tvEmpty;
    private RiskDonutView riskDonut;
    private RecyclerView recyclerAlerts;
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvScanned = view.findViewById(R.id.tvScanned);
        tvDetected = view.findViewById(R.id.tvDetected);
        tvReportsSent = view.findViewById(R.id.tvReportsSent);
        tvHighRisk = view.findViewById(R.id.tvHighRisk);
        tvMediumRisk = view.findViewById(R.id.tvMediumRisk);
        tvLowRisk = view.findViewById(R.id.tvLowRisk);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        riskDonut = view.findViewById(R.id.riskDonut);
        recyclerAlerts = view.findViewById(R.id.recyclerAlerts);

        dbHelper = new DatabaseHelper(getActivity());
        recyclerAlerts.setLayoutManager(new LinearLayoutManager(getActivity()));

        view.findViewById(R.id.tvSeeAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.historyFragment, null, Nav.tabs());
            }
        });

        view.findViewById(R.id.ivNotifications).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotificationSettings();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        List<ScamModel> scams = dbHelper.getAllScams();
        int total = scams.size();

        // Clean messages are not stored, so the scan counter is kept in prefs;
        // it can only ever be behind the database, never ahead of it.
        tvScanned.setText(String.valueOf(Math.max(AppPrefs.getScannedCount(requireContext()), total)));
        tvDetected.setText(String.valueOf(total));
        tvReportsSent.setText(String.valueOf(dbHelper.getReportedCount()));

        int high = 0, medium = 0, low = 0;
        for (ScamModel scam : scams) {
            String risk = ScamDetector.riskLevelFor(scam.getScamType());
            if (RiskUi.HIGH.equals(risk)) {
                high++;
            } else if (RiskUi.MEDIUM.equals(risk)) {
                medium++;
            } else {
                low++;
            }
        }

        tvHighRisk.setText(String.valueOf(high));
        tvMediumRisk.setText(String.valueOf(medium));
        tvLowRisk.setText(String.valueOf(low));
        riskDonut.setCounts(high, medium, low);

        showAlerts(scams);
    }

    private void showAlerts(List<ScamModel> scams) {
        if (scams.isEmpty()) {
            recyclerAlerts.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        recyclerAlerts.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        List<ScamModel> newest = scams.subList(0, Math.min(MAX_ALERTS, scams.size()));
        recyclerAlerts.setAdapter(new AlertAdapter(getActivity(), newest,
                new ScamAdapter.OnScamClickListener() {
                    @Override
                    public void onScamClick(ScamModel scam) {
                        openDetails(scam);
                    }
                }));
    }

    private void openDetails(ScamModel scam) {
        String risk = ScamDetector.riskLevelFor(scam.getScamType());
        Bundle args = ScanArgs.of(scam, risk, new ArrayList<String>(), true);
        Navigation.findNavController(requireView()).navigate(R.id.scamDetailsFragment, args);
    }

    /** The bell opens this app's notification settings rather than a stub screen. */
    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:" + requireContext().getPackageName()));
        }
        startActivity(intent);
    }
}
