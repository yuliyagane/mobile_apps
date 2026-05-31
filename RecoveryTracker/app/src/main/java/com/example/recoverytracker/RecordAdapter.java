package com.example.recoverytracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    private List<RecoveryRecord> records;
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(RecoveryRecord record);
    }

    public RecordAdapter(List<RecoveryRecord> records, OnDeleteListener listener) {
        this.records = records;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecoveryRecord record = records.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        holder.tvDate.setText(sdf.format(new Date(record.getDate())));
        holder.tvPain.setText("😖 " + record.getPainLevel());
        holder.tvActivity.setText("💪 " + record.getActivityLevel() + "%");
        holder.tvMedications.setText("💊 " + record.getMedications());
        holder.tvNotes.setText("📝 " + record.getNotes());

        holder.itemView.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvPain, tvActivity, tvMedications, tvNotes;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPain = itemView.findViewById(R.id.tvPain);
            tvActivity = itemView.findViewById(R.id.tvActivity);
            tvMedications = itemView.findViewById(R.id.tvMedications);
            tvNotes = itemView.findViewById(R.id.tvNotes);
        }
    }
}