package com.group2.scamshield;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.group2.scamshield.models.ScamModel;
import com.group2.scamshield.utils.RiskUi;
import com.group2.scamshield.utils.ScanArgs;
import java.util.Locale;

/** Picks who the report goes to and collects optional notes. */
public class ReportScamFragment extends Fragment {

    public static final String CHANNEL_TCRA = "tcra";
    public static final String CHANNEL_OPERATOR = "operator";

    /** TCRA's scam-reporting SMS shortcode. */
    public static final String TCRA_SHORTCODE = "15040";

    private RadioButton radioTcraLeft, radioTcraRight, radioOperatorLeft, radioOperatorRight;
    private String channel = CHANNEL_TCRA;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_scam, container, false);

        final Bundle args = getArguments();
        ScamModel scam = ScanArgs.scam(args);
        String risk = ScanArgs.risk(args);

        int riskColor = ContextCompat.getColor(requireContext(), RiskUi.color(risk));
        int riskBg = ContextCompat.getColor(requireContext(), RiskUi.background(risk));

        view.findViewById(R.id.cardSubject).setBackgroundTintList(ColorStateList.valueOf(riskBg));

        TextView tvScamType = view.findViewById(R.id.tvScamType);
        tvScamType.setText(scam == null ? "" : scam.getScamType());
        tvScamType.setTextColor(riskColor);

        TextView tvRiskLabel = view.findViewById(R.id.tvRiskLabel);
        tvRiskLabel.setText(getString(RiskUi.headline(risk)).toUpperCase(Locale.getDefault()));
        tvRiskLabel.setTextColor(riskColor);
        view.findViewById(R.id.dotRisk).setBackgroundTintList(ColorStateList.valueOf(riskColor));

        radioTcraLeft = view.findViewById(R.id.radioTcraLeft);
        radioTcraRight = view.findViewById(R.id.radioTcraRight);
        radioOperatorLeft = view.findViewById(R.id.radioOperatorLeft);
        radioOperatorRight = view.findViewById(R.id.radioOperatorRight);

        view.findViewById(R.id.rowTcra).setOnClickListener(v -> selectChannel(CHANNEL_TCRA));
        view.findViewById(R.id.rowOperator).setOnClickListener(v -> selectChannel(CHANNEL_OPERATOR));
        selectChannel(CHANNEL_TCRA);

        final EditText etNotes = view.findViewById(R.id.etNotes);
        final TextView tvCounter = view.findViewById(R.id.tvCounter);
        tvCounter.setText(getString(R.string.report_counter, 0));
        etNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCounter.setText(getString(R.string.report_counter, s.length()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.btnContinue).setOnClickListener(v -> {
            Bundle next = ScanArgs.withReport(args, channel, etNotes.getText().toString().trim());
            Navigation.findNavController(v).navigate(R.id.reviewReportFragment, next);
        });

        return view;
    }

    /** Both dots on a row mirror the same choice, as in the reference. */
    private void selectChannel(String value) {
        channel = value;
        boolean tcra = CHANNEL_TCRA.equals(value);
        radioTcraLeft.setChecked(tcra);
        radioTcraRight.setChecked(tcra);
        radioOperatorLeft.setChecked(!tcra);
        radioOperatorRight.setChecked(!tcra);
    }
}
