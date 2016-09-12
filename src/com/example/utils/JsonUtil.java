package com.example.utils;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.bean.EkuiProtocol;

import android.os.Bundle;
import android.util.Log;

public class JsonUtil {
	
	public static String buildLoginJsonStr(Bundle b){
		String ip = b.getString(EkuiProtocol.BUNDLE_LOGIN_IP,null);
		JSONObject object = new JSONObject();
		try {
			object.put("IP", ip);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Log.i("123", "login jsonStr="+object.toString());
		return object.toString();
	}
	
	public static Bundle phaseLoginJson(String jsonStr){
		String ip = null;
		try {
			JSONObject obj = new JSONObject(jsonStr);
			ip = obj.getString("IP");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Bundle b = new Bundle();
		b.putString(EkuiProtocol.BUNDLE_LOGIN_IP, ip);
		return b;
	}
	
	public static String buildLogoutJsonStr(Bundle b){
		int h = b.getInt(EkuiProtocol.BUNDLE_LOGOUT_HANDLE,0);
		JSONObject obj = new JSONObject();
		try {
			obj.put("Handle", h);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj.toString();
	}
	
	public static Bundle phaseLogoutJsonStr(String jsonStr){
		int handle = -1;
		try {
			JSONObject obj = new JSONObject(jsonStr);
			handle = obj.getInt("Handle");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Bundle b = new Bundle();
		b.putInt(EkuiProtocol.BUNDLE_LOGOUT_HANDLE, handle);
		return b;
	}
	
	
	public static String loginJsonStr(String ip){
		JSONObject object = new JSONObject();
		
		JSONObject child = new JSONObject();
		try {
			child.put("IP", ip);
			object.put("LOGIN", child);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return object.toString();
	}
	
}
