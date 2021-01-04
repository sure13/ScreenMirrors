package com.action.screenmirror;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.action.screenmirror.bean.DeviceAdapter;
import com.action.screenmirror.bean.DeviceInfo;
import com.action.screenmirror.interf.IConnectCallback;
import com.action.screenmirror.interf.IReceiverData;
import com.action.screenmirror.interf.IRequsetConnect;
import com.action.screenmirror.model.TcpSocketClient;
import com.action.screenmirror.model.TcpSocketServer;
import com.action.screenmirror.model.TcpSocketServer.ConnectCallBack;
import com.action.screenmirror.model.UdpSocketSearcher;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.IpUtils;
import com.action.screenmirror.utils.LogUtils;
import com.action.screenmirror.utils.ThreadPoolManager;

import android.R.bool;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class SendActivity extends Activity{
	
	private static final String TAG = "SendActivity";
	private static final int REQUEST_CODE = 1;
	private DeviceAdapter deviceAdapter;
	private ListView mListView;
	private UdpSocketSearcher mSearcher;
	private TcpSocketServer tServer;
	
	private static SendActivity instance;
	private boolean requestConnect = false;
	private int requestPosition = -1;
	
	public static SendActivity getInstance(){
		if (instance == null) {
			instance = new SendActivity();
		}
		return instance;
	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_send);
		instance = this;
		initView();
		mSearcher = new UdpSocketSearcher(this, false);
		startService(new Intent(SendActivity.this, SendServer.class));
		Log.i("wxy","--------------SendActivity onCreate---------------------");
	}

	
	private void initView() {

		mListView = findViewById(R.id.remote_device_list);
		wifiInfo = findViewById(R.id.tv_wifi_info);
		deviceAdapter = new DeviceAdapter(this);
		mListView.setAdapter(deviceAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				List<DeviceInfo> deviceInfos = deviceAdapter.getDeviceInfos();
				DeviceInfo deviceInfo = deviceInfos.get(position);
				final String remoteAddress = deviceInfo.getValidAddress();
				InetAddress localIp = IpUtils.getLocalIp();
				String localIpAddress = localIp.getHostAddress();
				requestPosition = position;
			    Log.i(TAG, "hdb--onItemClick----remoteAddress:"+remoteAddress+"   localIpAddress:"+localIpAddress+"  deviceInfos:"+deviceInfos.size());
			    mSearcher.sendConnectInfo(remoteAddress, localIpAddress,new IRequsetConnect() {
				
			     	@Override
				    public void connectTimeOut() {
				 	     requestConnect = false;
					     updateConnectState(DeviceInfo.CONNECT_TIMEOUT);
				          }
			     });
//			    isTouchConnectionReceiver = true;
//			    if (isTouchConnectionReceiver) {
//					receiverTouchConnectionInfo();
//					}
			    SendServer server = SendServer.getInstance();
			    if (server != null) {
				    server.setIpAddr(remoteAddress);
				}    
			   		
		}
		});	
	}

