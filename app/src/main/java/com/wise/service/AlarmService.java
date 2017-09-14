package com.wise.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.wise.config.Config;
import com.wise.util.Logger;

/**
 * @author Wu
 *
 * 十分钟上传一次位置信息
 */
public class AlarmService extends Service {

	private int TEN_MINUTES = 1000*60*5;// five minutes

	private int IntevalNum;

	SharedPreferences pref;
	SharedPreferences.Editor editor;

	private Handler objHandler = new Handler();

	private Runnable mTasks = new Runnable(){
		public void run(){
			Logger.I("AlarmService", "定时任务...............");
			Intent intent = new Intent(LocationService.ACC_ON);
			sendBroadcast(intent);

			SharedPreferences preferences = getSharedPreferences(Config.Shared_Preferences, Context.MODE_PRIVATE);
			IntevalNum = preferences.getInt("nick_IntevalNum", IntevalNum);
			Logger.I("AlarmService", "定时设置..............."+IntevalNum);
			// 添加具体需要做的事情：
			objHandler.postDelayed(mTasks, IntevalNum*1000);
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		objHandler.removeCallbacks(mTasks);
		objHandler.postDelayed(mTasks, 1000);
		Logger.I("AlarmService", "定时服务启动  onCreate:");
		initShare();
	}

	private void initShare(){

		editor = getSharedPreferences(Config.Shared_Preferences, MODE_PRIVATE).edit();
		pref = getSharedPreferences(Config.Shared_Preferences, MODE_PRIVATE);
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logger.I("AlarmService", "定时服务  onStartCommand:");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		objHandler.removeCallbacks(mTasks);
		Logger.I("AlarmService", "定时服务          onDestroy:");
	}

}
