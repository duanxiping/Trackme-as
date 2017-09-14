package com.wise.trackme.activity;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wise.config.Config;
import com.wise.config.Msg;
import com.wise.config.Tools;
import com.wise.core.CoreService;
import com.wise.crash.AppManager;
import com.wise.location.GPSLocationListener;
import com.wise.location.GPSLocationManager;
import com.wise.location.GPSProviderStatus;
import com.wise.model.DialogDismissEvent;
import com.wise.model.FiveMinutesEvent;
import com.wise.model.UIUpdataEvent;
import com.wise.service.AlarmService;
import com.wise.service.LocationService;
import com.wise.service.NetThread;
import com.wise.service.NetThread.postDataThread;
import com.wise.util.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import com.wise.service.AlxLocationService;
import com.wise.location.MyLocation;

public class LocationActivity extends AppCompatActivity {

	public static final String CHECK_OUT = "com.wise.checkout";
	public static final String CHECK_In = "com.wise.checkin";

	private ProgressDialog Dialog;//提示框
	private TextView tv_location, tv_time, tv_device_name,tv_coordinates;
	private NotificationManager nm;
	private Notification notification;

	private String lat  = "";
	private String lon  = "";
	private String flag = "";// 定位状态
	private MyBroadCast myBroadCast;
	private String ObjectId = "";
	// 存储具体的位置
	private String detailLocation = "";
	private String ObjectRegNum   ="";
	boolean isSatellite = true;
	private AlertDialog.Builder dialog_gps = null;

	private String Latitude;
	private String Longitude;
	private boolean isLatLonNull;

	SharedPreferences pref;
	SharedPreferences.Editor editor;

	private Configuration config;
	private DisplayMetrics dm;
	private Resources resources;
	private GPSLocationManager gpsLocationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);

		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
		getSupportActionBar().setIcon(R.mipmap.ic_app);

		resources =getResources();//获得res资源对象
		config = resources.getConfiguration();//获得设置对象
		dm = resources.getDisplayMetrics();
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationManager.addGpsStatusListener(listener);//侦听GPS状态
		AppManager.getAppManager().addActivity(this);

		editor = getSharedPreferences(Config.Shared_Preferences, MODE_PRIVATE).edit();
		pref = getSharedPreferences(Config.Shared_Preferences, MODE_PRIVATE);

		editor.putBoolean("isLogout", false);
		editor.commit();

		//注册EventBus
		EventBus.getDefault().register(this);
		init();

		if(!isServiceRunning(LocationActivity.this,"com.wise.service.LocationService")){
			//开启核心服务 一直运行
			startCoreService();
		}

		gpsLocationManager = GPSLocationManager.getInstances(LocationActivity.this);
	}





	/**
	 * @Description: 判断服务是否在运行
	 * @param: @param mContext
	 * @param: @param className 是包名+服务的类名
	 * @param: @return
	 * @return: boolean
	 */
	public static boolean isServiceRunning(Context mContext, String className) {
		boolean isRunning = false;
		ActivityManager activityManager = (ActivityManager)
				mContext.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(1000);
		if (!(serviceList.size()>0)) {
			return false;
		}
		for (int i=0; i<serviceList.size(); i++) {
			if (serviceList.get(i).service.getClassName().equals(className) == true) {
				isRunning = true;
				break;
			}
		}
		return isRunning;
	}

	/**
	 * 启动核心常驻服务
	 */
	private void startCoreService(){
		SharedPreferences.Editor editor = getSharedPreferences("trackme", MODE_PRIVATE).edit();
		SharedPreferences pref = getSharedPreferences("trackme", MODE_PRIVATE);
		editor.putBoolean("isStop_location_alarm", false);
		editor.commit();
		/**
		 * 启动Service，让Service在Activity结束后依然存在
		 */
		startService(new Intent(LocationActivity.this, CoreService.class));
		sendBroadcast(new Intent(Config.Stop_service));
	}


	/**
	 * @param event
	 */
	public void onEventMainThread(UIUpdataEvent event) {
		if(!TextUtils.isEmpty(event.getDetailLocation())){
			tv_location.setText(event.getDetailLocation());
		}
		tv_time.setText(getTime());
//		tv_coordinates.setText( "Lat : " + event.getLat() + "\n" + "Lon: " + event.getLon());
		Latitude = pref.getString("lat", Latitude);
		Longitude = pref.getString("lon", Longitude);
		tv_coordinates.setText("Lat : " +StringToInt(Latitude)+ "\n" + "Lon: " +StringToInt(Longitude) );
		tv_device_name.setText(ObjectRegNum);

	}

	/**
	 * @param event
	 */
	public void onEventMainThread(FiveMinutesEvent event) {
		tv_time.setText(getTime());
//		tv_coordinates.setText( "Lat : " + event.getLocation().getLatitude() + "\n" + "Lon: " + event.getLocation().getLongitude());
		Latitude = pref.getString("lat", Latitude);
		Longitude = pref.getString("lon", Longitude);
		tv_coordinates.setText("Lat : " +StringToInt(Latitude)+ "\n" + "Lon: " +StringToInt(Longitude) );
	}


	/**
	 * @param event
	 */
	public void onEventMainThread(DialogDismissEvent event) {
		if("L_ERROE".equals(event.getmMsg())){
//			Toast.makeText(getApplicationContext(), "Can Not Get Location", Toast.LENGTH_SHORT).show();
		}else{
			Toast.makeText(getApplicationContext(), event.getmMsg(), Toast.LENGTH_SHORT).show();
		}
		if (Dialog != null) {
			Dialog.dismiss();
		}
	}


	/**
	 * @param event
	 */
