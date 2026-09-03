package com.example.scamshield;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.scamshield.utils.Nav;
import com.example.scamshield.utils.ScanArgs;

/** Confirmation step, named after the channel the report actually went to. */
public class ReportSentFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_sent, container, false);

        Bundle args = getArguments();
        String channel = args == null
                ? ReportScamFragment.CHANNEL_TCRA
                : args.getString(ScanArgs.CHANNEL, ReportScamFragment.CHANNEL_TCRA);
        String channelName = ReportScamFragment.CHANNEL_TCRA.equals(channel)
                ? getString(R.string.channel_tcra)
                : getString(R.string.channel_operator);

        ((TextView) view.findViewById(R.id.tvSentSubtitle))
                .setText(getString(R.string.sent_subtitle, channelName));
        ((TextView) view.findViewById(R.id.tvNext1))
                .setText(getString(R.string.sent_next_1, channelName));

        view.findViewById(R.id.btnDone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.homeFragment, null, Nav.tabs());
            }
        });

        view.findViewById(R.id.btnViewHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.historyFragment, null, Nav.tabs());
            }
        });

        return view;
    }
}
