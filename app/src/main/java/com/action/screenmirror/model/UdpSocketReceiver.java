package com.action.screenmirror.model;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.R.integer;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import com.action.screenmirror.audio.AudioTrack;
import com.action.screenmirror.bean.ByteList;
import com.action.screenmirror.interf.IConnectCallback;
import com.action.screenmirror.utils.ByteUtils;
import com.action.screenmirror.utils.Config;
import com.action.screenmirror.utils.IpUtils;
import com.action.screenmirror.utils.ThreadPoolManager;


public class UdpSocketReceiver {
	
	
	
	
	private static final String TAG = "UdpSocketReceiver";
	private static final int RECEIVER_DATA = 0;
	private static final int RECEIVER_AUDIO_DATA = 1;
	private DatagramSocket mDatagramSocket;
	private DatagramSocket mDatagramAudioSocket;
//	private TcpSocketClient tClient;
	private boolean isReceiver = false;
	private boolean isHandle = false;
	private boolean isAudioHandle = false;
	private ArrayList<byte[]> pcks = new ArrayList<byte[]>();
	private ArrayList<DatagramPacket> dataPcks = new ArrayList<DatagramPacket>();
	private ArrayList<DatagramPacket> audioPcks = new ArrayList<DatagramPacket>();
	private Object pLock = new Object();
	private Object pAudioLock = new Object();
	private SurfaceView mSurfaceView;
	private IConnectCallback mConnectCallback;
	private static int VIDEO_WIDTH = 1024;//720;//1080;//1024;//
	private static int VIDEO_HEIGHT = 600;//1560;//1920;//600;//
	private  AudioTrack mAudioTrack;

