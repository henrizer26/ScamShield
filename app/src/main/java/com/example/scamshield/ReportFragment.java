package com.example.scamshield;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import com.example.scamshield.database.DatabaseHelper;
import com.example.scamshield.utils.NotificationHelper;
import com.example.scamshield.utils.ScamDetector;

public class ReportFragment extends Fragment {

    private EditText etPhone, etMessage;
    private Button btnAnalyze, btnReport;
    private TextView tvResult;
    private ImageView ivResult;
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        etPhone = view.findViewById(R.id.etPhone);
        etMessage = view.findViewById(R.id.etMessage);
        btnAnalyze = view.findViewById(R.id.btnAnalyze);
        btnReport = view.findViewById(R.id.btnReport);
        tvResult = view.findViewById(R.id.tvResult);
        ivResult = view.findViewById(R.id.ivResult);

        dbHelper = new DatabaseHelper(getActivity());

        // Analyze button
        btnAnalyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzeScam();
            }
        });

        // Report button
        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportScam();
            }
        });

        return view;
    }

    private void analyzeScam() {
        String phone = etPhone.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (phone.isEmpty()) {
            etPhone.setError(getString(R.string.error_phone_required));
            etPhone.requestFocus();
            return;
        }

        if (message.isEmpty()) {
            etMessage.setError(getString(R.string.error_message_required));
            etMessage.requestFocus();
            return;
        }

        // Use ScamDetector
        ScamDetector.ScamResult result = ScamDetector.detect(message, phone);

        if (result.isScam) {
            // Build result message
            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.result_scam_detected)).append("\n\n");
            sb.append(getString(R.string.result_type, result.scamType)).append("\n");
            sb.append(getString(R.string.result_danger_level, result.dangerLevel)).append("\n\n");
            sb.append(getString(R.string.result_indicators)).append("\n");
            for (String indicator : result.indicators) {
                sb.append("• ").append(indicator).append("\n");
            }

            tvResult.setText(sb.toString());
            int scamColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark);
            tvResult.setTextColor(scamColor);
            showResultIcon(R.drawable.warning_24, scamColor);

            // Save to database
            long id = dbHelper.addScam(phone, message, result.scamType);
            if (id != -1) {
                Toast.makeText(getActivity(), R.string.toast_scam_saved, Toast.LENGTH_SHORT).show();
            }

            // Send notification
            NotificationHelper.createNotificationChannel(getActivity());
            NotificationHelper.showScamAlert(getActivity(), phone, result.scamType);

        } else {
            tvResult.setText(R.string.result_safe);
            int safeColor = ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark);
            tvResult.setTextColor(safeColor);
            showResultIcon(R.drawable.check_circle_24, safeColor);
        }
    }

    /** Shows the verdict icon that used to lead the result text as an emoji. */
    private void showResultIcon(@DrawableRes int iconRes, @ColorInt int color) {
        ivResult.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(ivResult, ColorStateList.valueOf(color));
        ivResult.setVisibility(View.VISIBLE);
    }

    private void reportScam() {
        String phone = etPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            etPhone.setError(getString(R.string.error_phone_required));
            etPhone.requestFocus();
            return;
        }

        // Report to TCRA
        Toast.makeText(getActivity(),
                getString(R.string.toast_report_sent, phone),
                Toast.LENGTH_LONG).show();

        etPhone.setText("");
        etMessage.setText("");
        tvResult.setText("");
        ivResult.setVisibility(View.GONE);
    }
}