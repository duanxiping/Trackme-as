package com.wise.core;

import android.content.Context;
import android.util.Log;

import com.marswin89.marsdaemon.DaemonApplication;
import com.marswin89.marsdaemon.DaemonConfigurations;
import com.wise.crash.CrashHandler;
import com.wise.model.LocationBuffer;
import com.wise.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation 1<br/>
 * override one method is ok.<br/>
 *
 * Created by Mars on 12/24/15.
 */
public class MyApplication1 extends DaemonApplication {


    public List<LocationBuffer> locationBuffer = new ArrayList<LocationBuffer>();


    /**
     * you can override this method instead of {@link android.app.Application attachBaseContext}
     * @param base
     */
    @Override
    public void attachBaseContextByDaemon(Context base) {
        super.attachBaseContextByDaemon(base);
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();

        CrashHandler crashHandler= CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
        Logger.I("AAA", " MyApplication1 ====== onCreate");
    }

    /**
     * give the configuration to lib in this callback
     * @return
     */
    @Override
    protected DaemonConfigurations getDaemonConfigurations() {
        DaemonConfigurations.DaemonConfiguration configuration1 = new DaemonConfigurations.DaemonConfiguration(
                "com.wise.trackme.activity:process1",//一定是包名跟进程名字
                CoreService.class.getCanonicalName(),
                Receiver1.class.getCanonicalName());

        DaemonConfigurations.DaemonConfiguration configuration2 = new DaemonConfigurations.DaemonConfiguration(
                "com.wise.trackme.activity:process2",//一定是包名跟进程名字
                Service2.class.getCanonicalName(),
                Receiver2.class.getCanonicalName());

        DaemonConfigurations.DaemonListener listener = new MyDaemonListener();
        //return new DaemonConfigurations(configuration1, configuration2);//listener can be null
        return new DaemonConfigurations(configuration1, configuration2, listener);
    }


    class MyDaemonListener implements DaemonConfigurations.DaemonListener{
        @Override
        public void onPersistentStart(Context context) {
        }

        @Override
        public void onDaemonAssistantStart(Context context) {
        }

        @Override
        public void onWatchDaemonDaed() {
        }
    }
}
