package com.example.scamshield;

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
import com.example.scamshield.models.ScamModel;
import com.example.scamshield.utils.RiskUi;
import com.example.scamshield.utils.ScanArgs;
import java.util.List;

/** The verdict screen: risk headline, what was found, and the two actions. */
public class ScanResultFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_result, container, false);

        final Bundle args = getArguments();
        ScamModel scam = ScanArgs.scam(args);
        boolean isScam = args != null && args.getBoolean(ScanArgs.IS_SCAM, true);
        String risk = isScam
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
        TextView tvPreview = view.findViewById(R.id.tvMessagePreview);
        View dotDanger = view.findViewById(R.id.dotDanger);
        LinearLayout indicatorContainer = view.findViewById(R.id.indicatorContainer);

        ivVerdict.setImageResource(isScam ? R.drawable.shield_alert_24 : R.drawable.shield_check_24);
        ImageViewCompat.setImageTintList(ivVerdict, ColorStateList.valueOf(riskColor));

        tvTitle.setText(isScam ? RiskUi.headline(risk) : R.string.result_safe);
        tvTitle.setTextColor(riskColor);
        tvSubtitle.setText(isScam ? R.string.result_scam_subtitle : R.string.result_safe_subtitle);

        tvScamType.setText(scam == null ? "" : scam.getScamType());
        tvDangerLevel.setText(RiskUi.label(risk));
        dotDanger.setBackgroundTintList(ColorStateList.valueOf(riskColor));

        tvPreview.setText(scam == null ? "" : scam.getMessage());
        tvPreview.setBackgroundTintList(ColorStateList.valueOf(riskBg));

        addIndicators(inflater, indicatorContainer, ScanArgs.indicators(args), riskColor);

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
