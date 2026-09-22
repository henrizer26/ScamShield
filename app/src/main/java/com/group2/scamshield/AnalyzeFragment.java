package com.group2.scamshield;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.group2.scamshield.database.DatabaseHelper;
import com.group2.scamshield.models.ScamModel;
import com.group2.scamshield.utils.AppPrefs;
import com.group2.scamshield.utils.DateFormats;
import com.group2.scamshield.utils.Nav;
import com.group2.scamshield.utils.NotificationHelper;
import com.group2.scamshield.utils.ScamDetector;
import com.group2.scamshield.utils.ScanArgs;
import java.util.ArrayList;

/**
 * "Analyze Message": the sender and message form, then the detector.
 *
 * <p>A message shared into the app from Messages (MainActivity receives it)
 * arrives here already filled in.
 */
public class AnalyzeFragment extends Fragment {

    /** Set by MainActivity when the user shares an SMS into the app. */
    public static final String ARG_SHARED_TEXT = "sharedText";

    private EditText etPhone;
    private EditText etMessage;
    private DatabaseHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analyze, container, false);

        etPhone = view.findViewById(R.id.etPhone);
        etMessage = view.findViewById(R.id.etMessage);
        dbHelper = new DatabaseHelper(getActivity());

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.homeFragment, null, Nav.tabs()));

        view.findViewById(R.id.btnAnalyzeNow).setOnClickListener(v -> analyze(v));

        // A message shared from another app arrives as an argument.
        Bundle args = getArguments();
        if (args != null && !TextUtils.isEmpty(args.getString(ARG_SHARED_TEXT))) {
            etMessage.setText(args.getString(ARG_SHARED_TEXT));
            args.remove(ARG_SHARED_TEXT);
        }

        return view;
    }

    private void analyze(View anchor) {
        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            etMessage.setError(getString(R.string.analyze_needs_message));
            etMessage.requestFocus();
            return;
        }

        String sender = etPhone.getText().toString().trim();
        String phone = TextUtils.isEmpty(sender)
                ? getString(R.string.none_placeholder)
                : sender;

        ScamDetector.ScamResult result = ScamDetector.detect(message, phone);
        AppPrefs.incrementScannedCount(requireContext());

        int id = -1;
        long now = System.currentTimeMillis();

        if (result.shouldAlert()) {
            long rowId = dbHelper.addScam(phone, message, result.scamType, now);
            if (rowId != -1) {
                id = (int) rowId;
            }
            NotificationHelper.showRiskAlert(getActivity(), phone, result);
        }

        ScamModel scam = new ScamModel(id, phone, message,
                result.shouldAlert() ? result.scamType : getString(R.string.result_safe),
                DateFormats.stamp(now), false);

        Bundle args = ScanArgs.of(scam, result.dangerLevel,
                new ArrayList<>(result.indicators), result.classification, result.score);

        // Cleared first, so coming back from the verdict offers an empty form.
        etPhone.setText("");
        etMessage.setText("");
        Navigation.findNavController(anchor).navigate(R.id.scanResultFragment, args);
    }
}
