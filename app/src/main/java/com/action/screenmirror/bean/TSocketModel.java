package com.action.screenmirror.bean;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.action.screenmirror.model.ReceiverTouchDataRunnable;
import com.action.screenmirror.utils.ThreadPoolManager;

public class TSocketModel {
	
	private Socket touchSocket;
	private DataInputStream touchDis;
	private DataOutputStream touchDos;
	
	private DataOutputStream videoDos;
	
	private DataOutputStream audioDos;
	
	private boolean isTouchRun = false;
	private ReceiverTouchDataRunnable runnable = null;
	
	public ReceiverTouchDataRunnable getRunnable() {
		return runnable;
	}

	public void setRunnable(ReceiverTouchDataRunnable runnable) {
		this.runnable = runnable;
	}

	public boolean isTouchRun() {
		return isTouchRun;
	}

	public void setTouchRun(boolean isTouchRun) {
		this.isTouchRun = isTouchRun;
	}
	
	public void setTouchSockets(Socket touchSocket,DataInputStream touchDis,DataOutputStream touchDos) {
		this.touchSocket = touchSocket;
		this.touchDis = touchDis;
		this.touchDos = touchDos;
	}
	public Socket getTouchSocket() {
		return touchSocket;
	}
	public void setTouchSocket(Socket touchSocket) {
		this.touchSocket = touchSocket;
	}
	public DataInputStream getTouchDis() {
		return touchDis;
	}
	public void setTouchDis(DataInputStream touchDis) {
		this.touchDis = touchDis;
	}
	public DataOutputStream getTouchDos() {
		return touchDos;
	}
	public void setTouchDos(DataOutputStream touchDos) {
		this.touchDos = touchDos;
	}
	
	private void closeTouch(){
		if(null != touchDis){
			try {
				touchDis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(null != touchDos){
			try {
				touchDos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (null != touchSocket) {
			try {
				touchSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		touchDis = null;
		touchDos = null;
		touchSocket = null;
	}
	private Object mLock = new Object();
	public boolean isConnect(){
		synchronized (mLock) {
			if (null != touchDis) {
				return true;
			}
			return false;
		}
		
	}
	
	public void close(){
		synchronized (mLock) {
			isTouchRun = false;
			if (runnable != null) {
				ThreadPoolManager.getInstance().remove(runnable);
				runnable = null;
			}
			closeTouch();
		}
		
	}
	
	public DataOutputStream getVideoDos() {
		return videoDos;
	}
	public void setVideoDos(DataOutputStream videoDos) {
		this.videoDos = videoDos;
	}
	
	
	public DataOutputStream getAudioDos() {
		return audioDos;
	}
	public void setAudioDos(DataOutputStream audioDos) {
		this.audioDos = audioDos;
	}
	

}
