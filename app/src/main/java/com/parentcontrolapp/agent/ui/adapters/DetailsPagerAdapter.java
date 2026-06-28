package com.parentcontrolapp.agent.ui.adapters;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.parentcontrolapp.agent.ui.tabs.TabActivityFragment;
import com.parentcontrolapp.agent.ui.tabs.TabDeviceSettingsFragment;
import com.parentcontrolapp.agent.ui.tabs.TabScreenshotsFragment;
import com.parentcontrolapp.agent.ui.tabs.TabTimelineFragment;

public class DetailsPagerAdapter extends FragmentStateAdapter {

    private final String deviceId;

    public DetailsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String deviceId) {
        super(fragmentActivity);
        this.deviceId = deviceId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Bundle args = new Bundle();
        args.putString("device_id", deviceId);

        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new TabActivityFragment();
                break;
            case 1:
                fragment = new TabTimelineFragment();
                break;
            case 2:
                fragment = new TabScreenshotsFragment();
                break;
            case 3:
                fragment = new TabDeviceSettingsFragment();
                break;
            default:
                fragment = new TabActivityFragment();
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}