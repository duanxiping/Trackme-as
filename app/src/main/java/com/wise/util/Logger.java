package com.wise.util;

import android.util.Log;

/**
 * 功能： 日志过滤工具
 * 作者： 小段
 * 日期： 2017/9/5 09:04
 * 邮箱： descriable
 */
public class Logger {

    public static final int VERSION = 1;
    public static final int DEBUGE  = 2;
    public static final int INFO    = 3;
    public static final int WARN    = 4;
    public static final int ERROR   = 5;
    public static final int NOTHING = 6;
    public static final int LEVEL   = VERSION;

    public static void V(String tag, String msg){
        if (LEVEL <= VERSION) {
            Log.v(tag, msg);
        }
    }

    public static void E(String tag, String msg){
        if (LEVEL <= ERROR) {
            Log.e(tag, msg);
        }
    }

    public static void D(String tag, String msg){
        if (LEVEL <= DEBUGE) {
            Log.d(tag, msg);
        }
    }

    public static void W(String tag, String msg){
        if (LEVEL <= WARN) {
            Log.w(tag, msg);
        }
    }

    public static void I(String tag, String msg){
        if (LEVEL <= INFO) {
            Log.i(tag, msg);
        }
    }

}
