package com.action.screenmirror.model;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;

import com.action.screenmirror.SendServer;
import com.action.screenmirror.bean.DeviceInfo;
import com.action.screenmirror.interf.IConnectRequest;
import com.action.screenmirror.interf.IReceiverData;
import com.action.screenmirror.interf.IRequsetConnect;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.DeviceInformation;
import com.action.screenmirror.utils.IpUtils;
import com.action.screenmirror.utils.LogUtils;
import com.action.screenmirror.utils.ThreadPoolManager;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class UdpSocketSearcher {

	private String ipAddress;

	private InetAddress broadcastAddress;
	private InetAddress broadcastAddress6;
	private volatile boolean receiver = false;
	private MulticastSocket multicastSocket;
	private MulticastSocket multicastSocketIpv6;
	private static final String IPV6_BROADCAST_ADDR = "FF02::1";

	private static final String TAG = "UdpSocketSearcher";

	private static final long UPDATE_DEVICES_DELAY_OUT = 6000;

	private Context mContext;
	// private boolean isNetConnet = true;
	private boolean isReceiver;

	public UdpSocketSearcher(Context context, boolean isReceiver) {
		mContext = context;
		this.isReceiver = isReceiver;
		// startReceiverUdpBrodcast();
	}

	public void resetScanAndLink() {
		mHandler.removeMessages(Config.HandlerGlod.GET_BROADCASTADDRESS);
		// closeConnect();
		stopReceiverUdpBrodcast();
		SystemClock.sleep(100);
		startReceiverUdpBrodcast();
	}

	private boolean isIpAddress() {
		String hostAddress = broadcastAddress.getHostAddress();
		String[] fristIP = hostAddress.split("\\.");
		LogUtils.i("hdb------hostAddress:" + hostAddress + "  fristIP:" + fristIP);
		if (ipAddress.equalsIgnoreCase("0.0.0.0")) {
			return false;
		}
		if (ipType == IpUtils.IP_TYPE_IPV46) {
			return true;
		}
		String[] splitIp = ipAddress.split("\\.");
		LogUtils.i("hdb------fristIP:" + fristIP.length + "  splitIp:" + splitIp.length);
		if (fristIP != null && fristIP.length > 0 && splitIp != null && splitIp.length > 0
				&& fristIP[0].equalsIgnoreCase(splitIp[0])) {

			return true;
		}
		return false;
	}

	private int ipType = IpUtils.IP_TYPE_NORMAL;

	private ReceiverBroadPackageIpv4Runnable ipv4Runnable;

	private ReceiverBroadPackageIpv6Runnable ipv6Runnable;

	public void startReceiverUdpBrodcast() {
		Log.i(TAG,"wxy-------startReceiverUdpBrodcast");

		ipType = IpUtils.getLocalIpType();
		InetAddress localIp = IpUtils.getLocalIp();
		if (localIp != null) {
			ipAddress = localIp.getHostAddress();
		}
		if (localIp == null || ipAddress == null || ipType == IpUtils.IP_TYPE_NORMAL) {
			Log.e(TAG, "hdb------localIp:" + localIp + "  ipAddress:" + ipAddress + "  ipType:" + ipType);
			if (ipAddress != null) {
				String[] split = ipAddress.split("%");
				ipAddress = split[0];
				Log.i(TAG, "hdb----split--localIp:" + localIp + "  ipAddress:" + ipAddress);
			}
			mHandler.removeMessages(Config.HandlerGlod.GET_BROADCASTADDRESS);
			mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.GET_BROADCASTADDRESS,
					Config.HandlerGlod.GET_BROADCASTADDRESS_DELAY);
			return;
		}
	//	stopReceiverUdpBrodcast();
		if ((ipType & IpUtils.IP_TYPE_IPV4) == IpUtils.IP_TYPE_IPV4) {// has
																		// ipv4
																		// addr
			try {
				broadcastAddress = IpUtils.getBroadcastAddress();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if (broadcastAddress == null || ipAddress == null || !isIpAddress()) {
				Log.i(TAG, "hdb------broadcastAddress:" + broadcastAddress + "  ipAddress:" + ipAddress);
				mHandler.removeMessages(Config.HandlerGlod.GET_BROADCASTADDRESS);
				mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.GET_BROADCASTADDRESS,
						Config.HandlerGlod.GET_BROADCASTADDRESS_DELAY);
				return;
			}
			try {
				if (multicastSocket == null) {
					
					multicastSocket = new MulticastSocket(Config.PortGlob.MULTIPORT);
				}
				Log.i("wxy", "---------------------broadcastAddress-----------------------------::::"+broadcastAddress);
				multicastSocket.joinGroup(broadcastAddress);
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "hdb-----IP_TYPE_IPV4--joinGroup-error-");
			}

			// multicastSocket.setLoopbackMode(true);
		}

		if ((ipType & IpUtils.IP_TYPE_IPV6) == IpUtils.IP_TYPE_IPV6) { // has
																		// ipv6
																		// addr
			LogUtils.i("hdb----isIPv6---");
			try {
				if (multicastSocketIpv6 == null) {
					multicastSocketIpv6 = new MulticastSocket(Config.PortGlob.MULTIPORT_IPV6);
				}
				broadcastAddress6 = InetAddress.getByName(IPV6_BROADCAST_ADDR);
				multicastSocketIpv6.setNetworkInterface(IpUtils.getIpv6NetworkInterface());
				InetSocketAddress socketAddress = new InetSocketAddress(broadcastAddress6,
						Config.PortGlob.MULTIPORT_IPV6);
				multicastSocketIpv6.joinGroup(socketAddress, IpUtils.getIpv6NetworkInterface());
			} catch (Exception e) {
				e.printStackTrace();
				Log.i(TAG, "hdb-----IP_TYPE_IPV6--joinGroup-error-");
			}

		}

		LogUtils.i(TAG, "hdb----ipAddress:" + ipAddress + "  broadcastAddress:" + broadcastAddress);
		if (ipAddress != null) {
			setStop(false);
			mHandler.sendEmptyMessage(Config.HandlerGlod.SEND_BROADCAST);
			mHandler.sendEmptyMessage(Config.HandlerGlod.CHECK_DEVICE_LIVE);
			receiverBroadPackage();
		}
		
        if (isReceiver) {
		    receiverConnectInfo();
		}

		
	}

	private void receiverBroadPackage() {
		LogUtils.i(TAG, "hdb-----receiverBroadPackage-----");
		IpUtils.openWifiBrocast(mContext); // for some phone can
		if (multicastSocket != null) {
			if (null == ipv4Runnable) {
				ipv4Runnable = new ReceiverBroadPackageIpv4Runnable();
			}
			ThreadPoolManager.getInstance().execute(ipv4Runnable);
		}

		if (multicastSocketIpv6 != null) {
			if (null == ipv6Runnable) {
				ipv6Runnable = new ReceiverBroadPackageIpv6Runnable();
			}
			ThreadPoolManager.getInstance().execute(ipv6Runnable);
		}
	}

	private void sendData() {
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
			    _sendData();				
			}
		});
	}

	protected void _sendData() {
		try {
			Log.i(TAG,"wxy-------_sendData");
			String ip4Address = "";
			if ((ipType & IpUtils.IP_TYPE_IPV4) == IpUtils.IP_TYPE_IPV4 && multicastSocket != null) {

				ArrayList<InetAddress> localIp4List = IpUtils.getLocalIp4List();
				ip4Address = localIp4List.get(0).getHostAddress();
				byte[] data;
				if (isReceiver) {
					data = ("receiverip:" + ip4Address + ":" + DeviceInformation.getSerialNumber(mContext)).getBytes();
				} else {
					data = ("sendip:" + ip4Address + ":" + DeviceInformation.getSerialNumber(mContext)).getBytes();
				}

				broadcastAddress = IpUtils.getBroadcastAddress();
				DatagramPacket pack = new DatagramPacket(data, data.length, broadcastAddress, Config.PortGlob.MULTIPORT);
				multicastSocket.send(pack);
				Log.i(TAG, "hdb-----sendBack  ipv4--"+new String(data));
			}
			if ((ipType & IpUtils.IP_TYPE_IPV6) == IpUtils.IP_TYPE_IPV6 && multicastSocketIpv6 != null) {
				String ip6Address = "";
				ArrayList<InetAddress> localIp6List = IpUtils.getLocalIp6List();
				if (localIp6List.size() == 1) {
					ip6Address = localIp6List.get(0).getHostAddress();
				} else {
					ip6Address = localIp6List.get(1).getHostAddress();
				}
				byte[] data;
				if (isReceiver) {
					data = ("receiver6ip:&&:" + ip6Address + ":&&:" + ip4Address + ":&&:" + DeviceInformation
							.getSerialNumber(mContext)).getBytes();
				} else {
					data = ("send6ip:&&:" + ip6Address + ":&&:" + ip4Address + ":&&:" + DeviceInformation
							.getSerialNumber(mContext)).getBytes();
				}

				broadcastAddress6 = InetAddress.getByName(IPV6_BROADCAST_ADDR);
				DatagramPacket pack = new DatagramPacket(data, data.length, broadcastAddress6,
						Config.PortGlob.MULTIPORT_IPV6);
				multicastSocketIpv6.send(pack);
				 Log.i(TAG, "hdb-----sendBack  ipv6--"+new String(data));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopReceiverUdpBrodcast() {
		//LogUtils.i(TAG, "hdb------ipType:" + ipType);
		Log.i(TAG,"wxy-------stopReceiverUdpBrodcast--multicastSocket:"+multicastSocket);
		receiver = false;
		setStop(true);
		removeReceiverRunnable();
		if (multicastSocket != null) {
			if ((ipType & IpUtils.IP_TYPE_IPV4) == IpUtils.IP_TYPE_IPV4 && broadcastAddress != null) {
				try {
					multicastSocket.leaveGroup(broadcastAddress);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			multicastSocket.close();
			multicastSocket = null;
		}
		if (multicastSocketIpv6 != null) {

			if ((ipType & IpUtils.IP_TYPE_IPV6) == IpUtils.IP_TYPE_IPV6 && broadcastAddress6 != null) {
				try {
					multicastSocketIpv6.leaveGroup(broadcastAddress6);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			multicastSocketIpv6.close();
			multicastSocketIpv6 = null;
		}

	//	closeRequestSocket();
		closeAllSocket();

		mHandler.removeMessages(Config.HandlerGlod.SEND_BROADCAST);
		mHandler.removeMessages(Config.HandlerGlod.CHECK_IPTYPE);
		mHandler.removeMessages(Config.HandlerGlod.GET_BROADCASTADDRESS);
		mHandler.removeMessages(Config.HandlerGlod.CHECK_DEVICE_LIVE);
	}

	private void removeReceiverRunnable() {
		if (ipv4Runnable != null) {
			ThreadPoolManager.getInstance().remove(ipv4Runnable);
			ipv4Runnable = null;
		}
		if (ipv6Runnable != null) {
			ThreadPoolManager.getInstance().remove(ipv6Runnable);
			ipv6Runnable = null;
		}
	}

	public void checkIptype() {
		int localIpType = IpUtils.getLocalIpType();
		if (localIpType != ipType && localIpType == IpUtils.IP_TYPE_IPV46) {// restart
																			// search
																			// model
			LogUtils.i(TAG, "hdb-------CHECK_IPTYPE---localIpType:" + localIpType);
			stopReceiverUdpBrodcast();
			SystemClock.sleep(100);
			mHandler.removeMessages(Config.HandlerGlod.GET_BROADCASTADDRESS);
			Log.i(TAG,"wxy-------checkIptype");
			startReceiverUdpBrodcast();
		}

	}

	private class ReceiverBroadPackageIpv4Runnable implements Runnable {

		@Override
		public void run() {
			try {
				receiver = true;
				while (receiver) {
					if (multicastSocket != null) {
						// LogUtils.i(TAG, "hdb----ipAddress:" + ipAddress);

						byte[] data = new byte[80];
						DatagramPacket pack = new DatagramPacket(data, data.length);
						multicastSocket.receive(pack);
						String receiverData = new String(pack.getData(), pack.getOffset(), pack.getLength());
						LogUtils.i(TAG, "hdb-------receive:" + receiverData + "  ipAddress:" + ipAddress);
						if (receiverData != null) {
							if ((isReceiver && receiverData.startsWith(Config.ActionKey.SEND_PRE))
									|| (!isReceiver && receiverData.startsWith(Config.ActionKey.RECEIVER_PRE))) {
								handleReceiverData(receiverData);
							}
						}

					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
			if (receiver) {// for maybe bug
				LogUtils.e(TAG, "hdb--ReceiverBroadPackageIpv4Runnable-receiver:" + receiver);
				mHandler.removeMessages(Config.HandlerGlod.GET_BROADCASTADDRESS);
				mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.GET_BROADCASTADDRESS,
						Config.HandlerGlod.GET_BROADCASTADDRESS_DELAY);
			}
		}

	}

	private class ReceiverBroadPackageIpv6Runnable implements Runnable {

		@Override
		public void run() {
			try {
				receiver = true;
				while (receiver) {
					if (multicastSocketIpv6 != null) {
						// LogUtils.i(TAG, "hdb----ipAddress:" + ipAddress);

						byte[] data = new byte[80];
						DatagramPacket pack = new DatagramPacket(data, data.length);
						multicastSocketIpv6.receive(pack);
						String receiverData = new String(pack.getData(), pack.getOffset(), pack.getLength());
						LogUtils.i(TAG, "hdb----multicastSocketIpv6---receive:" + receiverData + "  ipAddress:"
								+ ipAddress);
						// if (receiverData != null &&
						// receiverData.startsWith(Config.ActionKey.CLIENT_IP_KEY))
						// {
						// sendBack(receiverData);
						//
						// }

						if (receiverData != null) {
							if ((isReceiver && receiverData.startsWith(Config.ActionKey.SEND_PRE))
									|| (!isReceiver && receiverData.startsWith(Config.ActionKey.RECEIVER_PRE))) {
								handleReceiverData(receiverData);
								
							}
						}

					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
			if (receiver) {// for maybe bug
				LogUtils.e(TAG, "hdb--ReceiverBroadPackageIpv6Runnable---receiver:" + receiver);
				mHandler.removeMessages(Config.HandlerGlod.GET_BROADCASTADDRESS);
				mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.GET_BROADCASTADDRESS,
						Config.HandlerGlod.GET_BROADCASTADDRESS_DELAY);
			}
		}

	}

	private void handleReceiverData(String back) {
		long cureentTime = SystemClock.uptimeMillis();
		String remoteName;
		if (back.startsWith("receiverip:") || back.startsWith("sendip:")) {
			String[] split = back.split(":");
			if (split.length == 3) {
				String remoteServerIp = split[1];
				remoteName = split[2];
				DeviceInfo deviceInfo = getDevice(remoteServerIp);
				if (deviceInfo == null) {
					DeviceInfo deviceInfo1 = new DeviceInfo(remoteServerIp, remoteName);
					deviceInfo1.setTime(cureentTime);
					deviceList.add(deviceInfo1);
					onReceiverDevice();
				} else {
					deviceInfo.setTime(cureentTime);
				}
			}

		} else if (back.startsWith("receiver6ip:") || back.startsWith("send6ip:")) {
			String[] split = back.split(":&&:");
			String remoteServerIp = split[2];
			remoteName = split[3];
			String ipv6Address = split[1];
			LogUtils.i(TAG, "hdb--------remoteServerIp:" + remoteServerIp + "  remoteName:" + remoteName + "  ipv6:"
					+ ipv6Address);
			DeviceInfo deviceInfo = getDevice(remoteServerIp);
			DeviceInfo device6Info = getDevice(ipv6Address);
			if (deviceInfo == null && device6Info == null) {
				DeviceInfo deviceInfo1 = new DeviceInfo(remoteServerIp, remoteName);
				deviceInfo1.setIp6Address(ipv6Address);
				deviceInfo1.setTime(cureentTime);
				deviceList.add(deviceInfo1);
				onReceiverDevice();
			} else {
				if (deviceInfo != null && device6Info == null) {
					deviceInfo.setTime(cureentTime);
					deviceInfo.setIp6Address(ipv6Address);
				} else if (deviceInfo == null && device6Info != null) {
					device6Info.setTime(cureentTime);
					device6Info.setIpAddress(remoteServerIp);
				} else if (deviceInfo != null && device6Info != null) {
					deviceInfo.setTime(cureentTime);
				}
			}
		}
	}

	private ArrayList<DeviceInfo> deviceList = new ArrayList<DeviceInfo>();

	private DeviceInfo getDevice(String ip) {
		if (deviceList.size() == 0)
			return null;
		for (int i = 0; i < deviceList.size(); i++) {
			if (ip.equalsIgnoreCase(deviceList.get(i).getIpAddress())
					|| ip.equalsIgnoreCase(deviceList.get(i).getIp6Address())) {
				return deviceList.get(i);
			}
		}
		return null;
	}

	private Object mLock = new Object();

	ArrayList<DeviceInfo> tempList = null;

	private void updateDevices() {
		synchronized (mLock) {
			Log.i("wxy","--------------------updateDevices---------------------count::::"+deviceList.size());
			if (deviceList.size() <= 0)
				return;
			boolean needMove = false;
			if (tempList == null) {
				tempList = new ArrayList<DeviceInfo>();
			}
			tempList.clear();
			for (int i = 0; i < deviceList.size(); i++) {
//				LogUtils.e("hdb---UPDATE_DEVICES---updatetime:"
//						+ (SystemClock.uptimeMillis() - deviceList.get(i).getTime()));
				Log.i("wxy","-------------updatetime::::::"+ (SystemClock.uptimeMillis() - deviceList.get(i).getTime())+ "--------------------");
				if ((SystemClock.uptimeMillis() - deviceList.get(i).getTime()) > UPDATE_DEVICES_DELAY_OUT) {
					needMove = true;
					tempList.add(deviceList.get(i));
				}
			}
			if (needMove) {
//				LogUtils.e("hdb-----needMove------");
				deviceList.removeAll(tempList);
				tempList.clear();
				onReceiverDevice();
			}
		}

	}

	private DatagramSocket mDatagramSocket;
	private IRequsetConnect mIRequsetConnect;

	public void removeConnectTMessages(){
		mHandler.removeMessages(Config.HandlerGlod.REQUEST_CONNECT_TIMEOUT);
	}
	public void sendConnectInfo(final String ip, final String localIp,IRequsetConnect iRequsetConnect) {
		mIRequsetConnect = iRequsetConnect;
		mHandler.removeMessages(Config.HandlerGlod.REQUEST_CONNECT_TIMEOUT);
		mHandler.sendEmptyMessageDelayed(Config.HandlerGlod.REQUEST_CONNECT_TIMEOUT, Config.HandlerGlod.REQUEST_CONNECT_TIMEOUT_DELAY);
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				try {
//					if (mDatagramSocket == null) {
//						mDatagramSocket = new DatagramSocket(Config.PortGlob.UDPPACKETPORT);
//					}
					if (mDatagramSocket != null) {
						mDatagramSocket.close();
						mDatagramSocket = null;
					}
					if(mDatagramSocket==null){
						mDatagramSocket = new DatagramSocket(null);
						mDatagramSocket.setReuseAddress(true);
						mDatagramSocket.bind(new InetSocketAddress(Config.PortGlob.UDPPACKETPORT));
						}
					byte[] data = (CONNECTPRE + localIp).getBytes();
					DatagramPacket mDatagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ip),
							Config.PortGlob.UDPPACKETPORT);
					Log.i(TAG, "----------sendConnectInfo---data:"+new String(data));
					mDatagramSocket.send(mDatagramPacket);
					setStop(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.i("wxy", "------------------sendConnectInfo-------------------");
			//	closeRequestSocket();
			}
		});

	}
	
	private static String CONNECTPRE = "connectip:";
	private DatagramSocket mReceiverDatagramSocket;

	private void receiverConnectInfo() {
		Log.i(TAG, "hdb--receiverConnectInfo---");
		ThreadPoolManager.getInstance().execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					if (mReceiverDatagramSocket != null) {
						mReceiverDatagramSocket.close();
						mReceiverDatagramSocket = null;
					}
//					mReceiverDatagramSocket = new DatagramSocket(Config.PortGlob.UDPPACKETPORT);
					if(mReceiverDatagramSocket==null){
						mReceiverDatagramSocket = new DatagramSocket(null);
						mReceiverDatagramSocket.setReuseAddress(true);
						mReceiverDatagramSocket.bind(new InetSocketAddress(Config.PortGlob.UDPPACKETPORT));
						}
					while (isReceiver) {
						byte[] receiverData = new byte[128];
						DatagramPacket pack = new DatagramPacket(receiverData, receiverData.length);
						mReceiverDatagramSocket.receive(pack);

						byte[] data = pack.getData();
						
						Log.i(TAG, "hdb--receiverConnectInfo--length:" + data.length 
								+"  length:" + pack.getLength()+"  Offset:" + pack.getOffset());
						String string = new String(data);
						if (string != null && string.startsWith(CONNECTPRE)) {
							String ip = string.substring(CONNECTPRE.length(), pack.getLength());
							Log.i(TAG, "--------------substring2:"+ip+"  :"+ip.length());
							if (iConnectRequest != null) {
								Log.i("wxy", "----------receiverConnectInfo----substring2:"+ip+"  :"+ip.length());
								iConnectRequest.onRequestConnect(ip);
							}
							setStop(false);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
//				if (mReceiverDatagramSocket != null) {
//					mReceiverDatagramSocket.close();
//					mReceiverDatagramSocket = null;
//				}	
			}
		});
		
	}
	
	
	private DatagramSocket mStopDatagramSocket;
	private static String STOPDISCONNECTION = "stopdisconnection";
	private static  boolean isStop = true;
	public boolean getStop() {
		return isStop;
	}
	public void setStop( boolean isStop) {
		this.isStop = isStop;
	}

	public void sendDisconnectInfo(final String ip, final String serverIp) {
		Log.i("wxy2", "-----------------sendDisconnectInfo----------ip:"+serverIp+"  serverIp:"+serverIp);
		ThreadPoolManager.getInstance().execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					if (mStopDatagramSocket != null) {
						mStopDatagramSocket.close();
						mStopDatagramSocket = null;
					}
					boolean isSend = true;
				//	    mStopDatagramSocket = new DatagramSocket(Config.PortGlob.STOP_DISCONNECTION);
					if (mStopDatagramSocket == null) {
						 mStopDatagramSocket = new DatagramSocket(null);
					    mStopDatagramSocket.setReuseAddress(true);
					    mStopDatagramSocket.bind(new InetSocketAddress(Config.PortGlob.STOP_DISCONNECTION));
					}	
					if (isSend) {
					    byte[] data = (STOPDISCONNECTION + ip).getBytes();
						DatagramPacket mDatagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(serverIp),
								Config.PortGlob.STOP_DISCONNECTION);
//						Log.i(TAG, "wxy---sendConnectInfo---data:"+new String(data));
						mStopDatagramSocket.send(mDatagramPacket);
						isSend  = false;
						stopReceiverUdpBrodcast();
						setStop(true);
						Log.i(TAG, "wxy---sendConnectInfo---data:"+ip);
					}
					   

				} catch (Exception e) {	
					setStop(false);
					e.printStackTrace();
				}
//				if (mStopDatagramSocket != null) {
//					mStopDatagramSocket.close();
//					mStopDatagramSocket = null;
//				}
			}
		});		
	}

	
//
//	private DatagramSocket mTouchConnectionSucessfulDatagramSocket;
//	private static String TOUCHCONNECTIONSUCESSFUL = "touchconnectionsucessful";
//	public void sendTouchConnectionSucessfulInfo(final String ip) {
//		Log.i("wxy", "-----------------sendTouchConnectionSucessfulInfo--------------------");
//		ThreadPoolManager.getInstance().execute(new Runnable() {
//			
//			@Override
//			public void run() {
//				try {
//					if (mTouchConnectionSucessfulDatagramSocket != null) {
//						mTouchConnectionSucessfulDatagramSocket.close();
//						mTouchConnectionSucessfulDatagramSocket = null;
//					}
////					   mTouchConnectionSucessfulDatagramSocket = new DatagramSocket(Config.PortGlob.TOUCH_CONNECTION_SUCESSFUL);
//					if (mTouchConnectionSucessfulDatagramSocket == null) {
//						   mTouchConnectionSucessfulDatagramSocket = new DatagramSocket(null);
//						   mTouchConnectionSucessfulDatagramSocket.setReuseAddress(true);
//						   mTouchConnectionSucessfulDatagramSocket.bind(new InetSocketAddress(Config.PortGlob.TOUCH_CONNECTION_SUCESSFUL));
//					}	
//					    byte[] data = (TOUCHCONNECTIONSUCESSFUL).getBytes();
//						DatagramPacket mDatagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ip),
//								Config.PortGlob.TOUCH_CONNECTION_SUCESSFUL);
////						Log.i(TAG, "wxy---sendConnectInfo---data:"+new String(data));
//						mTouchConnectionSucessfulDatagramSocket.send(mDatagramPacket);
//						setStop(false);
//						Log.i("wxy", "---------------isStop----------------:::"+isStop);
//
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				if (mTouchConnectionSucessfulDatagramSocket != null) {
//					mTouchConnectionSucessfulDatagramSocket.close();
//					mTouchConnectionSucessfulDatagramSocket = null;
//					}
//				
//				
//			}
//		});		
//	}
	
	
	private void closeRequestSocket(){
		if (mDatagramSocket != null) {
			mDatagramSocket.close();
			mDatagramSocket = null;
		}
	}

	
	private void closeAllSocket(){
		if (mDatagramSocket != null) {
			mDatagramSocket.close();
			mDatagramSocket = null;
		}
		if (mStopDatagramSocket != null) {
			mStopDatagramSocket.close();
			mStopDatagramSocket = null;
		}
//		if (mTouchConnectionSucessfulDatagramSocket != null) {
//			mTouchConnectionSucessfulDatagramSocket.close();
//			mTouchConnectionSucessfulDatagramSocket = null;
//		}
		if (mReceiverDatagramSocket != null) {
			mReceiverDatagramSocket.close();
			mReceiverDatagramSocket = null;
		}	
	}
	private Handler mHandler = new SearcherHandler(this);

	public static class SearcherHandler extends Handler {
		WeakReference<UdpSocketSearcher> weakReference;

		public SearcherHandler(UdpSocketSearcher mSearcher) {
			weakReference = new WeakReference<UdpSocketSearcher>(mSearcher);

		}

		@Override
		public void handleMessage(Message msg) {
			UdpSocketSearcher mSearcher = weakReference.get();
			if (mSearcher == null)
				return;

			switch (msg.what) {

			case Config.HandlerGlod.GET_BROADCASTADDRESS:
				LogUtils.i(TAG, "hdb-------GET_BROADCASTADDRESS---");
				removeMessages(Config.HandlerGlod.GET_BROADCASTADDRESS);
				mSearcher.startReceiverUdpBrodcast();
				break;
			case Config.HandlerGlod.CHECK_IPTYPE:
				mSearcher.checkIptype();
				break;
			case Config.HandlerGlod.SEND_BROADCAST:
				Log.i("wxy","-------SEND_BROADCAST-------------:::::"+isStop);
				if (isStop) {
					mSearcher.stopReceiverUdpBrodcast();	
				}else{
					mSearcher.sendData();
					removeMessages(Config.HandlerGlod.SEND_BROADCAST);
					sendEmptyMessageDelayed(Config.HandlerGlod.SEND_BROADCAST, Config.HandlerGlod.SEND_BROADCAST_DELAY);
				}
				
				break;
			case Config.HandlerGlod.CHECK_DEVICE_LIVE:
//				LogUtils.e(TAG, "hdb-------CHECK_DEVICE_LIVE---");
				Log.i("wxy","-------CHECK_DEVICE_LIVE-------------:::::"+isStop);
				mSearcher.updateDevices();
				removeMessages(Config.HandlerGlod.CHECK_DEVICE_LIVE);
				sendEmptyMessageDelayed(Config.HandlerGlod.CHECK_DEVICE_LIVE,
						Config.HandlerGlod.CHECK_DEVICE_LIVE_DELAY);
				break;
			case Config.HandlerGlod.REQUEST_CONNECT_TIMEOUT:
				if (mSearcher.mIRequsetConnect != null) {
					mSearcher.mIRequsetConnect.connectTimeOut();
				}
				break;

			default:
				break;
			}

		}

	}
	
	private synchronized void onReceiverDevice(){
		if (iReceiverData != null) {
			iReceiverData.onReveiver(deviceList);
		}
		if (iConnectRequest != null) {
			iConnectRequest.onReveiver(deviceList);
		}
	}

	private IReceiverData iReceiverData;

	public void setOnReceiverDataListener(IReceiverData iReceiverData) {
		this.iReceiverData = iReceiverData;
	}
	
	private IConnectRequest iConnectRequest;

	public void setOnReceiverDataListener(IConnectRequest iConnectRequest) {
		this.iConnectRequest = iConnectRequest;
	}

}
