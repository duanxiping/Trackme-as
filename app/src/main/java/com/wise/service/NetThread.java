package com.wise.service;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wise.util.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;


public class NetThread {
	public static class postDataThread extends Thread {
		Handler handler;
		String url;
		int what;
		List<NameValuePair> params;

		public postDataThread(Handler handler, String url, List<NameValuePair> params, int what){
			this.handler = handler;
			this.url = url;
			this.what = what;
			this.params = params;
		}
		@Override
		public void run(){
			super.run();
			System.out.println(url);
			HttpPost httpPost = new HttpPost(url);
			Message message = null;
			HttpClient client = null;
			HttpResponse httpResponse = null;
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

				Logger.D("LocationService", "--------------------====-----" + httpPost.getRequestLine());
				client = new DefaultHttpClient();
				client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 9000);
				client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 9000);
				httpResponse = client.execute(httpPost);
				if(httpResponse.getStatusLine().getStatusCode() == 200){
					String strResult = EntityUtils.toString(httpResponse.getEntity());
					message = new Message();
					message.what = what;
					message.obj = strResult;
					handler.sendMessage(message);
					System.out.println(strResult);
				}else{
					message = new Message();
					message.what = what;
					message.obj = "Exception";
					handler.sendMessage(message);
					System.out.println("返回状态=" + httpResponse.getStatusLine().getStatusCode());
				}
			}catch (SocketTimeoutException e) {
				e.printStackTrace();
				message = new Message();
				message.what = what;
				message.obj = "SocketTimeoutException";
				handler.sendMessage(message);
			}catch (SocketException e) {
				e.printStackTrace();
				message = new Message();
				message.what = what;
				message.obj = "SocketTimeoutException";
				handler.sendMessage(message);
			}catch (Exception e) {
				message = new Message();
				message.what = what;
				message.obj = "Exception";
				handler.sendMessage(message);
				e.printStackTrace();
			}
			finally{//释放资源
				httpPost = null;
				params = null;
				message = null;
				client = null;
				httpResponse = null;
				System.gc();
			}
		}
	}



	public static class getDataStringThread extends Thread {
		Handler handler;
		String url;
		int what;

		public getDataStringThread(Handler handler, String url, int what){
			this.handler = handler;
			this.url = url;
			this.what = what;
		}
		@Override
		public void run(){
			super.run();
			System.out.println(url);
			Message message = null;
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url.toString());
			try {
				Logger.D("LocationService", "--------------------====-----" + url);

				httpGet.addHeader("Accept-Language","en-us");
				HttpResponse httpResponse = httpClient.execute(httpGet);
				Logger.D("LocationService", "getStatusCode()。。。。= " + httpResponse.getStatusLine().getStatusCode());

				if(httpResponse.getStatusLine().getStatusCode() == 200){
					String strResult = EntityUtils.toString(httpResponse.getEntity());
					message = new Message();
					message.what = what;
					message.obj = strResult;
					handler.sendMessage(message);
					System.out.println(strResult);
				}else{
					message = new Message();
					message.what = what;
					message.obj = "Exception";
					handler.sendMessage(message);
					System.out.println("返回状态=" + httpResponse.getStatusLine().getStatusCode());
				}
			}catch (SocketTimeoutException e) {
				e.printStackTrace();
				message = new Message();
				message.what = what;
				message.obj = "SocketTimeoutException";
				handler.sendMessage(message);
			}catch (SocketException e) {
				e.printStackTrace();
				message = new Message();
				message.what = what;
				message.obj = "SocketTimeoutException";
				handler.sendMessage(message);
			}catch (Exception e) {
				message = new Message();
				message.what = what;
				message.obj = "Exception";
				handler.sendMessage(message);
				e.printStackTrace();
			}
			finally{//释放资源
				httpClient = null;
				message = null;
				httpGet = null;
//				httpResponse = null;
				System.gc();
			}
		}
	}




	public static class GetLocation implements Runnable {
		String tLat;
		String tLon;
		Handler tHandler;
		int tWhere;

		Context mContext;
		/**
		 * 获取地址
		 * @param Lat
		 * @param Lon
		 * @param handler
		 */
		public GetLocation(String Lat, String Lon, Handler handler, int where, Context context) {
			tLat = Lat;
			tLon = Lon;
			tHandler = handler;
			tWhere = where;
			mContext = context;
		}

		public void run() {
			String Address = geocodeAddress(mContext,tLat, tLon);
			Message msg = new Message();
			if(Address == null || Address.equals("")){
				msg.what = tWhere;
				msg.obj = "";
				tHandler.sendMessage(msg);
			}else{
				msg.what = tWhere;
				msg.obj = Address;
				tHandler.sendMessage(msg);
			}
		}
	}


	/**
	 * @param context
	 * @param latitude
	 * @param longitude
	 * @return
	 */
	public static String geocodeAddress(Context context, String latitude, String longitude){
		try {
			StringBuilder url = new StringBuilder();
			url.append("http://maps.googleapis.com/maps/api/geocode/json?latlng=");
			url.append(latitude).append(",");
			url.append(longitude);
			url.append("&sensor=true");
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url.toString());
			httpGet.addHeader("Accept-Language","en-us");
			HttpResponse httpResponse = httpClient.execute(httpGet);
			Log.d("LocationService", "getStatusCode()。。。。= " + httpResponse.getStatusLine().getStatusCode());
			if(httpResponse.getStatusLine().getStatusCode() == 200){
				HttpEntity entity = httpResponse.getEntity();
				String response = EntityUtils.toString(entity,"utf-8");
				JSONObject jsonObject;
				try {
					jsonObject = new JSONObject(response);
					JSONArray resultArray = jsonObject.getJSONArray("results");
					if(resultArray.length()>0){
						JSONObject subObject = resultArray.getJSONObject(0);
						String address = subObject.getString("formatted_address");
//						Log.d("LocationService", "获取地理反编译位置。。。。" + address);
						return address;
					}
				} catch (JSONException e) {
					e.printStackTrace();
					return null;
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return "";
	}
}