//	private boolean isTouchConnectionReceiver ;
//	private DatagramSocket mTouchConnectionDatagramSocket;
//	private static String TOUCHCONNECTIONSUCESSFUL = "touchconnectionsucessful";
//	public void receiverTouchConnectionInfo() {
//		Log.i("wxy", "-----------------receiverTouchConnectionInfo--------------------");
//		ThreadPoolManager.getInstance().execute(new Runnable() {
//			
//			@Override
//			public void run() {
//				
//				try {
//					if (mTouchConnectionDatagramSocket != null) {
//						mTouchConnectionDatagramSocket.close();
//						mTouchConnectionDatagramSocket = null;
//					}
//					mTouchConnectionDatagramSocket = new DatagramSocket(Config.PortGlob.TOUCH_CONNECTION_SUCESSFUL);
//					while (isTouchConnectionReceiver) {
//						byte[] receiverData = new byte[128];
//						DatagramPacket pack = new DatagramPacket(receiverData, receiverData.length);
//						mTouchConnectionDatagramSocket.receive(pack);
//						byte[] data = pack.getData();
//						String string = new String(data);
////						Log.i("wxy", "-----------------receiverDisconnectInfo-------data-------------"+string);
//						if (string != null && string.startsWith(TOUCHCONNECTIONSUCESSFUL)) {
////							Log.i("wxy", "-------------------substring2:");	
//						    onConnectSuccess();
//							isTouchConnectionReceiver = false;
//						}
//						
//					}
//
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				if (mTouchConnectionDatagramSocket != null) {
//					mTouchConnectionDatagramSocket.close();
//					mTouchConnectionDatagramSocket = null;
//				}
//			}
//		});
//		
//	}


	

	public void onConnectSuccess() {
		requestConnect = false;
		mSearcher.removeConnectTMessages();
		updateConnectState(DeviceInfo.CONNECT_SUCCESS);	
		requestRecord();
	}


	public void onConnectFail() {
		requestConnect = false;
		requestPosition = -1;
		mSearcher.removeConnectTMessages();
		List<DeviceInfo> deviceInfos = deviceAdapter.getDeviceInfos();
		updateListFail(deviceInfos);
	}
	
	private void updateConnectState(int state){
		List<DeviceInfo> deviceInfos = deviceAdapter.getDeviceInfos();
		if (deviceInfos.size() > 0 && requestPosition >= 0 && deviceInfos.size() > requestPosition) {
			DeviceInfo deviceInfo = deviceInfos.get(requestPosition);
			deviceInfo.setConnectState(state);
			updateList(deviceInfos);
		}
		
	}
	
	private MediaProjectionManager mProjectionManager;
	private TextView wifiInfo;
	private void requestRecord(){
		SendServer server = SendServer.getInstance();
		
		if (server == null || server.getStartEncode()) {
			return;
		}
		Log.i(TAG, "hdb---requestRecord--");
		mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
		if (mProjectionManager != null) {
			Intent createScreenCaptureIntent = mProjectionManager.createScreenCaptureIntent();
			startActivityForResult(createScreenCaptureIntent, REQUEST_CODE);
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(TAG, "hdb---requestRecord--requestCode:"+requestCode+"  resultCode:"+resultCode);
		if (requestCode == REQUEST_CODE) {
			MediaProjection mediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
			
			SendServer server = SendServer.getInstance();
			if (server != null) {
				server.startReceord(mediaProjection);
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		requestConnect = false;
		getWifiState();	
	}
	
	
	

	private void getWifiState(){
		ConnectivityManager mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = mConnectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiInfo.isConnected()) {
			onWifiConnect();
		} else {
			displayWifiInfo(false);
		}
	}
	
	private void updateList(final List<DeviceInfo> deviceInfos){
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	for (int i = 0; i < deviceInfos.size(); i++) {
        			DeviceInfo deviceInfo = deviceInfos.get(i);
        			String validAddress = deviceInfo.getValidAddress();
        			SendServer server = SendServer.getInstance();
        			if (server != null /*&& !Config.useUdp*/) {
        				boolean connected = server.findConnectedByIp(validAddress);
        				if (connected) {
        					deviceInfo.setConnectState(DeviceInfo.CONNECT_SUCCESS);
        				}
        			}
        		}
        		if (deviceAdapter != null) {
                    deviceAdapter.setDeviceInfos(deviceInfos);
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        });
	}
	
	private void updateListFail(final List<DeviceInfo> deviceInfos){
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	for (int i = 0; i < deviceInfos.size(); i++) {
        			DeviceInfo deviceInfo = deviceInfos.get(i);
        			String validAddress = deviceInfo.getValidAddress();
        			SendServer server = SendServer.getInstance();
        			if (server != null) {
        				boolean connected = server.findConnectedByIp(validAddress);
        				if (connected) {
        					deviceInfo.setConnectState(DeviceInfo.CONNECT_SUCCESS);
        				}else {
        					deviceInfo.setConnectState(DeviceInfo.CONNECT_FAIL);
						}
        			}
        		}
        		if (deviceAdapter != null) {
                    deviceAdapter.setDeviceInfos(deviceInfos);
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        });
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	//	onWifiDisconnect();
	}
	
	private void displayWifiInfo(boolean connect){
		if (connect) {
			wifiInfo.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);
		}else {
			wifiInfo.setVisibility(View.VISIBLE);
		    mListView.setVisibility(View.GONE);
	    }	
	}
	
	
	public void onWifiConnect(){
		displayWifiInfo(true);
		mSearcher.startReceiverUdpBrodcast();
		mSearcher.setOnReceiverDataListener(new IReceiverData() {
			
			@Override
			public void onReveiver(final ArrayList<DeviceInfo> deviceInfos) {
				Log.i(TAG, "hdb---onReveiver----deviceInfos:"+deviceInfos.size());
		        updateList(deviceInfos);
			}
		});
		
		SendServer server = SendServer.getInstance();
		Log.i(TAG, "wj---onWifiConnect--server:"+server);
		if (server != null){
			Log.i(TAG, "wj---setConnectCallBack--");
			server.setConnectCallBack(new ConnectCallBack() {
				
				@Override
				public void onTouchConnectSuccess() {
					Log.i(TAG, "wxy---onTouchConnectSuccess--");
					onConnectSuccess();
					
				}
				
				@Override
				public void onTouchConnectFail() {
					Log.i(TAG, "wxy---onConnectFail--");
					onConnectFail();
					
				}
			}); 
		}
	}
	
	public void onWifiDisconnect(){
		mSearcher.setOnReceiverDataListener(null);
		mSearcher.stopReceiverUdpBrodcast();
		displayWifiInfo(false);
	}
	
	
	@Override
	protected void onDestroy() {
			SendServer server = SendServer.getInstance();
			if ((server != null) && (!server.hasConnect())) {
				server.close();
				onWifiDisconnect();
				Log.i("wxy","-----------onDestroy----111----------");
			}
		super.onDestroy();
	}
}
