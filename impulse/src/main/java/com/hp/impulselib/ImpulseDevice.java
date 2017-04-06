package com.hp.impulselib;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Date;
import java.util.Set;

/**
 * Known info concerning an ImpulseDevice
 */
public class ImpulseDevice implements Parcelable{

    public static final String PREFIX_HP_DEVICE_NAME = "HP sprocket";
    public static final String PREFIX_POLAROID_DEVICE_NAME = "Polaroid ZIP";
    public static final String HP_LE_DEVICE_NAME = "HPMalta-";
    public static final String HP_MALTA_NAME = "MALTA";
    private static short PolaroidCustomerCode = 0x4341; // Code for Polaroid ZIP
    private static short HPCustomerCode = 0x4850; // Code for HP Sprocket
    private static short NOTDefinedCustomerCode = 0x0000;

    private BluetoothDevice mBluetoothDevice;
    private short mRssi = 0;
    private long mRssiAt = Long.MIN_VALUE; // System clock at last rssi capture if any
    private boolean mBonded = false;
    private ImpulseDeviceState mState; // If known
    public boolean isConnected;

    // Malta attribute
    private int calibratedRssi;
    private DeviceColorEnum color = DeviceColorEnum.unknown;
    private PrinterStatusEnum printerStatus = PrinterStatusEnum.unknown;

    public enum DeviceColorEnum {
        white, red, green, blue, pink, yellow, orange, purple, brown, grey, black, unknown;
    }

    public enum PrinterStatusEnum {
        ready,
        printing,
        out_of_paper,
        print_buffer_full,
        cover_open,
        paper_jam,
        unknown;
    }

    private ImpulseDevice(BluetoothDevice bluetoothDevice) {
        mBluetoothDevice = bluetoothDevice;
    }

    private ImpulseDevice(ImpulseDevice other) {
        mBluetoothDevice = other.mBluetoothDevice;
        mRssi = other.mRssi;
        mRssiAt = other.mRssiAt;
        mBonded = other.mBonded;
        mState = other.mState;
        calibratedRssi = other.getCalibratedRssi();
        color = other.getColor();
        printerStatus = other.getPrinterStatus();
    }


