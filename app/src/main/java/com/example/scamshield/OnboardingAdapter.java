package com.example.scamshield;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class OnboardingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SIMPLE = 0;
    private static final int TYPE_ANALYZE = 1;
    private static final int TYPE_DASHBOARD = 2;

    private String[] titles;
    private String[] descriptions;
    private int[] images;

    public OnboardingAdapter(String[] titles, String[] descriptions, int[] images) {
        this.titles = titles;
        this.descriptions = descriptions;
        this.images = images;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 3) {
            return TYPE_ANALYZE;
        } else if (position == 4) {
            return TYPE_DASHBOARD;
        }
        return TYPE_SIMPLE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_ANALYZE) {
            View view = inflater.inflate(R.layout.item_onboarding_analyze, parent, false);
            return new AnalyzeViewHolder(view);
        } else if (viewType == TYPE_DASHBOARD) {
            View view = inflater.inflate(R.layout.item_onboarding_dashboard, parent, false);
            return new DashboardViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_onboarding, parent, false);
            return new SimpleViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SimpleViewHolder) {
            SimpleViewHolder simpleHolder = (SimpleViewHolder) holder;
            simpleHolder.tvTitle.setText(titles[position]);
            simpleHolder.tvDescription.setText(descriptions[position]);
            if (position < images.length) {
                simpleHolder.ivImage.setImageResource(images[position]);
            }
        }
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    // ViewHolder for simple screens
    static class SimpleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvDescription;

        SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivOnboardingImage);
            tvTitle = itemView.findViewById(R.id.tvOnboardingTitle);
            tvDescription = itemView.findViewById(R.id.tvOnboardingDescription);
        }
    }

    // ViewHolder for Analyze screen
    static class AnalyzeViewHolder extends RecyclerView.ViewHolder {
        AnalyzeViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // ViewHolder for Dashboard screen
    static class DashboardViewHolder extends RecyclerView.ViewHolder {
        DashboardViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}