package com.example.bean;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import com.example.bean.EkuiHead.EkuiCmd;
import com.example.bean.ISocketConnect.SocketClientCallbackObserver;
import com.example.bean.ISocketConnect.SocketServerCallbackObserver;
import com.example.bean.SocketFactory.SocketType;
import com.example.utils.JsonUtil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import struct.JavaStruct;
import struct.StructException;

public class EkuiProtocol implements IProtocol,IConst {
	public static final String BUNDLE_LOGIN_IP = "MyIP" ;
	public static final String BUNDLE_LOGOUT_HANDLE = "MyHandle";
	private ISocketConnect conn;
	private SocketServerOB ssOB;
	private SocketClientOB scOB;
	private static final SocketType PROTOCOL_TYPE = SocketType.TCP_CONNECT; 
	private int sHandle;//client handle get from server
	private ByteBuffer readbuf = ByteBuffer.allocate(3*1024*1024);
	private int headLen = -1;
	private Handler handler;
	private String ipAck;
	IPushOb pushOb;
	String serverIP;
	private PacketProcessThread processThread = null;
	public EkuiProtocol() {
		// TODO Auto-generated constructor stub
		conn = SocketFactory.getSocket(PROTOCOL_TYPE); 
	}

	/**
	 * connect method for server
	 */
	private void serverConnect(){
		ssOB = new SocketServerOB();
		conn.attch(ssOB);
		Bundle bundle = new Bundle();
		bundle.putInt(ISocketConnect.BUNDLE_LOCAL_PORT, SERVICE_PORT);
		conn.setInfoBundle(bundle);
		conn.init();
		
	}

	/**
	 * connect method for client
	 */
	private void clientConnect(){
		scOB = new SocketClientOB();
		conn.attch(scOB);
		conn.init(serverIP, SERVICE_PORT, CLIENT_PORT);
		
	}

	@Override
	public void connect() {
		readbuf.clear();
		clientConnect();
	}

	@Override
	public void disconnect(){
		conn.detach(scOB);
		conn.detach(ssOB);
		conn.deInit();
	}

