package com.parentcontrolapp.agent.ui.adapters;

import android.annotation.SuppressLint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.parentcontrolapp.agent.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<DeviceItem> devices;
    private final OnDeviceClickListener listener;
    private final OnDeviceMoreClickListener moreListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
    }

    public interface OnDeviceMoreClickListener {
        void onDeviceMoreClick(DeviceItem device, View anchorView);
    }

    public DeviceAdapter(List<DeviceItem> devices, OnDeviceClickListener listener, OnDeviceMoreClickListener moreListener) {
        this.devices = devices != null ? devices : new ArrayList<>();
        this.listener = listener;
        this.moreListener = moreListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<DeviceItem> newDevices) {
        this.devices = newDevices != null ? newDevices : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceItem device = devices.get(position);
        holder.bind(device, listener, moreListener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName, tvDeviceName, tvStatus;
        ImageView imgDevice;
        View viewStatusDot;
        ImageButton btnMore;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tv_child_name);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            imgDevice = itemView.findViewById(R.id.img_device);
            viewStatusDot = itemView.findViewById(R.id.view_status_dot);
            btnMore = itemView.findViewById(R.id.btn_more);
        }

        void bind(DeviceItem device, OnDeviceClickListener listener, OnDeviceMoreClickListener moreListener) {
            tvChildName.setText(device.getChildName());
            tvDeviceName.setText(device.getDeviceName());

            tvStatus.setText(device.getStatusText());
            tvStatus.setTextColor(device.getStatusColor());

            if (viewStatusDot != null) {
                GradientDrawable dotBackground = new GradientDrawable();
                dotBackground.setShape(GradientDrawable.OVAL);
                dotBackground.setColor(device.getStatusColor());
                viewStatusDot.setBackground(dotBackground);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDeviceClick(device);
            });
            btnMore.setOnClickListener(v -> {
                if (moreListener != null) moreListener.onDeviceMoreClick(device, v);
            });
        }
    }
}