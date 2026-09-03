package com.example.scamshield;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

    private final int[] titles;
    private final int[] accents;
    private final int[] descriptions;
    private final int[] images;

    public OnboardingAdapter(int[] titles, int[] accents, int[] descriptions, int[] images) {
        this.titles = titles;
        this.accents = accents;
        this.descriptions = descriptions;
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.ivImage.setImageResource(images[position]);
        holder.tvTitle.setText(titles[position]);
        holder.tvDescription.setText(descriptions[position]);

        // A page without an accent keeps its headline on one line.
        if (accents[position] == 0) {
            holder.tvAccent.setVisibility(View.GONE);
        } else {
            holder.tvAccent.setText(accents[position]);
            holder.tvAccent.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final TextView tvTitle, tvAccent, tvDescription;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivOnboardingImage);
            tvTitle = itemView.findViewById(R.id.tvOnboardingTitle);
            tvAccent = itemView.findViewById(R.id.tvOnboardingAccent);
            tvDescription = itemView.findViewById(R.id.tvOnboardingDescription);
        }
    }
}
