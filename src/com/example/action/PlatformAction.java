package com.example.action;

public class PlatformAction {
	private static PlatformAction mInstance = null;
	public static PlatformAction getInstance(){
		if (mInstance == null) {
			mInstance = new PlatformAction();
		}
		return mInstance;
	}
	private PlatformAction(){}
	
	String connectIP;
	public String getIP() {
		return connectIP;
	}
	public PlatformAction setIP(String iP) {
		connectIP = iP;
		return this;
	}
}
