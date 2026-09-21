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

/** History rows: risk tile, scam type and number, date on the right. */
public class ScamAdapter extends RecyclerView.Adapter<ScamAdapter.ViewHolder> {

    private final Context context;
    private final List<ScamModel> scamList;
    private final OnScamClickListener listener;

    public interface OnScamClickListener {
        void onScamClick(ScamModel scam);
    }

    public ScamAdapter(Context context, List<ScamModel> scamList, OnScamClickListener listener) {
        this.context = context;
        this.scamList = scamList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_scam, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ScamModel scam = scamList.get(position);

        holder.tvScamType.setText(scam.getScamType());
        holder.tvPhone.setText(scam.getPhoneNumber());
        holder.tvDate.setText(DateFormats.dayLabel(context, scam.getDateTime()));
        holder.tvTime.setText(DateFormats.timeLabel(scam.getDateTime()));

        // Same mapping as the dashboard, so a type such as "Fake Traditional
        // Healer Scam" is never shown as safe here.
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
        return scamList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View tileRisk;
        final ImageView ivRisk;
        final TextView tvScamType, tvPhone, tvRiskLabel, tvDate, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tileRisk = itemView.findViewById(R.id.tileRisk);
            ivRisk = itemView.findViewById(R.id.ivRisk);
            tvScamType = itemView.findViewById(R.id.tvScamType);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvRiskLabel = itemView.findViewById(R.id.tvRiskLabel);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
