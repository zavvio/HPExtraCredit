package com.hp.impulselib.bt;

import android.util.Log;

import com.hp.impulselib.Impulse;
import com.hp.impulselib.ImpulseBinding;
import com.hp.impulselib.ImpulseDevice;
import com.hp.impulselib.ImpulseDeviceState;
import com.hp.impulselib.ImpulseService;
import com.hp.impulselib.SendListener;
import com.hp.impulselib.SendListenerSPP;
import com.hp.impulselib.TrackListener;
import com.hp.impulselib.util.Bytes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class ImpulsePrintClient extends ImpulseBaseClient {
    private static final UUID OPP_UUID = UUID.fromString("00001105-0000-1000-8000-00805f9b34fb");
    private static final String LOG_TAG = "ImpulsePrintClient";
    private static final String IMAGE_NAME = "img.jpg";
    private static final String IMAGE_MIME_TYPE = "image/jpeg";
    private byte[] imageBytes;
    private SendListenerSPP printListener;
    private ObexClient obexClient;
    private int printCount = 1;
    private int printMode;
    private int postCount;
    ImpulseClient impulseClient;
    ImpulseBinding impulseBinding;
    AutoCloseable tracker;

    private ImpulsePrintClient(ImpulseDevice device, byte[] images, final SendListenerSPP listener,ImpulseBinding binding) {
        super(device);
        imageBytes = images;
        printListener = listener;
        impulseBinding = binding;
    }

    private ImpulsePrintClient(ImpulsePrintClient other) {
        super(other.mDevice);
        imageBytes = other.imageBytes;
        printListener = other.printListener;
        printCount = other.printCount;
        printMode = other.printMode;
        postCount = other.postCount;
        impulseBinding = other.impulseBinding;
    }

    public int getPrintMode(){ return printMode; }

    public void setPrintMode(int printMode){ this.printMode = printMode; }

    public int getPrintCount(){ return printCount; }

    public void setPrintCount(int printCount){ this.printCount = printCount; }

    public int getPostCount(){ return postCount; }

    public void setPostCount(int postCount){ this.postCount = postCount; }


    public void print(){
        mRfcomm = new RfcommClient(mDevice.getDevice(), UuidSpp, new RfcommClient.RfcommListener() {
            @Override
            public void onConnect() {
                sendPrintReadyPacket(imageBytes,printCount,printMode, postCount);
            }

            @Override
            public void onData(InputStream in) throws IOException {
                while (in.available() >= MIN_RESPONSE_PACKET_SIZE) {
                    final byte data[] = new byte[MIN_RESPONSE_PACKET_SIZE];
                    in.read(data);
                    handlePacket(printListener, new Packet(data));
                }
            }

            @Override
            public void onError(IOException e) {
                printListener.onError(Impulse.ErrorConnectionFailed);
                close();
            }
        });
    }

    public static class Builder {
        final ImpulsePrintClient prototype;

        public Builder(ImpulseDevice device, byte[] image, SendListenerSPP listener, ImpulseBinding binding) {
            prototype = new ImpulsePrintClient(device, image, listener, binding);
        }

        public Builder setPrintCount(int printCount) {
            prototype.printCount = printCount;
            return this;
        }

        public Builder setPrintMode(int printMode) {
            prototype.printMode = printMode;
            return this;
        }

        public Builder setPostCount(int postCount) {
            prototype.postCount = postCount;
            return this;
        }

        public ImpulsePrintClient build() {
            return new ImpulsePrintClient(prototype);
        }
    }

    private void sendPrintReadyPacket(byte[] printBytes, int printCount, int printMode, int post) {
        if (mRfcomm == null) return;

        Packet packet = new Packet(CommandPrintReady, mDevice.getCustomerCode());
        int postBit0 = 0;
        int postBit1 = 0;
        if(post == 4){
            postBit0 = 0xF;
        }else if(post == 9){
            postBit0 = 0xFF;
            postBit1 = 0x01;
        }
        byte payload[] = {
                (byte) ((0xFF0000 & printBytes.length) >> 16),
                (byte) ((0x00FF00 & printBytes.length) >> 8),
                (byte) (0x0000FF & printBytes.length),
                (byte) printCount,
                (byte) post,
                (byte) postBit1,
                (byte) postBit0,
                (byte) printMode,
                (byte) 0x00
        };
        packet.setPayload(payload);
        Log.d(LOG_TAG, "TX " + Bytes.toHex(packet.getBytes()));
        write(packet);

    }

    private void handlePacket(SendListenerSPP listener, Packet packet) {
        Log.d(LOG_TAG, "RX " + Bytes.toHex(packet.getBytes()));
        if (packet.getCommand() == CommandErrorMessage) {
            byte[] payload = packet.getPayload();
            byte errorCode = payload[0];
            switch(errorCode){
                case Impulse.ErrorNone:
                    sendImage();
                    break;
                case Impulse.ErrorBusy:
                case Impulse.ErrorPaperJam:
                case Impulse.ErrorPaperEmpty:
                case Impulse.ErrorPaperMismatch:
                case Impulse.ErrorDataError:
                case Impulse.ErrorCoverOpen:
                case Impulse.ErrorSystemError:
                case Impulse.ErrorBatteryLow:
                case Impulse.ErrorBatteryFault:
                case Impulse.ErrorHighTemperature:
                case Impulse.ErrorLowTemperature:
                case Impulse.ErrorCooling:
                case Impulse.ErrorCancel:
                    printListener.onError(errorCode);
                    close();
                    break;
                default:
                    close();
                    break;
            }
        }
    }
    
    private void sendImage(){
        mDevice.setPrinterStatus(ImpulseDevice.PrinterStatusEnum.printing);
        obexClient = new ObexClient(mDevice.getDevice(), OPP_UUID, new ObexConnectionListener());
    }

    class ObexConnectionListener implements ObexClient.ConnectionListener {
        @Override
        public void onConnect() {

            obexClient.put(IMAGE_NAME, IMAGE_MIME_TYPE,
                    new ByteArrayInputStream(imageBytes),
                    new ObexPutListener());
        }

        @Override
        public void onError(IOException e) {
            printListener.onError(Impulse.ErrorConnectionFailed);
            obexClient = null;
            close();
            mDevice.setPrinterStatus(ImpulseDevice.PrinterStatusEnum.ready);
        }
    }

    class ObexPutListener implements ObexClient.PutListener {
        @Override
        public void onPutProgress(int sent) {
            printListener.onProgress(imageBytes.length, sent, false);
        }

        @Override
        public void onPutSuccess() {
            close();
            checkPrinterStatus();
            printListener.onProgress(imageBytes.length, -1, true);
            obexClient.close();


        }

        @Override
        public void onError(IOException e) {
            printListener.onError(Impulse.ErrorConnectionFailed);
            obexClient = null;
            close();
            mDevice.setPrinterStatus(ImpulseDevice.PrinterStatusEnum.ready);
        }

        private void checkPrinterStatus() {
            tracker = impulseBinding.track(mDevice, new TrackListener() {
                boolean isBusyYet = false;
                @Override
                public void onState(ImpulseDeviceState state) {
                    mDevice.setState(state);
                    if (state.getError() == Impulse.ErrorBusy) {
                        mDevice.setPrinterStatus(ImpulseDevice.PrinterStatusEnum.printing);
                        isBusyYet = true;
                    } else if (state.getError() == Impulse.ErrorNone && isBusyYet) {
                        printListener.onDone();
                        mDevice.setPrinterStatus(ImpulseDevice.PrinterStatusEnum.ready);
                        try {
                            tracker.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        mDevice.setPrinterStatus(ImpulseDevice.PrinterStatusEnum.printing);
                        //TODO: Handle other errors
                    }
                }

                @Override
                public void onError(int errorCode) {
                    if(errorCode == Impulse.ErrorConnectionFailed) {
                        try {
                            tracker.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mDevice.setPrinterStatus(ImpulseDevice.PrinterStatusEnum.ready);
                    }
                }
            });
        }
    }

}
