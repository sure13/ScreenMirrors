package com.action.screenmirror.model;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import org.json.JSONException;
import org.json.JSONObject;

import com.action.screenmirror.SendActivity;
import com.action.screenmirror.audio.AudioTrack;
import com.action.screenmirror.interf.IConnectCallback;
import com.action.screenmirror.interf.IOndisconnectCallback;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.LogUtils;
import com.action.screenmirror.utils.ThreadPoolManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class TcpSocketClient  {

	public static final String TAG = "TcpSocketReceiver";

	private String serverIp;
	private Socket touchSocket;
	private DataOutputStream touchDos;
	private DataInputStream touchDis;

	private boolean isRun = true;

	private SurfaceView mSurfaceView;
	private IConnectCallback mConnectCallback;
//	private static int VIDEO_WIDTH = 1024;//720;//1080;//1024;//
//	private static int VIDEO_HEIGHT = 600;//1560;//1920;//600;//
	
//	private String  remoteAddress;
//	
//	public  TcpSocketClient(String remoteAddress){
//		this.remoteAddress = remoteAddress;
//	}

	public TcpSocketClient(SurfaceView surfaceView,IConnectCallback connectCallback) {
		mSurfaceView = surfaceView;
		mConnectCallback = connectCallback;
		mSurfaceView.getHolder().addCallback(new Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder arg0) {
				Log.i(TAG, "hdb---surfaceDestroyed----");
				releseServer();
				isConnection = false;
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder hodler) {
				Log.i(TAG, "hdb---surfaceCreated----");
				ThreadPoolManager.getInstance().execute(new Runnable() {
					
					@Override
					public void run() {
						isRun = true;						
					}
				});
				
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	/**
	 * 涓庤繙绋嬫湇鍔＄寤虹珛杩炴帴
	 * 
	 * @param serverIp
	 *            杩滅▼鏈嶅姟绔澶囩殑
	 */
	public void startDisPlayRomoteDesk(String serverIp) {
		Log.i(TAG,"hdb---startDisPlayRomoteDesk---ip:" + serverIp);
		this.serverIp = serverIp;
		startTouchServer();
	//	startServer(serverIp);
	}

	/**
	 * 鍜岃繙绋嬫湇鍔″櫒寤虹珛TCP杩炴帴锛岀敤浜庡睆骞曚簨浠剁殑浜や簰
	 */
	private void startTouchServer() {

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					touchSocket = new Socket(serverIp, Config.PortGlob.TOUCHPORT);
					touchDos = new DataOutputStream(touchSocket.getOutputStream());
					touchDis = new DataInputStream(touchSocket.getInputStream());
					Log.i("wxy","-------------------startTouchServer sucessful----------------------:");
					if (mConnectCallback != null) {
						mConnectCallback.onTouchConnectionSucessful();
					}
					isConnection = true;
				} catch (Exception e) {
					Log.i("wxy","-------------------startTouchServer error----------------------:" );
					e.printStackTrace();
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
					isConnection = false;
				}
//				receiveAliveData();
			}
		}).start();

	}


	private void releseTouchServer() {
		LogUtils.i("hdb----onDestroy----releseTouchServer:");
		if (touchDos != null) {
			try {
				touchDos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (touchDis != null) {
			try {
				touchDis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (touchSocket != null) {
			try {
				touchSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		touchDis = null;
		touchDos = null;
		touchSocket = null;
		isConnection = false;
	}


	public void releseServer() {
		Log.i(TAG, "hdb---releseServer--");
		releseTouchServer();
	}

	public void reConnect(final String ip) {
		Log.e(TAG, "hdb------reConnect----");
		releseServer();
		startDisPlayRomoteDesk(ip);

	}

	/**
	 * 鍙戦�佹湰鍦板睆骞曚簨浠跺埌杩滅▼鏈嶅姟绔�
	 * 
	 * @param actionType
	 *            鏃堕棿绫诲瀷
	 * @param changeX
	 *            浜嬩欢瀵瑰簲鐨� X 鍊�
	 * @param changeY
	 *            浜嬩欢瀵瑰簲鐨� 鍊�
	 */
	public synchronized void sendTouchData(final int actionType, final int changeX, final int changeY) {
		LogUtils.i(TAG, "sendTouchData---action:" + actionType + "  changeX:" + changeX + "  changeY:" + changeY
				+ "  dos:" + touchDos);

		ThreadPoolManager.getInstance().execute(new Runnable() {
			@Override
			public void run() {
				if (touchDos != null) {
					if (changeX >= 0 && changeY >= 0) {
						JSONObject jObject = new JSONObject();
						try {
							jObject.put(Config.MotionEventKey.JACTION, actionType);
							jObject.put(Config.MotionEventKey.JX, changeX);
							jObject.put(Config.MotionEventKey.JY, changeY);
						} catch (JSONException e1) {
							e1.printStackTrace();
						}
						byte[] jBytes = jObject.toString().getBytes();
						byte[] intToByte = new byte[1];
						intToByte[0] = (byte) jBytes.length;
						byte[] data = new byte[jBytes.length + 1];
						System.arraycopy(intToByte, 0, data, 0, 1);
						System.arraycopy(jBytes, 0, data, 1, jBytes.length);
//						Log.i(TAG, "wxy----data:" + new String(data));
						writeTouchData(data);
					}

				}
			}

		});

	}

	private synchronized void writeTouchData(byte[] data) {
		try {
			if (touchDos != null) {
				touchDos.write(data);
				touchDos.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
		}
	}

	long time = 0;

	private void receiveAliveData() {
		try {
			while (true) {
				time = SystemClock.uptimeMillis();
				byte[] aLive = new byte[5];
				if (touchDis == null) {
					return;
				}
				touchDis.read(aLive);
				LogUtils.i("hdb----timeLive:" + (SystemClock.uptimeMillis() - time));
				time = SystemClock.uptimeMillis();
				mHandler.removeMessages(Config.HandlerGlod.TIME_OUT);
				mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.TIME_OUT, 10000);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private boolean isConnection = false;
	
	public boolean hasConnect(){
		Log.i("wxy","----------------Client isConnection---------------------------"+isConnection);
		return isConnection;
	}

	private Handler mHandler = new TcpHandler(this);

	private DataCoder mCoder;

	public static class TcpHandler extends Handler {
		private WeakReference<TcpSocketClient> weakReference;
		public TcpHandler(TcpSocketClient tcpSocketReceiver) {
			weakReference = new WeakReference<TcpSocketClient>(tcpSocketReceiver);
		}

		@Override
		public void handleMessage(Message msg) {
			TcpSocketClient mTcpSocketReceiver = weakReference.get();
			if (mTcpSocketReceiver == null)
				return;
			switch (msg.what) {

			case Config.HandlerGlod.CONNECT_FAIL:
				mTcpSocketReceiver.releseServer();
				if (mTcpSocketReceiver.mConnectCallback != null) {
					mTcpSocketReceiver.mConnectCallback.onDisconnect();
					mTcpSocketReceiver.mConnectCallback.onTouchConnectionFail();
				}
				break;

			case Config.HandlerGlod.CONNECT_SUCCESS:
				if (mTcpSocketReceiver.mConnectCallback != null) {
					mTcpSocketReceiver.mConnectCallback.onConnect();
				}
				break;
			case Config.HandlerGlod.TIME_OUT:
				Log.i(TAG, "hdb--TIME_OUT---mConnectCallback:"+mTcpSocketReceiver.mConnectCallback);
				mTcpSocketReceiver.releseServer();
				if (mTcpSocketReceiver.mConnectCallback != null) {
					mTcpSocketReceiver.mConnectCallback.onDisconnect();
				}
				break;
			case Config.HandlerGlod.CHECK_ALIVE:
				Log.i(TAG, "hdb--CHECK_ALIVE---");
				if (mTcpSocketReceiver.checkCount == mTcpSocketReceiver.receiverCount) {
					sendEmptyMessage(Config.HandlerGlod.TIME_OUT);
					return;
				}
				mTcpSocketReceiver.checkCount = mTcpSocketReceiver.receiverCount;
				removeMessages(Config.HandlerGlod.CHECK_ALIVE);
				sendEmptyMessageDelayed(Config.HandlerGlod.CHECK_ALIVE, Config.HandlerGlod.CHECK_ALIVE_DELAY);
				break;
			case Config.HandlerGlod.AUDIO_CONNECT_SUCCESS:
	//			mTcpSocketReceiver.startAudioPlay();
				break;

			default:
				break;
			}

		}
	}
	
	
	
	
	
	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	private int receiverCount = 0;
	private int checkCount = -1;
	
	private static String getByteStringHex(byte[] data, int len) {
		char[] DIGITS_UPPER = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		if (data.length < len) {
			return "wrong length!";
		}
		StringBuffer ret = new StringBuffer(512);
		for (int i = 0; i < len; i++) {
			byte hf = (byte) ((data[i] >> 4) & 0x0F);
			byte lf = (byte) (data[i] & 0x0F);
			ret.append(DIGITS_UPPER[hf]);
			ret.append(DIGITS_UPPER[lf]);
			if ((i + 1) < len)
				ret.append(",");
		}
		return ret.toString();
	}
	
	public static int bufferToInt(byte[] src) {
		int value;
		value = (int) ((src[0] & 0xFF) | ((src[1] & 0xFF) << 8) | ((src[2] & 0xFF) << 16));
		return value;
	}
	
	public static int bufferToInt4(byte[] src) {
		int value;
		value = (int) ((src[0] & 0xFF) | ((src[1] & 0xFF) << 8) | ((src[2] & 0xFF) << 16) | ((src[3] & 0xFF) << 24));
		return value;
	}

}
