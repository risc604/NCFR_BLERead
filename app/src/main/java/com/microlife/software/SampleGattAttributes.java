package com.microlife.software;

/**
 * Created by ChiaHao on 2016/1/18.
 */

        import java.util.HashMap;

public class SampleGattAttributes
{
    private static HashMap<String, String> attributes = new HashMap();
    //public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String MLC_BLE_CHARACTERISTIC = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String NOTIFY_CHARACTERISTIC = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static String WTIYE_CHARACTERISTIC = "0000fff2-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        //attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        //attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        //attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put("GetInfomation", "GetInfomation");
        attributes.put("SetNotifiable", "SetNotifiable");
        attributes.put("SendCommand", "SendCommand");
    }

    public static String lookup(String uuid, String defaultName)
    {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
