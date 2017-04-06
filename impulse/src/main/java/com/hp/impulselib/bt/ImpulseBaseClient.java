package com.hp.impulselib.bt;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.ImpulseDeviceState;
import com.hp.impulselib.util.Bytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ImpulseBaseClient implements AutoCloseable {
    private static final String LOG_TAG = "ImpulseBaseClient";
    protected static final UUID UuidSpp = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    /** Commands exchanged with device */
    public static int CommandPrintReady = 0x0000; // Client, "Print status check for Manta"
    public static int CommandPrintCancel = 0x0001; // Client, "Image data sending cancellation to manta (Android Only)"
    public static int CommandPrintStart = 0x0002;  // Device, "Notice Image Printing of Manta"
    public static int CommandPrintFinish = 0x0003;  // Device "Notice Image Printing complete of Manta (delete by Multi-send issue)"
    public static int CommandGetAccessoryInfo = 0x0100; // Client "Request current device information for Manta"
    public static int CommandSetAccessoryInfo = 0x0101; // Client "Set the device information for Manta"
    public static int CommandAccessoryInfo = 0x0102; // Device "Forward current device information for Manta"
    public static int CommandStartSend = 0x0200; // Device "Notice Image Data Sending start for Manta"
    public static int CommandEndReceive = 0x0201; // Device "Notice Image Data Sending complete for Manta"
    public static int CommandUpgradeReady = 0x0300; // Client "Upgrade status check for Manta"
    public static int CommandUpgradeCancel = 0x0301; // Client "Firmware Data Sending Cancelation for Manta"
    public static int CommandUpgradeAck = 0x0302; // Device "Forward Current Device's Upgrade Information for Manta"
    public static int CommandErrorMessage = 0x0400; // Device "Forward Error status of Manta"
    public static int CommandBulkTransfer = 0x0500; // "Use for data transfer (FW, TMD) via SPP for Manta"

    protected final ImpulseDevice mDevice;
    protected RfcommClient mRfcomm;

    public int MIN_RESPONSE_PACKET_SIZE = 34;

    public ImpulseBaseClient(ImpulseDevice device) {
        mDevice = device;
    }

    protected void write(Packet packet) {
        if (mRfcomm != null) {
            Log.d(LOG_TAG, "TX " + Bytes.toHex(packet.getBytes()));
            mRfcomm.write(packet.getBytes());
        }
    }

    @Override
    public void close() {
        if (mRfcomm != null) {
            mRfcomm.close();
            mRfcomm = null;
        }
    }


}