//	public void onEventMainThread(OpenGPSEvent event) {
//		if(dialog_gps ==null){
//			dialog_gps = new AlertDialog.Builder(LocationActivity.this);
//			dialog_gps.setTitle(getResources().getString(R.string.prompt));
//			dialog_gps.setMessage(getResources().getString(R.string.open_gps));
//			dialog_gps.setPositiveButton(getResources().getString(R.string.ok),
//					new android.content.DialogInterface.OnClickListener() {
//
//						@Override
//						public void onClick(DialogInterface arg0, int arg1) {
//							// 转到手机设置界面，用户设置GPS
//							Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//							startActivityForResult(intent, 5); // 设置完成后返回到原来的界面
//						}
//					});
//			dialog_gps.setNeutralButton(getResources().getString(R.string.cancel), new android.content.DialogInterface.OnClickListener() {
//
//				@Override
//				public void onClick(DialogInterface arg0, int arg1) {
//					arg0.dismiss();
//					dialog_gps = null;
//				}
//			} );
//			dialog_gps.show();
//		}
//	}

	/**
	 * 点击事件监听
	 */
	OnClickListener onClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if(!LoginActivity.isGPSOpen(LocationActivity.this)) {
				dialog_gps = new AlertDialog.Builder(LocationActivity.this);
				dialog_gps.setTitle(getResources().getString(R.string.prompt));
				dialog_gps.setMessage(getResources().getString(R.string.open_gps));
				dialog_gps.setPositiveButton(getResources().getString(R.string.ok),
						new android.content.DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								// 转到手机设置界面，用户设置GPS
								Intent intent = new Intent(
										Settings.ACTION_LOCATION_SOURCE_SETTINGS);
								startActivityForResult(intent, 5); // 设置完成后返回到原来的界面
							}
						});

				dialog_gps.setNeutralButton(getResources().getString(R.string.cancel), new android.content.DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						arg0.dismiss();
					}
				});
				dialog_gps.show();
			}else if(!isConnected(LocationActivity.this)){
				Toast.makeText(LocationActivity.this,getString(R.string.network_wrong), Toast.LENGTH_SHORT).show();
				return;
			}else{
//				Log.e("LocationActivity","可见卫星  ：" + numOfSatellites  + "\n"
//						+ "连接卫星 ：" + useOfSatellites);
				isLatLonNull = AlxLocationService.isWrongPosition(MyLocation.getInstance().latitude,MyLocation.getInstance().longitude);
//				Log.e("LocationActivity","当前经纬度是否为空  ："+isLatLonNull );
				Logger.E("LocationActivity", "当前经纬度是否为空  ："+isLatLonNull);
				Logger.E("LocationActivity", "当前经纬度================== ："+MyLocation.getInstance().latitude+MyLocation.getInstance().longitude);
				switch (v.getId()) {
					case R.id.bt_on_work:
//						AlertDialog.Builder bulder1 = new AlertDialog.Builder(
//								LocationActivity.this);
//						bulder1.setTitle(R.string.prompt);// 设置标题
//						bulder1.setMessage(R.string.acquire_gps);
//						bulder1.setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog,
//												int which) {
//						Log.i("LocationActivity","当前经纬度是否为空  ："+isLatLonNull );
//								if(isLatLonNull = false){
//
//									try {
//										Intent intent = new Intent(CHECK_In);
//										intent.putExtra("isUpload", true);
//										sendBroadcast(intent);
//										Dialog = ProgressDialog.show(LocationActivity.this,"",getString(R.string.in_commit),true);
//										Dialog.setCancelable(false);
//									} catch (Exception e) {
//										e.printStackTrace();
//									}
//								}else{
//									Toast.makeText(LocationActivity.this, "No GPS signal" , Toast.LENGTH_LONG).show();
//								}
//							}
//						});
//						bulder1.setNegativeButton(android.R.string.cancel,
//								new DialogInterface.OnClickListener() {
//									@Override
//									public void onClick(DialogInterface dialog,
//														int which) {

//										Dialog = ProgressDialog.show(
//												LocationActivity.this,"",getString(R.string.in_commit), true);
//										Dialog.setCancelable(false);
//										onWork();

						if(isLatLonNull != true){

						Dialog = ProgressDialog.show(
								LocationActivity.this,"",getString(R.string.in_commit), true);
						Dialog.setCancelable(false);

						Intent intent = new Intent(CHECK_In);
						intent.putExtra("isUpload", true);
						sendBroadcast(intent);
//							onWork();
						}else{
									Toast.makeText(LocationActivity.this, "No location signal" , Toast.LENGTH_LONG).show();
						}

//								});
//						bulder1.show();
						break;
					case R.id.bt_off_work:
//						if(isLatLonNull = false){
//							Intent intent = new Intent(CHECK_OUT);
//							intent.putExtra("isUpload", true);
//							sendBroadcast(intent);
//							Dialog = ProgressDialog.show( LocationActivity.this,"",getString(R.string.in_commit), true);
//							Dialog.setCancelable(false);

						if(isLatLonNull != true){
							Dialog = ProgressDialog.show(
									LocationActivity.this,"",getString(R.string.in_commit), true);
							Dialog.setCancelable(false);

						Intent intent2 = new Intent(CHECK_OUT);
						intent2.putExtra("isUpload", true);
						sendBroadcast(intent2);
//							offWork();
						}else{
							Toast.makeText(LocationActivity.this, "No location signal" , Toast.LENGTH_LONG).show();
						}
						break;
				}
			}
		}
	};


	/**
	 * Handler 消息处理
	 */
	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case Msg.CHECK_IN:
