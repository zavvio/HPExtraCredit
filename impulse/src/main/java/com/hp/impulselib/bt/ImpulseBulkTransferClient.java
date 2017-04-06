package com.hp.impulselib.bt;

import android.os.Handler;
import android.util.Log;

import com.hp.impulselib.Impulse;
import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.util.Bytes;
import com.hp.impulselib.util.Tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ImpulseBulkTransferClient extends ImpulseBaseClient {
    private static final int UPGRADE_STARTED = 0;
    private static final int UPGRADE_IN_PROGRESS = 1;
    private static final int UPGRADE_DONE = 2;
    private static final int UPGRADE_ERROR = 3;
    private static final int IDLE = -1;
    private static final String LOG_TAG = "BulkTransferClient";
    private static final int BULK_TRANSFER_HEADER_SIZE = 10;
    private static final int RECOMMENDED_PACKAGE_SIZE = 580;

    private byte[] firmwareBytes;
    private final int DELAY_BETWEEN_BULK_TRANSFER_IN_MILISECONDS = 100;
    private static final int MAX_DELAY = 300;
    private static final int DELAY_INTERVAL = 50;
    private static final int RETRY_DELAY = 5000;

    private int state = IDLE;
    private ImpulseBulkTransferListener impulseBulkTransferListener;
    private int additionalDelay = 0;


    public ImpulseBulkTransferClient(ImpulseDevice device, byte[] fwBytes, final ImpulseBulkTransferListener listener) {
        super(device);
        firmwareBytes = fwBytes;
        impulseBulkTransferListener = listener;

        initiateTransfer();
    }

    private void initiateTransfer(){
        mRfcomm = new RfcommClient(mDevice.getDevice(), UuidSpp, new RfcommClient.RfcommListener() {
            @Override
            public void onConnect() {
                sendUpdateReadyPacket(firmwareBytes);
            }

            @Override
            public void onData(InputStream in) throws IOException {
                while (in.available() >= MIN_RESPONSE_PACKET_SIZE) {
                    final byte data[] = new byte[MIN_RESPONSE_PACKET_SIZE];
                    //noinspection ResultOfMethodCallIgnored (We know exactly how much is available)
                    in.read(data);
                    handlePacket(impulseBulkTransferListener, new Packet(data));
                }
            }

            @Override
            public void onError(IOException e) {
                Log.d(LOG_TAG, "onError() " + e);
                if (state == UPGRADE_DONE)
                    state = IDLE;
                else
                    impulseBulkTransferListener.onError(e);
                close();
            }
        });
    }


    private void handlePacket(ImpulseBulkTransferListener listener, Packet packet) {
        Log.d(LOG_TAG, "RX " + Bytes.toHex(packet.getBytes()));
        if (packet.getCommand() == CommandStartSend) {
            int packetSize = getMaxPayloadSize(packet) - 20;
            if (packetSize < 0) packetSize = RECOMMENDED_PACKAGE_SIZE;

            int startPosition = 0;
            state = UPGRADE_STARTED;
            repeatSendBulkUpdate(listener, DELAY_BETWEEN_BULK_TRANSFER_IN_MILISECONDS + additionalDelay, startPosition, packetSize);
        } else if (packet.getCommand() == CommandEndReceive) {
            state = UPGRADE_DONE;
            listener.onDone();
            state = IDLE;
        } else if (packet.getCommand() == CommandErrorMessage) {
            if (state == UPGRADE_STARTED || state == UPGRADE_IN_PROGRESS)  {
                state = IDLE;
                if(packet.getErrorCode() == Impulse.ErrorWrongCustomer && additionalDelay <= MAX_DELAY) {
                    mRfcomm.close();
                    additionalDelay += DELAY_INTERVAL;
                    Log.d(LOG_TAG, "Retry transfer at slower rate: " + DELAY_BETWEEN_BULK_TRANSFER_IN_MILISECONDS + additionalDelay);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            initiateTransfer();
                        }
                    }, RETRY_DELAY);

                } else {
                    listener.onError(new Exception(String.valueOf(packet.getErrorCode())));
                }
            }
        }
    }

    public interface ImpulseBulkTransferListener {
        void onDone();

        void onProgress(int progressInPercentage);

        void onError(Exception e);
    }

    private void repeatSendBulkUpdate(final ImpulseBulkTransferListener listener, final long delay, final int start, final int pSize) {
        Tasks.runMainDelayed(delay, new Runnable() {

            int startPosition = start;
            int packetSize = pSize;

            @Override
            public void run() {
                if (mRfcomm == null || state == IDLE) return;

                int endPosition = startPosition + packetSize;

                Packet packetBulk = new Packet(CommandBulkTransfer, mDevice.getCustomerCode(), packetSize + BULK_TRANSFER_HEADER_SIZE);
                // add data size
                packetBulk.mData[8] = (byte) ((0xFF00 & packetSize) >> 8);
                packetBulk.mData[9] = (byte) (0x00FF & packetSize);

                packetBulk.setBulkPayload(Arrays.copyOfRange(firmwareBytes, startPosition, endPosition));
                write(packetBulk);

                startPosition = startPosition + packetSize;

                if ((startPosition + packetSize) > firmwareBytes.length) {
                    packetSize = firmwareBytes.length - startPosition;
                }

                int progressInPercentage;
                if (firmwareBytes.length <= 0) {
                    listener.onError(new Exception());
                    return;
                }
                progressInPercentage = (startPosition * 100) / firmwareBytes.length;

                listener.onProgress(progressInPercentage);

                if (startPosition < firmwareBytes.length) {
                    repeatSendBulkUpdate(listener, delay, startPosition, packetSize);
                    state = UPGRADE_IN_PROGRESS;
                } else {
                    Log.d(LOG_TAG, "DONE");
                }

            }

        });
    }

    private void sendUpdateReadyPacket(byte[] fwBytes) {
        if (mRfcomm == null) return;

        Packet packet = new Packet(CommandUpgradeReady, mDevice.getCustomerCode());

        byte payload[] = {
                (byte) ((0xFF0000 & fwBytes.length) >> 16),
                (byte) ((0x00FF00 & fwBytes.length) >> 8),
                (byte) (0x0000FF & fwBytes.length),
                (byte) (0x02)
        };
        packet.setPayload(payload);
        Log.d(LOG_TAG, "TX " + Bytes.toHex(packet.getBytes()));
        write(packet);

    }

    /*
     * Max Payload size are set from return package on byte 10-11
     */
    private int getMaxPayloadSize(Packet packet) {
        byte[] bytes = packet.getBytes();
        return (bytes[10] & 0xFF) << 8 | (bytes[11] & 0xFF);
    }

    @Override
    public void close() {
        super.close();
    }

}