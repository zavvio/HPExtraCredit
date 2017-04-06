package com.hp.impulselib.bt;

import java.nio.ByteBuffer;

class Packet {
    private int PACKET_SIZE = 34;
    private short START_CODE = 0x1B2A;

    private byte FROM_CLIENT = 0x00;
    private byte PRODUCT_CODE = 0x00;

    private int COMMAND_POSITION = 6;
    private int PAYLOAD_POSITION = 8;
    private int BULK_TRANSFER_ADDITIONAL_HEADER = 2;

    byte mData[] = new byte[PACKET_SIZE];;

    Packet(int command, short customerCode) {
        ByteBuffer out = ByteBuffer.wrap(mData);
        out.clear();
        out.putShort(START_CODE);
        out.putShort(customerCode);
        out.put(FROM_CLIENT);
        out.put(PRODUCT_CODE);
        out.putShort((short)command);

        // Remaining 26 bytes of payload (not defined)
    }

    Packet(int command, short customerCode, int packageSize) {
        PACKET_SIZE = packageSize;
        mData = new byte[PACKET_SIZE];
        ByteBuffer out = ByteBuffer.wrap(mData);
        out.clear();
        out.putShort(START_CODE);
        out.putShort(customerCode);
        out.put(FROM_CLIENT);
        out.put(PRODUCT_CODE);
        out.putShort((short)command);

    }

    /** Create a packet based on input data. Expected to be PACKET_SIZE in length. */
    Packet(byte data[]) {
        System.arraycopy(data, 0, mData, 0, PACKET_SIZE);
    }

    /** Return all bytes. Do not modify. */
    public byte[] getBytes() {
        return mData;
    }

    /** Return the payload portion */
    public byte[] getPayload() {
        byte payload[] = new byte[PACKET_SIZE - PAYLOAD_POSITION];
        System.arraycopy(mData, PAYLOAD_POSITION, payload, 0, PACKET_SIZE - PAYLOAD_POSITION);
        return payload;
    }

    /** Set payload bytes (up to PACKET_SIZE - PAYLOAD_POSITION) */
    public void setPayload(byte payload[]) {
        System.arraycopy(payload, 0, mData, PAYLOAD_POSITION, payload.length);
    }

    /** Only should be used in Bulk transfer */
    public void setBulkPayload(byte payload[]) {
        System.arraycopy(payload, 0, mData, PAYLOAD_POSITION + BULK_TRANSFER_ADDITIONAL_HEADER, payload.length);
    }

    /** Return the command code */
    public int getCommand() {
        return (mData[COMMAND_POSITION] << 8) | mData[COMMAND_POSITION + 1];
    }

    public int getNextPayloadSize() {
        return ((mData[PAYLOAD_POSITION + BULK_TRANSFER_ADDITIONAL_HEADER] & 0xFF) << 8) | (mData[PAYLOAD_POSITION + BULK_TRANSFER_ADDITIONAL_HEADER + 1] & 0xFF);
    }

    public int getErrorCode() {
        // the first byte in payload is error code
        return mData[PAYLOAD_POSITION];
    }
}