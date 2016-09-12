#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <stdlib.h>
#include <semaphore.h>
#include "include/play_def.h"
#include "include/stream_type.h"
#include "include/net_sdk.h"
#include <unistd.h>
#include "com_example_utils_JniUtil.h"




#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "JNI", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "JNI", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "JNI", __VA_ARGS__))


typedef struct{
	char set_time_method_name[32];
	char request_method_name[32];
}yv12_info_t;

typedef struct{
	char * y;
	char * u;
	char * v;
	unsigned long long time;
	int width;
	int height;
	int enable;

	/* multi thread */
	int method_ready;
	JavaVM * jvm;
	JNIEnv * env;
	jmethodID request_render_method,set_time_method;
	jobject callback_obj;
	pthread_mutex_t lock;
	unsigned long long first_time;
}YV12_display_t;

static yv12_info_t    * g_yuv_info     = NULL;
static YV12_display_t * g_yuv_display  = NULL;

void inputHWData2HWPlayer(const unsigned char *data,int len);



void yv12gl_display(const unsigned char * y, const unsigned char *u,const unsigned char *v, int width, int height, unsigned long long time)
{
	if(g_yuv_display == NULL) return;
	if (!g_yuv_display->enable) return;
	g_yuv_display->time = time/1000;

	if( g_yuv_display->jvm->AttachCurrentThread(&g_yuv_display->env,NULL)!= JNI_OK) {
		LOGE("%s: AttachCurrentThread() failed", __FUNCTION__);
		return;
	}
	/* get JAVA method first */
	if (!g_yuv_display->method_ready) {
		jclass cls =  g_yuv_display->env->GetObjectClass(g_yuv_display->callback_obj);
		if (cls == NULL) {
			LOGE("FindClass() Error.....");
			goto error;
		}
		//�ٻ�����еķ���

		g_yuv_display->request_render_method = g_yuv_display->env->GetMethodID(cls,g_yuv_info->request_method_name,"()V");
		g_yuv_display->set_time_method = g_yuv_display->env->GetMethodID(cls,g_yuv_info->set_time_method_name,"(J)V");
		if (g_yuv_display->request_render_method == NULL || g_yuv_display->set_time_method == NULL) {
			LOGE("%d  GetMethodID() Error.....",__LINE__);
			goto error;
		}
		g_yuv_display->method_ready=1;
	}
	g_yuv_display->env->CallVoidMethod(g_yuv_display->callback_obj,g_yuv_display->set_time_method,g_yuv_display->time);
	pthread_mutex_lock(&g_yuv_display->lock);
	if (width!=g_yuv_display->width || height!=g_yuv_display->height) {
		LOGI("g_display->width = %d  width=%d",g_yuv_display->width,width);
		if(g_yuv_display->y!=NULL){
			free(g_yuv_display->y);
			g_yuv_display->y = NULL;
		}
		if(g_yuv_display->u!=NULL){
			free(g_yuv_display->u);
			g_yuv_display->u = NULL;
		}
		if(g_yuv_display->v!=NULL){
			free(g_yuv_display->v);
			g_yuv_display->v = NULL;
		}
		g_yuv_display->y = (char *)realloc(g_yuv_display->y,width*height);
		g_yuv_display->u = (char *)realloc(g_yuv_display->u,width*height/4);
		g_yuv_display->v = (char *)realloc(g_yuv_display->v,width*height/4);
		g_yuv_display->width = width;
		g_yuv_display->height = height;
	}
	memcpy(g_yuv_display->y,y,width*height);
	memcpy(g_yuv_display->u,u,width*height/4);
	memcpy(g_yuv_display->v,v,width*height/4);
	pthread_mutex_unlock(&g_yuv_display->lock);

	g_yuv_display->env->CallVoidMethod(g_yuv_display->callback_obj,g_yuv_display->request_render_method,NULL);

	//	if (g_yuv_display->jvm->DetachCurrentThread() != JNI_OK) {
	//		LOGE("%s: DetachCurrentThread() failed", __FUNCTION__);
	//	}
	return;

	error:
	//	if (g_yuv_display->jvm->DetachCurrentThread() != JNI_OK) {
	//		LOGE("%s: DetachCurrentThread() failed", __FUNCTION__);
	//	}
	return;
}


JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVInit
(JNIEnv *env, jclass){
	if(g_yuv_info == NULL){
		g_yuv_info = (yv12_info_t*)malloc(sizeof(yv12_info_t));
		memset(g_yuv_info,0,sizeof(yv12_info_t));
	}
	if(g_yuv_display == NULL){
		g_yuv_display = (YV12_display_t*)malloc(sizeof(YV12_display_t));
		memset(g_yuv_display,0,sizeof(YV12_display_t));
		env->GetJavaVM(&g_yuv_display->jvm);
		//FIXME  now obj=JniYV12Util should be YV12Renderer
		pthread_mutex_init(&g_yuv_display->lock,NULL);
		g_yuv_display->width  = 352;
		g_yuv_display->height = 288;
		g_yuv_display->y = (char*)malloc(g_yuv_display->width*g_yuv_display->height);
		g_yuv_display->u = (char*)malloc(g_yuv_display->width*g_yuv_display->height/4);
		g_yuv_display->v = (char*)malloc(g_yuv_display->width*g_yuv_display->height/4);
		memset(g_yuv_display->y,0,g_yuv_display->width*g_yuv_display->height);//4:2:2 black
		memset(g_yuv_display->u,128,g_yuv_display->width*g_yuv_display->height/4);
		memset(g_yuv_display->v,128,g_yuv_display->width*g_yuv_display->height/4);
	}
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVDeinit
(JNIEnv *env, jclass){
	return;//we never release//FIXME
	if(g_yuv_info!=NULL){
		free(g_yuv_info);
		g_yuv_info = NULL;
	}
	if(g_yuv_display!=NULL){
		if(g_yuv_display->callback_obj!=NULL){
			env->DeleteGlobalRef(g_yuv_display->callback_obj);
		}
		if(g_yuv_display->y!=NULL){
			free(g_yuv_display->y);
			g_yuv_display->y = NULL;
		}
		if(g_yuv_display->u!=NULL){
			free(g_yuv_display->u);
			g_yuv_display->u = NULL;
		}
		if(g_yuv_display->v!=NULL){
			free(g_yuv_display->v);
			g_yuv_display->v = NULL;
		}
		pthread_mutex_destroy(&g_yuv_display->lock);
		free(g_yuv_display);
		g_yuv_display = NULL;
	}
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVSetCallbackObject
(JNIEnv *env, jclass, jobject callbackObject, jint flag){
	if(g_yuv_info==NULL)return;
	switch (flag) {
	case 0:
		g_yuv_display->callback_obj = env->NewGlobalRef(callbackObject);
		break;
	default:
		break;
	}
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVSetCallbackMethodName
(JNIEnv *env, jclass, jstring method_name, jint flag){
	if(g_yuv_info==NULL)return;
	const char * _method_name= env->GetStringUTFChars(method_name,NULL);
	switch (flag) {
	case 0:
		strcpy(g_yuv_info->set_time_method_name,_method_name);
		break;
	case 1:
		strcpy(g_yuv_info->request_method_name,_method_name);
		break;
	default:
		break;
	}
	env->ReleaseStringUTFChars(method_name,_method_name);
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVLock
(JNIEnv *, jclass){
	if(g_yuv_display == NULL){
		return;
	}
	pthread_mutex_lock(&g_yuv_display->lock);
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVUnlock
(JNIEnv *, jclass){
	if(g_yuv_display == NULL){
		return;
	}
	pthread_mutex_unlock(&g_yuv_display->lock);
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVSetEnable
(JNIEnv *, jclass){
	g_yuv_display->enable = 1;
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVRenderY
(JNIEnv *, jclass){
	if (g_yuv_display->y == NULL) {
		char value[4] = {0,0,0,0};
		glTexImage2D(GL_TEXTURE_2D,0,GL_LUMINANCE,2,2,0,GL_LUMINANCE,GL_UNSIGNED_BYTE,value);
	}
	else {
		//LOGI("render y");
		glTexImage2D(GL_TEXTURE_2D,0,GL_LUMINANCE,g_yuv_display->width,g_yuv_display->height,0,GL_LUMINANCE,GL_UNSIGNED_BYTE,g_yuv_display->y);
	}
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVRenderU
(JNIEnv *, jclass){
	if (g_yuv_display->u == NULL) {
		char value[] = {128};
		glTexImage2D(GL_TEXTURE_2D,0,GL_LUMINANCE,1,1,0,GL_LUMINANCE,GL_UNSIGNED_BYTE,value);
	}
	else {
		glTexImage2D(GL_TEXTURE_2D,0,GL_LUMINANCE,g_yuv_display->width/2,g_yuv_display->height/2,0,GL_LUMINANCE,GL_UNSIGNED_BYTE,g_yuv_display->u);
	}
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVRenderV
(JNIEnv *, jclass){
	if (g_yuv_display->v == NULL) {
		char value[] = {128};
		glTexImage2D(GL_TEXTURE_2D,0,GL_LUMINANCE,1,1,0,GL_LUMINANCE,GL_UNSIGNED_BYTE,value);
	}
	else {
		glTexImage2D(GL_TEXTURE_2D,0,GL_LUMINANCE,g_yuv_display->width/2,g_yuv_display->height/2,0,GL_LUMINANCE,GL_UNSIGNED_BYTE,g_yuv_display->v);
	}
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_YUVsetData
(JNIEnv *env, jclass, jbyteArray data, jint len, jint w, jint h){
	char *buf = (char *)env->GetByteArrayElements(data,0);
	unsigned char* y = (unsigned char *)buf;
	unsigned char* u = y+w*h;
	unsigned char* v = u+w*h/4;
	yv12gl_display(y,u,v,w,h,0);

	env->ReleaseByteArrayElements(data,(signed char*)buf,0);
}



typedef struct StreamResource
{
	JavaVM * jvm;
	JNIEnv * env;
	jobject obj;
	USER_HANDLE handle;
	LIVE_STREAM_HANDLE live_stream_handle;
	jmethodID mid;
	PLAY_HANDLE play_handle;
	int media_head_len;
	int is_exit;	//退出标记位
	sem_t exit_sem;

}TStream;
static struct StreamResource * res = NULL;

char * g_h264Buffer = NULL;


JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_setH264Data
(JNIEnv *env, jclass, jbyteArray h264, jint len, jint w, jint h,jint isI){
	char *buf = (char *)env->GetByteArrayElements(h264,0);
	stream_head hw_head;
	memset(&hw_head,0,sizeof(stream_head));
	hw_head.tag = HW_MEDIA_TAG;
	hw_head.sys_time = time(NULL);
	hw_head.len = sizeof(stream_head) + len;
	hw_head.type = isI ? 1: 0;
	struct timeval val;
	gettimeofday(&val,NULL);
	uint64_t timestamp = (unsigned long long)val.tv_sec * 1000 + val.tv_usec / 1000;
	hw_head.time_stamp = timestamp *1000;

	if(g_h264Buffer==NULL){
		g_h264Buffer =(char *) malloc(2*1024*1024);
		memset(g_h264Buffer,0,2*1024*1024);
	}

	memcpy(g_h264Buffer,&hw_head,sizeof(stream_head));
	memcpy(g_h264Buffer+sizeof(stream_head),buf,len);

	inputHWData2HWPlayer((const unsigned char*)g_h264Buffer,hw_head.len);

	env->ReleaseByteArrayElements(h264,(signed char*)buf,0);
}


JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_setHWData
(JNIEnv *env, jclass, jbyteArray data, jint len){
	char *buf = (char *)env->GetByteArrayElements(data,0);
	inputHWData2HWPlayer((const unsigned char*)buf,len);
	env->ReleaseByteArrayElements(data,(signed char*)buf,0);
}

char *g_buf=NULL;

JNIEXPORT jbyteArray JNICALL Java_com_example_utils_JniUtil_H264toHWStream
(JNIEnv *env, jclass, jbyteArray h264, jint len, jint isI){
	char *buf = (char *)env->GetByteArrayElements(h264,0);
	stream_head hw_head;
	memset(&hw_head,0,sizeof(stream_head));
	hw_head.tag = HW_MEDIA_TAG;
	hw_head.sys_time = time(NULL);
	hw_head.len = sizeof(stream_head) + len;
	hw_head.type = isI ? 1: 0;
	struct timeval val;
	gettimeofday(&val,NULL);
	uint64_t timestamp = (unsigned long long)val.tv_sec * 1000 + val.tv_usec / 1000;
	hw_head.time_stamp = timestamp *1000;

	//	if(g_h264Buffer==NULL){
	//		g_h264Buffer =(char *) malloc(2*1024*1024);
	//		memset(g_h264Buffer,0,2*1024*1024);
	//	}
	//
	//	memcpy(g_h264Buffer,&hw_head,sizeof(stream_head));
	//	memcpy(g_h264Buffer+sizeof(stream_head),buf,len);

	jbyteArray array = env->NewByteArray(hw_head.len);
	char *hwBuf = (char *)env->GetByteArrayElements(array,0);
	memcpy(hwBuf,&hw_head,sizeof(stream_head));
	memcpy(hwBuf+sizeof(stream_head),buf,len);

	env->ReleaseByteArrayElements(h264,(signed char*)buf,0);
	env->ReleaseByteArrayElements(array,(signed char*)hwBuf,0);



	return array;

}



void inputHWData2HWPlayer(const unsigned char *data,int len){
	if(res==NULL){
		return;
	}
	if(res->is_exit){
		return;
	}
	int ret = hwplay_input_data(res->play_handle,(const char*) data ,len);
	LOGI("input_data ret=%d",ret);
}


static void on_source_callback(PLAY_HANDLE handle, int type, const char* buf, int len, unsigned long timestamp, long sys_tm, int w, int h, int framerate, int au_sample, int au_channel, int au_bits, long user){

	LOGE("type=%d  len=%d  w=%d  h=%d  timestamp=%ld sys_tm=%ld  framerate=%d  au_sample=%d  au_channel=%d au_bits=%d",type,len,w,h,timestamp,sys_tm,framerate,au_sample,au_channel,au_bits);

	if(type == 0){//音频
		//		audio_play(buf,len,au_sample,au_channel,au_bits);
		//		audio_play(buf, len);//add cbj
	}else if(type == 1){//视频
		unsigned char* y = (unsigned char *)buf;
		unsigned char* u = y+w*h;
		unsigned char* v = u+w*h/4;
		yv12gl_display(y,u,v,w,h,timestamp);
	}
}

static void on_live_stream_fun(LIVE_STREAM_HANDLE handle,int stream_type,const char* buf,int len,long userdata){
	if(res == NULL)return;
	if(res->is_exit == 1)return;
	int ret = hwplay_input_data(res->play_handle, buf ,len);
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_hwPlayerInit
(JNIEnv *env, jclass){
	if(res == NULL){
		res =(TStream*)malloc(sizeof(TStream));
		memset(res,0,sizeof(TStream));
		env->GetJavaVM(&res->jvm);
		res->media_head_len = 0;
		res->obj = NULL;
		res->is_exit = 1;
		sem_init(&res->exit_sem,0,0);
	}
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_hwPlayerDeinit
(JNIEnv *env, jclass){
	if(res!=NULL){
		res->is_exit = 1;
		sem_trywait(&res->exit_sem);
		if(res->obj!=NULL){
			env->DeleteGlobalRef(res->obj);
			res->obj = NULL;
			sem_destroy(&res->exit_sem);
			free(res);
			res = NULL;
		}
	}
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_hwPlayerPlay
(JNIEnv *, jclass){
	if(res == NULL) return;
	hwplay_init(1,0,0);
	RECT area;
	HW_MEDIAINFO media_head;
	memset(&media_head,0,sizeof(media_head));

	media_head.media_fourcc = HW_MEDIA_TAG;
	media_head.au_channel = 1;
	media_head.au_sample = 8;
	media_head.au_bits = 16;
	media_head.adec_code = ADEC_AAC;
	media_head.vdec_code = VDEC_H264;

	PLAY_HANDLE  ph = hwplay_open_stream((const char*)&media_head,sizeof(media_head),1024*1024,0,area);
	res->play_handle = ph;
	hwplay_register_source_data_callback(ph,on_source_callback,0);

	res->is_exit = 0;
	hwplay_play(res->play_handle);
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_hwPlayerStop
(JNIEnv *, jclass){
	//TODO
}




JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_hwNetInit
(JNIEnv *, jclass){
	hwnet_init(5888);
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_hwNetDeinit
(JNIEnv *, jclass){

}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_hwLogin
(JNIEnv *env, jclass, jstring ip){
	if(res==NULL)return;

	const char* _ip = env->GetStringUTFChars(ip,0);
	LOGI("login  ip=%s",_ip);

	res->handle = hwnet_login(_ip,5198,"admin","12345");
	env->ReleaseStringUTFChars(ip,_ip);
}

JNIEXPORT jboolean JNICALL Java_com_example_utils_JniUtil_hwLogout
(JNIEnv *, jclass){
	if(res==NULL)return false;
	if(res->handle<0)return false;
	BOOL ret = 0;
	int i=0;
	do {
		ret = hwnet_logout(res->handle);
		if(ret==1){
			return true;
		}else{
			usleep(200);
			i++;
		}
	} while (i<10);
	return false;
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_hwReqStream
(JNIEnv *, jclass){
	if(res==NULL)return;
	res->live_stream_handle = hwnet_get_live_stream(res->handle,0,0,0,on_live_stream_fun,0);
}

JNIEXPORT jboolean JNICALL Java_com_example_utils_JniUtil_hwStopStream
  (JNIEnv *, jclass){
	if(res==NULL)return false;
	if(res->live_stream_handle<0)return false;
	int i=0;
	BOOL ret = 0;
	do {
		ret = hwnet_close_live_stream(res->live_stream_handle);
		if(ret ==1){
			return true;
		}else{
			usleep(200);
			i++;
		}

	} while (i<10);
	return false;
}

JNIEXPORT void JNICALL Java_com_example_utils_JniUtil_hwReReqStream
  (JNIEnv *, jclass){

}

