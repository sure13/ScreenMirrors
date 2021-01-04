package com.action.screenmirror.interf;

public interface IConnectCallback {

	void onDisconnect();
	void onConnect();
	
	void onTouchConnectionSucessful();
	void onTouchConnectionFail();
}
