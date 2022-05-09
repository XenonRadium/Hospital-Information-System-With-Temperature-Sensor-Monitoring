package com.example.prototype;

import android.app.Application;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

public class App extends Application implements LifecycleObserver {

    public static boolean IS_APP_IN_FOREGROND = false;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void appInForeground(){
        IS_APP_IN_FOREGROND = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void appInBackground(){
        IS_APP_IN_FOREGROND = false;
    }

    public Boolean getForegroundStatus(){
        return IS_APP_IN_FOREGROND;
    }
}
