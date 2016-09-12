package com.example.action;

import java.util.HashSet;

import com.example.action.TestSocket.DoReceiveFoo;
import com.example.bean.IProtocol;
import com.example.bean.ProtocolFactory;
import com.example.bean.ProtocolFactory.ProtocolType;
import com.example.utils.PhoneConfigUtil;
import com.example.view.YUVGLSurfaceView;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ProtocolClientAction implements IProtocol.IPushOb{
	private static ProtocolClientAction mInstance = null;
	public static ProtocolClientAction getInstance(){
		if (mInstance==null) {
			mInstance = new ProtocolClientAction();
		}
		return mInstance;
	}
	private ProtocolClientAction(){}
	
	IProtocol mp;
	Context mContext;
	
	private HashSet<DoReceiveFoo> mFoos = new HashSet<DoReceiveFoo>();
	
	
	public void register(DoReceiveFoo foo){
		mFoos.add(foo);
	}
	
	public void unRegister(DoReceiveFoo foo){
		mFoos.remove(foo);
	}
	
	private void doFoos(byte [] data,int len){
		for(DoReceiveFoo foo:mFoos){
			foo.foo(data, len);
		}
	}
	
	Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case IProtocol.MSG_LOGIN_OK:
				logMeIn();
				break;

			default:
				break;
			}
			super.handleMessage(msg);
		}
		
	};
	
	Handler faHandler = null;
	public void setfaHandler(Handler handler){
		this.faHandler = handler;
	}
	
	
	public void init(Context context,String ip){
		mContext = context;
		mp = ProtocolFactory.getProtocol(ProtocolType.HW5198);
		mp.setCallback(this);
		mp.setHandler(handler);
		Bundle b = new Bundle();
		b.putString("DestinationIP", ip);
		mp.setInfo(b);
		mp.connect();
	}
	
	public void logMeIn(){
		
		String ip = PhoneConfigUtil.getPhoneIp(mContext);
		Log.i("123", "logmein  ip="+ip);
		mp.login(ip);
	}
	
	
	public void logMeOut(){
		if (mp!=null) {
			mp.logout();
		}
	}
	
	public void reLink(){
		logMeIn();
		faHandler.sendEmptyMessage(YUVGLSurfaceView.MSG_TIMER_START);
	}
	
	
	@Override
	public void onPushDataComing(byte[] data, int len) {
		doFoos(data,len);
	}
	
}
