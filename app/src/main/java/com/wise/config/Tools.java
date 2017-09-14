package com.wise.config;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Tools {
	
	public static String getCurrentTime(){
		SimpleDateFormat formatter    =   new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date curDate    =   new Date(System.currentTimeMillis());//获取当前时间
		String str    =    formatter.format(curDate);
		return str;
	}

}