	public UdpSocketReceiver(SurfaceView surfaceView,IConnectCallback connectCallback) {
		mSurfaceView = surfaceView;
		mConnectCallback = connectCallback;
		mSurfaceView.getHolder().addCallback(new Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder arg0) {
				Log.i(TAG, "hdb---surfaceDestroyed----");
				Log.i("wxy", "---------------UdpSocketReceiver--------------------surfaceDestroyed-----------------------------");	
				close();
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder hodler) {
				Log.i(TAG, "hdb---surfaceCreated----");
				Log.i("wxy", "---------------UdpSocketReceiver--------------------surfaceCreated-----------------------------");				
				ThreadPoolManager.getInstance().execute(new Runnable() {
					
					@Override
					public void run() {
						initMediaDecoder(mSurfaceView.getHolder().getSurface());
					}
				});
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
				
			}
		});
	}
	
	
	
	
	
	private void add(byte[] data){
		synchronized (pLock) {
			pcks.add(data);
		}
	}
	private byte[] remove(){
		synchronized (pLock) {
			if (pcks.size() > 0) {
				return pcks.remove(0);
			}
			return null;
		}
	}
	
	private void addPacket(DatagramPacket data){
		synchronized (pLock) {
			dataPcks.add(data);
		}
	}

	private void addAudioPacket(DatagramPacket data){
		synchronized (pAudioLock) {
			audioPcks.add(data);
		}
	}

	private DatagramPacket removeAudioPacket(){
		synchronized (pAudioLock) {
			if (audioPcks.size() > 0) {
				return audioPcks.remove(0);
			}
			return null;
		}
	}
	private DatagramPacket removePacket(){
		synchronized (pLock) {
			if (dataPcks.size() > 0) {
				return dataPcks.remove(0);
			}
			return null;
		}
	}
	
	private Handler mHandler = new UdpHandler(this);

	public static class UdpHandler extends Handler {
		private WeakReference<UdpSocketReceiver> weakReference;
		public UdpHandler(UdpSocketReceiver udpSocketReceiver) {
			weakReference = new WeakReference<UdpSocketReceiver>(udpSocketReceiver);
		}

		@Override
		public void handleMessage(Message msg) {
			UdpSocketReceiver uReceiver = weakReference.get();
			switch (msg.what) {
			case RECEIVER_DATA:
				if (uReceiver.mConnectCallback != null) {
					uReceiver.mConnectCallback.onConnect();
				}
				uReceiver.handleData();
				break;
				
			case RECEIVER_AUDIO_DATA:
				if (uReceiver.mConnectCallback != null) {
					uReceiver.mConnectCallback.onConnect();
				}
				uReceiver.handleAudioData();
				break;

			default:
				break;
			}
		}
	}
	

	
	private void handleData(){
		ThreadPoolManager.getInstance().execute(new Runnable() {
			
			@Override
			public void run() {
				
				while (isHandle) {
//					byte[] data = remove();
					DatagramPacket pack = removePacket();
					if (pack != null) {
						byte[] data = new byte[pack.getLength()];
						System.arraycopy(pack.getData(), 0, data, 0, pack.getLength());
//					}
//					
//					if (data != null) {
						int index = data[0];
						int count = data[1];
						byte[] pckIndexb = new byte[3];
						byte[] lengthb = new byte[3];
						byte[] lengthc = new byte[3];
						System.arraycopy(data, 2, pckIndexb, 0, pckIndexb.length);
						System.arraycopy(data, 5, lengthb, 0, lengthb.length);
						System.arraycopy(data, 8, lengthc, 0, lengthc.length);
						int pckIndex = ByteUtils.bufferToInt(pckIndexb);
						int pLength = ByteUtils.bufferToInt(lengthb);
						int length = ByteUtils.bufferToInt(lengthc);
						
						byte[] buf = new byte[length];
						System.arraycopy(data, 11, buf, 0, buf.length);
						
//						Log.i(TAG, "hdb-------index:"+index+"  count:"+count+"  pckIndex:"+pckIndex+"  length:"+length+"  pLength:"+pLength);
						ByteList byteList = new ByteList(buf, index, count, pckIndex, length, pLength);
						byte[] bFrame = null;
						if (currentIndex == -1) {
							currentIndex = pckIndex;
							
						}
						if (currentIndex != pckIndex) {
							currentIndex = pckIndex;
							bFrame = getBuf();
						}
						addList(byteList);
						if (bFrame != null && decoder != null) {
							onFrame(bFrame);
						}
						
					}
				}
				
				
			}
		});

	}
		
	
	
	private void handleAudioData(){	
		Log.i("wxy" ,"---------------------handleAudioData-----------------");
		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				while (isAudioHandle) {
//					byte[] data = remove();
					DatagramPacket pack = removeAudioPacket();
					if (pack != null) {
						byte[] data = new byte[pack.getLength()];
						System.arraycopy(pack.getData(), 0, data, 0, pack.getLength());
//					}
//
//					if (data != null) {
						int index = data[0];
						int count = data[1];
						byte[] pckIndexb = new byte[3];
						byte[] lengthb = new byte[3];
						byte[] lengthc = new byte[3];
						System.arraycopy(data, 2, pckIndexb, 0, pckIndexb.length);
						System.arraycopy(data, 5, lengthb, 0, lengthb.length);
						System.arraycopy(data, 8, lengthc, 0, lengthc.length);
						int pckIndex = ByteUtils.bufferToInt(pckIndexb);
						int pLength = ByteUtils.bufferToInt(lengthb);
						int length = ByteUtils.bufferToInt(lengthc);

						byte[] buf = new byte[length];
						System.arraycopy(data, 11, buf, 0, buf.length);

//						Log.i(TAG, "hdb-------index:"+index+"  count:"+count+"  pckIndex:"+pckIndex+"  length:"+length+"  pLength:"+pLength);
						ByteList byteList = new ByteList(buf, index, count, pckIndex, length, pLength);
						byte[] bFrame = null;

						if (currentAudioIndex == -1) {
							currentAudioIndex = pckIndex;
						}
						if (currentAudioIndex != pckIndex) {
							currentAudioIndex = pckIndex;
							bFrame = getAudioBuf();
						}
						addAudioList(byteList);
//						if (bFrame != null && decoder != null) {
//							onFrame(bFrame);
//						}
//						Log.i("wj" ,"---------------------handleAudioData-----------------"+bFrame);
						
						if (bFrame != null) {
							mAudioTrack.writeSL(bFrame,bFrame.length);
						}
						
					}
				}


			}
		});
	}

	static final int sampleRate = 48000;//44100;//16000;
	static final int channels = 1;//AudioFormat.CHANNEL_IN_STEREO;
	static final int samleFormat = AudioFormat.ENCODING_PCM_16BIT;
	private int currentIndex = -1;
	private int currentAudioIndex = -1;
	private int nextIndex = -1;
	private ArrayList<ByteList> mCurrentLists = new ArrayList<ByteList>();
	private ArrayList<ByteList> mCurrentAudioLists = new ArrayList<ByteList>();
	private ArrayList<ByteList> mNextLists = new ArrayList<ByteList>();
	private synchronized byte[] addCurrentList(ByteList byteList){
		mCurrentLists.add(byteList);
		if (mCurrentLists.size() == byteList.getCount()) {
			byte[] data = new byte[byteList.getpLenght()];
			for (int i = 0; i < mCurrentLists.size(); i++) {
				byte[] buf = mCurrentLists.get(i).getBuf();
				int index = mCurrentLists.get(i).getIndex();
				int lenght = mCurrentLists.get(i).getLenght();
				
				System.arraycopy(buf, 0, data, index * 1000, lenght);
			}
//			Log.i(TAG, "hdb----addCurrentList----get--ok--currentIndex:"+currentIndex);
			mCurrentLists.clear();
			return data;
		}
		return null;
	}
	private Object mLock = new Object();
	private void addList(ByteList byteList){
		synchronized (mLock) {
			mCurrentLists.add(byteList);
		}
		
	}

	private Object mAudioLock = new Object();
	private void addAudioList(ByteList byteList){
		synchronized (mAudioLock) {
			mCurrentAudioLists.add(byteList);
		}

	}
	private byte[] getBuf(){
		synchronized (mLock) {
			if (mCurrentLists.size() == mCurrentLists.get(0).getCount()) {
				byte[] data = new byte[mCurrentLists.get(0).getpLenght()];
				for (int i = 0; i < mCurrentLists.size(); i++) {
					byte[] buf = mCurrentLists.get(i).getBuf();
					int index = mCurrentLists.get(i).getIndex();
					int lenght = mCurrentLists.get(i).getLenght();
					
					System.arraycopy(buf, 0, data, index * 1000, lenght);
				}
				Log.i(TAG, "hdb----getBuf----get--ok--currentIndex:"+currentIndex);
				mCurrentLists.clear();
				return data;
			}
			Log.e(TAG, "hdb----getBuferror----get--fail--currentIndex:"+currentIndex);
			mCurrentLists.clear();
			return null;
		}
		
	}

	private byte[] getAudioBuf(){
		synchronized (mAudioLock) {
			if (mCurrentAudioLists.size() == mCurrentAudioLists.get(0).getCount()) {
				byte[] data = new byte[mCurrentAudioLists.get(0).getpLenght()];
				for (int i = 0; i < mCurrentAudioLists.size(); i++) {
					byte[] buf = mCurrentAudioLists.get(i).getBuf();
					int index = mCurrentAudioLists.get(i).getIndex();
					int lenght = mCurrentAudioLists.get(i).getLenght();

					System.arraycopy(buf, 0, data, index * 1000, lenght);
				}
				Log.i(TAG, "hdb----getAudioBuf----get--ok--currentIndex:"+currentIndex);
				mCurrentAudioLists.clear();
				return data;
			}
	//		Log.e(TAG, "hdb----getAudioBuf----get--fail--currentIndex:"+currentIndex);
			mCurrentAudioLists.clear();
			return null;
		}

	}
	
	private synchronized byte[] addNextLists(ByteList byteList){
		mNextLists.add(byteList);
		if (mNextLists.size() == byteList.getCount()) {
			byte[] data = new byte[byteList.getpLenght()];
			for (int i = 0; i < mNextLists.size(); i++) {
				byte[] buf = mNextLists.get(i).getBuf();
				int index = mNextLists.get(i).getIndex();
				int lenght = mNextLists.get(i).getLenght();
				
				System.arraycopy(buf, 0, data, index * 1000, lenght);
			}
			mNextLists.clear();
//			Log.i(TAG, "hdb----addNextLists----get--ok--nextIndex:"+nextIndex);
			return data;
		}
		return null;
	}

	
	public void receiverUdpPckg() {
		Log.i(TAG, "hdb--receiverConnectInfo---");
//		Log.i("wj", "----------------------receiverConnectInfo---------------------------");
		mAudioTrack = new AudioTrack();
		mAudioTrack.startSL(sampleRate, samleFormat, channels);
//		if(tClient == null){
//			tClient = new TcpSocketClient();
//		}
		ThreadPoolManager.getInstance().execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					if (mDatagramSocket != null) {
						mDatagramSocket.close();
						mDatagramSocket = null;
					}
			//		mDatagramSocket = new DatagramSocket(Config.PortGlob.DATAPORT);
					if (mDatagramSocket == null) {
						mDatagramSocket = new DatagramSocket(null);
						mDatagramSocket.setReuseAddress(true);
						mDatagramSocket.bind(new InetSocketAddress(Config.PortGlob.DATAPORT));
					}	
					isReceiver = true;
					while (isReceiver) {
						byte[] receiverData = new byte[1024];
						DatagramPacket pack = new DatagramPacket(receiverData, receiverData.length);
						mDatagramSocket.receive(pack);

//						byte[] data = new byte[pack.getLength()];
//						System.arraycopy(pack.getData(), 0, data, 0, pack.getLength());
//						add(data);
						addPacket(pack);
						if (!isHandle) {
							isHandle = true;
							mHandler.sendEmptyMessage(RECEIVER_DATA);
						}
//						Log.i(TAG, "hdb--receiverConnectInfo--length:" + pack.getLength());
						
						byte[] data = pack.getData();
						int index = data[0];
						int count = data[1];
						byte[] pckIndexb = new byte[3];
						System.arraycopy(data, 2, pckIndexb, 0, pckIndexb.length);
						int pckIndex = ByteUtils.bufferToInt(pckIndexb);
						
						Log.i(TAG, "hdb---receiverUdpPckg----index:"+index+"  count:"+count+"  pckIndex:"+pckIndex);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		Log.i("wj", "----------------------receiverConnectInfo-------------audio--------------");

		ThreadPoolManager.getInstance().execute(new Runnable() {

			@Override
			public void run() {
				try {
					if (mDatagramAudioSocket != null) {
						mDatagramAudioSocket.close();
						mDatagramAudioSocket = null;
					}
			//		mDatagramAudioSocket = new DatagramSocket(Config.PortGlob.AUDIOPORT);
					if (mDatagramAudioSocket == null) {
						mDatagramAudioSocket = new DatagramSocket(null);
						mDatagramAudioSocket.setReuseAddress(true);
						mDatagramAudioSocket.bind(new InetSocketAddress(Config.PortGlob.AUDIOPORT));
					}	
					isReceiver = true;
//					Log.i("wj", "----------------------receiverConnectInfo-------isReceiver--------------------"+isReceiver);
					while (isReceiver) {
						byte[] receiverData = new byte[1024];
						DatagramPacket pack = new DatagramPacket(receiverData, receiverData.length);
						mDatagramAudioSocket.receive(pack);
						addAudioPacket(pack);
//						Log.i("wj", "----------------------receiverConnectInfo-------isAudioHandle--------------------"+isAudioHandle);
						if (!isAudioHandle) {
							isAudioHandle = true;
							mHandler.sendEmptyMessage(RECEIVER_AUDIO_DATA);
						}
						
//						Log.i(TAG, "hdb--receiverConnectInfo--length:" + pack.getLength());

						byte[] data = pack.getData();
						int index = data[0];
						int count = data[1];
						byte[] pckIndexb = new byte[3];
						System.arraycopy(data, 2, pckIndexb, 0, pckIndexb.length);
						int pckIndex = ByteUtils.bufferToInt(pckIndexb);

//						Log.i(TAG, "hdb---receiverUdpPckg----index:"+index+"  count:"+count+"  pckIndex:"+pckIndex);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});		
	}
	
	private MediaCodec decoder = null;

	
	private void initMediaDecoder(Surface surface) {
		synchronized (mCoderLock) {
			Log.i(TAG, "hdb----initMediaDecoder--VIDEO_WIDTH:"+VIDEO_WIDTH+"  VIDEO_HEIGHT:"+VIDEO_HEIGHT);
			Log.i("wxy", "hdb-----------initMediaDecoder--VIDEO_WIDTH:"+VIDEO_WIDTH+"  VIDEO_HEIGHT:"+VIDEO_HEIGHT);
	        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT);
	        try {
	            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
	            decoder.configure(mediaFormat, surface, null, 0);
	            decoder.start();
	        } catch (IOException e) {
	            e.printStackTrace();
	            mHandler.sendEmptyMessage(Config.HandlerGlod.CONNECT_FAIL);
	        }
		}  
		
    }
	
	private Object mCoderLock = new Object();
	private void closeCoder(){
		synchronized (mCoderLock) {
			Log.i(TAG, "hdb----closeCoder---");
			if (null != decoder) {
				decoder.stop();
				decoder.release();
				decoder = null;
			}
		}
		
	}
	
	private int mCount = 0;

	private AudioCoder mAudioCoder;
	public void onFrame(byte[] buf) {
		synchronized (mCoderLock) {
			if (decoder != null) {
				ByteBuffer[] inputBuffers = decoder.getInputBuffers();
	            int inputBufferIndex = decoder.dequeueInputBuffer(0);
	            if (inputBufferIndex >= 0) {
	                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	                inputBuffer.clear();
	                inputBuffer.put(buf, 0, buf.length);
	                decoder.queueInputBuffer(inputBufferIndex, 0, buf.length, mCount * 1000, 0);
	                mCount++;
	        }
	           MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
	           int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo,0);
	           while (outputBufferIndex >= 0) {
	                decoder.releaseOutputBuffer(outputBufferIndex, true);
	                outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
	        }
			}
			
		}
        
    }
	
	public void close(){
		isReceiver = false;
		isHandle = false;
		isAudioHandle = false;
		if (mDatagramSocket != null) {
			mDatagramSocket.close();
			mDatagramSocket = null;
		}
		if (mDatagramAudioSocket != null) {
			mDatagramAudioSocket.close();
			mDatagramAudioSocket = null;
		}
		closeCoder();
		if (mConnectCallback != null) {
			mConnectCallback.onDisconnect();
		}
	}
		

}
