package com.group2.scamshield;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.group2.scamshield.models.ScamModel;
import com.group2.scamshield.utils.RiskUi;
import com.group2.scamshield.utils.ScamDetector;
import com.group2.scamshield.utils.ScanArgs;
import java.util.List;

/** The verdict screen: risk headline, what was found, and the two actions. */
public class ScanResultFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_result, container, false);

        final Bundle args = getArguments();
        ScamModel scam = ScanArgs.scam(args);
        String classification = args == null ? ScamDetector.SCAM
                : args.getString(ScanArgs.CLASSIFICATION,
                args.getBoolean(ScanArgs.IS_SCAM, true) ? ScamDetector.SCAM : ScamDetector.SAFE);
        boolean isRisky = !ScamDetector.SAFE.equals(classification);
        int score = args == null ? 0 : args.getInt(ScanArgs.SCORE, 0);
        String risk = isRisky
                ? RiskUi.bucket(args == null ? null : args.getString(ScanArgs.DANGER))
                : RiskUi.LOW;

        int riskColor = ContextCompat.getColor(requireContext(), RiskUi.color(risk));
        int riskBg = ContextCompat.getColor(requireContext(), RiskUi.background(risk));

        // The wash behind the page carries the risk colour into the background.
        view.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{riskBg, ContextCompat.getColor(requireContext(), R.color.bg_app)}));

        ImageView ivVerdict = view.findViewById(R.id.ivVerdict);
        TextView tvTitle = view.findViewById(R.id.tvVerdictTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvVerdictSubtitle);
        TextView tvScamType = view.findViewById(R.id.tvScamType);
        TextView tvDangerLevel = view.findViewById(R.id.tvDangerLevel);
        TextView tvRiskScore = view.findViewById(R.id.tvRiskScore);
        TextView tvPreview = view.findViewById(R.id.tvMessagePreview);
        View dotDanger = view.findViewById(R.id.dotDanger);
        LinearLayout indicatorContainer = view.findViewById(R.id.indicatorContainer);

        ivVerdict.setImageResource(isRisky ? R.drawable.shield_alert_24 : R.drawable.shield_check_24);
        ImageViewCompat.setImageTintList(ivVerdict, ColorStateList.valueOf(riskColor));

        tvTitle.setText(ScamDetector.SCAM.equals(classification)
                ? R.string.result_class_scam
                : ScamDetector.SUSPICIOUS.equals(classification)
                ? R.string.result_class_suspicious
                : R.string.result_class_safe);
        tvTitle.setTextColor(riskColor);
        tvSubtitle.setText(ScamDetector.SCAM.equals(classification)
                ? R.string.result_scam_subtitle
                : ScamDetector.SUSPICIOUS.equals(classification)
                ? R.string.result_suspicious_subtitle
                : R.string.result_safe_subtitle);

        tvScamType.setText(scam == null ? "" : scam.getScamType());
        tvDangerLevel.setText(RiskUi.label(risk));
        tvRiskScore.setText(getString(R.string.result_score, score));
        dotDanger.setBackgroundTintList(ColorStateList.valueOf(riskColor));

        tvPreview.setText(scam == null ? "" : scam.getMessage());
        tvPreview.setBackgroundTintList(ColorStateList.valueOf(riskBg));

        addIndicators(inflater, indicatorContainer, ScanArgs.indicators(args), riskColor);

        view.findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigateUp();
            }
        });

        view.findViewById(R.id.btnViewDetails).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.scamDetailsFragment, args);
            }
        });

        view.findViewById(R.id.btnReportScam).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.reportScamFragment, args);
            }
        });

        return view;
    }

    /** Shared with the details screen: one tinted bullet per indicator. */
    static void addIndicators(LayoutInflater inflater, LinearLayout container,
                              List<String> indicators, int riskColor) {
        container.removeAllViews();

        if (indicators.isEmpty()) {
            View row = inflater.inflate(R.layout.item_indicator, container, false);
            row.findViewById(R.id.dot).setVisibility(View.INVISIBLE);
            ((TextView) row.findViewById(R.id.tvIndicator))
                    .setText(R.string.result_no_indicators);
            container.addView(row);
            return;
        }

        for (String indicator : indicators) {
            View row = inflater.inflate(R.layout.item_indicator, container, false);
            row.findViewById(R.id.dot).setBackgroundTintList(ColorStateList.valueOf(riskColor));
            ((TextView) row.findViewById(R.id.tvIndicator)).setText(indicator);
            container.addView(row);
        }
    }
}
