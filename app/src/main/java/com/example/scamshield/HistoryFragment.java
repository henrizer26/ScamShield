package com.example.scamshield;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.scamshield.database.DatabaseHelper;
import com.example.scamshield.models.ScamModel;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private Button btnClearAll;
    private DatabaseHelper dbHelper;
    private ScamAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        btnClearAll = view.findViewById(R.id.btnClearAll);

        dbHelper = new DatabaseHelper(getActivity());

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        loadScamHistory();

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearAllDialog();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadScamHistory();
    }

    private void loadScamHistory() {
        List<ScamModel> scamList = dbHelper.getAllScams();

        if (scamList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            btnClearAll.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            btnClearAll.setVisibility(View.VISIBLE);

            adapter = new ScamAdapter(getActivity(), scamList, new ScamAdapter.OnScamClickListener() {
                @Override
                public void onScamClick(ScamModel scam) {
                    showScamOptionsDialog(scam);
                }
            });
            recyclerView.setAdapter(adapter);
        }
    }

    private void showScamOptionsDialog(final ScamModel scam) {
        String[] options = {
                getString(R.string.option_call),
                getString(R.string.option_report_tcra),
                getString(R.string.option_delete),
                getString(R.string.option_copy)
        };

        // One vector per option, in the same order as the labels above.
        final int[] icons = {
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_scam_options);
        builder.setAdapter(optionsAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Call
                        callNumber(scam.getPhoneNumber());
                        break;
                    case 1: // Report to TCRA
                        reportToTCRA(scam);
                        break;
                    case 2: // Delete
                        deleteScam(scam);
                        break;
                    case 3: // Copy Details
                        copyDetails(scam);
                        break;
                }
            }
        });
        builder.show();
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
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.toast_no_sms_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteScam(ScamModel scam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_delete_title);
        builder.setMessage(R.string.dialog_delete_message);
        builder.setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteScam(scam.getId());
                loadScamHistory();
                Toast.makeText(getActivity(), R.string.toast_scam_deleted, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.show();
    }

    private void copyDetails(ScamModel scam) {
        String details = getString(R.string.copy_phone, scam.getPhoneNumber()) + "\n"
                + getString(R.string.copy_type, scam.getScamType()) + "\n"
                + getString(R.string.copy_message, scam.getMessage()) + "\n"
                + getString(R.string.copy_date, scam.getDateTime()) + "\n"
                + getString(R.string.copy_status, getString(scam.isReported()
                        ? R.string.status_reported : R.string.status_pending));

        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(getString(R.string.clipboard_label), details);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), R.string.toast_details_copied, Toast.LENGTH_SHORT).show();
    }

    private void showClearAllDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_clear_title);
        builder.setMessage(R.string.dialog_clear_message);
        builder.setPositiveButton(R.string.action_delete_all, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deleteAllScams();
                loadScamHistory();
                Toast.makeText(getActivity(), R.string.toast_history_cleared, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.show();
    }
}