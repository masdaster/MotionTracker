package com.wishsalad.wishimu;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String INDEX = "index";
    private static final String SEND_ORIENTATION = "send_orientation";
    private static final String SEND_RAW = "send_raw";
    private static final String SAMPLE_RATE = "sample_rate";

    private static final String DEBUG_FORMAT = "%.2f;%.2f;%.2f";

    private ToggleButton start;
    private TextView lblIP;
    private EditText txtIp;
    private EditText txtPort;
    private Spinner spnIndex;
    private CheckBox chkSendOrientation;
    private CheckBox chkSendRaw;
    private Spinner spnSampleRate;
    private CheckBox chkDebug;
    private LinearLayout debugView;
    private TextView acc;
    private TextView gyr;
    private TextView mag;
    private TextView imu;
    private LinearLayout emptyLayout;

    private Timer t = new Timer();

    private boolean isServiceRunning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        emptyLayout = (LinearLayout) findViewById(R.id.empty_layout);

        txtIp = (EditText) findViewById(R.id.ip);
        txtPort = (EditText) findViewById(R.id.port);
        spnIndex = (Spinner) findViewById(R.id.index);
        chkSendOrientation = (CheckBox) findViewById(R.id.sendOrientation);
        chkSendRaw = (CheckBox) findViewById(R.id.sendRaw);
        spnSampleRate = (Spinner) this.findViewById(R.id.sampleRate);
        start = (ToggleButton) findViewById(R.id.start);
        debugView = (LinearLayout) findViewById(R.id.debugView);
        chkDebug = (CheckBox) findViewById(R.id.debug);
        acc = (TextView) findViewById(R.id.acc);
        gyr = (TextView) findViewById(R.id.gyr);
        mag = (TextView) findViewById(R.id.mag);
        imu = (TextView) findViewById(R.id.imu);

        txtIp.setText(preferences.getString(IP, "192.168.1.1"));
        txtPort.setText(preferences.getString(PORT, "5555"));
        chkSendOrientation.setChecked(preferences.getBoolean(SEND_ORIENTATION, true));
        chkSendRaw.setChecked(preferences.getBoolean(SEND_RAW, true));
        chkDebug.setChecked(false);
        populateSampleRates(preferences.getInt(SAMPLE_RATE, 0));
        populateIndex(preferences.getInt(INDEX, 0));

        chkDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDebugVisibility(chkDebug.isChecked());
            }
        });

        setDebugVisibility(chkDebug.isChecked());

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        isServiceRunning = false;

        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
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
            }
        });

        emptyLayout.requestFocus();
    }

    private void startUdpSenderService(String toIp, int port, byte deviceIndex, boolean sendOrientation, boolean sendRaw, int sampleRate) {
        try {
            DatagramPacket p = new DatagramPacket(new byte[] {}, 0);

            DatagramSocket socket = new DatagramSocket();
            p.setAddress(InetAddress.getByName(toIp));
            p.setPort(port);
        }
        catch(Exception e) {
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
        List<DeviceIndex> deviceIndexes = new ArrayList<DeviceIndex>();
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
        List<SampleRate> sampleRates = Arrays.asList(new SampleRate[]{
                new SampleRate(SensorManager.SENSOR_DELAY_NORMAL, "Slowest - 5 FPS"),
                new SampleRate(SensorManager.SENSOR_DELAY_UI, "Average - 16 FPS"),
                new SampleRate(SensorManager.SENSOR_DELAY_GAME, "Fast - 50 FPS"),
                new SampleRate(SensorManager.SENSOR_DELAY_FASTEST, "Fastest - no delay")
        });

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
        ArrayAdapter<T> adapter = new ArrayAdapter<T>(this,
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
//
//    private void debugRaw(float[] acc, float[] gyr, float[] mag) {
//        this.acc.setText(String.format(DEBUG_FORMAT, acc[0], acc[1], acc[2]));
//        this.gyr.setText(String.format(DEBUG_FORMAT, gyr[0], gyr[1], gyr[2]));
//        this.mag.setText(String.format(DEBUG_FORMAT, mag[0], mag[1], mag[2]));
//    }
//
//    private void debugImu(float[] imu) {
//        this.imu.setText(String.format(DEBUG_FORMAT, imu[0], imu[1], imu[2]));
//    }

    private void save() {
        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        preferences.edit()
                .putString(IP, txtIp.getText().toString())
                .putString(PORT, txtPort.getText().toString())
                .putInt(INDEX, getSelectedDeviceIndex())
                .putBoolean(SEND_ORIENTATION, chkSendOrientation.isChecked())
                .putBoolean(SEND_RAW, chkSendRaw.isChecked())
                .putInt(SAMPLE_RATE, getSelectedSampleRateId())
                .commit();
    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        chkDebug.setChecked(false);
//        setDebugVisibility(false);
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        chkDebug.setChecked(false);
//        setDebugVisibility(false);
//        if (isServiceRunning)
//            stopUdpSenderService();
//        save();
//    }
//
//    public void error(final String text) {
//        final Activity activity = this;
//
//        new AlertDialog.Builder(activity).setTitle("Error").setMessage(text).setNeutralButton("OK", null).show();
//        if (isServiceRunning) {
//            stopUdpSenderService();
//            start.setChecked(false);
//            txtIp.setEnabled(true);
//            txtPort.setEnabled(true);
//            chkSendOrientation.setEnabled(true);
//            chkSendRaw.setEnabled(true);
//        }
//    }
}
