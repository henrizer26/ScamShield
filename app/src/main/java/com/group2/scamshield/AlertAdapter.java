package com.group2.scamshield;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.group2.scamshield.models.ScamModel;
import com.group2.scamshield.utils.DateFormats;
import com.group2.scamshield.utils.RiskUi;
import com.group2.scamshield.utils.ScamDetector;
import java.util.List;

/**
 * "Recent Alerts" rows on the dashboard. Same data as the history list, but
 * the reference shows the message text here and only the time on the right.
 */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {

    private final Context context;
    private final List<ScamModel> alerts;
    private final ScamAdapter.OnScamClickListener listener;

    public AlertAdapter(Context context, List<ScamModel> alerts,
                        ScamAdapter.OnScamClickListener listener) {
        this.context = context;
        this.alerts = alerts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ScamModel scam = alerts.get(position);

        holder.tvScamType.setText(scam.getScamType());
        holder.tvMessage.setText(scam.getMessage());
        holder.tvTime.setText(DateFormats.timeLabel(scam.getDateTime()));

        String risk = ScamDetector.riskLevelFor(scam.getScamType());
        int riskColor = ContextCompat.getColor(context, RiskUi.color(risk));

        holder.tvRiskLabel.setText(RiskUi.label(risk));
        holder.tvRiskLabel.setTextColor(riskColor);
        holder.tileRisk.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, RiskUi.background(risk))));
        ImageViewCompat.setImageTintList(holder.ivRisk, ColorStateList.valueOf(riskColor));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onScamClick(scam);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View tileRisk;
        final ImageView ivRisk;
        final TextView tvScamType, tvMessage, tvRiskLabel, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tileRisk = itemView.findViewById(R.id.tileRisk);
            ivRisk = itemView.findViewById(R.id.ivRisk);
            tvScamType = itemView.findViewById(R.id.tvScamType);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvRiskLabel = itemView.findViewById(R.id.tvRiskLabel);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
