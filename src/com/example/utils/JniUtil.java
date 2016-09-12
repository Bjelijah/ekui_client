package com.example.utils;

public class JniUtil {
	
	static{
		System.loadLibrary("hwplay");
		System.loadLibrary("player_jni");
	}
	
	
	public static native void YUVInit();			//初始化
	public static native void YUVDeinit();			//释放内存
	public static native void YUVSetCallbackObject(Object callbackObject,int flag);
	public static native void YUVSetCallbackMethodName(String methodStr,int flag);
	public static native void YUVLock();
	public static native void YUVUnlock();
	public static native void YUVSetEnable();//开始显示YUV数据
	public static native void YUVRenderY();			//渲染Y数据
	public static native void YUVRenderU();			//渲染U数据
	public static native void YUVRenderV();			//渲染V数据
	
	public static native void YUVsetData(byte [] data,int len,int w,int h);
	public static native void setH264Data(byte [] data,int len,int w,int h,int isI);
	public static native void setHWData(byte [] data,int len);
	public static native byte [] H264toHWStream(byte[] inH264,int inLen,int isI);
	
	//HW play
	public static native void hwPlayerInit();
	public static native void hwPlayerDeinit();
	public static native void hwPlayerPlay();
	public static native void hwPlayerStop();
	
	
	//HW net
	public static native void hwNetInit();//
	public static native void hwNetDeinit();
	public static native void hwLogin(String ip);
	public static native boolean hwLogout();
	public static native void hwReqStream();
	public static native boolean hwStopStream();
	public static native void hwReReqStream();
}
