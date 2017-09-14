package com.wise.service;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;
import com.wise.config.Config;
import com.wise.config.Msg;
import com.wise.config.Tools;
import com.wise.core.MyApplication1;
import com.wise.location.MyLocation;
import com.wise.model.DialogDismissEvent;
import com.wise.model.LocationBuffer;
import com.wise.model.UIUpdataEvent;
import com.wise.trackme.activity.LocationActivity;
import com.wise.util.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;

public class LocationService extends Service {

	private static final String TAG = "LocationService";
	private static final String CHECK_OUT = "com.wise.checkout";
	private static final String CHECK_In = "com.wise.checkin";
	public static final String ACC_ON = "com.wise.acc.on";
	public static final String UPLOAD_L_B = "com.wise.upload_lb";

	private LocationManager locationManager;
//	private LocationListner gpsListner = null;
//	private LocationListner wifiListner = null;
	private UploadBroadCast ploadBroadCast;

	boolean isUpload_check = false;
	private String flag = ""; // 定位方式
	private String ObjectId = "";
	private WakeLock wakeLock;
	boolean isCheckOut = false;
	boolean isCheckIn = false;
	private String StatusDes = "";
	private boolean mLocation;

	private String lat = "0";
	private String lon = "0";

	private SharedPreferences pref;
	private SharedPreferences.Editor editor;
	private boolean isFirstStrat = false;
	private boolean isLocationOk = false;
	private MyApplication1 app;
	private boolean isLatLonNull;
	private String Latitude;
	private String Longitude;
	private String LatitudeGps;
	private String LongitudeGps;
	private LocationListners gpsListner = null;


	public IBinder onBind(Intent arg0) {
		return null;
	}




	public void onCreate() {
		super.onCreate();

		objHandler.removeCallbacks(mTasks);
		objHandler.postDelayed(mTasks, 1000);

		app = (MyApplication1) getApplication();

		initBro();


		// 获取系统自带google定位管理对象
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

//		wifiListner = new LocationListner();
//		gpsListner  = new LocationListner();

		editor = getSharedPreferences(Config.Shared_Preferences, MODE_PRIVATE).edit();
		pref = getSharedPreferences(Config.Shared_Preferences, MODE_PRIVATE);

		Logger.I(TAG, "LocationService onCreate");
		PowerManager pManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this
				.getClass().getCanonicalName());
		wakeLock.acquire();
		ploadBroadCast = new UploadBroadCast();
		IntentFilter filter = new IntentFilter();
		filter.addAction(CHECK_OUT);
		filter.addAction(CHECK_In);
		filter.addAction(ACC_ON);

		filter.addAction(UPLOAD_L_B);

		registerReceiver(ploadBroadCast, filter);

		SharedPreferences preferences = getSharedPreferences(Config.Shared_Preferences, Context.MODE_PRIVATE);
		ObjectId = preferences.getString("ObjectId", "");

		Logger.E(TAG, "服务取出id    ： " + ObjectId);

