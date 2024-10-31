package com.example.shadowtest.location;

import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HookITelephonyManager {

    private static class InvocationHandlerImpl implements InvocationHandler, HookCallback {

        private final Object original;

        public InvocationHandlerImpl(Object original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.i("FakeLocation", "TelephonyManager.methodName: " + method.getName());
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
            if ("getNeighboringCellInfo".equals(method.getName())) {
                List<NeighboringCellInfo> infos = new ArrayList<>();
                NeighboringCellInfo info = new NeighboringCellInfo();
                infos.add(info);
                resultInfo.setResult(infos);
                return true;
            } else if ("getAllCellInfo".equals(method.getName())) {
                List<CellInfo> result = new ArrayList<>();
                try {
                    CellInfoWcdma cellInfoWcdma = CellInfoWcdma.class.getDeclaredConstructor().newInstance();
                    result.add(cellInfoWcdma);
                } catch (Exception e) {
                    Log.e("FakeLocation", "", e);
                }
                resultInfo.setResult(result);
                return true;
            } else if ("getCellLocation".equals(method.getName())) {
                try {
                    CellIdentityWcdma cellIdentityWcdma = CellIdentityWcdma.class.getDeclaredConstructor().newInstance();
                    resultInfo.setResult(cellIdentityWcdma);
                    return true;
                } catch (Exception e) {
                    Log.e("FakeLocation", "", e);
                }
            }
            return false;
        }

        public void onCallAfter(Method method, Object[] args, Object result) {

        }
    }

    public static void hook() {
        try {
            Class<?> ServiceManagerClass = Class.forName("android.os.ServiceManager");
            Method checkServiceMethod = ServiceManagerClass.getDeclaredMethod("checkService", String.class);
            checkServiceMethod.setAccessible(true);

            IBinder service = (IBinder) checkServiceMethod.invoke(null, Context.TELEPHONY_SERVICE);
            Class<?> ITelephonyClass = Class.forName("com.android.internal.telephony.ITelephony");

            Class<?> ITelephony_StubClass = Class.forName("com.android.internal.telephony.ITelephony$Stub");
            Method asInterfaceMethod = ITelephony_StubClass.getDeclaredMethod("asInterface", IBinder.class);
            asInterfaceMethod.setAccessible(true);
            IInterface iInterface = (IInterface) asInterfaceMethod.invoke(null, service);

            IInterface proxy = (IInterface) Proxy.newProxyInstance(service.getClass().getClassLoader(), new Class[]{ ITelephonyClass }, new InvocationHandlerImpl(iInterface));
            Field sCacheField = ServiceManagerClass.getDeclaredField("sCache");
            sCacheField.setAccessible(true);
            Map<String, IBinder> sCache = (Map<String, IBinder>) sCacheField.get(null);
            sCache.put(Context.TELEPHONY_SERVICE, new BinderWrapper(service, proxy));

            Field sITelephonyField = TelephonyManager.class.getDeclaredField("sITelephony");
            sITelephonyField.setAccessible(true);
            sITelephonyField.set(null, proxy);

        } catch (Exception e) {
            Log.e("FakeLocation", "hookLocation", e);
        }
    }
}
