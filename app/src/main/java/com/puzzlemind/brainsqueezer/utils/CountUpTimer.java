package com.puzzlemind.brainsqueezer.utils;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

public abstract class CountUpTimer {

    private final long interval;
    private long base;

    public CountUpTimer(long interval) {
        this.interval = interval;
    }

    public void start() {
        base = SystemClock.elapsedRealtime();
        handler.sendMessage(handler.obtainMessage(MSG));
    }

    public void stop() {
        handler.removeMessages(MSG);
    }

    public void reset() {
        synchronized (this) {
            base = SystemClock.elapsedRealtime();
        }
    }

    public void resume(int timePassed){
        base = SystemClock.elapsedRealtime() - timePassed * 1000L;
        handler.sendMessage(handler.obtainMessage(MSG));
    }

    abstract public void onTick(int elapsedTime);

    private static final int MSG = 1;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            synchronized (CountUpTimer.this) {
                long elapsedTime = SystemClock.elapsedRealtime() - base;
                onTick((int)elapsedTime/1000);
                sendMessageDelayed(obtainMessage(MSG), interval);
            }
        }
    };

}