package com.action.screenmirror.model;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.media.projection.MediaProjection;
import android.util.Log;

import com.action.screenmirror.SendActivity;
import com.action.screenmirror.bean.TSocketModel;
import com.action.screenmirror.interf.IDisconnectCallback;
import com.action.screenmirror.interf.IOnEncodeData;
import com.action.screenmirror.model.TcpSocketServer.ConnectCallBack;
import com.action.screenmirror.utils.ByteUtils;
import com.action.screenmirror.utils.Config;

public class UdpSocketSend {
	
	
	protected static final String TAG = "UdpSocketSend";
	private MediaEncoder mediaEncoder;
	private DatagramSocket mDatagramSocket;
	private DatagramSocket mAudioDatagramSocket;
	private TcpSocketServer tServer;

	public void startReceord(MediaProjection mediaProjection) {
		Log.i("wxy", "-------------------------startReceord-----------------------------------------");
		if (null == mediaEncoder) {
			mediaEncoder = new MediaEncoder();
			mediaEncoder.startForUdp(mediaProjection, new IOnEncodeData() {
				
				@Override
				public void OnData(byte[] buf) {
//					Log.i(TAG, "hdb--------buf:"+buf.length);
					Log.i("wj", "-----------send---------buf:"+buf.length);
					sendData(buf);
				}
			});
			
			if (mDatagramSocket == null) {
				try {
					mDatagramSocket = new DatagramSocket(Config.PortGlob.DATAPORT);
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}
		}
		if (mAudioCoder == null) {
			mAudioCoder = new AudioCoder();
			mAudioCoder.startAudioRecord(new IDisconnectCallback() {
				
				@Override
				public void onDisconnect(TSocketModel socketMole) {
					// TODO Auto-generated method stub
					
				}
			}, new IOnEncodeData() {
				
				@Override
				public void OnData(byte[] buf) {
					Log.i("wxy", "-----------send-----audio----buf:"+buf.length);
					sendAudioData(buf);
					
				}
			});
			
		}
		
		if (mAudioDatagramSocket == null) {
			try {
				mAudioDatagramSocket = new DatagramSocket(Config.PortGlob.AUDIOPORT);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		
//		//add touch send 
//		Log.i("wxy", "------------------- send touch data -------------------"+tServer);
//				if (tServer == null) {
//					tServer = new TcpSocketServer(SendActivity.getInstance());						
//				}
//				tServer.initServer();	

	}
	
	private int packageCount = 0;
	private synchronized void sendData(byte[] buf){
		packageCount ++;
		if (packageCount > 50000) {
			packageCount = 1;
		}
		try {
			
			int length = buf.length;
			int count = (length / 1000);
			int offsize = length % 1000;
			if (offsize > 0) {
				count ++ ;
			}
//			Log.i(TAG, "hdb------length:"+length+"  count:"+count+"  offsize:"+offsize);
			byte[] data = null;
			int dataLenth = 0;
			for (int i = 0; i < count; i++) {
				if (i == count - 1) {
					dataLenth = length - (i * 1000);
					data = new byte[dataLenth + 11];
					
				}else {
					dataLenth = 1000;
					data = new byte[1000 + 11];
				}
				
				byte[] len = ByteUtils.intToBuffer(dataLenth);
				byte[] pLen = ByteUtils.intToBuffer(length);
				byte[] pCount = ByteUtils.intToBuffer(packageCount);
				byte index = (byte) (i & 0xFF);
//				Log.i(TAG, "hdb------i:"+i+"  packageCount:"+packageCount+"  dataLenth:"+dataLenth);
				
				data[0] = index; 		//鎷嗗垎鍖呬綅缃�
				data[1] = (byte) (count & 0xFF);	//鎷嗗垎鍖呬釜鏁�
				System.arraycopy(pCount, 0, data, 2, pCount.length);//鏁村寘搴忓彿
				System.arraycopy(pLen, 0, data, (2 + pCount.length), pLen.length);//鏁村寘闀垮害
				System.arraycopy(len, 0, data, (2 + pCount.length + pLen.length), len.length);//鎷嗗垎鍖呴暱搴�
				System.arraycopy(buf, i*1000, data, (2 + pCount.length + pLen.length + len.length), dataLenth);//鎷嗗垎鍖呭唴瀹�
				
				DatagramPacket mDatagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(mIp),
						Config.PortGlob.DATAPORT);
//				Log.i("wj","------------------sendData-------------------"+mDatagramPacket);
				mDatagramSocket.send(mDatagramPacket);
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private int audioPackageCount = 0;
	private synchronized void sendAudioData(byte[] buf){
		audioPackageCount ++;
		if (audioPackageCount > 50000) {
			audioPackageCount = 1;
		}
		try {
			
			int length = buf.length;
			int count = (length / 1000);
			int offsize = length % 1000;
			if (offsize > 0) {
				count ++ ;
			}
//			Log.i(TAG, "hdb------length:"+length+"  count:"+count+"  offsize:"+offsize);
			byte[] data = null;
			int dataLenth = 0;
			for (int i = 0; i < count; i++) {
				if (i == count - 1) {
					dataLenth = length - (i * 1000);
					data = new byte[dataLenth + 11];
					
				}else {
					dataLenth = 1000;
					data = new byte[1000 + 11];
				}
				
				byte[] len = ByteUtils.intToBuffer(dataLenth);
				byte[] pLen = ByteUtils.intToBuffer(length);
				byte[] pCount = ByteUtils.intToBuffer(audioPackageCount);
				byte index = (byte) (i & 0xFF);
				
				data[0] = index; 		//鎷嗗垎鍖呬綅缃�
				data[1] = (byte) (count & 0xFF);	//鎷嗗垎鍖呬釜鏁�
				System.arraycopy(pCount, 0, data, 2, pCount.length);//鏁村寘搴忓彿
				System.arraycopy(pLen, 0, data, (2 + pCount.length), pLen.length);//鏁村寘闀垮害
				System.arraycopy(len, 0, data, (2 + pCount.length + pLen.length), len.length);//鎷嗗垎鍖呴暱搴�
				System.arraycopy(buf, i*1000, data, (2 + pCount.length + pLen.length + len.length), dataLenth);//鎷嗗垎鍖呭唴瀹�
				
				DatagramPacket mDatagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(mIp),
						Config.PortGlob.AUDIOPORT);
//				Log.i("wj","------------------sendAudioData-------------------"+mDatagramPacket);
				mAudioDatagramSocket.send(mDatagramPacket);
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String mIp;
	private AudioCoder mAudioCoder;
	public void setIpAddr(String ip){
		mIp = ip;
	}
	
	public void closeSocket(){
		if (mDatagramSocket != null) {
			mDatagramSocket.close();
			mDatagramSocket = null;
		}
		if (mAudioDatagramSocket != null) {
			mAudioDatagramSocket.close();
			mAudioDatagramSocket = null;
		}
	}
	
	public void stopEncode(){
		if (mediaEncoder != null) {
			mediaEncoder.stopScreen();
			mediaEncoder = null;
		}
		if (mAudioCoder != null) {
			mAudioCoder.stopRecord();
			mAudioCoder = null;
		}
	}
	
	public boolean hasStart(){
		if (mediaEncoder != null) {
			return mediaEncoder.hasStart();
		}

		return false;
    	
    }
	
}