		locationManager.addGpsStatusListener(listener);//侦听GPS状态

	}


	private int numOfSatellites = 0;
	private int useOfSatellites = 0;

	private GpsStatus.Listener listener = new GpsStatus.Listener() {
		@Override
		public void onGpsStatusChanged(int i) {
			GpsStatus gpsStatus= locationManager.getGpsStatus(null);
			switch (i){
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

					Iterable<GpsSatellite> allSatellites = gpsStatus.getSatellites();
					Iterator<GpsSatellite> iterator = allSatellites.iterator();
					int satellites = 0;
					int useInfix = 0;
					int maxSatellites=gpsStatus.getMaxSatellites();
					while(iterator.hasNext() && satellites<maxSatellites){
						satellites++;
						GpsSatellite satellite = iterator.next();
						if (satellite.usedInFix())
							useInfix++;
					}
					useOfSatellites = useInfix;
					numOfSatellites = satellites;
					Logger.E("LocationService","现在的卫星数为========="+numOfSatellites);

					break;
			}
		}
	};


		/**
         * @Description: 发送广播启动定时服务
         * @param:
         * @return: void
         */
	private void startAlarmService(){
		Intent alarm_service = new Intent("start_my_alarm_service");
		sendBroadcast(alarm_service);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {

		isFirstStrat = intent.getBooleanExtra("isFirstStart",false);
		Logger.I(TAG, "是否第一次启动服务--------------------------------------------------" + " " + isFirstStrat);
		Test("onStartCommand");
		return super.onStartCommand(intent, flags, startId);
	}

	private Boolean isFirst = true;
	private Boolean isGPS = true;
	private class LocationListners implements LocationListener {

		public void onLocationChanged(Location location) {
			if (isFirst) {
				Log.e(TAG, "是否第一次 ：" + isFirst);
				isFirst = false;
				LatitudeGps = String.valueOf(location.getLatitude());
				LongitudeGps = String.valueOf(location.getLongitude());

				Logger.E("LocationService", "Gps定位的经纬度=="+LatitudeGps+LongitudeGps);

				if (location.getProvider().equals("gps")){
					isGPS = false;
				}else {

					isGPS = true;
				}

			} else {
				LatitudeGps = String.valueOf(location.getLatitude());
				LongitudeGps = String.valueOf(location.getLongitude());

				if (location.getProvider().equals("gps")){
					isGPS = false;
				}else {

					isGPS = true;
				}

//				Logger.E("LocationService", "Gps定位的经纬度=="+LatitudeGps+LongitudeGps+isGPS);
			}

			Logger.E("LocationService", "Gps定位的经纬度=="+LatitudeGps+LongitudeGps+isGPS);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}
		@Override
		public void onProviderDisabled(String provider) {
		}
	}


	private void startLocation(){

		if(locationManager == null){
			locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		}
		if(gpsListner == null){
			gpsListner = new LocationListners();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(getApplication().checkSelfPermission(Manifest.permission.CALL_PHONE)== PackageManager.PERMISSION_GRANTED) {
			}
		}else{
			locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,5000 , 0, gpsListner);
			locationManager.addGpsStatusListener(listener);//侦听GPS状态
		}



	}

	private void removeLocationListener(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(getApplication().checkSelfPermission(Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED) {
			}
		}else{
			if (gpsListner != null) {
				locationManager.removeUpdates(gpsListner);
			}
			locationManager = null;
		}
	}

	private Handler objHandler = new Handler();
	private Runnable mTasks = new Runnable(){
		public void run(){
			startLocation();

			if (isGPS != true){
				Latitude = LatitudeGps;
				Longitude = LongitudeGps;
				isLatLonNull = AlxLocationService.isWrongPosition(Double.valueOf(Latitude),Double.valueOf(Longitude));
//				Toast.makeText(LocationService.this, "当前是GPS定位"
//						+"      经度为：======"+Latitude+"   纬度为：====="+Longitude,0).show();
				Logger.I("LocationService","当前是GPS定位"+"      经度为：======"+Latitude+"   纬度为：====="+Longitude);

			}else {
				Latitude = String.valueOf(MyLocation.getInstance().latitude);
				Longitude = String.valueOf(MyLocation.getInstance().longitude);
				isLatLonNull = AlxLocationService.isWrongPosition(Double.valueOf(Latitude),Double.valueOf(Longitude));
				Logger.I("LocationService","当前是Wifi或者是基站定位"+"      经度为：======"+Latitude+"   纬度为：====="+Longitude);
			}

			Logger.I("LocationService","经纬度是否为空======："+ isLatLonNull+"    经度为：======"+Latitude+"   纬度为：====="+Longitude);

			isGPS = true;
			if (isLatLonNull != true) {
				isLocationOk = true;
				Logger.I("LocationService","我一直在走呢======："+ isLatLonNull);
				mLocation = isLatLonNull;
				editor.putString("lat", Latitude);
				editor.putString("lon", Longitude);
				editor.putBoolean("isLatLonNull", isLatLonNull);
//			editor.putString("gps_flag", flag);
				editor.commit();
				if (isUpload_check) {
					Logger.E(TAG, "isUpload =============== " + isUpload_check);
					isUpload_check = false;
					updateLocation(mLocation);
				}
				if (isFirstStrat) {
					isFirstStrat = false;
					startAlarmService();//定位成功之后开启定时提交数据任务
					Logger.E(TAG, " 定时任务提交== " + isFirstStrat);
				}

			}
			objHandler.postDelayed(mTasks, 10000);
		}
	};


	/**
	 * 发送广播 到主页面去签到
	 *
	 * @param location
	 *
	 */
	public void updateLocation(boolean location) {
		Intent intent = new Intent("location");
		if (location != true) {
			intent.putExtra("lat", Latitude);
			intent.putExtra("lon", Longitude);
//			intent.putExtra("gps_flag", gps_flag);
			intent.putExtra("isCheckOut", isCheckOut);
			intent.putExtra("isCheckIn", isCheckIn);
			sendBroadcast(intent);

			Logger.E("LocationService", "发送通知到主页更新经纬度："+Latitude+Longitude);
			if(isCheckOut || isCheckIn){
				isCheckOut = false;
				isCheckIn  = false;
				Test("updateLocation");
			}
		}
	}

