package com.wise.core;
//package com.wise.core;
//
//import android.app.Service;
//import android.content.Intent;
//import android.os.Handler;
//import android.os.IBinder;
//import android.util.Log;
//
//public class LoactionCoreService extends Service {
//
//	
//	private int TEN_MINUTES = 1000 * 10;// five minutes
//	
//	
//	@Override
//	public IBinder onBind(Intent intent) {
//		return null;
//	}
//	
//	@Override
//	public void onCreate() {
//		super.onCreate();
//		
//		objHandler.postDelayed(mTasks, 1000);
//		
//		
//		Log.e("AAA", "定位服务：" + "onCreate()");
//	}
//
//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		Log.e("AAA", "定位服务：" + "onStartCommand()");
//		startAlarmService();
//		return super.onStartCommand(intent, flags, startId);
//	}
//
//	@Override
//	public void onDestroy() {
//		super.onDestroy();
//		objHandler.removeCallbacks(mTasks);
//		Log.e("AAA", "定位服务：" + "onDestroy()");
//	}
//
//
//	/**
//	 * @Description: 发送广播启动定时服务
//	 * @param:
//	 * @return: void
//	 */
//	private void startAlarmService(){
//		Intent alarm_service = new Intent("start_my_alarm_service");
//		sendBroadcast(alarm_service);
//	}
//
//
//	private Handler objHandler = new Handler();
//
//	private Runnable mTasks = new Runnable(){
//		public void run(){
//
//			Log.e("AAA", "定时获取定位任务------go spurs go --------");
//			// 添加具体需要做的事情：
//			objHandler.postDelayed(mTasks, TEN_MINUTES);
//		}
//	};
//}
