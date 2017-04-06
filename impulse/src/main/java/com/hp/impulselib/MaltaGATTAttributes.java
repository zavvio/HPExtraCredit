package com.hp.impulselib;

import java.util.HashMap;


/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class MaltaGATTAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_ACCESS_SERVICE_UUID = "00001800-0000-1000-8000-00805f9b34fb";
    public static String DEVICE_NAME_CHARACTERISTIC_UUID = "00002a00-0000-1000-8000-00805f9b34fb";

    static {
        // Malta Services.
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery Information Service");
        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Generic Attribute Service");
        attributes.put(GENERIC_ACCESS_SERVICE_UUID, "Generic Access Service");
        // Malta Characteristics.
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level");
        attributes.put("00002a26-0000-1000-8000-00805f9b34fb", "Firmware Revision String");
        attributes.put("00002a24-0000-1000-8000-00805f9b34fb", "Model Number");
        attributes.put("00002a25-0000-1000-8000-00805f9b34fb", "Serial Number");
        attributes.put("00002a23-0000-1000-8000-00805f9b34fb", "System ID");
        attributes.put(DEVICE_NAME_CHARACTERISTIC_UUID, "Device Name");
        attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "Appearance");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
