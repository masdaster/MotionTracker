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
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Logger;

import io.github.masdaster.motion_tracker.motion_receivers.OrientationProvider;
import io.github.masdaster.motion_tracker.motion_receivers.calculation_strategies.OrientationProviderStrategy;

public class SenderService extends Service {
    public static final String
            CHANNEL_ID = "ForegroundServiceChannel",
            INTENT_EXTRA_OPTIONS = "ServiceOptions";
    private static final Logger logger = Logger.getLogger("SenderService");
    private final DatagramPacket datagramPacket = new DatagramPacket(new byte[]{}, 0);
    private final byte[] buffer = new byte[50];
    private PowerManager.WakeLock powerWakeLock;
    private WifiManager.WifiLock wifiWakeLock;
    private SensorManager sensorManager;
    private OrientationProvider orientationProvider;
    private SenderServiceOptions serviceOptions;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                orientationProvider.register(sensorManager, serviceOptions.sampleRate);
            }
        }
    };
    private final BackgroundWorker backgroundWorker = new BackgroundWorker(this::run);

    private static int putFloat(float f, int position, byte[] buffer) {
        int tmp = Float.floatToIntBits(f);
        buffer[position++] = (byte) (tmp);
        buffer[position++] = (byte) (tmp >> 8);
        buffer[position++] = (byte) (tmp >> 16);
        buffer[position++] = (byte) (tmp >> 24);
        return position;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        powerWakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "de.z-byte.freepie:power-lock");
        wifiWakeLock = getSystemService(WifiManager.class).createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "de.z-byte.freepie:wifi-lock");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMotionTracking();
    }

    public void stopMotionTracking() {
        stopForeground(SenderService.STOP_FOREGROUND_REMOVE);
        backgroundWorker.stop();
        if (orientationProvider != null) {
            orientationProvider.unregister(sensorManager);
            //noinspection SynchronizeOnNonFinalField
            synchronized (orientationProvider) {
                orientationProvider.notifyAll();
            }
            orientationProvider = null;
        }
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception ignored) {
        }
        if (powerWakeLock.isHeld()) {
            powerWakeLock.release();
        }
        if (wifiWakeLock.isHeld()) {
            wifiWakeLock.release();
        }
    }

    @SuppressLint("WakelockTimeout")
    public void startMotionTracking() {
        startForeground(1, createNotification());
        orientationProvider = OrientationProvider.Create(serviceOptions.sensorType);
        wifiWakeLock.acquire();
        powerWakeLock.acquire();
        registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        orientationProvider.register(sensorManager, serviceOptions.sampleRate);
        backgroundWorker.start();
    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.foreground_service_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.foreground_service_notification_channel_description));
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_icon_monochrome)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_is_running))
                .setContentIntent(pendingIntent)
                .build();
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!backgroundWorker.isRunning()) {
            serviceOptions = intent.getParcelableExtra(INTENT_EXTRA_OPTIONS);
            if (serviceOptions != null) {
                startMotionTracking();
            } else {
                logger.severe("No service options were provided.");
                stopSelf(startId);
            }
        }
        return START_STICKY;
    }

    private byte getFlagByte() {
        OrientationProviderStrategy.CalculatedDataType dataType = orientationProvider.getDataType();
        return (byte) ((dataType == OrientationProviderStrategy.CalculatedDataType.RAW ? 0x01 : 0x00) |
                (dataType == OrientationProviderStrategy.CalculatedDataType.ORIENTATION ? 0x02 : 0x00));
    }

    private void send(DatagramSocket socket) throws IOException {
        int position = 0;
        buffer[position++] = serviceOptions.deviceIndex;
        buffer[position++] = getFlagByte();
        for (float f : orientationProvider.getData()) {
            position = putFloat(f, position, buffer);
        }
        datagramPacket.setData(buffer, 0, position);
        socket.send(datagramPacket);
    }

    private void run(BackgroundWorker backgroundWorker) {
        final DatagramSocket socket;
        try {
            socket = new DatagramSocket();
            datagramPacket.setAddress(InetAddress.getByName(serviceOptions.serverIp));
            datagramPacket.setPort(serviceOptions.serverPort);
        } catch (Exception e) {
            logger.severe("Can't create endpoint for " + serviceOptions.serverIp + ":" + serviceOptions.serverPort + ": " + e.getMessage() + ".");
            backgroundWorker.stop();
            return;
        }
        while (backgroundWorker.isRunning()) {
            try {
                while (backgroundWorker.isRunning()) {
                    //noinspection SynchronizeOnNonFinalField
                    synchronized (orientationProvider) {
                        orientationProvider.wait(1000);
                        send(socket);
                    }
                }
            } catch (InterruptedException | IOException ignored) {
            }
        }
        try {
            socket.disconnect();
        } catch (Exception ignored) {
        }
    }

    public class Binder extends android.os.Binder {
        public boolean isRunning() {
            return backgroundWorker.isRunning();
        }

        public void stopMotionTracking() {
            SenderService.this.stopMotionTracking();
        }

        public void startMotionTracking() {
            if (serviceOptions != null) {
                SenderService.this.startMotionTracking();
            } else {
                logger.severe("No service options were provided.");
            }
        }

        public void addOnStateChangeListener(BackgroundWorker.OnStateChangeListener listener) {
            backgroundWorker.addOnStateChangeListener(listener);
        }

        public void removeOnStateChangeListener(BackgroundWorker.OnStateChangeListener listener) {
            backgroundWorker.removeOnStateChangeListener(listener);
        }

        public float[] getOrientationData() {
            return orientationProvider.getData();
        }

        public OrientationProviderStrategy.CalculatedDataType getOrientationDataType() {
            return orientationProvider.getDataType();
        }
    }
}
