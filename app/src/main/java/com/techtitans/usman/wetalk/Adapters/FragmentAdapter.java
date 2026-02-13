package com.techtitans.usman.wetalk.Adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.techtitans.usman.wetalk.Fragments.CallsFragment;
import com.techtitans.usman.wetalk.Fragments.ChatFragment;
import com.techtitans.usman.wetalk.Fragments.StatusFragment;

public class FragmentAdapter extends FragmentPagerAdapter {


    public FragmentAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    public FragmentAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:return new ChatFragment();
            case 1:return new StatusFragment();
            case 2:return new CallsFragment();
        }
        return new ChatFragment();
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            case 0:return "Chats";
            case 1:return "Status";
            case 2:return "Calls";
        }
        return "Chats";
    }
}
