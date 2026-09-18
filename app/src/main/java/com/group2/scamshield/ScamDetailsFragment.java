package com.group2.scamshield;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.group2.scamshield.models.ScamModel;
import com.group2.scamshield.utils.DateFormats;
import com.group2.scamshield.utils.RiskUi;
import com.group2.scamshield.utils.ScanArgs;
import java.util.Locale;

/** Everything stored about one scam, plus reporting and blocking. */
public class ScamDetailsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scam_details, container, false);

        final Bundle args = getArguments();
        final ScamModel scam = ScanArgs.scam(args);
        String risk = ScanArgs.risk(args);

        int riskColor = ContextCompat.getColor(requireContext(), RiskUi.color(risk));

        TextView tvRiskPill = view.findViewById(R.id.tvRiskPill);
        tvRiskPill.setText(getString(RiskUi.headline(risk)).toUpperCase(Locale.getDefault()));
        tvRiskPill.setBackgroundTintList(ColorStateList.valueOf(riskColor));

        ((TextView) view.findViewById(R.id.tvScamType))
                .setText(scam == null ? "" : scam.getScamType());
        ((TextView) view.findViewById(R.id.tvFrom))
                .setText(scam == null ? "" : scam.getPhoneNumber());
        ((TextView) view.findViewById(R.id.tvDateDetected))
                .setText(scam == null ? "" : DateFormats.full(scam.getDateTime()));
        ((TextView) view.findViewById(R.id.tvMessage))
                .setText(scam == null ? "" : scam.getMessage());

        LinearLayout indicators = view.findViewById(R.id.indicatorContainer);
        ScanResultFragment.addIndicators(inflater, indicators, ScanArgs.indicators(args), riskColor);

        view.findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigateUp();
            }
        });

        view.findViewById(R.id.btnShare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareScam(scam);
            }
        });

        view.findViewById(R.id.btnReportScam).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.reportScamFragment, args);
            }
        });

        view.findViewById(R.id.btnBlockNumber).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmBlock(scam);
            }
        });

        return view;
    }

    private void shareScam(ScamModel scam) {
        if (scam == null) {
            return;
        }
        String body = getString(R.string.copy_phone, scam.getPhoneNumber()) + "\n"
                + getString(R.string.copy_type, scam.getScamType()) + "\n"
                + getString(R.string.copy_date, DateFormats.full(scam.getDateTime())) + "\n"
                + getString(R.string.copy_message, scam.getMessage());

        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
                .putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(intent, getString(R.string.cd_share)));
    }

    /**
     * Android owns the blocked-numbers list, so the app explains the step and
     * opens that screen rather than pretending to block the number itself.
     */
    private void confirmBlock(ScamModel scam) {
        String number = scam == null ? "" : scam.getPhoneNumber();

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.block_title)
                .setMessage(getString(R.string.block_message, number))
                .setPositiveButton(R.string.action_open_settings, (dialog, which) -> openBlockedNumbers())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void openBlockedNumbers() {
        try {
            TelecomManager telecom =
                    (TelecomManager) requireContext().getSystemService(Context.TELECOM_SERVICE);
            if (telecom != null) {
                startActivity(telecom.createManageBlockedNumbersIntent());
                return;
            }
            Toast.makeText(getActivity(), R.string.toast_no_block_screen, Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(getActivity(), R.string.toast_no_block_screen, Toast.LENGTH_SHORT).show();
        }
    }
}
