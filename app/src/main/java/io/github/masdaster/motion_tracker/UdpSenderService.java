package io.github.masdaster.motion_tracker;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpSenderService extends Service implements SensorEventListener {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final byte SEND_RAW = 0x01;
    private static final byte SEND_ORIENTATION = 0x02;
    private static final byte SEND_NONE = 0x00;
    final float[] rotationMatrix = new float[16];
    private final IBinder mBinder = new MyBinder();
    private final DatagramPacket p = new DatagramPacket(new byte[]{}, 0);
    private final byte[] buf = new byte[50];
    final float[] acc = new float[]{0, 0, 0};
    final float[] mag = new float[]{0, 0, 0};
    final float[] gyr = new float[]{0, 0, 0};
    final float[] imu = new float[]{0, 0, 0};
    final float[] rotationVector = new float[3];
    final float[] R_ = new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
    final float[] I = new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
    DatagramSocket socket;
    byte deviceIndex;
    boolean sendOrientation;
    boolean sendRaw;
    Thread worker;
    volatile boolean running;
    private PowerManager mPowerManager;
    private WifiManager mWifiManager;
    private int sampleRate;
    private SensorManager sensorManager;
    private boolean hasGyro;
    private final BroadcastReceiver screen_off_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                register_sensors();
            }
        }
    };
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;

    public void register_sensors() {
        sensorManager.unregisterListener(this);
        if (sendRaw) {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    sampleRate);
            if (hasGyro)
                sensorManager.registerListener(this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                        sampleRate);

            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    sampleRate);
        }
        if (sendOrientation) {
            if (hasGyro)
                sensorManager.registerListener(this,
                        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                        sampleRate);
            else {
                if (!sendRaw) {
                    sensorManager.registerListener(this,
                            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                            sampleRate);
                    sensorManager.registerListener(this,
                            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                            sampleRate);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        enableNotification();
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    public void stop() {
        try {
            unregisterReceiver(screen_off_receiver);
        } catch (Exception ignored) {
        }
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        if (wifiLock != null) {
            wifiLock.release();
            wifiLock = null;
        }
        if (sensorManager != null)
            sensorManager.unregisterListener(this);
        running = false;
        synchronized (this) {
            notifyAll();
        }
        if (worker != null) {
            try {
                worker.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void enableNotification() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText("ForegroundServiceChannel")
                .setContentIntent(pendingIntent)
                .setTicker("Ticker text")
                .build();

        startForeground(1, notification);
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stop();
        PowerManager.WakeLock wl = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "de.z-byte.freepie:send-lock");
        WifiManager.WifiLock nl = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "de.z-byte.freepie:network-lock");
        registerReceiver(screen_off_receiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        deviceIndex = intent.getByteExtra("deviceIndex", (byte) 0);
        final int port = intent.getIntExtra("port", 5555);
        final String ip = intent.getStringExtra("toIp");

        sendRaw = intent.getBooleanExtra("sendRaw", true);
        sendOrientation = intent.getBooleanExtra("sendOrientation", true);
        sampleRate = intent.getIntExtra("sampleRate", 0);

        final UdpSenderService this_ = this;

        running = true;

        worker = new Thread(() -> {
            try {
                socket = new DatagramSocket();
                p.setAddress(InetAddress.getByName(ip));
                p.setPort(port);
            } catch (Exception e) {
                Log.e("Error", "Can't create endpoint " + e.getMessage() + " " + ip + ":" + port);
                running = false;
                return;
            }

            while (running) {
                try {
                    while (running) {
                        synchronized (this_) {
                            this_.wait();
                            Send();
                        }
                    }
                } catch (InterruptedException | IOException ignored) {
                }
            }
            try {
                socket.disconnect();
            } catch (Exception ignored) {
            }
            socket = null;
        });

        worker.start();

        hasGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;

        register_sensors();

        wakeLock = wl;
        wifiLock = nl;

        wifiLock.acquire();
        wakeLock.acquire();

        return START_STICKY;
    }

    private byte getFlagByte(boolean raw, boolean orientation) {
        return (byte) ((raw ? SEND_RAW : SEND_NONE) |
                (orientation ? SEND_ORIENTATION : SEND_NONE));
    }

    private int put_float(float f, int pos, byte[] buf) {
        int tmp = Float.floatToIntBits(f);
        buf[pos++] = (byte) (tmp);
        buf[pos++] = (byte) (tmp >> 8);
        buf[pos++] = (byte) (tmp >> 16);
        buf[pos++] = (byte) (tmp >> 24);
        return pos;
    }

    private void Send() throws IOException {
        Log.d("Info", "Send");

        int pos = 0;

        buf[pos++] = deviceIndex;
        buf[pos++] = getFlagByte(sendRaw, sendOrientation);

        if (sendRaw) {
            //Acc
            pos = put_float(acc[0], pos, buf);
            pos = put_float(acc[1], pos, buf);
            pos = put_float(acc[2], pos, buf);

            //Gyro
            pos = put_float(gyr[0], pos, buf);
            pos = put_float(gyr[1], pos, buf);
            pos = put_float(gyr[2], pos, buf);

            //Mag
            pos = put_float(mag[0], pos, buf);
            pos = put_float(mag[1], pos, buf);
            pos = put_float(mag[2], pos, buf);
        }

        if (sendOrientation) {
            pos = put_float(imu[0], pos, buf);
            pos = put_float(imu[1], pos, buf);
            pos = put_float(imu[2], pos, buf);
        }

        p.setData(buf, 0, pos);

        socket.send(p);
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (this) {
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    System.arraycopy(sensorEvent.values, 0, acc, 0, 3);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    System.arraycopy(sensorEvent.values, 0, mag, 0, 3);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    System.arraycopy(sensorEvent.values, 0, gyr, 0, 3);
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    System.arraycopy(sensorEvent.values, 0, rotationVector, 0, 3);
                    break;
            }

            if (sendOrientation) {
                if (!hasGyro) {
                    boolean ignored = SensorManager.getRotationMatrix(R_, I, acc, mag);
                    SensorManager.getOrientation(R_, imu);
                } else {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
                    SensorManager.getOrientation(rotationMatrix, imu);
                }
            }

            notifyAll();
        }
    }

    public static class MyBinder extends Binder {
    }
}
