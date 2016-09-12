package com.example.bean;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import android.os.Bundle;
import android.util.Log;

public class TCPSocketServer implements ISocketConnect,IConst {

	private HashSet<SocketServerCallbackObserver> mCallback = null;


	private ServerSocket mServersocket = null;
	private int mLocalPort = -1;
	private ConncetThread connThread;//

	private int handleNum = 0;
	
	private int getNewHandleNum(){
		return handleNum++;
	}
	
	private List<SocketHandle> mSocketHandle = new ArrayList<TCPSocketServer.SocketHandle>();
	
	/**
	 * @deprecated {@link} init()}
	 */
	@Override
	public void init(String destinationIP, int destinationPort, int localPort) {

	}

	@Override
	public void setInfoBundle(Bundle b) {
		if (mLocalPort==-1) {
			mLocalPort = b.getInt(BUNDLE_LOCAL_PORT,-1);
		}
	}

	@Override
	public void init() {

		//start connect thread
		connThread = new ConncetThread(mLocalPort);
	}

	@Override
	public void deInit() {
		close();
	}

	@Override
	public void attch(SocketServerCallbackObserver ob) {
		if (mCallback==null) {
			mCallback = new HashSet<ISocketConnect.SocketServerCallbackObserver>();
		}
		mCallback.add(ob);
	}

	@Override
	public void detach(SocketServerCallbackObserver ob) {
		if (mCallback!=null) {
			mCallback.remove(ob);
		}
	}

	private void doReceiveData(int handle,byte [] data,int len){
		for(SocketServerCallbackObserver ob:mCallback){
			ob.onConnectReceive(handle,data, len);
		}
	}

	private void doConnected(int handle){
		for(SocketServerCallbackObserver ob:mCallback){
			ob.onConnect(handle);
		}
	}

	private void doDisconnect(int handle){
		for(SocketServerCallbackObserver observer : mCallback){
			observer.onDisconnect(handle);
		}
	}

	@Override
	public void send(byte[] data, int len) {
		
		
		
		SocketHandle socketHandle = mSocketHandle.get(0);
		socketHandle.sendData(data, len);
	}


	class ConncetThread extends Thread{

		int port;
		private boolean running = false;
		public ConncetThread(int port) {

			this.port = port;
			running = true;
		}

		public void endThread(){
			running = false;
		}
		@Override
		public void run() {
			try {
				mServersocket = new ServerSocket(mLocalPort);
				SocketHandle sHandle = null;
				try {
					while(running){
						Socket socket = mServersocket.accept();
						if (mSocketHandle.size()!=0) {//FIXME just support one client in the same time
							for(SocketHandle h:mSocketHandle){
								h.closeHandle();
							}
							mSocketHandle.clear();
						}
						
						sHandle = new SocketHandle(socket);
						sHandle.createHandle();
						mSocketHandle.add(sHandle);
						int handle = getNewHandleNum();
						sHandle.setHandle(handle);
						if (socket.isConnected()) {
							doConnected(handle);
						}
					}
				} catch (Exception e) {
					if (sHandle!=null) {
						sHandle.closeHandle();
					}
					
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			super.run();
		}
	}




	class SocketHandle{
		Socket socket;
		DataOutputStream out;
		DataInputStream in;
		SendThread sendThread;
		RecThrad recThrad;
		int handle = 0;
	
		public int getHandle() {
			return handle;
		}
		public void setHandle(int handle) {
			this.handle = handle;
			recThrad.setHandle(handle);
		}
		public SocketHandle(Socket socket) {
			this.socket = socket;
		}
		public void createHandle() throws IOException{
			out = new DataOutputStream(socket.getOutputStream());// 获取网络输出�?
			in = new DataInputStream(socket.getInputStream());// 获取网
			recThrad = new RecThrad(in);
			recThrad.start();
			sendThread = new SendThread(out);
			sendThread.start();
		}
		
		public void sendData(byte [] data,int len){
			sendThread.pushData(data, len);
		}
		
		public void closeHandle(){
			try {
				if (socket!=null && !socket.isClosed()) {
					socket.close();
					socket = null;
				}
				if (out!=null) {
					out.close();
					out = null;
				}
				if (in!=null) {
					in.close();
					in = null;
				}
				if (recThrad!=null) {
					recThrad.endThread();
					recThrad.join();
					recThrad = null;
				}
				if (sendThread!=null) {
					sendThread.endThread();
					sendThread.join();
					sendThread = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
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

	private void sendData(DataOutputStream out,byte[] data) {
		if (out != null) {
			try {
				out.write(data);
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 发�?�线�?
	 */
	class SendThread extends Thread {
		private Queue<SendPack> packetQueue =  new ArrayBlockingQueue<SendPack>(5); 
		private boolean running = false;
		private DataOutputStream out;
		public SendThread(DataOutputStream out) {
			running = true;
			this.out = out;
		}
		public void endThread(){
			running =false;
		}
		public void pushData(byte [] data,int len){
			packetQueue.offer(new SendPack(data, len));
			synchronized (this) {
				notify();
			}
		}
		public SendPack popData(){
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return packetQueue.poll();
		}
		@Override
		public void run() {
			super.run();
			while (running) {
				SendPack packet = popData();
				if (packet!=null) {
					sendData(out,packet.getData());
				}
			}
		}
	}

	/**
	 * 接收数据线程 关闭资源 打开资源
	 */
	class RecThrad extends Thread {
		private byte [] tmpBuffer;
		private boolean running = false;
		private  DataInputStream in;
		private int handle;
		private byte [] mReceiveData = new byte [3*1024*1024];
		public RecThrad(DataInputStream in) {
			running = true;
			this.in = in;
		}
		public void setHandle(int handle){
			this.handle = handle;
		}
		public void endThread(){
			running = false;
		}
		public void run() {
			super.run();
			if (running) {
				if (in != null) {
					int len = 0;
					try {
						while ((len = in.read(mReceiveData)) > 0) {
							tmpBuffer = new byte[len];
							System.arraycopy(mReceiveData, 0, tmpBuffer, 0, len);
							Log.e("", "fanliang......接收数据 ="
									+ new String(tmpBuffer));
							doReceiveData(handle,tmpBuffer,len);
							tmpBuffer = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
						doDisconnect(handle);
					}
				}
			}
		}
	}

	private void close() {
		if (connThread!=null) {
			try {
				mServersocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e){
				e.printStackTrace();
			}
			connThread.endThread();
			connThread = null;
		}
		for(SocketHandle h:mSocketHandle){
			h.closeHandle();
		}
	}

	@Override
	public void attch(SocketClientCallbackObserver ob) {	}

	@Override
	public void detach(SocketClientCallbackObserver ob) {	}


}
