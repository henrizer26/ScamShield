package com.example.scamshield;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.scamshield.models.ScamModel;
import com.example.scamshield.utils.ScamDetector;
import java.util.List;

public class ScamAdapter extends RecyclerView.Adapter<ScamAdapter.ViewHolder> {

    private Context context;
    private List<ScamModel> scamList;
    private OnScamClickListener listener;

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
        ScamModel scam = scamList.get(position);

        holder.tvPhone.setText(scam.getPhoneNumber());
        holder.tvMessage.setText(scam.getMessage());
        holder.tvScamType.setText(scam.getScamType());
        holder.tvDateTime.setText(scam.getDateTime());

        setLeadingIcon(holder.tvPhone, R.drawable.call_24,
                ContextCompat.getColor(context, R.color.text_primary), 18);
        setLeadingIcon(holder.tvDateTime, R.drawable.schedule_24,
                ContextCompat.getColor(context, R.color.text_secondary), 14);

        // Danger level color, using the same mapping as the dashboard so a
        // type such as "Fake Traditional Healer Scam" is not shown as low risk.
        String risk = ScamDetector.riskLevelFor(scam.getScamType());
        int riskColorRes;
        if ("High".equals(risk)) {
            riskColorRes = android.R.color.holo_red_dark;
        } else if ("Medium".equals(risk)) {
            riskColorRes = android.R.color.holo_orange_dark;
        } else {
            riskColorRes = android.R.color.holo_blue_dark;
        }
        int riskColor = ContextCompat.getColor(context, riskColorRes);
        holder.tvScamType.setTextColor(riskColor);
        setLeadingIcon(holder.tvScamType, R.drawable.warning_24, riskColor, 16);

        if (scam.isReported()) {
            holder.tvStatus.setText(R.string.status_reported);
            int reportedColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
            holder.tvStatus.setTextColor(reportedColor);
            setLeadingIcon(holder.tvStatus, R.drawable.check_circle_24, reportedColor, 14);
        } else {
            holder.tvStatus.setText(R.string.status_pending);
            int pendingColor = ContextCompat.getColor(context, android.R.color.holo_orange_dark);
            holder.tvStatus.setTextColor(pendingColor);
            setLeadingIcon(holder.tvStatus, R.drawable.pending_24, pendingColor, 14);
        }

        // Click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onScamClick(scam);
                }
            }
        });
    }

    /**
     * Puts a tinted vector in front of a row label. The drawable is mutated so
     * the tint stays on this row, and sized explicitly because the 24dp
     * intrinsic bounds are too heavy beside 12-16sp text.
     */
    private void setLeadingIcon(TextView view, @DrawableRes int iconRes, @ColorInt int color, int sizeDp) {
        float density = context.getResources().getDisplayMetrics().density;

        Drawable icon = ContextCompat.getDrawable(context, iconRes);
        if (icon != null) {
            icon = icon.mutate();
            DrawableCompat.setTint(icon, color);
            int size = Math.round(sizeDp * density);
            icon.setBounds(0, 0, size, size);
        }

        view.setCompoundDrawablesRelative(icon, null, null, null);
        view.setCompoundDrawablePadding(Math.round(6 * density));
    }

    @Override
    public int getItemCount() {
        return scamList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPhone, tvMessage, tvScamType, tvDateTime, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvScamType = itemView.findViewById(R.id.tvScamType);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}