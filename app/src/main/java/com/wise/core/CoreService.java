package com.wise.core;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.wgc.cmwgc.service.StrongService;
import com.wise.config.Config;
import com.wise.service.AlarmService;
import com.wise.service.LocationService;
import com.wise.util.Logger;

import com.wise.service.AlxLocationService;
import com.wise.util.Utils;

public class CoreService extends Service {

	private int TEN_MINUTES = 1000 * 5;// five 
	private ServiceBroadcast mServiceBroadcast;
	private boolean isStop = false;
	private SharedPreferences pref;
	private SharedPreferences.Editor editor;



	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case 1:
					startCoreServerAgin();
					break;

				default:
					break;
			}

		};
	};

	/**
	 * 使用aidl 启动Service2
	 */
	private StrongService startS2 = new StrongService.Stub() {
		@Override
		public void stopService() throws RemoteException {
			Intent i = new Intent(getBaseContext(), LocationService.class);
			getBaseContext().stopService(i);
			Intent ii = new Intent(getBaseContext(), AlarmService.class);
			getBaseContext().stopService(ii);
			Intent iii = new Intent(getBaseContext(), AlxLocationService.class);
			getBaseContext().stopService(iii);
		}

		@Override
		public void startService() throws RemoteException {
			Intent i = new Intent(getBaseContext(), LocationService.class);
			getBaseContext().startService(i);
			Intent ii = new Intent(getBaseContext(), AlarmService.class);
			getBaseContext().startService(ii);
			Intent iii = new Intent(getBaseContext(), AlxLocationService.class);
			getBaseContext().startService(iii);
		}
	};

	/**
	 * 在内存紧张的时候，系统回收内存时，会回调OnTrimMemory， 重写onTrimMemory当系统清理内存时从新启动Service2
	 */
	@Override
	public void onTrimMemory(int level) {
		/*
		 * 启动service2
		 */
		startCoreServerAgin();

	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void onCreate() {
		super.onCreate();
		pref = getSharedPreferences("trackme", MODE_PRIVATE);
		editor = getSharedPreferences("trackme", MODE_PRIVATE).edit();
		isStop = pref.getBoolean("isStop_location_alarm", false);
		// 注册广播接收类
		mServiceBroadcast = new ServiceBroadcast();
		IntentFilter filter = new IntentFilter();
		filter.addAction("start_my_location_service");
		filter.addAction("start_my_wifi_cell_service");
		filter.addAction("start_my_alarm_service");
		filter.addAction(Config.Stop_service);
		registerReceiver(mServiceBroadcast, filter);
		objHandler.postDelayed(mTasks, 1000);
		Logger.I("CoreService", "核心服务：" + "onCreate()");
//		Log.e("CoreService", "CoreServer 正在启动..............." );
//		startCoreServerAgin();
		/*
		 * 此线程用监听Service2的状态
		 */
	new Thread() {
		public void run() {
			while (true) {
				boolean isRun = Utils.isServiceWork(CoreService.this,
						"com.wise.service.LocationService");
				if (isRun == false) {
					Message msg = Message.obtain();
					msg.what = 1;
					handler.sendMessage(msg);
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
	}.start();

}


	/**
	 * 判断Service2是否还在运行，如果不是则启动Service2
	 */
	private void startCoreServerAgin() {
		boolean isRun = Utils.isServiceWork(CoreService.this,
				"com.wise.service.LocationService");
		if (isRun == false) {
			try {
				startS2.startService();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Logger.I("CoreService", "核心服务：" + "onStartCommand()--" + isStop);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		objHandler.removeCallbacks(mTasks);
		Intent service_again =new Intent(getApplicationContext(),CoreService.class);
		startService(service_again);
		Logger.I("CoreService", "核心服务：" + "onDestroy()");
	}



	/**
	 * @Description:发送广播启动定位服务
	 * @param:
	 * @return: void
	 */
	private void startLocationService(){
		Intent location_service = new Intent("start_my_location_service");
		sendBroadcast(location_service);
	}


	/**
	 * @Description:发送广播启动wifi和基站定位服务
	 * @param:
	 * @return: void
	 */
	private void startWifiCellService(){
		Intent location_service = new Intent("start_my_wifi_cell_service");
		sendBroadcast(location_service);
	}


	/**
	 * @Description:发送广播启动定时服务
	 * @param:
	 * @return: void
	 */
	private void startIntevalService(){
		Intent location_service = new Intent("start_my_alarm_service");
		sendBroadcast(location_service);
	}


	/**
	 * @ClassName:  ServiceBroadcast
	 * @Description:广播接收器
	 * @author: Android_Robot
	 * @date:   2016-4-28 下午5:41:15
	 *
	 */
	class ServiceBroadcast extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(intent.getAction().equals("start_my_location_service")){
				Intent location_service = new Intent(context, LocationService.class);
				location_service.putExtra("isFirstStart", true);
				startService(location_service);// 启动service获取位置
			}else if(intent.getAction().equals("start_my_alarm_service")){
				Intent intent_5_minutes_updata_service = new Intent(context, AlarmService.class);
				startService(intent_5_minutes_updata_service);
			}else if (intent.getAction().equals("start_my_wifi_cell_service")){
				Intent intent_wifi_cell_service = new Intent(context, AlxLocationService.class);
				startService(intent_wifi_cell_service);
			}else if(intent.getAction().equals(Config.Stop_service)){
				isStop = false;
				Logger.I("CoreService", "0000000000000--" + isStop);
				objHandler.postDelayed(mTasks, 1000);
			}
		}
	}

	private Handler objHandler = new Handler();

	private Runnable mTasks = new Runnable(){
		public void run(){
			Logger.I("CoreService", "核心服务心跳包！！！！！！！！---" + isStop );
			if(!isStop){
				startLocationService();//发送广播启动定位服务
				startIntevalService();//发送广播启动定时服务
				startWifiCellService();//发送广播启动wifi和基站定位服务
			}
//			objHandler.postDelayed(mTasks, 5000);
		}
	};

}
