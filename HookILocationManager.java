package com.example.shadowtest.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class HookILocationManager {

    private static Location getFakeLocation() {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setAccuracy(8f);
        location.setBearing(0.1f);
        location.setLatitude(30.206861065952626);
        location.setLongitude(120.2508544921875);
        return location;
    }

    private static void updateLocation(IInterface transport) {
        try {
            Class<?> LocationListenerTransport = Class.forName("android.location.LocationManager$LocationListenerTransport");
            Class<?> IRemoteCallback = Class.forName("android.os.IRemoteCallback");
            Method onLocationChangedMethod = LocationListenerTransport.getDeclaredMethod("onLocationChanged", List.class, IRemoteCallback);
            onLocationChangedMethod.setAccessible(true);
            List<Location> locations = new ArrayList<>();
            locations.add(getFakeLocation());
            onLocationChangedMethod.invoke(transport, locations, null);
        } catch (Exception e) {
            Log.e("FakeLocation", "updateLocation", e);
        }
    }

    private static class InvocationHandlerImpl implements InvocationHandler, HookCallback {

        private final Object original;

        public InvocationHandlerImpl(Object original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.i("FakeLocation", "methodName: " + method.getName());
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

        @Override
        public boolean onCallBefore(Method method, Object[] args, ResultInfo resultInfo) {
            if ("getLastLocation".equals(method.getName())) {
                resultInfo.setResult(getFakeLocation());
                return true;
            } else if ("registerLocationListener".equals(method.getName())) {
                final int transportIndex;
                if (Build.VERSION.SDK_INT >= 31) {
                    transportIndex = 2;
                } else {
                    transportIndex = 1;
                }

                if (args[transportIndex] instanceof IInterface) {
                    IInterface transport = (IInterface) args[transportIndex];
                    updateLocation(transport);
                }
                resultInfo.setResult(null);
                return true;
            } else if ("unregisterLocationListener".equals(method.getName())) {
                resultInfo.setResult(null);
                return true;
            }
            return false;
        }

        @Override
        public void onCallAfter(Method method, Object[] args, Object result) {

        }
    }

    public static void hook(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Field mServiceField = LocationManager.class.getDeclaredField("mService");
            mServiceField.setAccessible(true);
            IInterface mService = (IInterface) mServiceField.get(locationManager);

            Class<?> ILocationManager = Class.forName("android.location.ILocationManager");
            IInterface proxy = (IInterface) Proxy.newProxyInstance(mService.getClass().getClassLoader(), new Class[]{ ILocationManager }, new InvocationHandlerImpl(mService));
            mServiceField.set(locationManager, proxy);
        } catch (Exception e) {
            Log.e("FakeLocation", "hookLocation", e);
        }
    }
}
