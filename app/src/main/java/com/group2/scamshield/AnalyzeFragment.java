package com.group2.scamshield;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.group2.scamshield.database.DatabaseHelper;
import com.group2.scamshield.models.ScamModel;
import com.group2.scamshield.utils.AppPrefs;
import com.group2.scamshield.utils.Nav;
import com.group2.scamshield.utils.NotificationHelper;
import com.group2.scamshield.utils.ProtectionManager;
import com.group2.scamshield.utils.ScamDetector;
import com.group2.scamshield.utils.ScanArgs;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * "Analyze Message": the five ways of supplying a message, then the detector.
 *
 * <p>Paste and Manual Input open the same small sender/message form, Share
 * explains the system share sheet (MainActivity receives it), Read SMS asks for
 * the permission the background receiver needs, and OCR is not implemented.
 */
public class AnalyzeFragment extends Fragment {

    /** Set by MainActivity when the user shares an SMS into the app. */
    public static final String ARG_SHARED_TEXT = "sharedText";

    private static final int SMS_PERMISSION_REQUEST = 200;

    private TextView tvPending;
    private TextView tvReadSmsSub;
    private DatabaseHelper dbHelper;

    private String pendingPhone = "";
    private String pendingMessage = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analyze, container, false);

        tvPending = view.findViewById(R.id.tvPending);
        tvReadSmsSub = view.findViewById(R.id.tvReadSmsSub);
        dbHelper = new DatabaseHelper(getActivity());

        view.findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.homeFragment, null, Nav.tabs());
            }
        });

        view.findViewById(R.id.rowPaste).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pasteFromClipboard();
            }
        });

        view.findViewById(R.id.rowShare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast(R.string.analyze_share_hint);
            }
        });

        view.findViewById(R.id.rowScreenshot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toast(R.string.analyze_ocr_unavailable);
            }
        });

        view.findViewById(R.id.rowReadSms).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSmsPermission();
            }
        });

        view.findViewById(R.id.rowManual).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });

        view.findViewById(R.id.btnAnalyzeNow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyze(v);
            }
        });

        // A message shared from another app arrives as an argument.
        Bundle args = getArguments();
        if (args != null && !TextUtils.isEmpty(args.getString(ARG_SHARED_TEXT))) {
            pendingMessage = args.getString(ARG_SHARED_TEXT);
            args.remove(ARG_SHARED_TEXT);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePendingState();
        updateSmsRowState();
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);

        String clipped = "";
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null
                && clipboard.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            CharSequence text = item.coerceToText(requireContext());
            clipped = text == null ? "" : text.toString();
        }

        if (TextUtils.isEmpty(clipped)) {
            toast(R.string.analyze_clipboard_empty);
            showInputDialog();
            return;
        }

        pendingMessage = clipped;
        toast(R.string.analyze_clipboard_pasted);
        showInputDialog();
    }

    /** Sender and message, pre-filled with whatever has been collected so far. */
    private void showInputDialog() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_manual_input, null, false);
        final EditText etPhone = content.findViewById(R.id.etPhone);
        final EditText etMessage = content.findViewById(R.id.etMessage);
        etPhone.setText(pendingPhone);
        etMessage.setText(pendingMessage);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_add_message)
                .setView(content)
                .setPositiveButton(R.string.action_add, (dialog, which) -> {
                    pendingPhone = etPhone.getText().toString().trim();
                    pendingMessage = etMessage.getText().toString().trim();
                    updatePendingState();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void requestSmsPermission() {
        if (hasSmsPermission()) {
            toast(ProtectionManager.isActive(requireContext())
                    ? R.string.analyze_read_sms_on
                    : R.string.analyze_read_sms_ready);
            return;
        }
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.RECEIVE_SMS},
                SMS_PERMISSION_REQUEST);
    }

    private boolean hasSmsPermission() {
        return ProtectionManager.hasSmsPermission(requireContext());
    }

    private void updateSmsRowState() {
        if (ProtectionManager.isActive(requireContext())) {
            tvReadSmsSub.setText(R.string.analyze_read_sms_on);
        } else if (hasSmsPermission()) {
            tvReadSmsSub.setText(R.string.analyze_read_sms_ready);
        } else {
            tvReadSmsSub.setText(R.string.analyze_read_sms_sub);
        }
    }

    private void updatePendingState() {
        if (TextUtils.isEmpty(pendingMessage)) {
            tvPending.setVisibility(View.GONE);
            return;
        }
        String from = TextUtils.isEmpty(pendingPhone)
                ? getString(R.string.none_placeholder)
                : pendingPhone;
        tvPending.setText(getString(R.string.analyze_ready, from, pendingMessage));
        tvPending.setVisibility(View.VISIBLE);
    }

    private void analyze(View anchor) {
        if (TextUtils.isEmpty(pendingMessage)) {
            toast(R.string.analyze_needs_message);
            showInputDialog();
            return;
        }

        String phone = TextUtils.isEmpty(pendingPhone)
                ? getString(R.string.none_placeholder)
                : pendingPhone;

        ScamDetector.ScamResult result = ScamDetector.detect(pendingMessage, phone);
        AppPrefs.incrementScannedCount(requireContext());

        int id = -1;
        String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        if (result.shouldAlert()) {
            long rowId = dbHelper.addScam(phone, pendingMessage, result.scamType);
            if (rowId != -1) {
                id = (int) rowId;
            }
            NotificationHelper.showRiskAlert(getActivity(), phone, result);
        }

        ScamModel scam = new ScamModel(id, phone, pendingMessage,
                result.shouldAlert() ? result.scamType : getString(R.string.result_safe),
                dateTime, false);

        Bundle args = ScanArgs.of(scam, result.dangerLevel,
                new ArrayList<>(result.indicators), result.classification, result.score);

        // The verdict replaces this screen, so Back returns to the tabs.
        pendingPhone = "";
        pendingMessage = "";
        Navigation.findNavController(anchor).navigate(R.id.scanResultFragment, args);
    }

    private void toast(int messageRes) {
        Toast.makeText(getActivity(), messageRes, Toast.LENGTH_SHORT).show();
    }
}
