package com.example.bean;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import android.os.Bundle;
import android.util.Log;

public class TCPSocketClient implements ISocketConnect{

	private String mDestinationIP=null;
	private int mDestinationPort=-1;

	private DataOutputStream out;// 发�?�数据流
	private DataInputStream in;// 接收数据�?
	private Socket mSocket;// socket连接对象
	private SocketAddress address;
	private int timeOut = 1000 * 30;// 延迟时间
	// 启动�?个线程，不停接收服务器数�?
	private RecThrad mRecThrad;// 接收数据线程
	private SendThread mSendThread;// 发�?�线�?
	private ConnectThread connThread;//
	private boolean threadBoo = true;

	private HashSet<SocketClientCallbackObserver> mCallback = null;
	private byte [] mReceiveData = new byte [3*1024*1024];


	@Override
	public void init(String destinationIP, int destinationPort, int localPort) {
		// TODO Auto-generated method stub
		mDestinationIP = destinationIP;
		mDestinationPort =destinationPort;
		connThread = new ConnectThread(mDestinationIP, mDestinationPort);
		connThread.start();
	}

	@Override
	public void setInfoBundle(Bundle b) {
		// TODO Auto-generated method stub
		if (mDestinationIP==null) {
			mDestinationIP = b.getString("DestinationIP",null);
		}
		if (mDestinationPort == -1) {
			mDestinationPort = b.getInt("DestinationPort",-1);
		}
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		connThread = new ConnectThread(mDestinationIP, mDestinationPort);
		connThread.start();
	}

	@Override
	public void deInit() {
		// TODO Auto-generated method stub
		close();
	}

	@Override
	public void attch(SocketClientCallbackObserver ob) {
		// TODO Auto-generated method stub
		if (mCallback==null) {
			mCallback = new HashSet<ISocketConnect.SocketClientCallbackObserver>();
		}
		mCallback.add(ob);
	}

	@Override
	public void detach(SocketClientCallbackObserver ob) {
		// TODO Auto-generated method stub
		if (mCallback!=null) {
			mCallback.remove(ob);
		}
	}

	private void doReceiveData(byte [] data,int len){
		for(SocketClientCallbackObserver ob:mCallback){
			ob.onConnectReceive(data, len);
		}
	}

	private void doConnected(){
		for(SocketClientCallbackObserver ob:mCallback){
			ob.onConnect();
		}
	}

	private void doDisconnect(){
		for(SocketClientCallbackObserver observer : mCallback){
			observer.onDisconnect();
		}
		close();//FIXME
	}

	@Override
	public void send(byte[] data, int len) {
		// TODO Auto-generated method stub

		if (mSendThread!=null) {
			mSendThread.pushData(data, len);
		}
	}


	private void sendData(byte[] data) {
		for(int i=0;i<16;i++){
			Log.i("123", "data  : "+String.format("0x%x", data[i]));
		}
		
		if (out != null) {
			try {
				out.write(data);
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}




	class ConnectThread extends Thread {
		String ip;
		int port;
		public ConnectThread(String ip, int port) {
			this.ip = ip;
			this.port = port;
		}

		@Override
		public void run() {
			super.run();
			mSocket = new Socket();
			try {
				mSocket.setTcpNoDelay(true);
//				mSocket.setSendBufferSize(64*1024);
//				mSocket.setReceiveBufferSize(64*1024);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			address = new InetSocketAddress(ip, port);
			try {
				mSocket.connect(address, timeOut);
				mSocket.isConnected();
				//				callBack.connected(TcpLongSocket.this);
				doConnected();
				
				out = new DataOutputStream(mSocket.getOutputStream());// 获取网络输出�?
				in = new DataInputStream(mSocket.getInputStream());// 获取网络输入�?
				threadBoo = true;
				mRecThrad = new RecThrad();
				mRecThrad.start();
				mSendThread = new SendThread();
				mSendThread.start();
			} catch (IOException e1) {
				Log.e("123", "fanliang......连接失败");
				e1.printStackTrace();
				try {
					if (out != null) {
						out.close();
					}
					if (in != null) {
						in.close();
					}
					if (mSocket != null && !mSocket.isClosed()) {// 判断socket不为空并且是连接状�??
						mSocket.close();// 关闭socket
					}
				} catch (Exception e2) {
					// TODO: handle exception
				}
				doDisconnect();
			}
		}
	}

	class SendPack{
		byte [] data;
		int len;
		public SendPack(byte[] data,int len) {
			this.data =data;
			this.len = len;
		}
		public byte[] getData() {
			return data;
		}
		public void setData(byte[] data) {
			this.data = data;
		}
		public int getLen() {
			return len;
		}
		public void setLen(int len) {
			this.len = len;
		}
	}

	/**
	 * 发�?�线�?
	 */
	class SendThread extends Thread {
		private Queue<SendPack> packetQueue =  new ArrayBlockingQueue<SendPack>(15); 
		public synchronized void pushData(byte [] data,int len){
		
			
			
			packetQueue.offer(new SendPack(data, len));
			synchronized (mSendThread) {
				notify();
			}
		}
		public SendPack popData(){
			
			return packetQueue.poll();
		}
		@Override
		public void run() {
			super.run();
			while (threadBoo) {
				SendPack packet = popData();
				if (packet!=null) {
					sendData(packet.getData());
				}else{
					try {
						synchronized (mSendThread) {
							wait();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			this.close();
		}

		public void close() {
			threadBoo = false;
		}
	}

	/**
	 * 接收数据线程 关闭资源 打开资源
	 */
	
	private int testnum = 0;
	
	class RecThrad extends Thread {
		private byte [] tmpBuffer;
		public void run() {
			super.run();
			if (threadBoo) {
				if (in != null) {
					int len = 0;
					try {
						while ((len = in.read(mReceiveData)) > 0) {
							tmpBuffer = new byte[len];
							System.arraycopy(mReceiveData, 0, tmpBuffer, 0, len);
//							Log.e("", "fanliang......接收数据 ="
//									+ new String(tmpBuffer));
//							doReceiveData(tmpBuffer,len);
							tmpBuffer = null;
//							
							testnum++;
							Log.d("123", "recnum="+testnum+" rec len="+len);
//							if (testnum>10) {
//								Log.e("123", "test over");
//								break;
//							}
							
						}
					} catch (IOException e) {
						e.printStackTrace();
						try {
							if (out != null) {
								out.close();
							}
							if (in != null) {
								in.close();
							}
							if (mSocket != null && !mSocket.isClosed()) {// 判断socket不为空并且是连接状�??
								mSocket.close();// 关闭socket
							}
						} catch (Exception e2) {
							// TODO: handle exception
						}
						doDisconnect();
					}
				}
			}
			this.close();
		}

		public void close() {
			threadBoo = false;

		}
	}



	private void close() {
		threadBoo = false;
		try {
			if (out != null) {
				out.close();
			}
			if (in != null) {
				in.close();
			}
			if (mSocket != null && !mSocket.isClosed()) {// 判断socket不为空并且是连接状�??
				mSocket.close();// 关闭socket
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			out = null;
			in = null;
			mSocket = null;// 制空socket对象
			mRecThrad = null;
			mSendThread = null;
			connThread = null;
		}
	}



	@Override
	public void attch(SocketServerCallbackObserver ob) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detach(SocketServerCallbackObserver ob) {
		// TODO Auto-generated method stub
		
	}
}
