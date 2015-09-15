/*
 * Copyright (C) 2014-2015 ISTI - CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.cnr.isti.doremi.sleeplogger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.TimerTask;

public class MyActivity extends Activity {

    private static final String TAG = MyActivity.class.getName();
    private static int UI_REFRESH_INTERVAL = 1000;

    public void onSettingsButtonClick() {}

    public void startSampling() {
        Intent myIntent = new Intent(MyActivity.this, SensorSamplingService.class);
        startService(myIntent);
    }

    public void stopSampling() {
        Intent myIntent = new Intent(MyActivity.this, SensorSamplingService.class);
        stopService(myIntent);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_my);
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart()");
		super.onStart();
	}

	@Override
	protected void onStop()	{
		Log.d (TAG, "onStop()");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d (TAG, "onDestroy()");
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d (TAG, "onPause()");
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.d (TAG, "onResume()");
		super.onResume();
	}
}
