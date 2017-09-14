package com.wise.core;
//package com.wise.core;
//
//import android.app.Service;
//import android.content.Intent;
//import android.os.Handler;
//import android.os.IBinder;
//import android.util.Log;
//
//public class AlarmTimeService extends Service {
//
//	private int TEN_MINUTES = 1000 * 10;// five minutes
//	
//	@Override
//	public IBinder onBind(Intent intent) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	
//	
//	@Override
//	public void onCreate() {
//		super.onCreate();
//		objHandler.postDelayed(mTasks, 1000);
//		Log.e("AAA", "定时提交数据服务：" + "onCreate()");
//	}
//
//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		Log.e("AAA", "定时提交数据服务：" + "onStartCommand()");
//		return super.onStartCommand(intent, flags, startId);
//	}
//
//	@Override
//	public void onDestroy() {
//		super.onDestroy();
//		objHandler.removeCallbacks(mTasks);
//		Log.e("AAA", "定时提交数据服务：" + "onDestroy()");
//
//	}
//
//
//	private Handler objHandler = new Handler();
//
//	private Runnable mTasks = new Runnable(){
//		public void run(){
//
//			Log.e("AAA", "定时任务------go spurs go --------");
//			// 添加具体需要做的事情：
//			objHandler.postDelayed(mTasks, TEN_MINUTES);
//		}
//	};
//	
//
//}