	@Override
	public void login(String myIP) {//for client
		// TODO Auto-generated method stub
		readbuf.clear();
		Bundle b = new Bundle();
		b.putString(BUNDLE_LOGIN_IP, myIP);
		byte [] body = JsonUtil.buildLoginJsonStr(b).getBytes();
		byte [] head = null;
		EkuiHead headbuf = new EkuiHead();
		try {
			head = buildHead(headbuf,EkuiCmd.LOGIN, body.length);
			//			for(int i=0;i<16;i++){
			//				Log.i("123", "head["+i+"] :"+String.format("0x%x", head[i]));
			//			}


		} catch (StructException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte [] stream = new byte[head.length+body.length];
		System.arraycopy(head, 0, stream, 0, head.length);
		System.arraycopy(body, 0, stream, head.length, body.length);
		Log.i("123", "send login stream len="+stream.length+"  head len="+head.length+"  body len="+body.length);

		//		for(int i=0;i<16;i++){
		//			Log.i("123", "head "+String.format("0x%x", head[i])+ "    stream"+String.format("0x%x", stream[i]));
		//		}
		conn.send(stream, stream.length);
	}

	@Override
	public void logout() {//client
		readbuf.clear();
		Bundle b = new Bundle();
		b.putInt(BUNDLE_LOGOUT_HANDLE, sHandle);
		byte [] body = JsonUtil.buildLogoutJsonStr(b).getBytes();
		byte [] head = null;
		EkuiHead headbuf = new EkuiHead();
		try {
			head = buildHead(headbuf,EkuiCmd.LOGOUT, body.length);
		} catch (StructException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte [] stream = new byte[head.length+body.length];
		System.arraycopy(head, 0, stream, 0, head.length);
		System.arraycopy(body, 0, stream, head.length, body.length);
		conn.send(stream, stream.length);
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		conn.deInit();
	}

	@Override
	public void pushStream(byte[] data, int len) {//server
		byte [] head = null;
		EkuiHead headbuf = new EkuiHead();
		try {
			head = buildHead(headbuf,EkuiCmd.PUSH, len);
		} catch (StructException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte [] stream = new byte[head.length+len];
		System.arraycopy(head, 0, stream, 0, head.length);
		System.arraycopy(data, 0, stream, head.length, len);
		conn.send(stream, stream.length);
	}

	private byte [] buildHead(EkuiHead head,EkuiCmd cmd,int bodyLen) throws StructException, IOException{
		Log.i("123", "sync:"+String.format("0x%x", head.getSync()));
		head.setCommand(cmd.getVal());
		head.setPayload_len(bodyLen);
		return JavaStruct.pack(head);
	}

	private int getHeadLen(){
		if (headLen==-1) {
			try {
				headLen = JavaStruct.pack(new EkuiHead()).length;
			} catch (StructException e) {
				e.printStackTrace();
			}
		}
		return headLen;
	}

	private int searchFlag(byte [] data){
		int pos = 0;
		for(int i=0;i<data.length;i++){
			if (0xa5 == data[i]) {
				pos = i;
				break;
			}
		}
		return pos;
	}


	private  void packMsg(byte [] data){
//		if (readbuf.remaining()< data.length) {
//			Log.e("123", "remain < data len clear");
//			readbuf.clear();
//		}
//	
//		readbuf.put(data);
//		
//		new Thread(){
//			public void run() {
//				processPack();
//			};
//		}.start();
		
		if (processThread==null) {
			processThread = new PacketProcessThread();
			processThread.start();
		}
		processThread.pushData(data);
		
		
		
	}

	private synchronized boolean processPack(){
		int headLen = getHeadLen();
		int dataLen = -1;
		int tempPos = 0;
		int tempEnd = 0;
		int tempLen = 0;
		if (readbuf.position()<headLen) {
			Log.e("123", "readbuf pos < headLen error return false");
			return false;
		}
		
		readbuf.flip();
		while(readbuf.remaining()>headLen){

			if((byte)0xa5!=readbuf.get()){
				Log.e("123", "readbuf .get != 0xa5 continue");
				continue;
			} else {
				//get head
				readbuf.position(readbuf.position()-1);//get() move 1 position
				byte [] head = new byte [headLen];
				readbuf.mark();
				readbuf.get(head);
				EkuiHead headObj = new EkuiHead();

				try {
					JavaStruct.unpack(headObj, head);
					dataLen = headObj.getPayload_len();
				} catch (StructException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (dataLen<0||dataLen>2*1024*1024) {
					//错误的头数据 回退标签
					Log.e("123", "head -> data len < 0 error");
					readbuf.reset();
					readbuf.position(readbuf.position()+1);
					continue;
				}
				if (readbuf.remaining()<dataLen) {
					//数据不全	
					Log.e("123", "remain < datalen  remain="+readbuf.remaining()+"   datalen="+dataLen);
					readbuf.reset();
					tempLen = readbuf.remaining();
					break;
				}

				//get body

				short cmd = 0;
				try {
					cmd = headObj.getCommand();
				} catch (IOException e) {
					e.printStackTrace();
				}
				byte [] body = new byte[dataLen];
				readbuf.get(body);

				if (EkuiCmd.getCmd(cmd)==EkuiCmd.PUSH) {
					doPush(body,dataLen);
				}else{
					String jsonStr = new String(body);
					phaseMsg(EkuiCmd.getCmd(cmd), jsonStr);
				}
			}
		}
		readbuf.compact();
		readbuf.position(tempLen);
		return true;
	}


	@Deprecated
	private synchronized boolean processMsg(byte [] data,int len){
		Log.i("123", "len="+len);
		int pos = searchFlag(data);
		if (pos!=0) {
			Log.i("123", "pos="+pos);
		}



		readbuf.put(data,pos,len-pos);
		int headLen = getHeadLen();
		if(readbuf.position()<headLen){
			Log.e("123","pos < headlen");
			readbuf.clear();
			return false;
		}
		byte [] readBufArray = readbuf.array();
		int dataLen = -1;
		byte [] head = new byte[headLen];
		System.arraycopy(readBufArray, 0, head, 0, headLen);
		EkuiHead headObj = new EkuiHead();
		try {
			JavaStruct.unpack(headObj, head);
			dataLen = headObj.getPayload_len();
		} catch (StructException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(headObj.getSync()!=  (byte) (0xa5) ){
			//			Log.e("123", "sync!=0xa5");
			//			for(int i=0;i<16;i++){
			//				Log.i("123", "head "+ String.format("0x%x", head[i]));
			//			}
			//	
			readbuf.clear();
			return false;
		}else{



		}



		if (dataLen<0 || dataLen > 3*1024*1024) {
			readbuf.clear();
			return false;
		}

		short cmd = 0;
		try {
			cmd = headObj.getCommand();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.i("123", "dataLen="+dataLen);
		byte [] body = new byte[dataLen];


		System.arraycopy(readBufArray, headLen, body, 0, dataLen);
		if (EkuiCmd.getCmd(cmd)==EkuiCmd.PUSH) {
			doPush(body,dataLen);
		}else{
			String jsonStr = new String(body);
			phaseMsg(EkuiCmd.getCmd(cmd), jsonStr);
		}
		readbuf.clear();
		return true;
	}

	private void phaseMsg(EkuiCmd cmd,String jsonStr){
		switch(cmd){
		case LOGIN:
			Log.d("123", "get login msg");
			doLogin(JsonUtil.phaseLoginJson(jsonStr));
			break;
		case LOGOUT:
			Log.d("123", "get logout msg");
			doLogout(JsonUtil.phaseLogoutJsonStr(jsonStr));
			break;
		default:
			break;
		}
	}

	private void doLogout(Bundle b){
		int handle = b.getInt(BUNDLE_LOGOUT_HANDLE);
		Message msg = new Message();
		msg.what = MSG_LOGOUT_ACK;
		msg.obj = handle;
		handler.sendMessage(msg);
	}


	private void doLogin(Bundle b){
		String ip = b.getString(EkuiProtocol.BUNDLE_LOGIN_IP);
		ipAck = ip;
		Message msg = new Message();
		msg.what = MSG_LOGIN_ACK;
		msg.obj = ip;
		handler.sendMessage(msg);
	}

	private void doPush(byte [] data,int len){
		Log.d("123", "get push msg len="+len);
		pushOb.onPushDataComing(data, len);
	}

	class PacketProcessThread extends Thread{
		private Queue<byte []> dataQueue = new ArrayBlockingQueue<byte[]>(300);
		private ByteBuffer buf = ByteBuffer.allocate(30*1024*1024);
		private AtomicReference<Queue<byte []>> ar = null;
		
		public void pushData(byte [] data){
			dataQueue.offer(data);
		}
		public byte [] popData(){
			return ar.get().poll();
		}
		
		public PacketProcessThread() {
			// TODO Auto-generated constructor stub
			ar = new AtomicReference<Queue<byte[]>>(dataQueue);
		}
		
		private void processPacket(ByteBuffer readbuf){
			int headLen = getHeadLen();
			int dataLen = -1;
			int tempPos = 0;
			int tempEnd = 0;
			int tempLen = 0;
			if (readbuf.position()<headLen) {
				Log.e("123", "readbuf pos < headLen error return false");
				return ;
			}
			
			readbuf.flip();
			while(readbuf.remaining()>headLen){

				if((byte)0xa5!=readbuf.get()){
					Log.e("123", "readbuf .get != 0xa5 continue");
					continue;
				} else {
					//get head
					readbuf.position(readbuf.position()-1);//get() move 1 position
					byte [] head = new byte [headLen];
					readbuf.mark();
					readbuf.get(head);
					EkuiHead headObj = new EkuiHead();

					try {
						JavaStruct.unpack(headObj, head);
						dataLen = headObj.getPayload_len();
					} catch (StructException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (dataLen<0||dataLen>2*1024*1024) {
						//错误的头数据 回退标签
						Log.e("123", "head -> data len < 0 error");
						readbuf.reset();
						readbuf.position(readbuf.position()+1);
						continue;
					}
					if (readbuf.remaining()<dataLen) {
						//数据不全	
						Log.e("123", "remain < datalen  remain="+readbuf.remaining()+"   datalen="+dataLen);
						readbuf.reset();
						tempLen = readbuf.remaining();
						break;
					}

					//get body

					short cmd = 0;
					try {
						cmd = headObj.getCommand();
					} catch (IOException e) {
						e.printStackTrace();
					}
					byte [] body = new byte[dataLen];
					readbuf.get(body);

					if (EkuiCmd.getCmd(cmd)==EkuiCmd.PUSH) {
						doPush(body,dataLen);
					}else{
						String jsonStr = new String(body);
						phaseMsg(EkuiCmd.getCmd(cmd), jsonStr);
					}
				}
			}
			readbuf.compact();
			readbuf.position(tempLen);
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			while(true){
				byte [] data = popData();
				if (data!=null) {
					buf.put(data);
					processPacket(buf);
				}	
			}	
		}
	}
	
	
	
	class SocketClientOB implements SocketClientCallbackObserver{

		@Override
		public void onConnect() {
			// TODO Auto-generated method stub
			handler.sendEmptyMessage(MSG_LOGIN_OK);
		}

		@Override
		public void onConnectReceive(byte[] data, int len) {
			// TODO Auto-generated method stub
			//			processMsg(data, len);
			packMsg(data);
		}

		@Override
		public void onDisconnect() {
			// TODO Auto-generated method stub

		}

	}

	class SocketServerOB implements SocketServerCallbackObserver{

		@Override
		public void onConnect(int handle) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onConnectReceive(int handle, byte[] data, int len) {
			// TODO Auto-generated method stub
			sHandle = handle;
//			processMsg(data, len);
			packMsg(data);
		}

		@Override
		public void onDisconnect(int handle) {
			// TODO Auto-generated method stub

		}

	}

	@Override
	public void setHandler(Handler h) {
		this.handler = h;
	}

	@Override
	public void setCallback(IPushOb ob) {
		this.pushOb = ob;
	}

	@Override
	public void setInfo(Bundle b) {
		// TODO Auto-generated method stub

		String s = b.getString("DestinationIP",null);
		if (s!=null) {
			serverIP =s;
		}
		conn.setInfoBundle(b);
	}



}
