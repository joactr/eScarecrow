package com.example.scarecrow.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.scarecrow.MainActivity;
import com.example.scarecrow.R;
import com.example.scarecrow.databinding.FragmentHomeBinding;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Switch yourSwitchButton = requireActivity().findViewById(R.id.sActivatePreview);
        PreviewView previewView = requireActivity().findViewById(R.id.previewView);
        yourSwitchButton.setChecked(true); // true is open, false is closed.

        yourSwitchButton.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b){ previewView.setVisibility(View.VISIBLE); }
            else  { previewView.setVisibility(View.GONE); }
        });
        ((MainActivity) getActivity()).iniciarCamara();

    }
}