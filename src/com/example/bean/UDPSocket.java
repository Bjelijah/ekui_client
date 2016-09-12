package com.example.bean;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import android.os.Bundle;
import android.util.Log;



public class UDPSocket implements ISocketConnect {

	private HashSet<SocketClientCallbackObserver> mCallbackSet = new HashSet<ISocketConnect.SocketClientCallbackObserver>();
	private HashSet<SocketServerCallbackObserver> mServerCallback = new HashSet<ISocketConnect.SocketServerCallbackObserver>();
	private DatagramSocket mSocket = null;  
	private InetAddress mDestinationAddress = null;
	private int mDestinationPort = -1;
	private int mLoaclPort = -1;
	private SendThread mSendThread = null;
	private ReceiveThread mReceiveThread = null;
	private byte [] mReceiveData = new byte[3*1024*1024];

	
	@Override
	public void setInfoBundle(Bundle b) {
		if (mDestinationAddress == null) {
			String dIP = b.getString(BUNDLE_DESTINATION_IP,null);
			try {
				mDestinationAddress = InetAddress.getByName(dIP);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		if (mDestinationPort == -1) {
			mDestinationPort = b.getInt(BUNDLE_DESTINATION_PORT,-1);
		}
		if (mLoaclPort == -1) {
			mLoaclPort = b.getInt(BUNDLE_LOCAL_PORT,-1);
		}
	}

	@Override
	public void init() {//for server   mDestinationIP and port will be told by client's login method
		if (mSocket!=null) {
			try {
				endSocket();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			mSocket = new DatagramSocket(mLoaclPort);
			startSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	
	}
	
	
	@Override
	public void init(String destinationIP, int destinationPort, int localPort) {//for client
		if (mSocket!=null) {
			try {
				endSocket();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		try {
			mSocket = new DatagramSocket(localPort);
			mDestinationAddress = InetAddress.getByName(destinationIP);
			mDestinationPort = destinationPort;
		
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		startSocket();
	}

	private void startSocket(){
		if (mSendThread!=null || mReceiveThread!=null) {
			try {
				endSocket();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mSendThread = new SendThread();
		mSendThread.start();
		mReceiveThread = new ReceiveThread();
		mReceiveThread.start();
		doConnect();
	}
	
	private void endSocket() throws InterruptedException{
		if (mSendThread!=null) {
			mSendThread.endThread();
		
			mSendThread = null;
		}
		if (mReceiveThread!=null) {
			mReceiveThread.endThread();
		
			mReceiveThread = null;
		}
		if (mSocket!=null) {
			mSocket.disconnect();
			mSocket.close();
			Log.i("123", "socket is bind="+mSocket.isBound());
			mSocket = null;
		}
		
	}
	
	
	@Override
	public void deInit() {
		try {
			endSocket();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void attch(SocketClientCallbackObserver ob) {
		if (mCallbackSet == null) {
			mCallbackSet = new HashSet<ISocketConnect.SocketClientCallbackObserver>();
		}
		Log.i("123","udp attch client callback");
		mCallbackSet.add(ob);
	}

	@Override
	public void detach(SocketClientCallbackObserver ob) {
		mCallbackSet.remove(ob);
	}

	private void doReceiveData(byte [] data,int len){
		for(SocketClientCallbackObserver ob:mCallbackSet){
			if (null!=ob) {
				ob.onConnectReceive(data,len);
			}
		}
		for(SocketServerCallbackObserver ob:mServerCallback){
			if (null!=ob) {
				ob.onConnectReceive(0, data, len);
			}
		}
	}
	
	private void doConnect(){
		Log.i("123", "udp client do connect");
		for(SocketClientCallbackObserver ob:mCallbackSet){
			if (null!=ob) {
				Log.i("123", "ob onconnect");
				ob.onConnect();
			}
		}
		for(SocketServerCallbackObserver ob:mServerCallback){
			if (null!=ob) {
				ob.onConnect(0);
			}
		}
	}
	
	private void doDisconnect(){
		for(SocketClientCallbackObserver ob:mCallbackSet){
			if (null!=ob) {
				ob.onDisconnect();
			}
		}
		for(SocketServerCallbackObserver ob:mServerCallback){
			if (null!=ob) {
				ob.onDisconnect(0);
			}
		}
	}
	
	
	
	@Override
	public void send(byte[] data, int len) {
//		for (int i = 0; i < 16; i++) {
//			Log.i("123", "send "+String.format("0x%x", data[i]));
//		}
		DatagramPacket pack = new DatagramPacket(data, len,mDestinationAddress,mDestinationPort);
		if (mSendThread!=null) {
			mSendThread.pushData(pack);
		}
	}


	private class SendThread extends Thread{
		private Queue<DatagramPacket> packetQueue =  new ArrayBlockingQueue<DatagramPacket>(5); 
		private boolean bRunning = false;
		public void pushData(DatagramPacket dp){
			packetQueue.offer(dp);
			synchronized (mSendThread) {
				notify();
			}

		}
		
		public DatagramPacket popData(){
			try {
				synchronized (mSendThread) {
					wait();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return packetQueue.poll();
		}
		public SendThread() {
			bRunning = true;
		}
		public void endThread(){
			bRunning = false;
		}
		@Override
		public void run() {
			while(bRunning){
				DatagramPacket packet = popData();
				if (packet!=null) {
					try {
						mSocket.send(packet);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			super.run();
		}
	}
	
	private int recNum=0;
	
	private class ReceiveThread extends Thread{
		private boolean bRunning = false;
		public ReceiveThread() {
			bRunning = true;
		}
		public void endThread(){
			bRunning = false;
		}
		@Override
		public void run() {
			while(bRunning){
				DatagramPacket pack = new DatagramPacket(mReceiveData, mReceiveData.length);
				Log.i("123", "receive socket");
				try {
					mSocket.receive(pack);
				} catch (IOException e) {
					e.printStackTrace();
				}
				byte [] data = pack.getData();
				int dataLen = pack.getLength();
				byte [] buf = new byte[dataLen];
				System.arraycopy(data, 0, buf, 0, dataLen);
				
				recNum++;
				Log.d("123", "recNum="+recNum+" rece len="+dataLen);
				
				doReceiveData(buf,dataLen);//FIXME
			}
			super.run();
		}
	}

	@Override
	public void attch(SocketServerCallbackObserver ob) {
		// TODO Auto-generated method stub
		if (mServerCallback==null) {
			mServerCallback = new HashSet<ISocketConnect.SocketServerCallbackObserver>();
		}
		mServerCallback.add(ob);
	}

	@Override
	public void detach(SocketServerCallbackObserver ob) {
		// TODO Auto-generated method stub
		if (null!=mServerCallback) {
			mServerCallback.remove(ob);
		}
	}
	
}
