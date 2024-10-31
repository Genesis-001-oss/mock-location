package com.example.shadowtest.location;

import java.lang.reflect.Method;

public interface HookCallback {
    boolean onCallBefore(Method method, Object[] args, ResultInfo resultInfo);
    void onCallAfter(Method method, Object[] args, Object result);
}
