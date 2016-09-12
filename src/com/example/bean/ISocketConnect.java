package com.example.bean;

import android.os.Bundle;

public interface ISocketConnect {
	static final String BUNDLE_DESTINATION_IP = "DestinationIP";
	static final String BUNDLE_DESTINATION_PORT = "DestinationPort";
	static final String BUNDLE_LOCAL_PORT = "LocalPort";
	void init(String destinationIP,int destinationPort,int localPort);
	void setInfoBundle(Bundle b);
	void init();
	void deInit();
	void attch(SocketClientCallbackObserver ob);
	void detach(SocketClientCallbackObserver ob);
	
	void attch(SocketServerCallbackObserver ob);
	void detach(SocketServerCallbackObserver ob);
	void send(byte [] data,int len);
	interface SocketClientCallbackObserver{
		void onConnect();
		void onConnectReceive(byte [] data,int len);
		void onDisconnect();
	}
	interface SocketServerCallbackObserver{
		void onConnect(int handle);
		void onConnectReceive(int handle,byte [] data,int len);
		void onDisconnect(int handle);
	}
	
	
}
