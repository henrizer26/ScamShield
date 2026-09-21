package com.group2.scamshield;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.group2.scamshield.database.DatabaseHelper;
import com.group2.scamshield.models.ScamModel;
import com.group2.scamshield.utils.AppPrefs;
import com.group2.scamshield.utils.Nav;
import com.group2.scamshield.utils.NotificationHelper;
import com.group2.scamshield.utils.RiskUi;
import com.group2.scamshield.utils.ScamDetector;
import com.group2.scamshield.utils.ScanArgs;
import com.group2.scamshield.views.RiskDonutView;
import androidx.fragment.app.Fragment;
import java.util.List;

/** Dashboard: totals, the verdict ring and the newest alerts. */
public class HomeFragment extends Fragment {

    private static final int MAX_ALERTS = 3;

    private TextView tvScanned, tvDetected, tvReportsSent;
    private TextView tvScamCount, tvSuspiciousCount, tvSafeCount, tvEmpty;
    private RiskDonutView riskDonut;
    private RecyclerView recyclerAlerts;
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvScanned = view.findViewById(R.id.tvScanned);
        tvDetected = view.findViewById(R.id.tvDetected);
        tvReportsSent = view.findViewById(R.id.tvReportsSent);
        tvScamCount = view.findViewById(R.id.tvScamCount);
        tvSuspiciousCount = view.findViewById(R.id.tvSuspiciousCount);
        tvSafeCount = view.findViewById(R.id.tvSafeCount);
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
                NotificationHelper.openSettings(requireContext());
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
        int stored = scams.size();

        // Clean messages are not stored, so the scan counter is kept in prefs;
        // it can only ever be behind the database, never ahead of it.
        int scanned = Math.max(AppPrefs.getScannedCount(requireContext()), stored);

        tvScanned.setText(String.valueOf(scanned));
        tvDetected.setText(String.valueOf(stored));
        tvReportsSent.setText(String.valueOf(dbHelper.getReportedCount()));

        int scam = 0, suspicious = 0, safe = 0;
        for (ScamModel row : scams) {
            String verdict = ScamDetector.riskLevelFor(row.getScamType());
            if (RiskUi.SCAM.equals(verdict)) {
                scam++;
            } else if (RiskUi.SUSPICIOUS.equals(verdict)) {
                suspicious++;
            } else {
                safe++;
            }
        }

        // Safe messages leave no row behind, so they are whatever the scan
        // counter holds over and above the stored ones. Counting them here is
        // what makes the three verdicts add up to "Messages Scanned".
        safe += scanned - stored;

        tvScamCount.setText(String.valueOf(scam));
        tvSuspiciousCount.setText(String.valueOf(suspicious));
        tvSafeCount.setText(String.valueOf(safe));
        riskDonut.setCounts(scam, suspicious, safe);

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
        Navigation.findNavController(requireView())
                .navigate(R.id.scamDetailsFragment, ScanArgs.ofStored(scam));
    }
}
