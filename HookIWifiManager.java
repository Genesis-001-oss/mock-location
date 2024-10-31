package com.example.shadowtest.location;

import android.content.Context;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;

public class HookIWifiManager {

    private static class InvocationHandlerImpl implements InvocationHandler, HookCallback {

        private final Object original;

        public InvocationHandlerImpl(Object original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.i("FakeLocation", "WifiManager.methodName: " + method.getName());
            ResultInfo resultInfo = new ResultInfo();
            boolean isIntercept = onCallBefore(method, args, resultInfo);
            Object result;
            if (isIntercept) {
                result = resultInfo.getResult();
            } else {
                result = method.invoke(original, args);
            }
            onCallAfter(method, args, result);
            return result;
        }

        public boolean onCallBefore(Method method, Object[] args, ResultInfo resultInfo) {
            if ("getScanResults".equals(method.getName())) {
                resultInfo.setResult(new ArrayList<>());
                return true;
            }
            return false;
        }

        public void onCallAfter(Method method, Object[] args, Object result) {
            if ("getConnectionInfo".equals(method.getName())) {
                if (result != null) {
                    WifiInfo wifiInfo = (WifiInfo) result;
                    try {
                        Field mBSSIDField = WifiInfo.class.getDeclaredField("mBSSID");
                        Field mMacAddressField = WifiInfo.class.getDeclaredField("mMacAddress");
                        Field mWifiSsidField = WifiInfo.class.getDeclaredField("mWifiSsid");
                        mBSSIDField.setAccessible(true);
                        mMacAddressField.setAccessible(true);
                        mWifiSsidField.setAccessible(true);

                        Class<?> WifiSsidClass = Class.forName("android.net.wifi.WifiSsid");
                        Method createFromAsciiEncodedMethod = WifiSsidClass.getDeclaredMethod("createFromAsciiEncoded", String.class);
                        createFromAsciiEncodedMethod.setAccessible(true);
                        Object wifiSsid = createFromAsciiEncodedMethod.invoke(null, "");

                        mBSSIDField.set(wifiInfo, "00:00:00:00:00:00");
                        mMacAddressField.set(wifiInfo, "02:00:00:00:00:00");
                        mWifiSsidField.set(wifiInfo, wifiSsid);
                    } catch (Exception e) {
                        Log.e("FakeLocation", "", e);
                    }
                }
            }
        }
    }

    public static void hook() {
        try {
            Class<?> ServiceManagerClass = Class.forName("android.os.ServiceManager");
            Method checkServiceMethod = ServiceManagerClass.getDeclaredMethod("checkService", String.class);
            checkServiceMethod.setAccessible(true);

            IBinder service = (IBinder) checkServiceMethod.invoke(null, Context.WIFI_SERVICE);
            Class<?> IWifiManagerClass = Class.forName("android.net.wifi.IWifiManager");

            Class<?> IWifiManager_StubClass = Class.forName("android.net.wifi.IWifiManager$Stub");
            Method asInterfaceMethod = IWifiManager_StubClass.getDeclaredMethod("asInterface", IBinder.class);
            asInterfaceMethod.setAccessible(true);
            IInterface iInterface = (IInterface) asInterfaceMethod.invoke(null, service);

            IInterface proxy = (IInterface) Proxy.newProxyInstance(service.getClass().getClassLoader(), new Class[]{ IWifiManagerClass }, new InvocationHandlerImpl(iInterface));
            Field sCacheField = ServiceManagerClass.getDeclaredField("sCache");
            sCacheField.setAccessible(true);
            Map<String, IBinder> sCache = (Map<String, IBinder>) sCacheField.get(null);
            sCache.put(Context.WIFI_SERVICE, new BinderWrapper(service, proxy));

        } catch (Exception e) {
            Log.e("FakeLocation", "hookLocation", e);
        }
    }
}
