package com.example.shadowtest.location;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import java.io.FileDescriptor;
import java.lang.reflect.Method;

public class BinderWrapper implements IBinder {
    private final IBinder binder;
    private final IInterface proxy;

    public BinderWrapper(IBinder binder, IInterface proxy) {
        this.binder = binder;
        this.proxy = proxy;
    }

    @Override
    public final String getInterfaceDescriptor() throws RemoteException {
        return binder.getInterfaceDescriptor();
    }

    @Override
    public final boolean pingBinder() {
        return this.binder.pingBinder();
    }

    @Override
    public final boolean isBinderAlive() {
        return this.binder.isBinderAlive();
    }

    @Override
    public final IInterface queryLocalInterface(String descriptor) {
        return this.proxy;
    }

    @Override
    public final void dump(FileDescriptor fd, String[] args) throws RemoteException {
        this.binder.dump(fd, args);
    }

    @Override
    public final void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
        this.binder.dumpAsync(fd, args);
    }

    @Override
    public final boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return this.binder.transact(code, data, reply, flags);
    }

    @Override
    public final void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
        this.binder.linkToDeath(recipient, flags);
    }

    @Override
    public final boolean unlinkToDeath(DeathRecipient recipient, int flags) {
        return this.binder.unlinkToDeath(recipient, flags);
    }

    public IBinder getExtension() throws RemoteException {
        try {
            Method getExtensionMethod = IBinder.class.getDeclaredMethod("getExtension");
            getExtensionMethod.setAccessible(true);
            getExtensionMethod.invoke(this.binder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
