package io.github.masdaster.motion_tracker;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Created by Z-Byte on .
 */
public class SenderServiceOptions implements Parcelable {
    public static final Creator<SenderServiceOptions> CREATOR = new Creator<SenderServiceOptions>() {
        @Override
        public SenderServiceOptions createFromParcel(Parcel in) {
            return new SenderServiceOptions(in);
        }

        @Override
        public SenderServiceOptions[] newArray(int size) {
            return new SenderServiceOptions[size];
        }
    };
    @NonNull
    public final String serverIp;
    public final int serverPort;
    public final byte deviceIndex;
    @NonNull
    public final String sensorType;
    public final int sampleRate;

    private SenderServiceOptions(Parcel in) {
        serverIp = Objects.requireNonNull(in.readString());
        serverPort = in.readInt();
        deviceIndex = in.readByte();
        sensorType = Objects.requireNonNull(in.readString());
        sampleRate = in.readInt();
    }

    private SenderServiceOptions(@NonNull String serverIp, int serverPort, byte deviceIndex, @NonNull String sensorType, int sampleRate) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.deviceIndex = deviceIndex;
        this.sensorType = sensorType;
        this.sampleRate = sampleRate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(serverIp);
        dest.writeInt(serverPort);
        dest.writeInt(deviceIndex);
        dest.writeString(sensorType);
        dest.writeInt(sampleRate);
    }

    public static class Builder {
        @NonNull
        private String serverIp = "192.168.1.1";
        private int serverPort = 5555;
        private byte deviceIndex = 0;
        @NonNull
        private String sensorType = "rotation_vector";
        private int sampleRate = 1;

        public void setServerIp(@NonNull String serverIp) throws UnknownHostException {
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(serverIp);
            this.serverIp = serverIp;
        }

        public void setServerPort(int serverPort) throws IndexOutOfBoundsException {
            if (serverPort < 1 || serverPort > 65535) {
                throw new IndexOutOfBoundsException();
            }
            this.serverPort = serverPort;
        }

        public void setDeviceIndex(int serverDeviceIndex) throws IndexOutOfBoundsException {
            if (serverDeviceIndex < 0 || serverDeviceIndex > 16) {
                throw new IndexOutOfBoundsException();
            }
            this.deviceIndex = (byte) serverDeviceIndex;
        }

        public void setSensorType(@NonNull String sensorType) {
            this.sensorType = sensorType;
        }

        public void setSampleRate(int sampleRate) throws IndexOutOfBoundsException {
            if (sampleRate < 0 || sampleRate > 3) {
                throw new IndexOutOfBoundsException();
            }
            this.sampleRate = sampleRate;
        }

        public SenderServiceOptions build() {
            return new SenderServiceOptions(serverIp, serverPort, deviceIndex, sensorType, sampleRate);
        }
    }
}
