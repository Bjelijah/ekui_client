package com.example.ekuiclient;

import com.example.action.PlatformAction;
import com.example.utils.SharePerferenceUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

	EditText et;
	Button btn;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		et = (EditText) findViewById(R.id.login_ip_et);
		btn = (Button) findViewById(R.id.login_in_btn);
		btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//get ip
				String ip = et.getText().toString();
				PlatformAction.getInstance().setIP(ip);
				
				//start player
				Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
				startActivity(intent);
				SharePerferenceUtil.saveConnectIP(MainActivity.this, ip);
			}
		});
		
		
		String connectIP = SharePerferenceUtil.loadConnectIP(this);
		if (connectIP!=null) {
			et.setText(connectIP);
		}
		
		
		
		
		
	}

	
}
