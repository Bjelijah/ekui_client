package com.example.bean;

public class ProtocolFactory {
	
	private static IProtocol mp;
	public static enum ProtocolType{
		EKUI,
		HW5198,
	}
	
	public static IProtocol getProtocol(ProtocolType type){
		switch (type) {
		case EKUI:
			mp = new EkuiProtocol();
			break;
		case HW5198:
			mp = new HWProtocol();
			break;
		default:
			break;
		}
		return mp;
	}
	
}
