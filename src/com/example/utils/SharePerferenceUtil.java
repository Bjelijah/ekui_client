package com.example.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SharePerferenceUtil {
	private static final String SP_NAME = "connect_set";
	
	
	public static void saveConnectIP(Context context,String connectIP){
		SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putString("connect_ip", connectIP);
        editor.commit();
	}
	
	public static String loadConnectIP(Context mContext){
		SharedPreferences sharedPreferences = mContext.getSharedPreferences(SP_NAME,Context.MODE_PRIVATE);
		return sharedPreferences.getString("connect_ip", null);
	}
}
