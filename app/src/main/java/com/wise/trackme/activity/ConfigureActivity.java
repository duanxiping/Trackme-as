package com.wise.trackme.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.wise.config.Config;
import com.wise.crash.AppManager;
import com.wise.service.NetThread;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConfigureActivity extends Activity {
	private final static String TAG = "ConfigureActivity";
	private final static int SAVE = 1;
	private final static int GET_DATA = 2;

	EditText et_sim,et_vname,et_host_ip,et_Interval;
	String ObjectId = "";
	ProgressDialog progressDialog;

	SharedPreferences pref;
	SharedPreferences.Editor editor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_configure);
		AppManager.getAppManager().addActivity(this);
		editor = getSharedPreferences("ip", MODE_PRIVATE).edit();
		pref   = getSharedPreferences("ip", MODE_PRIVATE);
//    	LoginActivity.setConfig(pref.getString("host_ip", Config.URL));

		Init();
	}

	Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case SAVE:
					if(progressDialog != null){
						progressDialog.dismiss();
					}
					if(Config.ODG){
						Log.d(TAG, msg.obj.toString());
					}
					if(msg.obj.toString().indexOf("-1") >= 0){
						Toast.makeText(getApplicationContext(), R.string.commit_fail, Toast.LENGTH_SHORT).show();
					}else{
						Toast.makeText(getApplicationContext(), R.string.commit_success, Toast.LENGTH_SHORT).show();
						Intent intent = new Intent("change_name");
						intent.putExtra("name", et_vname.getText().toString().trim());
						sendBroadcast(intent);
						finish();
					}
					break;
				case GET_DATA:
					if(progressDialog != null){
						progressDialog.dismiss();
					}
					if(Config.ODG){
						Log.d(TAG, msg.obj.toString());
					}
					if ((msg.obj.toString()).indexOf("Exception") != -1) {

//					et_sim.setText(pref.getString("sim_name", ""));
//					et_vname.setText(pref.getString("my_name", ""));

						AlertDialog.Builder builder=new AlertDialog.Builder(ConfigureActivity.this);
						builder.setTitle(R.string.warning);
						builder.setMessage(R.string.msg_warning);
						builder.setIcon(R.drawable.ic_launcher);
						builder.setPositiveButton(R.string.warn_return, new android.content.DialogInterface.OnClickListener() {
							//正能量按钮 Positive
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								finish();
							}
						});
						builder.create().show();
					}else {
						try {
							String result = msg.obj.toString();
							JSONObject jsonObject = new JSONObject(result.substring(1, result.length() -2));
							String GSMVoiceNum = jsonObject.getString("GSMVoiceNum");
							String ObjectRegNum = jsonObject.getString("ObjectRegNum");
							String ObjectIntevalNum = jsonObject.getString("Interval");
							et_sim.setText(GSMVoiceNum);
							et_vname.setText(ObjectRegNum);
							et_Interval.setText(ObjectIntevalNum);
							et_Interval.setFocusable(true);




//						editor.putString("sim_name", GSMVoiceNum);
//						editor.putString("my_name", ObjectRegNum);
//				    	editor.commit();

						} catch (JSONException e) {
							e.printStackTrace();
							et_sim.setText(GetPhoneInfo());
						}
					}

					break;
			}
		}
	};

	/**
	 *  点击事件监听
	 */
	OnClickListener onClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.bt_configure_save:
					Save();
					break;

				case R.id.bt_configure_loginout:
//				ConfigureActivity.this.stopService(new Intent(ConfigureActivity.this, LocationService.class));
//				MyApplication.getInstance().exit();
					ConfigureActivity.this.finish();
					break;
			}
		}
	};

	private void Save(){
		String PhoneNumber = et_sim.getText().toString().trim();
		String VName = et_vname.getText().toString().trim();
		if("".equals(et_host_ip.getText().toString().trim()) ){
			Toast.makeText(ConfigureActivity.this, "IP不能为空" , Toast.LENGTH_SHORT).show();
			return;
		}

		if(PhoneNumber.equals("") || VName.equals("")){
			Toast.makeText(getApplicationContext(), R.string.can_not_null, Toast.LENGTH_SHORT).show();
			return;
		}else{
			progressDialog = ProgressDialog.show(ConfigureActivity.this, "", getString(R.string.in_commit),true);
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("p_strObjectID",ObjectId));
			params.add(new BasicNameValuePair("p_strSIM",PhoneNumber));
			params.add(new BasicNameValuePair("p_strVehicleName",VName));
			params.add(new BasicNameValuePair("p_strDriverName",""));
			params.add(new BasicNameValuePair("p_strDriveMobile",""));
			params.add(new BasicNameValuePair("p_strVehicleModel",""));
			params.add(new BasicNameValuePair("p_strVehicleBrand",""));
			params.add(new BasicNameValuePair("p_flFuelRatio","0"));
			params.add(new BasicNameValuePair("p_strRegistration",""));
			params.add(new BasicNameValuePair("p_strInsurance",""));
			params.add(new BasicNameValuePair("p_strPermit",""));
			params.add(new BasicNameValuePair("p_strLicense",""));
			params.add(new BasicNameValuePair("p_strLastService",""));
			params.add(new BasicNameValuePair("p_intServiceOdo","0"));
			params.add(new BasicNameValuePair("p_intFuelType","0"));
			new Thread(new NetThread.postDataThread(handler, Config.UpdateObjectInfo, params, SAVE)).start();
			if(Config.ODG){
				Log.d(TAG, ObjectId + "/" + PhoneNumber + "/" + VName);
			}
		}

	}

	private String GetPhoneInfo(){
		TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getLine1Number();
	}



	private void Init(){
		Intent intent = getIntent();
		et_sim = (EditText)findViewById(R.id.et_sim);
		et_vname = (EditText)findViewById(R.id.et_vname);
		/*cb_map = (CheckBox)findViewById(R.id.cb_map);*/
		et_host_ip = (EditText)findViewById(R.id.et_host_ip);

		et_host_ip.setText(pref.getString("host_ip", Config.URL));
		et_Interval = (EditText)findViewById(R.id.et_interval);

		EditText et_device_id = (EditText)findViewById(R.id.et_device_id);
		Button bt_configure_save = (Button)findViewById(R.id.bt_configure_save);
		bt_configure_save.setOnClickListener(onClickListener);
		Button bt_configure_loginout = (Button)findViewById(R.id.bt_configure_loginout);
		bt_configure_loginout.setOnClickListener(onClickListener);
		SharedPreferences preferences = getSharedPreferences(Config.Shared_Preferences, Context.MODE_PRIVATE);
		ObjectId = preferences.getString("ObjectId", "");
		boolean isMap = preferences.getBoolean("isMap", false);
		String Adress = preferences.getString("Adress", "");
		et_device_id.setText(Adress);
		progressDialog = ProgressDialog.show(ConfigureActivity.this, "", getString(R.string.get_data),true);

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("ObjectID",ObjectId));
		new Thread(new NetThread.postDataThread(handler, Config.GetObjectInfo, params, GET_DATA)).start();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				finish();
				break;
		}
		return super.onKeyDown(keyCode, event);
	}
}