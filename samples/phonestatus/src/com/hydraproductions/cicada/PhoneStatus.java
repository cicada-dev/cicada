package com.hydraproductions.cicada;

import org.cicadasong.cicadalib.CicadaApp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.SignalStrength; // API Level 7+
import android.util.Log;

public class PhoneStatus extends CicadaApp {

	public static final String TAG = PhoneStatus.class.getSimpleName();

	private Paint paint;

	private TelephonyManager telephonyManager = null;
	StatusPhoneStateListener phoneStateListener;
	double signalStrengthPercentage = (double) 0;

	protected double calculateSignalStrengthPercentage(SignalStrength signalStrength) {
		
		// TODO add CDMA signal strength calculations
		int gsmSignalStrength = signalStrength.getGsmSignalStrength();
		Log.i(TAG, String.format("GSM Signal Strength: %d", gsmSignalStrength));
		if (gsmSignalStrength == 99) {
			return 0;
		}
		return (gsmSignalStrength * 100) / 31;
	}
	
	protected void updateSignalStrength(SignalStrength signalStrength) {
		signalStrengthPercentage = calculateSignalStrengthPercentage(signalStrength);
		
		if (!PhoneStatus.this.isActive()) return;
		invalidate();
	}
	
	
	private class StatusPhoneStateListener extends PhoneStateListener {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			super.onSignalStrengthsChanged(signalStrength);
			updateSignalStrength(signalStrength);			
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "ON CREATE");
		phoneStateListener   = new StatusPhoneStateListener();
		telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);		
	}

	@Override
	public String getAppName() {
		return getString(R.string.app_name);
	}

	@Override
	protected void onResume() {

		Log.i(TAG, "ON RESUME");
		paint = new Paint();
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setTypeface(Typeface.DEFAULT_BOLD);
		paint.setAntiAlias(false);
		paint.setTextSize(16);
		
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		
		invalidate();
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "ON PAUSE");
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Log.i(TAG, "ON DRAW");
		int x = canvas.getWidth() / 2;
		int y = (int) (canvas.getHeight() - paint.ascent()) / 2;
		String readout = String.format("%3.2f%%", signalStrengthPercentage);
		canvas.drawText(readout, x, y, paint);
	}

	@Override
	protected void onButtonPress(WatchButton button) {
		// TODO Auto-generated method stub
	}
}
