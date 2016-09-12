package com.example.bean;

import android.os.Bundle;
import android.os.Handler;

public interface IProtocol {
	
	static final int MSG_LOGIN_ACK = 0xa0;
	static final int MSG_LOGOUT_ACK = 0xa1;
	static final int MSG_LOGIN_OK   = 0xa2;	
	
	void setHandler(Handler h);
	void setCallback(IPushOb ob);
	void setInfo(Bundle b);
	void connect();
	void disconnect();
	void login(String myIP);
	void logout();
	void pushStream(byte [] data,int len);
	interface IPushOb{
		void onPushDataComing(byte [] data,int len);
	}
}
