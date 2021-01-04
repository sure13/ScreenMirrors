package com.action.screenmirror;

import com.action.screenmirror.audio.AudioRecord;
import com.action.screenmirror.model.TcpSocketServer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SelectActivity extends Activity {

	private static final String TAG = "SelectActivity";
	private TcpSocketServer tServer;

//	public String[] permissionList = {Manifest.permission.CAPTURE_VIDEO_OUTPUT};

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.activity_select);

		Button btReceiver = findViewById(R.id.bt_receiver);
		Button btSend = findViewById(R.id.bt_send);
		
		if (tServer == null) {
			tServer = new TcpSocketServer(SelectActivity.this);						
		}
		
		btReceiver.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				SendServer mServer = SendServer.getInstance();
				if (mServer != null && tServer.hasConnect()) {
					Toast.makeText(getApplicationContext(), "Currently in TX mode !", Toast.LENGTH_SHORT).show();
					return;
				}
				startActivity(new Intent(SelectActivity.this, ReceiverActivity.class));
			}
		});

		btSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				startActivity(new Intent(SelectActivity.this, SendActivity.class));
			}
		});
	}
}
