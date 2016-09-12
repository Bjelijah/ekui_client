package com.example.action;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import android.util.Log;

public class TestSocket {
	private static TestSocket mInstance = null;
	public static TestSocket getInstance(){
		if (mInstance == null) {
			mInstance = new TestSocket();
		}
		return mInstance;
	}
	private TestSocket(){}
	
	private HashSet<DoReceiveFoo> mCallbackSet = null;
	private DatagramSocket mSocket;  
	private InetAddress mDestinationAddress;
	private int mDestinationPort;
	private byte [] mReceiveData = new byte[2*1024*1024];
	
	private SendThread mSendThread = null;
	private ReceiveThread mReceiveThread = null;
	public void register(DoReceiveFoo bar){
		if (mCallbackSet == null) {
			mCallbackSet = new HashSet<TestSocket.DoReceiveFoo>();
		}
		mCallbackSet.add(bar);
	}
	
	public void unRegister(DoReceiveFoo bar){
		if (mCallbackSet!=null) {
			mCallbackSet.remove(bar);
		}
	}
	
	private void doCallback(byte[]data,int len){
		for(DoReceiveFoo bar:mCallbackSet){
			bar.foo(data, len);
		}
	}
	
	public void init(String destinationIP,int localPort,int destinationPort){
		try {
			mSocket = new DatagramSocket(localPort);
			mDestinationAddress = InetAddress.getByName(destinationIP);
			mDestinationPort = destinationPort;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mSendThread = new SendThread();
		mSendThread.start();
		mReceiveThread = new ReceiveThread();
		mReceiveThread.start();
	}
	
	public void init(int localPort,int destinationPort){
		try {
			mSocket = new DatagramSocket(localPort);
			mDestinationPort = destinationPort;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		mSendThread = new SendThread();
		mSendThread.start();
		mReceiveThread = new ReceiveThread();
		mReceiveThread.start();
	}

	public void setDestinationIP(String destinationIP){
		try {
			mDestinationAddress = InetAddress.getByName(destinationIP);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deInit() throws InterruptedException{
		if (mSendThread!=null) {
			mSendThread.endThread();
			mSendThread.join();
			mSendThread = null;
		}
		if (mReceiveThread!=null) {
			mReceiveThread.endThread();
			mReceiveThread.join();
			mReceiveThread = null;
		}
	}
	
	public void sendPacket(byte [] data,int len){
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return packetQueue.poll();
		}
		public SendThread() {
			// TODO Auto-generated constructor stub
			bRunning = true;
		}
		public void endThread(){
			bRunning = false;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(bRunning){
				DatagramPacket packet = popData();
				if (packet!=null) {
					try {
						mSocket.send(packet);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			super.run();
		}
	}
	
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
				try {
					mSocket.receive(pack);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				byte [] data = pack.getData();
				int dataLen = pack.getLength();
				Log.i("123", "data len="+data.length);
				doCallback(data,dataLen);//FIXME
			}
			super.run();
		}
	}
	
	public interface DoReceiveFoo{
		public void foo(byte[] data,int len);
	}
}
