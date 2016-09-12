package com.example.view;

import java.util.Timer;
import java.util.TimerTask;

import com.example.action.PlatformAction;
import com.example.action.ProtocolClientAction;
import com.example.action.TestSocket;
import com.example.action.TestSocket.DoReceiveFoo;
import com.example.bean.I420Renderer;
import com.example.utils.IConst;
import com.example.utils.JniUtil;
import com.example.utils.PhoneConfigUtil;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

public class YUVGLSurfaceView extends GLSurfaceView implements DoReceiveFoo,IConst {

	Context mContext;
	I420Renderer mRenderer;
	TestSocket mSocketMgr = TestSocket.getInstance();
	
	ProtocolClientAction pcMgr = ProtocolClientAction.getInstance();
	
	Timer timer;
	long timestamp=-1;
	
	private static final int MSG_TIMER_STOP = 0xe0;
	public static final int MSG_TIMER_START = 0xe1;
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case MSG_TIMER_STOP:
				stopTimeTask();
				//重连
				Log.i("123", "client relink");
				pcMgr.reLink();
				break;
			case MSG_TIMER_START:
				Log.d("123", "time start!!");
				startTimeTask();
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};
	
	public YUVGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mContext = context;
		setEGLContextClientVersion(2);
		mRenderer = new I420Renderer(context, (GLSurfaceView)this,mHandler);
		setRenderer(mRenderer);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		JniUtil.hwPlayerInit();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
//		mSocketMgr.register(this);
//		mSocketMgr.init(PlatformAction.getInstance().getIP(), LOCAL_PORT, LOCAL_PORT);
//		byte [] data = PhoneConfigUtil.getPhoneIp(mContext).getBytes();
//		mSocketMgr.sendPacket(data, data.length);
//		JniUtil.hwPlayerPlay();
//		super.surfaceCreated(holder);
//		
		
		pcMgr.register(this);
		pcMgr.init(mContext, PlatformAction.getInstance().getIP());
		pcMgr.setfaHandler(mHandler);
		
		
//		pcMgr.logMeIn();
		JniUtil.hwPlayerPlay();
		
		mHandler.sendEmptyMessage(MSG_TIMER_START);
		super.surfaceCreated(holder);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
		pcMgr.logMeOut();
		super.surfaceDestroyed(holder);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// TODO Auto-generated method stub
		super.surfaceChanged(holder, format, w, h);
	}

	@Override
	public void foo(byte[] data, int len) {
		// TODO Auto-generated method stub
		byte [] buf = new byte[len];
		System.arraycopy(data, 0, buf, 0, len);
		Log.e("123", "get foo len="+len+"  datalen="+data.length);
		JniUtil.setHWData(buf, len);
	}
	
	private void startTimeTask(){
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.i("123", "time task running");
				long bar =mRenderer.getTime();
				Log.i("123", "timer bar="+bar+"   time="+timestamp);
				if (bar==timestamp) {
					Log.e("123", "bar==timestamp  stop");
					mHandler.sendEmptyMessage(MSG_TIMER_STOP);
				}
				timestamp = bar;
			}
		}, 5000,5000);
	}

	private void stopTimeTask(){
		Log.e("123", "timer stop");
		if (timer==null) {
			throw new NullPointerException("timer == null");
		}
		timer.cancel();
		timer.purge();
		timer = null;
	}
	
}