    public ImpulseDevice(Parcel in) {
        mBluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        mRssiAt = in.readLong();
        mBonded = in.readByte() != 0;
        isConnected = in.readByte() != 0;
        calibratedRssi = in.readInt();
        color = DeviceColorEnum.valueOf(in.readString());
        printerStatus = PrinterStatusEnum.valueOf(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mBluetoothDevice, flags);
        dest.writeLong(mRssiAt);
        dest.writeByte((byte) (mBonded ? 1 : 0));
        dest.writeByte((byte) (isConnected ? 1 : 0));
        dest.writeInt(calibratedRssi);
        dest.writeString(color.name() );
        dest.writeString(printerStatus.name());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ImpulseDevice> CREATOR = new Creator<ImpulseDevice>() {
        @Override
        public ImpulseDevice createFromParcel(Parcel in) {
            return new ImpulseDevice(in);
        }

        @Override
        public ImpulseDevice[] newArray(int size) {
            return new ImpulseDevice[size];
        }
    };

    public PrinterStatusEnum getPrinterStatus() {
        return printerStatus;
    }

    public void setPrinterStatus(PrinterStatusEnum printerStatus) {
        this.printerStatus = printerStatus;
    }

    public void setPrinterStatus(int printerStatus) {
        this.printerStatus = PrinterStatusEnum.values()[printerStatus];
    }

    public int getCalibratedRssi() {
        return calibratedRssi;
    }

    public void setCalibratedRssi(int calibratedRssi) {
        this.calibratedRssi = calibratedRssi;
    }

    public DeviceColorEnum getColor() {
        return color;
    }

    public void setColor(DeviceColorEnum color) {
        this.color = color;
    }

    public void setColor(int color) {
        this.color = DeviceColorEnum.values()[color];
    }

    public boolean getBonded() {
        return mBonded;
    }

    public void checkIfConnected(long timeoutMs) {
        long now = new Date().getTime();
        isConnected = (mState != null && now - mState.getUpdated() < timeoutMs) ||
                (mRssi != 0 || now - mRssiAt < timeoutMs);
    }

    public short getCustomerCode() {
        if (getDevice().getName().startsWith(PREFIX_HP_DEVICE_NAME))
            return HPCustomerCode;
        else if (getDevice().getName().startsWith(PREFIX_POLAROID_DEVICE_NAME))
            return PolaroidCustomerCode;
        else
            return NOTDefinedCustomerCode;
    }

    public int getRssi() {
        return mRssi;
    }

    public ImpulseDeviceState getState() {
        return mState;
    }

    public void setState(ImpulseDeviceState impulseDeviceState){
        mState = impulseDeviceState;
    }

    public static class Builder {
        final ImpulseDevice mPrototype;

        public Builder(ImpulseDevice device) {
            mPrototype = new ImpulseDevice(device);
        }

        public Builder(BluetoothDevice device) {
            mPrototype = new ImpulseDevice(device);
        }

        public Builder(Intent intent) {
            mPrototype = new ImpulseDevice((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
            mPrototype.mRssiAt = new Date().getTime();
            mPrototype.mRssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
        }

        public Builder setBonded(Set<BluetoothDevice> bondList) {
            for (BluetoothDevice device : bondList) {
                if (device.getAddress().equals(mPrototype.mBluetoothDevice.getAddress())) {
                    mPrototype.mBonded = true;
                    return this;
                }
            }
            mPrototype.mBonded = false;
            return this;
        }

        public Builder setBonded(boolean bonded) {
            mPrototype.mBonded = bonded;
            return this;
        }

        public Builder setState(ImpulseDeviceState state) {
            mPrototype.mState = state;
            return this;
        }

        public ImpulseDevice build() {
            return new ImpulseDevice(mPrototype);
        }
    }

    public String getAddress() {
        return mBluetoothDevice.getAddress();
    }

    public BluetoothDevice getDevice() {
        return mBluetoothDevice;
    }

    /**
     * Return true if the device might be an Impulse. The search is based only on Class Of Device;
     * If RELEASE mode, we do not show polaroid devices. (PREFIX_POLAROID_DEVICE_NAME)
     */
    public static boolean isImpulseClass(BluetoothDevice device) {
        boolean ret = false;
        if (device == null || device.getBluetoothClass() == null || device.getName() == null) {
            return ret;
        }

        BluetoothClass btClass = device.getBluetoothClass();

        if (!BuildConfig.DEBUG) {
            ret = (btClass.hasService(BluetoothClass.Service.OBJECT_TRANSFER) &&
                    (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING) &&
                    ((btClass.getDeviceClass() & 0xF0) == 0x80))
                    && (device.getName().startsWith(PREFIX_HP_DEVICE_NAME));

        } else {
            ret = (btClass.hasService(BluetoothClass.Service.OBJECT_TRANSFER) &&
                    (btClass.getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING) &&
                    ((btClass.getDeviceClass() & 0xF0) == 0x80))
                    && (device.getName().startsWith(PREFIX_HP_DEVICE_NAME) ||
                    device.getName().startsWith(PREFIX_POLAROID_DEVICE_NAME)); // 0x80 is the PRINTER bit

        }
        Log.d("ImpulseDevice", "ImpulseDevice:isImpulseClass:136 Device: " + device.getName() + " isImpulse: " + ret);
        return ret;
    }


    @Override
    public String toString() {
        return "ImpulseDevice(btDev=" + mBluetoothDevice +
                " name=" + mBluetoothDevice.getName() +
                " rssi=" + mRssi +
                " bonded=" + mBonded +
                " state=" + mState +
                ")";
    }
}
