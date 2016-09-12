package com.example.bean;

import java.util.HashSet;

import com.example.utils.JniUtil;

import android.R.bool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class HWProtocol implements IProtocol{

	Handler handler;
	String ip = null;
	IPushOb pushOb;
	@Override
	public void setHandler(Handler h) {
		handler = h;
	}

	@Override
	public void setCallback(IPushOb ob) {
		pushOb = ob;
	}

	@Override
	public void setInfo(Bundle b) {
		String 	serverIp = b.getString("DestinationIP",null);
		if (serverIp!=null) {
			ip = serverIp;
		}
	}

	@Override
	public void connect() {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				JniUtil.hwNetInit();
				return true;
			}
			protected void onPostExecute(Boolean result) {
				if (result) {
					handler.sendEmptyMessage(IProtocol.MSG_LOGIN_OK);
				}
			};
			
		}.execute();
	}

	@Override
	public void disconnect() {
		new AsyncTask<Void, Void, Void>(){
			@Override
			protected Void doInBackground(Void... params) {
				JniUtil.hwNetDeinit();
				return null;
			}
		}.execute();
	}

	@Override
	public void login(String myIP) {
		new AsyncTask<Void, Void, Void>(){
			@Override
			protected Void doInBackground(Void... params) {
				JniUtil.hwLogin(ip);
				JniUtil.hwReqStream();
				return null;
			}
		}.execute();
	}

	@Override
	public void logout() {
		new AsyncTask<Void, Void, Void>(){
			@Override
			protected Void doInBackground(Void... params) {
				boolean ret = JniUtil.hwStopStream();
				Log.i("123", "stop stream="+ret);
				ret =JniUtil.hwLogout();
				Log.i("123", "logout ret="+ret);
				return null;
			}
		}.execute();
	}

	
	/**
	 * server push data
	 * @param data
	 * @param len
	 */
	@Override
	public void pushStream(byte[] data, int len) {
		// TODO Auto-generated method stub
		
	}

}