//					Log.w("LocationActivity","上班签到返回信息 ：" +  msg.obj.toString());
					Logger.W("LocationActivity","上班签到返回信息 ：" +  msg.obj.toString());
					checkInResult(msg.obj.toString());
					break;
				case Msg.CHECK_OUT:
//					Log.w("LocationActivity","下班签到返回信息 ：" +  msg.obj.toString());
					Logger.W("LocationActivity","下班签到返回信息 ：" +  msg.obj.toString());
					checkOutResult(msg.obj.toString());
					break;
				case Msg.GET_LOCATION:
					detailLocation = (String) msg.obj;
					tv_location.setText(detailLocation);
					tv_time.setText(getTime());
					Latitude = pref.getString("lat", Latitude);
					Longitude = pref.getString("lon", Longitude);
					tv_coordinates.setText("Lat : " +StringToInt(Latitude)+ "\n" + "Lon: " +StringToInt(Longitude) );
					Logger.W("LocationActivity","我已经获得数据 ：" +  "Lat : " +StringToInt(Latitude)+ "\n" + "Lon: " +StringToInt(Longitude) );
					editor.putString("detailLocation", detailLocation);
					editor.commit();
					break;
				case Msg.GET_DATA:
					try {
						String result = msg.obj.toString();
						JSONObject jsonObject = new JSONObject(result.substring(1,result.length() - 2));
						ObjectRegNum = jsonObject.getString("ObjectRegNum");
						int ObjectIntevalNum = jsonObject.getInt("Interval");
//						Log.e("LocationActivity", "ffffffffffffffffffff : " + ObjectIntevalNum);
						Logger.E("LocationActivity", "ffffffffffffffffffff : " + ObjectIntevalNum);

						tv_device_name.setText(ObjectRegNum);
						editor.putString("nick_name", ObjectRegNum);
						editor.putInt("nick_IntevalNum", ObjectIntevalNum);
						editor.commit();
					} catch (JSONException e) {
						e.printStackTrace();
					}
					break;
				case Msg.UPDATA_LOCATION_AND_CHECK_IN:
