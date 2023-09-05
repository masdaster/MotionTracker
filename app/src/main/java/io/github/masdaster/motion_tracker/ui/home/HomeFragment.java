package io.github.masdaster.motion_tracker.ui.home;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.net.UnknownHostException;
import java.util.logging.Logger;

import io.github.masdaster.motion_tracker.BackgroundWorker;
import io.github.masdaster.motion_tracker.R;
import io.github.masdaster.motion_tracker.SenderService;
import io.github.masdaster.motion_tracker.SenderServiceOptions;
import io.github.masdaster.motion_tracker.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment implements ServiceConnection, BackgroundWorker.OnStateChangeListener {
    private final Logger logger = Logger.getLogger("HomeFragment");
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            this::onPermissionRequestResult);
    private HomeViewModel homeViewModel;
    private SenderService.Binder senderServiceBinder;
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.setViewModel(homeViewModel);
        binding.startServiceButton.setOnClickListener(v -> startSenderService());
        binding.stopServiceButton.setOnClickListener(v -> stopSenderService());
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        ensurePermissionsGranted();
    }

    @Override
    public void onPause() {
        super.onPause();
        Context context = requireContext();
        if(senderServiceBinder != null){
            context.unbindService(this);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        homeViewModel = null;
    }

    public void onPermissionRequestResult(boolean isGranted) {
        if (isGranted) {
            Context context = requireContext();
            context.bindService(new Intent(context, SenderService.class), this, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (service instanceof SenderService.Binder) {
            senderServiceBinder = (SenderService.Binder) service;
            senderServiceBinder.addOnStateChangeListener(this);
            onStateChange();
        } else {
            logger.warning("Unknown service is bound: " + name.getClassName() + ".");
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        setServiceButtonsEnabled(false);
        if (senderServiceBinder != null) {
            senderServiceBinder.removeOnStateChangeListener(this);
            senderServiceBinder = null;
        }
    }

    @Override
    public void onStateChange() {
        if (senderServiceBinder != null) {
            boolean isRunning = senderServiceBinder.isRunning();
            binding.startServiceButton.setEnabled(!isRunning);
            binding.stopServiceButton.setEnabled(isRunning);
        }
    }

    private void ensurePermissionsGranted() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
            onPermissionRequestResult(true);
        } else {
            setServiceButtonsEnabled(false);
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Nullable
    private SenderServiceOptions createSenderServiceOptions() {
        Context context = requireContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SenderServiceOptions.Builder optionsBuilder = new SenderServiceOptions.Builder();
        optionsBuilder.setSensorType(preferences.getString("sensor_type", "rotation_vector"));
        try {
            optionsBuilder.setServerIp(preferences.getString("server_ip", "192.168.1.1"));
        } catch (UnknownHostException ignored) {
            Toast.makeText(context, R.string.invalid_ip, Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            optionsBuilder.setServerPort(Integer.parseInt(preferences.getString("server_port", "5555")));
        } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
            Toast.makeText(context, R.string.invalid_port, Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            optionsBuilder.setDeviceIndex(preferences.getInt("server_device_index", 0));
        } catch (IndexOutOfBoundsException ignored) {
            Toast.makeText(context, R.string.invalid_server_device_index, Toast.LENGTH_SHORT).show();
            return null;
        }
        try {
            optionsBuilder.setSampleRate(Integer.parseInt(preferences.getString("sensor_sample_rate", "2")));
        } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
            Toast.makeText(context, R.string.invalid_sensor_sample_rate, Toast.LENGTH_SHORT).show();
            return null;
        }
        return optionsBuilder.build();
    }

    private void startSenderService() {
        SenderServiceOptions serviceOptions = createSenderServiceOptions();
        if (serviceOptions != null) {
            Context context = requireContext();
            Intent intent = new Intent(context, SenderService.class);
            intent.putExtra(SenderService.INTENT_EXTRA_OPTIONS, serviceOptions);
            context.startForegroundService(intent);
        }
    }

    private void stopSenderService() {
        if (senderServiceBinder != null) {
            senderServiceBinder.stopMotionTracking();
        }
        Context context = requireContext();
        context.stopService(new Intent(context, SenderService.class));
    }

    private void setServiceButtonsEnabled(boolean enabled) {
        binding.startServiceButton.setEnabled(enabled);
        binding.stopServiceButton.setEnabled(enabled);
    }
}