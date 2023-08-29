package io.github.masdaster.motion_tracker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {

    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String INDEX = "index";
    private static final String SEND_ORIENTATION = "send_orientation";
    private static final String SEND_RAW = "send_raw";
    private static final String SAMPLE_RATE = "sample_rate";

    private ToggleButton start;
    private EditText txtIp;
    private EditText txtPort;
    private Spinner spnIndex;
    private CheckBox chkSendOrientation;
    private CheckBox chkSendRaw;
    private Spinner spnSampleRate;
    private CheckBox chkDebug;
    private LinearLayout debugView;
    private LinearLayout emptyLayout;
    private boolean isServiceRunning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        emptyLayout = findViewById(R.id.empty_layout);

        txtIp = findViewById(R.id.ip);
        txtPort = findViewById(R.id.port);
        spnIndex = findViewById(R.id.index);
        chkSendOrientation = findViewById(R.id.sendOrientation);
        chkSendRaw = findViewById(R.id.sendRaw);
        spnSampleRate = this.findViewById(R.id.sampleRate);
        start = findViewById(R.id.start);
        debugView = findViewById(R.id.debugView);
        chkDebug = findViewById(R.id.debug);

        txtIp.setText(preferences.getString(IP, "192.168.1.1"));
        txtPort.setText(preferences.getString(PORT, "5555"));
        chkSendOrientation.setChecked(preferences.getBoolean(SEND_ORIENTATION, true));
        chkSendRaw.setChecked(preferences.getBoolean(SEND_RAW, true));
        chkDebug.setChecked(false);
        populateSampleRates(preferences.getInt(SAMPLE_RATE, 0));
        populateIndex(preferences.getInt(INDEX, 0));

        chkDebug.setOnClickListener(view -> setDebugVisibility(chkDebug.isChecked()));

        setDebugVisibility(chkDebug.isChecked());

        isServiceRunning = false;

        start.setOnClickListener(view -> {
            save();
            if (!isServiceRunning) {
                String ip = txtIp.getText().toString();
                int port = Integer.parseInt(txtPort.getText().toString());
                boolean sendOrientation = chkSendOrientation.isChecked();
                boolean sendRaw = chkSendRaw.isChecked();
                startUdpSenderService(ip, port, getSelectedDeviceIndex(), sendOrientation, sendRaw, getSelectedSampleRateId());
                isServiceRunning = true;
            } else {
                stopUdpSenderService();
                isServiceRunning = false;
            }

            start.setChecked(isServiceRunning);
            txtIp.setEnabled(!isServiceRunning);
            txtPort.setEnabled(!isServiceRunning);
            chkSendOrientation.setEnabled(!isServiceRunning);
            chkSendRaw.setEnabled(!isServiceRunning);
            emptyLayout.requestFocus();
        });

        emptyLayout.requestFocus();
    }

    private void startUdpSenderService(String toIp, int port, byte deviceIndex, boolean sendOrientation, boolean sendRaw, int sampleRate) {
        try {
            DatagramPacket p = new DatagramPacket(new byte[]{}, 0);

            p.setAddress(InetAddress.getByName(toIp));
            p.setPort(port);
        } catch (Exception e) {
            Log.e("Error", "Can't create endpoint " + e.getMessage());
        }

        Intent serviceIntent = new Intent(this, UdpSenderService.class);
        serviceIntent.putExtra("toIp", toIp);
        serviceIntent.putExtra("port", port);
        serviceIntent.putExtra("deviceIndex", deviceIndex);
        serviceIntent.putExtra("sendOrientation", sendOrientation);
        serviceIntent.putExtra("sendRaw", sendRaw);
        serviceIntent.putExtra("sampleRate", sampleRate);
        startForegroundService(serviceIntent);
    }

    private void stopUdpSenderService() {
        Intent serviceIntent = new Intent(this, UdpSenderService.class);
        stopService(serviceIntent);
    }

    private void setDebugVisibility(boolean show) {
        debugView.setVisibility(show ? LinearLayout.VISIBLE : LinearLayout.INVISIBLE);
    }

    private void populateIndex(int defaultIndex) {
        List<DeviceIndex> deviceIndexes = new ArrayList<>();
        for (byte index = 0; index < 16; index++) {
            deviceIndexes.add(new DeviceIndex(index));
        }

        DeviceIndex selectedDeviceIndex = null;
        for (DeviceIndex deviceIndex : deviceIndexes) {
            if (deviceIndex.getIndex() == defaultIndex) {
                selectedDeviceIndex = deviceIndex;
                break;
            }
        }

        populateSpinner(spnIndex, deviceIndexes, selectedDeviceIndex);
    }

    private void populateSampleRates(int defaultSampleRate) {
        // delay information from http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-monitor
        List<SampleRate> sampleRates = Arrays.asList(new SampleRate(SensorManager.SENSOR_DELAY_NORMAL, "Slowest - 5 FPS"),
                new SampleRate(SensorManager.SENSOR_DELAY_UI, "Average - 16 FPS"),
                new SampleRate(SensorManager.SENSOR_DELAY_GAME, "Fast - 50 FPS"),
                new SampleRate(SensorManager.SENSOR_DELAY_FASTEST, "Fastest - no delay"));

        SampleRate selectedSampleRate = null;
        for (SampleRate sampleRate : sampleRates) {
            if (sampleRate.getId() == defaultSampleRate) {
                selectedSampleRate = sampleRate;
                break;
            }
        }

        populateSpinner(spnSampleRate, sampleRates, selectedSampleRate);
    }

    private <T> void populateSpinner(Spinner spinner, List<T> items, T selectedItem) {
        ArrayAdapter<T> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        spinner.setAdapter(adapter);
        spinner.setSelection(items.indexOf(selectedItem), false);
    }

    private int getSelectedSampleRateId() {
        return ((SampleRate) spnSampleRate.getSelectedItem()).getId();
    }

    private byte getSelectedDeviceIndex() {
        return ((DeviceIndex) spnIndex.getSelectedItem()).getIndex();
    }

    private void save() {
        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        preferences.edit()
                .putString(IP, txtIp.getText().toString())
                .putString(PORT, txtPort.getText().toString())
                .putInt(INDEX, getSelectedDeviceIndex())
                .putBoolean(SEND_ORIENTATION, chkSendOrientation.isChecked())
                .putBoolean(SEND_RAW, chkSendRaw.isChecked())
                .putInt(SAMPLE_RATE, getSelectedSampleRateId())
                .apply();
    }
}