//	/**
//	 * @param location
//	 */
//	private void updataLatLon(Location location){
//		if (location != null) {
//			Intent intent = new Intent("latlon");
//			intent.putExtra("lat", String.valueOf(location.getLatitude()));
//			intent.putExtra("lon", String.valueOf(location.getLongitude()));
//			sendBroadcast(intent);
//			Log.e("LocationService", "发送通知到主页更新经纬度：");
//		}
//	}


	/**
	 * Handler 消息处理
	 */
	private Handler mHandler = new Handler(){

		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			lat  = pref.getString("lat", "0");//上一次的数据
			lon  = pref.getString("lon", "0");//上一次的数据
			switch (msg.what) {
				case Msg.UPDATA_LOCATION:
					Logger.I(TAG,"离线经度===" + lat+"   离线纬度====="+lon);
					if(msg.obj.toString().contains("Exception")){
						Toast.makeText(LocationService.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
					}else{
						Logger.I(TAG, "每隔五分钟接提交数据 返回信息（2222222222） :" + msg.obj.toString());

						if(mLocation = false){
							new Thread(new NetThread.GetLocation(Latitude, Longitude,
									mHandler, Msg.GET_FIVE_LOACTION, LocationService.this)).start();
						}else {
							new Thread(new NetThread.GetLocation(lat,lon,mHandler, Msg.GET_FIVE_LOACTION, LocationService.this)).start();
						}
					}
					break;
				case Msg.GET_FIVE_LOACTION:
					if(msg.obj.toString().contains("Exception")){
						Toast.makeText(LocationService.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
					}else{
						editor.putString("detailLocation", msg.obj.toString());
						editor.commit();
						Logger.I("LocationService", "五分钟更新一次地理位置（333333333）：" + msg.obj.toString());

//						if(mLocation  = false){
//							EventBus.getDefault().post(new UIUpdataEvent(msg.obj.toString(),mLocation));
//						}else {
//							EventBus.getDefault().post(new UIUpdataEvent(msg.obj.toString(),lat,lon));
//						}

						// 2016-06-21 提交没有网络缓存的数据//
						if(isLoadlb){
							upload_lb ++ ;
							if(upload_lb<app.locationBuffer.size()){
								Intent lb = new Intent(LocationService.UPLOAD_L_B);
								sendBroadcast(lb);
							}else {
								app.locationBuffer.clear();
								upload_lb = 0;
								isLoadlb = false;
								Logger.E(TAG, "提交完成````````````````````");
							}
						}
					}
					break;
			}
		};
	};


	/**
	 * @author Wu
	 *
	 * 广播接收器
	 */
	class UploadBroadCast extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			if(mLocation != true){

//				EventBus.getDefault().post(new OpenGPSEvent("open_gps"));
//
//			}else {
				if (intent.getAction().equals(CHECK_OUT)) {

					Logger.W(TAG, "下班 ：-------------------- " + isLocationOk);

					//如果定位不成功就消失提示框提示定位失败
					if(!isLocationOk){
						EventBus.getDefault().post(new DialogDismissEvent("L_ERROE"));
						return;
					}
					isUpload_check = intent.getBooleanExtra("isUpload", false);
					Logger.I(TAG, "下班 ：" + isUpload_check);
					isCheckOut = true;
					isCheckIn = false;
					StatusDes = "ACC ON, Check Out";
					// 定位下班
//					if (gpsListner != null) {
//						locationManager.removeUpdates(gpsListner);
//						gpsListner = null;
//					}
//					if (wifiListner != null) {
//						locationManager.removeUpdates(wifiListner);
//						wifiListner = null;
//					}
//
//					gpsListner = new LocationListner();
//					wifiListner = new LocationListner();
//					// 定位
//					if(Config.IS_DEBUG){
//						locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER,1000 * 30, 0, wifiListner);
//					}else{
//						locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,1000 * 30, 0, gpsListner);
//					}

//
//					gpsListner = new LocationListner();
//					wifiListner = new LocationListner();
//					locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,1000 * 30 , 0, gpsListner);
//					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000 * 30 , 0, wifiListner);
					Test("UploadBroadCast");
				}else if(intent.getAction().equals(CHECK_In)){
					//如果定位不成功就消失提示框提示定位失败
					Logger.W(TAG, "上班 ：-------------------- " + isLocationOk);
					if(!isLocationOk){
						EventBus.getDefault().post(new DialogDismissEvent("L_ERROE"));
						return;
					}

					isUpload_check = intent.getBooleanExtra("isUpload", false);
					Logger.I(TAG, "上班 ：" + isUpload_check);
					isCheckIn = true;
					isCheckOut = false;
					StatusDes = "ACC ON, Check In";
//					// 定位上班
//					if (gpsListner != null) {
//						locationManager.removeUpdates(gpsListner);
//						gpsListner = null;
//					}
//					if (wifiListner != null) {
//						locationManager.removeUpdates(wifiListner);
//						wifiListner = null;
//					}
//
//					gpsListner = new LocationListner();
//					wifiListner = new LocationListner();
					// 定位
//					if(Config.IS_DEBUG){
//						locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER,1000 * 30, 0, wifiListner);
//					}else{
//						locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,1000 * 30, 0, gpsListner);
//					}

//					gpsListner = new LocationListner();
//					wifiListner = new LocationListner();
//					locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,1000 * 30, 0, gpsListner);
//					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000 * 30, 0, wifiListner);
					Test("UploadBroadCast");
				}else if(intent.getAction().equals(ACC_ON)){
					Logger.E(TAG, "每隔五分钟接收到一次广播 （111111）  :Acc  on  " + mLocation);
					// 这里接收到的广播室 每个五分钟接收到一次广播去更新客户地理位置信息  ACC ON app登陆就开始每隔五分钟更新一次

					if(isNetworkAvailable(context)){
						if(mLocation != true){
							List<NameValuePair> params = new ArrayList<NameValuePair>();
							params.add(new BasicNameValuePair("ObjectID", ObjectId.trim()));
							params.add(new BasicNameValuePair("Lat", Latitude));
							params.add(new BasicNameValuePair("Lon", Longitude));
							params.add(new BasicNameValuePair("GPSFlag", flag));
							params.add(new BasicNameValuePair("StatusDes", "ACC ON"));
							/*2016-07-18   更改了位置信息接口 UpdateGPSDate2_json  添加了上传时间参数*/
							params.add(new BasicNameValuePair("GPSTime", Tools.getCurrentTime()));

							Logger.E(TAG, "提交时间 v   " + Tools.getCurrentTime());

							new Thread(new NetThread.postDataThread(mHandler,Config.UPDATE_LOCATION, params, Msg.UPDATA_LOCATION)).start();
						}else {
							lat  = pref.getString("lat", "0");//上一次的数据
							lon  = pref.getString("lon", "0");//上一次的数据
							List<NameValuePair> params = new ArrayList<NameValuePair>();
							params.add(new BasicNameValuePair("ObjectID", ObjectId.trim()));
							params.add(new BasicNameValuePair("Lat", lat));
							params.add(new BasicNameValuePair("Lon", lon));
							params.add(new BasicNameValuePair("GPSFlag", flag));
							params.add(new BasicNameValuePair("StatusDes", "ACC ON"));
							/*2016-07-18   更改了位置信息接口 UpdateGPSDate2_json  添加了上传时间参数*/
							params.add(new BasicNameValuePair("GPSTime", Tools.getCurrentTime()));
							new Thread(new NetThread.postDataThread(mHandler,Config.UPDATE_LOCATION, params, Msg.UPDATA_LOCATION)).start();
						}
					}else{
						if(mLocation != true){
							LocationBuffer lb = new LocationBuffer();
							lb.setGPSFlag(flag);
							lb.setLat(Latitude);
							lb.setLon(Longitude);
							lb.setGPSTime(Tools.getCurrentTime());
							app.locationBuffer.add(lb);
							Logger.E(TAG, "有缓存来来来 11111111");
						}else{
							LocationBuffer lb = new LocationBuffer();
							lb.setGPSFlag(flag);
							lb.setLat(lat);
							lb.setLon(lon);
							lb.setGPSTime(Tools.getCurrentTime());
							app.locationBuffer.add(lb);
							Logger.E(TAG, "有缓存来来来 22222");
						}
					}
				}else if(intent.getAction().equals(UPLOAD_L_B)){

					if(upload_lb < app.locationBuffer.size()){
						List<NameValuePair> params = new ArrayList<NameValuePair>();
						params.add(new BasicNameValuePair("ObjectID", ObjectId.trim()));
						params.add(new BasicNameValuePair("Lat", app.locationBuffer.get(upload_lb).getLat()));
						params.add(new BasicNameValuePair("Lon", app.locationBuffer.get(upload_lb).getLon()));
						params.add(new BasicNameValuePair("GPSFlag", app.locationBuffer.get(upload_lb).getGPSFlag()));
						params.add(new BasicNameValuePair("StatusDes", "ACC ON"));
						/*2016-07-18   更改了位置信息接口 UpdateGPSDate2_json  添加了上传时间参数*/
						params.add(new BasicNameValuePair("GPSTime", app.locationBuffer.get(upload_lb).getGPSTime()));

						new Thread(new NetThread.postDataThread(mHandler,Config.UPDATE_LOCATION, params, Msg.UPDATA_LOCATION)).start();

						Logger.E(TAG, "提交缓存数据    ++++++++++++++++ ");
					}

				}
			}
		}
	}

	int upload_lb = 0;
	boolean isLoadlb = false;

	private void Test(String tag){
		Logger.I(TAG, tag + "=======" + "isUpload = " + isUpload_check + " , isCheckOut = " + isCheckOut
				+ " , isCheckIn = " + isCheckIn);
	}

	public void onDestroy() {
		super.onDestroy();
		Logger.I(TAG, "LocationService  onDestroy()" );
		try {
			Test("onDestroy");
			if (ploadBroadCast != null) {
				unregisterReceiver(ploadBroadCast);
			}
//			if (gpsListner != null) {
//				locationManager.removeUpdates(gpsListner);
//				gpsListner = null;
//			}
//			if (wifiListner != null) {
//				locationManager.removeUpdates(wifiListner);
//				wifiListner = null;
//			}
			wakeLock.release();
			wakeLock = null;
			int notification_id = 19134639;
			NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nManager.cancel(notification_id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		app.locationBuffer.clear();
		unregisterReceiver(networkChangeReceiver);

		removeLocationListener();
	}



	/**
	 * 网络监听
	 */
	class NetworkChangeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager connectivityManager = (ConnectivityManager)
					context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			if(networkInfo != null && networkInfo.isAvailable()){
				if(app.locationBuffer.size()>0){
					isLoadlb = true;
					Intent lb = new Intent(LocationService.UPLOAD_L_B);
					sendBroadcast(lb);
				}

			}else{
				Toast.makeText(LocationService.this,"Network is unavailable!", Toast.LENGTH_SHORT).show();
			}
		}
	}






	private IntentFilter filter;/** 监听系统网络变化广播 */
	private NetworkChangeReceiver networkChangeReceiver;


	/**
	 * 初始化广播
	 */
	private void initBro() {
		filter = new IntentFilter();
		filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		networkChangeReceiver = new NetworkChangeReceiver();
		registerReceiver(networkChangeReceiver,filter);
	}



	/**
	 * 检查当前网络是否可用
	 * @param activity
	 * @return
	 */

	public static boolean isNetworkAvailable(Context activity){
		Context context = activity.getApplicationContext();
		// 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager == null){
			return false;
		} else{
			// 获取NetworkInfo对象
			NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();
			if (networkInfo != null && networkInfo.length > 0){
				for (int i = 0; i < networkInfo.length; i++){
					// 判断当前网络状态是否为连接状态
					if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED){

						Logger.I(TAG, "网络可用oooooooooooooooooooo" );
						return true;
					}
				}
			}
		}
		return false;
	}


}