//					Log.e("LocationActivity", "签到并且更新位置 : " + msg.obj.toString());
					Logger.E("LocationActivity", "签到上班并且更新位置 : " + msg.obj.toString());
					if(msg.obj.toString().contains("Exception")){
						EventBus.getDefault().post(new DialogDismissEvent("Exception"));
					}else{
						onWork();
					}
					break;
				case Msg.UPDATA_LOCATION_AND_CHECK_OUT:
//					Log.e("LocationActivity", "签到下班并且更新位置 : " + msg.obj.toString());
					Logger.E("LocationActivity", "签到下班并且更新位置 : " + msg.obj.toString());
					if(msg.obj.toString().contains("Exception")){
						EventBus.getDefault().post(new DialogDismissEvent("Exception"));
					}else{
//						Log.e("LocationActivity", "这是签到下班 ------------- " );
						Logger.E("LocationActivity", "这是签到下班 ------------- ");
						offWork();
					}
					break;
				case Msg.UPDATA_GPS_NUMBER :
//					tv_device_name.setText("可见卫星  ：" + numOfSatellites  + "\n" + "连接卫星 ：" + useOfSatellites);
					break;
			}
		}
	};

	/**
	 * onWork
	 */
	private void onWork() {
		if (lat.equals("")) {
			Toast.makeText(LocationActivity.this, R.string.location_error,
					Toast.LENGTH_SHORT).show();
			if (Dialog != null) {
				Dialog.dismiss();
			}
			return;
		} else {
			workMethod(1);
		}
	}

	/**
	 * s上下班打卡时候是否提交地理位置
	 */
	private void isUpdataLocation(boolean isCheckIn){
		Logger.I("LocationActivity", "主页收到通知====签到并提交地理位置====："
				+ "\n" + lat
				+ "\n" + lon
				+ "\n" + flag
				+ "\n" + "isCheckIn：" + isCheckIn
				+ "\n" + "isCheckOut：" + !isCheckIn
				+ "\n" + "id:" +  ObjectId.trim()
				+ "\n" +  "时间 ：" + Tools.getCurrentTime());
		if(isCheckIn){
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("ObjectID", ObjectId.trim()));
			params.add(new BasicNameValuePair("Lat", String.valueOf(MyLocation.getInstance().latitude)));
			params.add(new BasicNameValuePair("Lon", String.valueOf(MyLocation.getInstance().longitude)));
			params.add(new BasicNameValuePair("GPSFlag", flag));
			/*2016-07-18   更改了位置信息接口 UpdateGPSDate2_json  添加了上传时间参数*/
			params.add(new BasicNameValuePair("GPSTime", Tools.getCurrentTime()));
			params.add(new BasicNameValuePair("StatusDes", "ACC ON, Check In"));
			// 更新客户位置
			new Thread(new NetThread.postDataThread(handler,Config.UPDATE_LOCATION, params,
					Msg.UPDATA_LOCATION_AND_CHECK_IN)).start();
		}else{
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("ObjectID", ObjectId.trim()));
			params.add(new BasicNameValuePair("Lat", String.valueOf(MyLocation.getInstance().latitude)));
			params.add(new BasicNameValuePair("Lon", String.valueOf(MyLocation.getInstance().longitude)));
			params.add(new BasicNameValuePair("GPSFlag", flag));
			/*2016-07-18   更改了位置信息接口 UpdateGPSDate2_json  添加了上传时间参数*/
			params.add(new BasicNameValuePair("GPSTime", Tools.getCurrentTime()));
			params.add(new BasicNameValuePair("StatusDes", "ACC ON, Check Out"));
			// 更新客户位置
			new Thread(new NetThread.postDataThread(handler,Config.UPDATE_LOCATION, params,
					Msg.UPDATA_LOCATION_AND_CHECK_OUT)).start();
		}
	}


	/**
	 * offWork
	 */
	private void offWork() {


		if (lat == null || lon == null || "".equals(lat) || "".equals(lon)) {
			Toast.makeText(LocationActivity.this, R.string.location_errors,
					Toast.LENGTH_SHORT).show();
			if (Dialog != null) {
				Dialog.dismiss();
			}
			return;
		} else {
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("ObjectID", ObjectId.trim()));
			params.add(new BasicNameValuePair("EventType", "1"));
			// 上下班
			params.add(new BasicNameValuePair("EventStatus", "0"));
			params.add(new BasicNameValuePair("Lat", lat));
			params.add(new BasicNameValuePair("Lon", lon));
			params.add(new BasicNameValuePair("GPSFlag", flag));
			params.add(new BasicNameValuePair("StatusDes", "ACC On, Check Out"));
			new Thread(new postDataThread(handler, Config.Check, params, Msg.CHECK_OUT)).start();
		}
	}

	/**
	 * @param state
	 */
	public void workMethod(int state) {
		if (lat == null || lon == null || "".equals(lat) || "".equals(lon)) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.cannot_find), Toast.LENGTH_SHORT).show();
		} else {
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("ObjectID", ObjectId.trim()));
			params.add(new BasicNameValuePair("EventType", "1"));
			// 上下班
			params.add(new BasicNameValuePair("EventStatus", state + ""));
			params.add(new BasicNameValuePair("Lat", String.valueOf(MyLocation.getInstance().latitude)));
			params.add(new BasicNameValuePair("Lon", String.valueOf(MyLocation.getInstance().longitude)));
			params.add(new BasicNameValuePair("GPSFlag", flag));
			new Thread(new postDataThread(handler, Config.Check, params, Msg.CHECK_IN)).start();
		}
	}

	/**
	 * 判断是否成功
	 *
	 * @param str
	 */
	private void checkInResult(String str) {
		if (Dialog != null) {
			Dialog.dismiss();
		}
		if ("(0);".equals(str)) {
			//获取具体的位置
			new Thread(new NetThread.GetLocation(String.valueOf(MyLocation.getInstance().latitude), String.valueOf(MyLocation.getInstance().longitude), handler,
					Msg.GET_LOCATION, LocationActivity.this)).start();
			Toast.makeText(LocationActivity.this, getString(R.string.commit_success), Toast.LENGTH_SHORT) .show();
		} else if ((str).indexOf("Exception") != -1) {
			Toast.makeText(
					LocationActivity.this,
					getString(R.string.commit_fail) + ","+ getString(R.string.try_again), Toast.LENGTH_SHORT)
					.show();
		}
	}


	/**
	 * @param str
	 */
	private void checkOutResult(String str) {
		if (Dialog != null) {
			Dialog.dismiss();
		}
		if ("(0);".equals(str)) {
			Toast.makeText(LocationActivity.this,getString(R.string.commit_success), Toast.LENGTH_SHORT) .show();
		}else{
			Toast.makeText(LocationActivity.this, getString(R.string.commit_fail) + ","+  getString(R.string.try_again),
					Toast.LENGTH_SHORT);
		}
	}


	/**
	 * @author W
	 *
	 * 主页接收通知 获取地理反编译位置 更新ui
	 */
	class MyBroadCast extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("location")) {
				lat = intent.getStringExtra("lat");
				lon = intent.getStringExtra("lon");

//				Log.e(LocationActivity.class.getName(),"经纬度："+lat+lon);

				flag = intent.getStringExtra("gps_flag");
				boolean isCheckOut = intent.getBooleanExtra("isCheckOut", false);
				boolean isCheckIn = intent.getBooleanExtra("isCheckIn", false);
				new Thread(new NetThread.GetLocation(lat, lon, handler,Msg.GET_LOCATION, LocationActivity.this)).start();
				if(isCheckIn){
					Logger.I("LocationActivity", "开始签到---------上班--------------------------：" );
					isUpdataLocation(true);
				}else if (isCheckOut){
					Logger.I("LocationActivity", "开始签到---------下班--------------------------：" );
					isUpdataLocation(false);
				}
			} else if (intent.getAction().equals("change_name")) {
				tv_device_name.setText(intent.getStringExtra("name"));
				editor.putString("nick_name",intent.getStringExtra("name"));
				editor.commit();
			}
		}
	}

	protected boolean isRouteDisplayed() {
		return false;
	}


	protected void onDestroy() {
		if (myBroadCast != null) {
			unregisterReceiver(myBroadCast);
		}
//		startCoreService();
		SharedPreferences preferences = getSharedPreferences(Config.Shared_Preferences, Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.commit();
		super.onDestroy();
	}

	/**
	 * 经纬度转换
	 *
	 * @param str
	 * @return
	 */
	public static String StringToInt(String str) {
		Double point_doub = Double.parseDouble(str);
		DecimalFormat df = new DecimalFormat("#.000000");
		return df.format(point_doub);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			startForegroundRun();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	// 将年月日转换为时间戳
	public String getTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
		String str = formatter.format(curDate);
		return str;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Overflow 中显示图标
	 * */
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		// TODO Auto-generated method stub
		if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {
			if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
				try {
					Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
					m.setAccessible(true);
					m.invoke(menu, true);
				} catch (Exception e) {
				}
			}
		}
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public  boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				Log.e("123", ObjectId);
				Intent intent = new Intent(LocationActivity.this,
						ConfigureActivity.class);
				intent.putExtra("ObjectId", ObjectId.trim());
				startActivity(intent);
				break;

			case R.id.action_logout:
				editor.putBoolean("isLogout", true);
				editor.commit();
				stopService();

				LocationActivity.this.finish();
				break;

//			case R.id.action_chinese:
//				config.locale = Locale.SIMPLIFIED_CHINESE;
//				resources.updateConfiguration(config, dm);
//				break;
//
//			case R.id.action_English:
//				config.locale = Locale.US;
//				resources.updateConfiguration(config, dm);
//				break;

		}
		return true;
	}



	/**
	 * 停止  定位和定时提交数据服务
	 */
	public void stopService(){
		stopService(new Intent(LocationActivity.this, LocationService.class));
		stopService(new Intent(LocationActivity.this, AlarmService.class));
		SharedPreferences.Editor editor = getSharedPreferences("trackme", MODE_PRIVATE).edit();
		SharedPreferences pref = getSharedPreferences("trackme", MODE_PRIVATE);
		editor.putBoolean("isStop_location_alarm", true);
		editor.commit();
	}

	/**
	 * 初始化
	 */
	private void init() {
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		tv_location = (TextView) findViewById(R.id.tv_location);
		tv_time = (TextView) findViewById(R.id.tv_time);
		Button bt_on_work = (Button) findViewById(R.id.bt_on_work);
		bt_on_work.setOnClickListener(onClickListener);
		Button bt_off_work = (Button) findViewById(R.id.bt_off_work);
		bt_off_work.setOnClickListener(onClickListener);
		tv_device_name = (TextView) findViewById(R.id.tv_device_name);
		tv_coordinates = (TextView)findViewById(R.id.tv_coordinates);
		detailLocation = pref.getString("detailLocation", "");
		ObjectId       = pref.getString("ObjectId", "").trim();
		Logger.E("LocationActivity", "取出id    ： " + ObjectId);
		flag = pref.getString("gps_flag", Config.gps_flag);
		lat  = pref.getString("lat", "");
		lon  = pref.getString("lon", "");
		ObjectRegNum = pref.getString("nick_name", "");
		tv_device_name.setText(ObjectRegNum);
		tv_time.setText(getTime());
		if (!"".equals(lat) || !"".equals(lon)) {
			new Thread(new NetThread.GetLocation(lat, lon, handler,Msg.GET_LOCATION, LocationActivity.this)).start();
		}
		// 注册广播接收类
		myBroadCast = new MyBroadCast();
		IntentFilter filter = new IntentFilter();
		filter.addAction("location");
		filter.addAction("change_name");
		filter.addAction("latlon");
		filter.addAction("alarm_service");
		registerReceiver(myBroadCast, filter);
		// 读取账号信息
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("ObjectID", ObjectId.trim()));
		new Thread(new NetThread.postDataThread(handler, Config.GetObjectInfo,params, Msg.GET_DATA)).start();
	}


	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// 读取账号信息
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("ObjectID", ObjectId.trim()));
		new Thread(new NetThread.postDataThread(handler, Config.GetObjectInfo,
				params, Msg.GET_DATA)).start();
	}


	/**
	 * 判断网络是否连接
	 *
	 * @param context
	 * @return
	 */
	public static boolean isConnected(Context context){
		ConnectivityManager connectivity = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (null != connectivity) {
			NetworkInfo info = connectivity.getActiveNetworkInfo();
			if (null != info && info.isConnected()) {
				if (info.getState() == NetworkInfo.State.CONNECTED) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		dialog_gps = null;
		if(requestCode == 5){
			if(!LoginActivity.isGPSOpen(LocationActivity.this)){
				Toast.makeText(LocationActivity.this,getResources().getString(R.string.open_gps_fail), Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(LocationActivity.this,getResources().getString(R.string.open_gps_success), Toast.LENGTH_SHORT).show();
			}
		}
	}


	private void startForegroundRun(){
		if(notification == null){
			notification = new Notification();
		}else{
			notification.icon = R.drawable.ic_launcher;
			notification.tickerText = getString(R.string.app_name);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			notification.defaults |= Notification.DEFAULT_SOUND;
			Intent notificationIntent = new Intent(LocationActivity.this,
					LocationActivity.class); // 点击该通知后要跳转的Activity
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			PendingIntent contentItent = PendingIntent.getActivity(
					LocationActivity.this, 0, notificationIntent, 0);
//			notification.setLatestEventInfo(LocationActivity.this,getString(R.string.app_name),getString(R.string.service), contentItent);
			nm.notify(Config.notification_id, notification);
			moveTaskToBack(true);
		}
	}


	private LocationManager locationManager;
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
//					Logger.E("LocationActivity", "当前的卫星数=======    ： " + numOfSatellites);
//					Toast.makeText(LocationActivity.this, "当前的卫星数:  "+numOfSatellites ,Toast.LENGTH_SHORT).show();
                    handler.sendEmptyMessage(Msg.UPDATA_GPS_NUMBER);
					break;
			}
		}
	};
}