package com.wise.config;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.wise.util.Logger;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class BaseClass {
    public static String getMacAdress(Context context){
        try {
            //获取MAC地址  Android 6.0 获取mac 做了限制
            String wifiAddress = "";

            if(!isAndroidM()){
                WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifi.getConnectionInfo();
                wifiAddress = info.getMacAddress();
            }else{
                wifiAddress = macAddress();
            }
            Logger.E("TEST_BUG", "手机信息wifimac ： " + wifiAddress);
            if(wifiAddress != null){
                return wifiAddress;
            }
            String Imei = ((TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE)).getDeviceId();
            Logger.E("TEST_BUG", "手机信息imei ： " + Imei);
            if(Imei != null){
                return Imei;
            }
            String BluetoothAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
            Logger.E("TEST_BUG", "手机信息 BluetoothAddress  ： " + BluetoothAddress);
            return BluetoothAddress;
        } catch (Exception e){
            return "";
        }
    }

    // 有兴趣的朋友可以看下NetworkInterface在Android FrameWork中怎么实现的
    public static String macAddress() throws SocketException {
        String address = null;
        // 把当前机器上的访问网络接口的存入 Enumeration集合中
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        Logger.D("TEST_BUG", " interfaceName = " + interfaces );
        while (interfaces.hasMoreElements()) {
            NetworkInterface netWork = interfaces.nextElement();
            // 如果存在硬件地址并可以使用给定的当前权限访问，则返回该硬件地址（通常是 MAC）。
            byte[] by = netWork.getHardwareAddress();
            if (by == null || by.length == 0) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : by) {
                builder.append(String.format("%02X:", b));
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            String mac = builder.toString();
            Logger.D("TEST_BUG", "interfaceName="+netWork.getName()+", mac="+mac);
            // 从路由器上在线设备的MAC地址列表，可以印证设备Wifi的 name 是 wlan0
            if (netWork.getName().equals("wlan0")) {
                Logger.D("TEST_BUG", " interfaceName ="+netWork.getName()+", mac="+mac);
                address = mac;
            }
        }
        return address;
    }


    public static boolean isAndroidM(){

        return(Build.VERSION.SDK_INT>22);

    }
}
