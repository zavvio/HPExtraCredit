package com.hp.impulselib.bt;

import android.util.Log;

import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.ImpulseDeviceOptions;
import com.hp.impulselib.ImpulseDeviceState;
import com.hp.impulselib.util.Bytes;
import com.hp.impulselib.util.Tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;

public class ImpulseClient extends ImpulseBaseClient {
    private static final String LOG_TAG = "ImpulseClient";

    private static final long PollingTimeMs = 1000;

    private ImpulseDeviceState.Builder mState = new ImpulseDeviceState.Builder();
    private Timer mQueryTimer;
    private byte[] fwBytes;

    public ImpulseClient(ImpulseDevice device, final ImpulseListener listener) {
        super(device);

        mRfcomm = new RfcommClient(mDevice.getDevice(), UuidSpp, new RfcommClient.RfcommListener() {
            @Override
            public void onConnect() {
                repeatGetAccessoryInfo(0);
            }

            @Override
            public void onData(InputStream in) throws IOException {
                while (in.available() >= MIN_RESPONSE_PACKET_SIZE) {
                    final byte data[] = new byte[MIN_RESPONSE_PACKET_SIZE];
                    //noinspection ResultOfMethodCallIgnored (We know exactly how much is available)
                    in.read(data);
                    handlePacket(listener, new Packet(data));
                }
            }

            @Override
            public void onError(IOException e) {
                Log.d(LOG_TAG, "onError() " + e);
                listener.onError(e);
                close();
            }
        });
    }

    /** Request accessory info on the main thread Get accessory info on the main thread */
    private void repeatGetAccessoryInfo(long delay) {
        Tasks.runMainDelayed(delay, new Runnable() {
            @Override
            public void run() {
                if (mRfcomm == null) return;

                Packet packet = new Packet(CommandGetAccessoryInfo, mDevice.getCustomerCode());
                Log.d(LOG_TAG, "TX " + Bytes.toHex(packet.getBytes()));
                write(packet);
                repeatGetAccessoryInfo(PollingTimeMs);

            }
        });
    }

    private void handlePacket(ImpulseListener listener, Packet packet) {

        Log.d(LOG_TAG, "RX " + Bytes.toHex(packet.getBytes()));
        mState.setCommand(packet.getCommand());

        if (packet.getCommand() == CommandAccessoryInfo) {
            mState.setAccessoryInfo(packet.getPayload());
        } else if (packet.getCommand() == CommandErrorMessage) {
            mState.setAccessoryInfo(packet.getPayload());
        }

        listener.onInfo(mState.build());
    }



    @Override
    public void close() {
        super.close();

        Log.d(LOG_TAG, "close()");
        if (mQueryTimer != null) {
            mQueryTimer.cancel();
            mQueryTimer = null;
        }

    }

    public void setAccessoryInfo(ImpulseDeviceOptions info) {
        Packet setInfo = new Packet(CommandSetAccessoryInfo, mDevice.getCustomerCode());
        byte payload[] = {
                Bytes.toByte(info.getAutoExposure(), 0x00),
                Bytes.toByte(info.getAutoPowerOff(), 0x08),
                Bytes.toByte(info.getPrintMode(), 0x01)
        };
        Log.d(LOG_TAG, "setAccessoryInfo() " + Bytes.toHex(payload));
        setInfo.setPayload(payload);
        Log.d(LOG_TAG, "TX " + Bytes.toHex(setInfo.getBytes()));
        write(setInfo);
    }

    public interface ImpulseListener {
        void onInfo(ImpulseDeviceState info);
        void onError(IOException e);
        void onError(int errorCode);
    }

}