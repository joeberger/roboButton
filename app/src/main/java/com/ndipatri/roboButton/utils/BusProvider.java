package com.ndipatri.roboButton.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.DeadEvent;

public class BusProvider extends Bus {

    @Override
    public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.post(event);
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    post(event);
                }
            });
        }
    }

    @Override
    public void register(final Object listener) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.register(listener);
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    register(listener);
                }
            });
        }
    }
}
