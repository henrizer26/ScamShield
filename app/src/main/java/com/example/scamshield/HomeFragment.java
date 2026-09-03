package com.example.scamshield;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.scamshield.database.DatabaseHelper;
import com.example.scamshield.models.ScamModel;
import com.example.scamshield.utils.ScamDetector;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvTotalScams, tvTodayScams, tvHighRisk, tvMediumRisk, tvLowRisk, tvRecentAlerts;
    private Button btnViewHistory;
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvTotalScams = view.findViewById(R.id.tvTotalScams);
        tvTodayScams = view.findViewById(R.id.tvTodayScams);
        tvHighRisk = view.findViewById(R.id.tvHighRisk);
        tvMediumRisk = view.findViewById(R.id.tvMediumRisk);
        tvLowRisk = view.findViewById(R.id.tvLowRisk);
        tvRecentAlerts = view.findViewById(R.id.tvRecentAlerts);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);

        dbHelper = new DatabaseHelper(getActivity());

        loadStats();

        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.historyFragment);
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
        int today = dbHelper.getTodayScams();

        tvTotalScams.setText(String.valueOf(total));
        tvTodayScams.setText(String.valueOf(today));

        // Count by risk level, derived from the stored scam type.
        int high = 0, medium = 0, low = 0;
        for (ScamModel scam : scams) {
            String risk = ScamDetector.riskLevelFor(scam.getScamType());
            if ("High".equals(risk)) {
                high++;
            } else if ("Medium".equals(risk)) {
                medium++;
            } else {
                low++;
            }
        }

        tvHighRisk.setText(String.valueOf(high));
        tvMediumRisk.setText(String.valueOf(medium));
        tvLowRisk.setText(String.valueOf(low));

        if (total == 0) {
            tvRecentAlerts.setText(R.string.home_all_clear);
            setAlertIcon(R.drawable.check_circle_24, R.color.accent_success);
        } else {
            tvRecentAlerts.setText(getString(R.string.home_scams_detected, total));
            setAlertIcon(R.drawable.warning_24, R.color.accent_warning);
        }
    }

    /**
     * Sits the status vector above the centred alert text, in place of the
     * warning emoji the string used to start with.
     */
    private void setAlertIcon(@DrawableRes int iconRes, @ColorRes int colorRes) {
        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
        if (icon != null) {
            icon = icon.mutate();
            DrawableCompat.setTint(icon, ContextCompat.getColor(requireContext(), colorRes));
        }

        tvRecentAlerts.setCompoundDrawablesRelativeWithIntrinsicBounds(null, icon, null, null);
        tvRecentAlerts.setCompoundDrawablePadding(
                Math.round(8 * getResources().getDisplayMetrics().density));
    }
}
