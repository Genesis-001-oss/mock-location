package com.example.shadowtest.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

public class TestLocation {

    private final static String TAG = "FakeLocation";

    public static void init(Context context) {
        HookILocationManager.hook(context);
        HookIWifiManager.hook();
        HookITelephonyManager.hook();
    }

    public LocationListener locationListener = new LocationListener() {
        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        // Provider被enable时触发此函数，比如GPS被打开
        @Override
        public void onProviderEnabled(String provider) {
        }

        // Provider被disable时触发此函数，比如GPS被关闭
        @Override
        public void onProviderDisabled(String provider) {
        }

        //当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                //如果位置发生变化，重新显示地理位置经纬度
                Log.i(TAG, "监视地理位置变化-经纬度：" + location.getLongitude() + "   " + location.getLatitude() + ", location: " + location + ", provider: " + location.getProvider());
                Log.i(TAG, "onLocationChanged location: " + location);
            }
        }
    };

    private static void getLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Log.i(TAG, "location: " + location);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    private static void getWifiInfo(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        Log.i(TAG, "connectionInfo: " + connectionInfo);
        List<ScanResult> scanResults = wifiManager.getScanResults();
    }

    private static void getCellLocation(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        CellLocation location = manager.getCellLocation();
        Log.i(TAG, "cellLocation: " + location);

        try {
            Method getNeighboringCellInfo = manager.getClass().getDeclaredMethod("getNeighboringCellInfo");
            getNeighboringCellInfo.setAccessible(true);
            List<NeighboringCellInfo> infoLists = (List<NeighboringCellInfo>) getNeighboringCellInfo.invoke(manager);
            Log.i(TAG, "getNeighboringCellInfo() infoLists: " + infoLists);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<CellInfo> allCellInfo = manager.getAllCellInfo();
        Log.i(TAG, "allCellInfo: " + allCellInfo);
    }

    public static void test(Context context) {
        getLocation(context);
        getWifiInfo(context);
        getCellLocation(context);
    }
}
