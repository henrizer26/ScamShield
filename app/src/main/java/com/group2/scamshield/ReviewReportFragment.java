package com.group2.scamshield;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.group2.scamshield.database.DatabaseHelper;
import com.group2.scamshield.models.ScamModel;
import com.group2.scamshield.utils.DateFormats;
import com.group2.scamshield.utils.Nav;
import com.group2.scamshield.utils.RiskUi;
import com.group2.scamshield.utils.ScanArgs;

/** Last look before the report leaves the phone. */
public class ReviewReportFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review_report, container, false);

        final Bundle args = getArguments();
        final ScamModel scam = ScanArgs.scam(args);
        final String channel = args == null
                ? ReportScamFragment.CHANNEL_TCRA
                : args.getString(ScanArgs.CHANNEL, ReportScamFragment.CHANNEL_TCRA);
        final String notes = args == null ? "" : args.getString(ScanArgs.NOTES, "");

        String risk = ScanArgs.risk(args);
        int riskColor = ContextCompat.getColor(requireContext(), RiskUi.color(risk));

        ((TextView) view.findViewById(R.id.tvScamType))
                .setText(scam == null ? "" : scam.getScamType());
        ((TextView) view.findViewById(R.id.tvRiskLevel)).setText(RiskUi.label(risk));
        view.findViewById(R.id.dotRisk).setBackgroundTintList(ColorStateList.valueOf(riskColor));
        ((TextView) view.findViewById(R.id.tvFrom))
                .setText(scam == null ? "" : scam.getPhoneNumber());
        ((TextView) view.findViewById(R.id.tvDateDetected))
                .setText(scam == null ? "" : DateFormats.full(scam.getDateTime()));
        ((TextView) view.findViewById(R.id.tvReportingTo)).setText(channelLabel(channel));
        ((TextView) view.findViewById(R.id.tvNotes))
                .setText(TextUtils.isEmpty(notes) ? getString(R.string.none_placeholder) : notes);

        view.findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigateUp();
            }
        });

        view.findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(v, scam, channel, notes, args);
            }
        });

        return view;
    }

    private String channelLabel(String channel) {
        return ReportScamFragment.CHANNEL_TCRA.equals(channel)
                ? getString(R.string.report_channel_tcra)
                : getString(R.string.report_channel_operator);
    }

    /**
     * TCRA has a shortcode, so that goes straight to the SMS app; an operator
     * report has no fixed address, so the share sheet lets the user pick one.
     */
    private void send(View anchor, ScamModel scam, String channel, String notes, Bundle args) {
        String body = getString(R.string.tcra_report_body,
                scam == null ? "" : scam.getPhoneNumber(),
                scam == null ? "" : scam.getScamType());
        if (!TextUtils.isEmpty(notes)) {
            body = body + "\n" + notes;
        }

        boolean launched;
        if (ReportScamFragment.CHANNEL_TCRA.equals(channel)) {
            Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + ReportScamFragment.TCRA_SHORTCODE))
                    .putExtra("sms_body", body);
            launched = start(sms);
        } else {
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, body);
            launched = start(Intent.createChooser(share, getString(R.string.action_send_report)));
        }

        if (!launched) {
            Toast.makeText(getActivity(), R.string.toast_no_sms_app, Toast.LENGTH_SHORT).show();
            return;
        }

        // Only a stored scam can be marked; a clean message was never saved.
        if (scam != null && scam.getId() != -1) {
            new DatabaseHelper(requireContext()).markAsReported(scam.getId());
        }

        Navigation.findNavController(anchor)
                .navigate(R.id.reportSentFragment, args, Nav.afterReportSent());
    }

    private boolean start(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }
}
