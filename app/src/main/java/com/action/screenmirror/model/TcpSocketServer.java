package com.action.screenmirror.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.json.JSONObject;

import com.action.screenmirror.audio.AudioRecord;
import com.action.screenmirror.bean.TSocketModel;
import com.action.screenmirror.interf.IDisconnectCallback;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.LogUtils;
import com.action.screenmirror.utils.ThreadPoolManager;
import com.action.screenmirror.utils.EventInputUtils;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

public class TcpSocketServer  {

	private static boolean useSles = false;
	private static final String TAG = "TcpSocketSend";

	/** touch */
	private ServerSocket touchSocketsService;
	private TSocketModel mSocketMole = null;
	private ArrayList<TSocketModel> mTListSocketMoles = new ArrayList<TSocketModel>();

//	private DisplayManager mDisplayManager;
	private UdpSocketSearcher mSearcher;

	private Context mContext;

	public TcpSocketServer (Context context) {
		mContext = context;
//		mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
		releseServer();
	}

	public void initServer() {
		isRun = true;
		setConnection(false);
		initTouchServer();
	}
	

	private boolean isRun = false;
	private DataInputStream touchDis;
	private DataOutputStream touchDos;
	private Socket touchSocket;
	private static  boolean isConnection;
	public boolean getConnection() {
		return isConnection;
	}
	public void setConnection( boolean isConnection) {
		this.isConnection = isConnection;
	}
	private void initTouchServer() {
		Log.i("wxy","------------------initTouchServer--------------------");
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				try {
//					touchSocketsService = new ServerSocket(Config.PortGlob.TOUCHPORT);
					if (touchSocketsService == null) {
						touchSocketsService = new ServerSocket();
					    touchSocketsService.setReuseAddress(true);
					    touchSocketsService.bind(new InetSocketAddress(Config.PortGlob.TOUCHPORT));
					}
					while(isRun){
						Log.i(TAG, "hdb-initTouchServer-isRun--");	
						touchSocket = touchSocketsService.accept();
						touchDis = new DataInputStream(touchSocket.getInputStream());
						touchDos = new DataOutputStream(touchSocket.getOutputStream());
						
						InetAddress inetAddress = touchSocket.getInetAddress();
						String hostAddress = inetAddress.getHostAddress();
						Log.i(TAG, "hdb-initTouchServer-isRun--hostAddress:"+hostAddress);
						
						
						TSocketModel mTSocketMole = new TSocketModel();
						mTSocketMole.setTouchSockets(touchSocket, touchDis, touchDos);
						mTListSocketMoles.add(mTSocketMole);
						setConnection(true);
						mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.TOUCH_CONNECT_SUCCESS, 0);
						Log.i("wxy", "------------initTouchServer sucessful--------------");
					}
					
				} catch (Exception ex) {
					ex.printStackTrace();
					setConnection(false);
					mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
				}

			}
		});
	}
	
	public void releseServer(){
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
//		if (touchSocketsService != null) {
//			try {
//				touchSocketsService.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
		touchDis = null;
		touchDos = null;
		touchSocket = null;
	//	touchSocketsService = null;
		setConnection(false);
	}


	private ConnectCallBack mConnCallBack;

	public void setConnectCallBack(ConnectCallBack connCallBack) {
		mConnCallBack = connCallBack;
	}

	public interface ConnectCallBack {
		void onTouchConnectSuccess();
		void onTouchConnectFail();
	}

	protected void receiverTouchData() {
		Log.i("wxy", "------------------------receiverTouchData------------------------:"+mTListSocketMoles.size());
		int size = mTListSocketMoles.size();
		for (int i = 0; i < mTListSocketMoles.size(); i++) {
			TSocketModel socketMole = mTListSocketMoles.get(i);
			if (!socketMole.isTouchRun()) {
				socketMole.setTouchRun(true);
				ReceiverTouchDataRunnable reRunnable = new ReceiverTouchDataRunnable(socketMole,new IDisconnectCallback() {
					
					@Override
					public void onDisconnect(TSocketModel socketMole) {
						_close(socketMole);
						Log.i("wxy", "------------------------receiverTouchData------------onDisconnect:------");
						
					}
				});
				ThreadPoolManager.getInstance().execute(reRunnable);
				socketMole.setRunnable(reRunnable);
				Log.i("wxy", "------------------------receiverTouchData------------connect:------");
			}
		}
	}


	

	private Handler mHandler = new DataHandler(this);
	public static class DataHandler extends Handler {
		WeakReference<TcpSocketServer> weakReference;

		public DataHandler(TcpSocketServer mTransmitter) {
			weakReference = new WeakReference<TcpSocketServer>(mTransmitter);

		}

		@Override
		public void handleMessage(Message msg) {
			TcpSocketServer mSend = weakReference.get();
			if (mSend == null)
				return;

			switch (msg.what) {
			case Config.HandlerGlod.DATA_CONNECT_SUCCESS:
				Log.i(TAG, "hdb--------DATA_CONNECT_SUCCESS-----------");
				mSend.saveSocketModle();
				break;

			case Config.HandlerGlod.TOUCH_CONNECT_SUCCESS:
				Log.i(TAG, "hdb--------TOUCH_CONNECT_SUCCESS-----------");
				Log.i("wxy", "-----------------------TOUCH_CONNECT_SUCCESS-----------");
				mSend.saveSocketModle();
				break;
			case Config.HandlerGlod.CONNECT_FAIL:
				Log.i(TAG, "hdb--------CONNECT_FAIL-----------");
				mSend.connectFail();
				break;
			case Config.HandlerGlod.AUDIO_CONNECT_SUCCESS:
				Log.i(TAG, "hdb--------AUDIO_CONNECT_SUCCESS-----------");
				mSend.saveSocketModle();
				break;

			default:
				break;
			}

		}

	}
	
	private synchronized void _close(TSocketModel socketMole){
		if (socketMole != null) {
			socketMole.close();
			if (mTListSocketMoles.contains(socketMole)) {
				mTListSocketMoles.remove(socketMole);
				Log.i(TAG, "hdb---_close---mListSocketMoles.size():"+mTListSocketMoles.size());
				if (mTListSocketMoles.size() == 0) {
//					if (mediaEncoder != null) {
//						mediaEncoder.stopScreen();
////						mediaEncoder.release();
//						mediaEncoder = null;
//					}
//					if (mAudioCoder != null) {
//						mAudioCoder.stopRecord();
//						mAudioCoder = null;
//					}
//					if (mAudioRecord != null) {
//						mAudioRecord.stop();
//						mAudioRecord = null;
//					}
					startEncode = false;
				}
			}
			socketMole = null;
		}
		if (mConnCallBack != null) {
			mConnCallBack.onTouchConnectFail();
		}
		setConnection(false);
	}
	private void connectFail(){
		_close(mSocketMole);
		
	}
	
	private TSocketModel tmpModel = new TSocketModel();
	private synchronized void saveSocketModle(){
		boolean connectOver = connectOver();
		Log.i(TAG, "hdb----connectOver:"+connectOver);
//		Log.i("wxy", "--------------------connectOver-----------------------:"+connectOver);
		receiverTouchData();
		if (mConnCallBack != null) {
			mConnCallBack.onTouchConnectSuccess();
		}
		if (connectOver) {
			tmpModel = mSocketMole;
			Log.i(TAG, "hdb----connectOver:"+connectOver+"   "+mTListSocketMoles.contains(tmpModel));
			if (mTListSocketMoles.contains(tmpModel)) {
				return;
			}
			mTListSocketMoles.add(tmpModel);
//			if (mConnCallBack != null) {
//				mConnCallBack.onConnectSuccess();
//			}
//			receiverTouchData();
//			startAudioCoder();
//			if (mListSocketMoles.size() == 1) {
//				checkAlive();
//			}
			mSocketMole = null;
			if (startEncode) {
				return;
			}
			startEncode = true;
			
			
			
		}
	}
	
	private void checkAlive(){
		ThreadPoolManager.getInstance().execute(new Runnable() {
			
			@Override
			public void run() {
				while (mTListSocketMoles.size() > 0) {
					for (int i = 0; i < mTListSocketMoles.size(); i++) {
						TSocketModel socketMole = mTListSocketMoles.get(i);
						if (socketMole != null && socketMole.isConnect()) {
							try {
								LogUtils.e(TAG, "hdb-----sendUrgentData---");
								Socket touchT = socketMole.getTouchSocket();
								touchT.sendUrgentData(0xFF);
							} catch (IOException ex) {
								ex.printStackTrace();
								LogUtils.e(TAG, "hdb-----checkAlive--error-");
								_close(socketMole);
							}
						}
					}
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					
				}
			}
		});
	}
	
	private boolean startEncode = false;

	private AudioCoder mAudioCoder;
	private AudioRecord mAudioRecord;
	public boolean getStartEncode(){
		return startEncode;
	}
	private boolean connectOver(){
		if (null != mSocketMole && mSocketMole.isConnect()) {
			return true;
		}
		return false;
	}


	
	
	public void close(String ip) {
		Log.i("wxy2","------------------close------------------");
		TSocketModel moleByIp = findSocketMoleByIp(ip);
		_close(moleByIp);
	}
	
	private TSocketModel findSocketMoleByIp(String ip){
		if (mTListSocketMoles.size() > 0) {
			for (int i = 0; i < mTListSocketMoles.size(); i++) {
				TSocketModel socketMole = mTListSocketMoles.get(i);
				Socket touchSocket = socketMole.getTouchSocket();
				String hostAddress = touchSocket.getInetAddress().getHostAddress();
				if (ip.equalsIgnoreCase(hostAddress)) {
					return socketMole;
				}
			}
		}
		
		return null;
	}
	
	public boolean findConnectedByIp(String ip){
		if (mTListSocketMoles.size() > 0) {
			for (int i = 0; i < mTListSocketMoles.size(); i++) {
				TSocketModel socketMole = mTListSocketMoles.get(i);
				Socket touchSocket = socketMole.getTouchSocket();
				String hostAddress = touchSocket.getInetAddress().getHostAddress();
				if (ip.equalsIgnoreCase(hostAddress)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	
	public boolean hasConnect(){
		Log.i("wxy","-------------------size------------------------:::"+getConnection());
		return  (mTListSocketMoles.size() > 0) ? true : false;
		
	}
	
	public void closeAll(){
		if (mTListSocketMoles.size() > 0) {
			for (int i = 0; i < mTListSocketMoles.size(); i++) {
				TSocketModel socketMole = mTListSocketMoles.get(i);
				_close(socketMole);
			}
		}
	}
	

}
