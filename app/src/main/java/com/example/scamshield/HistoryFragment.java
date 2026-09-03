package com.example.scamshield;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.scamshield.database.DatabaseHelper;
import com.example.scamshield.models.ScamModel;
import com.example.scamshield.utils.DateFormats;
import com.example.scamshield.utils.RiskUi;
import com.example.scamshield.utils.ScamDetector;
import com.example.scamshield.utils.ScanArgs;
import java.util.ArrayList;
import java.util.List;

/** Stored scams, filtered by risk. Tapping a row opens its actions. */
public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private DatabaseHelper dbHelper;

    private final TextView[] chips = new TextView[4];
    /** null means "All"; otherwise a RiskUi bucket. */
    private String activeFilter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        dbHelper = new DatabaseHelper(getActivity());

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        chips[0] = view.findViewById(R.id.chipAll);
        chips[1] = view.findViewById(R.id.chipHigh);
        chips[2] = view.findViewById(R.id.chipMedium);
        chips[3] = view.findViewById(R.id.chipLow);

        final String[] buckets = {null, RiskUi.HIGH, RiskUi.MEDIUM, RiskUi.LOW};
        for (int i = 0; i < chips.length; i++) {
            final String bucket = buckets[i];
            chips[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applyFilter(bucket);
                }
            });
        }
        applyFilter(null);

        view.findViewById(R.id.btnFilter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterMenu(v);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadScamHistory();
    }

    private void applyFilter(String bucket) {
        activeFilter = bucket;
        chips[0].setSelected(bucket == null);
        chips[1].setSelected(RiskUi.HIGH.equals(bucket));
        chips[2].setSelected(RiskUi.MEDIUM.equals(bucket));
        chips[3].setSelected(RiskUi.LOW.equals(bucket));
        loadScamHistory();
    }

    /** The funnel repeats the chips and carries the clear-all action. */
    private void showFilterMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 0, 0, R.string.filter_all);
        menu.getMenu().add(0, 1, 1, R.string.risk_high);
        menu.getMenu().add(0, 2, 2, R.string.risk_medium);
        menu.getMenu().add(0, 3, 3, R.string.risk_low);
        menu.getMenu().add(0, 4, 4, R.string.dialog_clear_title);

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case 0:
                        applyFilter(null);
                        return true;
                    case 1:
                        applyFilter(RiskUi.HIGH);
                        return true;
                    case 2:
                        applyFilter(RiskUi.MEDIUM);
                        return true;
                    case 3:
                        applyFilter(RiskUi.LOW);
                        return true;
                    case 4:
                        confirmClearAll();
                        return true;
                    default:
                        return false;
                }
            }
        });
        menu.show();
    }

    private void loadScamHistory() {
        if (dbHelper == null) {
            return;
        }

        List<ScamModel> scamList = new ArrayList<>();
        for (ScamModel scam : dbHelper.getAllScams()) {
            if (activeFilter == null
                    || activeFilter.equals(ScamDetector.riskLevelFor(scam.getScamType()))) {
                scamList.add(scam);
            }
        }

        if (scamList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(activeFilter == null
                    ? R.string.history_empty
                    : R.string.history_empty_filter);
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ScamAdapter adapter = new ScamAdapter(getActivity(), scamList,
                new ScamAdapter.OnScamClickListener() {
                    @Override
                    public void onScamClick(ScamModel scam) {
                        showScamOptionsDialog(scam);
                    }
                });
        recyclerView.setAdapter(adapter);
    }

    /** Row actions: open the detail screen, or act on the number directly. */
    private void showScamOptionsDialog(final ScamModel scam) {
        String[] options = {
                getString(R.string.action_view_details),
                getString(R.string.option_call),
                getString(R.string.option_report_tcra),
                getString(R.string.option_delete),
                getString(R.string.option_copy)
        };

        // One vector per option, in the same order as the labels above.
        final int[] icons = {
                R.drawable.info_24,
                R.drawable.call_24,
                R.drawable.sms_24,
                R.drawable.delete_24,
                R.drawable.content_copy_24
        };

        ArrayAdapter<String> optionsAdapter =
                new ArrayAdapter<String>(requireContext(), R.layout.item_dialog_option, options) {
                    @NonNull
                    @Override
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        TextView row = (TextView) super.getView(position, convertView, parent);
                        row.setCompoundDrawablesRelativeWithIntrinsicBounds(icons[position], 0, 0, 0);
                        return row;
                    }
                };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.dialog_scam_options);
        builder.setAdapter(optionsAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        openDetails(scam);
                        break;
                    case 1:
                        callNumber(scam.getPhoneNumber());
                        break;
                    case 2:
                        reportToTCRA(scam);
                        break;
                    case 3:
                        deleteScam(scam);
                        break;
                    case 4:
                        copyDetails(scam);
                        break;
                }
            }
        });
        builder.show();
    }

    private void openDetails(ScamModel scam) {
        String risk = ScamDetector.riskLevelFor(scam.getScamType());
        Bundle args = ScanArgs.of(scam, risk, new ArrayList<String>(), true);
        Navigation.findNavController(requireView()).navigate(R.id.scamDetailsFragment, args);
    }

    private void callNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.toast_no_dialer_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void reportToTCRA(ScamModel scam) {
        // Report to TCRA (15040)
        String message = getString(R.string.tcra_report_body,
                scam.getPhoneNumber(), scam.getScamType());

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:15040"));
        smsIntent.putExtra("sms_body", message);

        try {
            startActivity(smsIntent);
            dbHelper.markAsReported(scam.getId());
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.toast_no_sms_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteScam(final ScamModel scam) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteScam(scam.getId());
                        loadScamHistory();
                        Toast.makeText(getActivity(), R.string.toast_scam_deleted,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void copyDetails(ScamModel scam) {
        String details = getString(R.string.copy_phone, scam.getPhoneNumber()) + "\n"
                + getString(R.string.copy_type, scam.getScamType()) + "\n"
                + getString(R.string.copy_message, scam.getMessage()) + "\n"
                + getString(R.string.copy_date, DateFormats.full(scam.getDateTime())) + "\n"
                + getString(R.string.copy_status, getString(scam.isReported()
                ? R.string.status_reported
                : R.string.status_pending));

        ClipboardManager clipboard =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.clipboard_label), details));
            Toast.makeText(getActivity(), R.string.toast_details_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_clear_title)
                .setMessage(R.string.dialog_clear_message)
                .setPositiveButton(R.string.action_delete_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteAllScams();
                        loadScamHistory();
                        Toast.makeText(getActivity(), R.string.toast_history_cleared,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
