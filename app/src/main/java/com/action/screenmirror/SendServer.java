package com.action.screenmirror;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.action.screenmirror.interf.IWifiState;
import com.action.screenmirror.model.TcpSocketServer;
import com.action.screenmirror.model.TcpSocketServer.ConnectCallBack;
import com.action.screenmirror.model.UdpSocketSearcher;
import com.action.screenmirror.model.UdpSocketSend;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.ThreadPoolManager;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjection;
import android.os.IBinder;
import android.util.Log;

public class SendServer extends Service{

	private static final String TAG = "SendServer";

	private TcpSocketServer tServer;
	private NetChangeReceiver mNetChangeReceiver;
	
	private static SendServer instance;

	private UdpSocketSend uSend;
	private UdpSocketSearcher mSearcher;
	public static SendServer getInstance(){
		return instance;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		instance = this;
		uSend = new UdpSocketSend();
		registNetChangeReceiver();	
		mSearcher = new UdpSocketSearcher(instance, true);
//		isDisconnectReceiver = true ;
		initTouchSocketService();
//		if (isDisconnectReceiver) {
//			Log.i("wxy2","------------onStartCommand-------------------");
//			receiverDisconnectInfo();
//		}
		receiverDisconnectInfo();
		Log.i("wxy","------------onStartCommand-------------------");
		return START_STICKY;
	}
	
	
	private void initTouchSocketService() {
		// TODO Auto-generated method stub
		//add touch send 
		if (tServer == null) {
			tServer = new TcpSocketServer(SendActivity.getInstance());						
		}
		tServer.initServer();	
	}


	private boolean isDisconnectReceiver ;
	private DatagramSocket mStopDatagramSocket;
	private static String STOPDISCONNECTION = "stopdisconnection";
	public void receiverDisconnectInfo() {
		Log.i("wxy", "-----------------receiverDisconnectInfo--------------------");
		ThreadPoolManager.getInstance().execute(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					if (mStopDatagramSocket != null) {
						mStopDatagramSocket.close();
						mStopDatagramSocket = null;
					}
					if (mStopDatagramSocket == null) {
						mStopDatagramSocket = new DatagramSocket(null);
					    mStopDatagramSocket.setReuseAddress(true);
					    mStopDatagramSocket.bind(new InetSocketAddress(Config.PortGlob.STOP_DISCONNECTION));
					}
					
//					mStopDatagramSocket = new DatagramSocket(Config.PortGlob.STOP_DISCONNECTION);
					isDisconnectReceiver = true ;
					while (isDisconnectReceiver) {
						byte[] receiverData = new byte[128];
						DatagramPacket pack = new DatagramPacket(receiverData, receiverData.length);
						mStopDatagramSocket.receive(pack);
						byte[] data = pack.getData();
						String string = new String(data);
						if (string != null && string.startsWith(STOPDISCONNECTION)) {
							String ip = string.substring(STOPDISCONNECTION.length(), pack.getLength());
							Log.i("wxy", "--------111-----------substring2:"+ip);	
//							isDisconnectReceiver = false;
//							stopEncode();
							closeConnect(ip);
						}
						
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
//				if (mStopDatagramSocket != null) {
//					mStopDatagramSocket.close();
//					mStopDatagramSocket = null;
//				}
			}
		});	
	}



	
	public boolean getStartEncode(){
	   return uSend.hasStart();
	}
//	
//	public void close(String ip){
//		tSend.close(ip);
//	}
//	
	public void stopEncode(){
		uSend.stopEncode();
	}
	
	public void setIpAddr(String ip){
		if (uSend == null) {
			uSend = new UdpSocketSend();
		}
		uSend.setIpAddr(ip);
	}
	
	public void startReceord(MediaProjection mediaProjection) {
		uSend.startReceord(mediaProjection);
	}
	
	public boolean findConnectedByIp(String ip) {
		return tServer.findConnectedByIp(ip);
	}
	
	public boolean hasConnect(){
		Log.i("wxy","------------tServer-------------"+tServer);
		if (tServer == null) {
			return false;
		}
		return tServer.hasConnect();
	}
	
	public void setConnectCallBack(ConnectCallBack connCallBack) {
		if (tServer != null) {
			tServer.setConnectCallBack(connCallBack);
		}
	}
	

	
	private void registNetChangeReceiver(){
		
		if (mNetChangeReceiver == null) {
			mNetChangeReceiver = new NetChangeReceiver(new IWifiState() {
				
				@Override
				public void disconnect() {
					SendActivity send = SendActivity.getInstance();
					if (send != null) {
						send.onWifiDisconnect();					
					}
				}
				
				@Override
				public void connect() {
					SendActivity send = SendActivity.getInstance();
					if (send != null) {
						send.onWifiConnect();
					}
				}
			});
			
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		registerReceiver(mNetChangeReceiver, filter);
	}
	
	private void unregistNetChangeReceiver(){
		if (mNetChangeReceiver != null) {
			unregisterReceiver(mNetChangeReceiver);
			mNetChangeReceiver = null;
		}
	}
	
	public void close(){
		if (uSend != null) {
			uSend.stopEncode();
			uSend.closeSocket();
			uSend = null;
		}
		if (tServer != null) {
			tServer.releseServer();
		//	tServer = null;
		}
		mSearcher.stopReceiverUdpBrodcast();
	}
	
	public void closeConnect(String ip){
		if (uSend != null) {
			uSend.stopEncode();
			uSend.closeSocket();
			uSend = null;
		}
		if (tServer != null) {
			tServer.close(ip);
	//		tServer = null;
		}
		mSearcher.setStop(true);
		mSearcher.stopReceiverUdpBrodcast();
		/*if (mStopDatagramSocket != null) {
			mStopDatagramSocket.close();
			mStopDatagramSocket = null;
		}*/
	} 
	
	
	
	@Override
	public void onDestroy() {
		unregistNetChangeReceiver();
		Log.i("wxy","------------------Service onDestroy-------------------");
		super.onDestroy();
	}

}
