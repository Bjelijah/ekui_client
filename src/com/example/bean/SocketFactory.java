package com.example.bean;

public class SocketFactory {
	public static enum SocketType{
		TCP_CONNECT,
		UDP_CONNECT;
	}
	public static ISocketConnect getSocket(SocketType type){
		switch(type){
		case TCP_CONNECT:
			return new TCPSocketClient();
		case UDP_CONNECT:
			return new UDPSocket();
			default:
			break;
		}
		return null;
	}
}
