package com.hydraproductions.cicada.phonestatus;

import org.cicadasong.cicadalib.CicadaApp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.SignalStrength; // API Level 7+
import android.util.Log;

/**
 * @See http://developer.android.com/reference/android/telephony/SignalStrength.html#getGsmSignalStrength%28%29
 * @See 3GPP TS 27.007 v6.3.0 (2003-06) - Section 8.5 - Signal Quality +CSQ 
 * @author Dominic Clifton <me@dominicclifton.name>
 */
public class PhoneStatus extends CicadaApp {

	public static final String TAG = PhoneStatus.class.getSimpleName();

	private Paint paint;
	private TelephonyManager telephonyManager;
	private StatusPhoneStateListener phoneStateListener;
	float signalStrengthPercentage;

	final static short GSM_STRENGTH_MIN = 0;
	final static short GSM_STRENGTH_MAX = 31;
	final static short GSM_STRENGTH_UNKNOWN = 99;
	
	protected float calculateSignalStrengthPercentage(SignalStrength signalStrength) {
		// TODO add CDMA signal strength calculations
		int gsmSignalStrength = signalStrength.getGsmSignalStrength();
		Log.i(TAG, String.format("GSM Signal Strength: %d", gsmSignalStrength));
		if (gsmSignalStrength == GSM_STRENGTH_MAX) {
			return 0;
		}
		return (float)(gsmSignalStrength * 100) / GSM_STRENGTH_UNKNOWN;
	}
	
	protected void updateSignalStrength(SignalStrength signalStrength) {
		signalStrengthPercentage = calculateSignalStrengthPercentage(signalStrength);
		
		if (!PhoneStatus.this.isActive()) {
			return;
		}
		invalidate();
	}
	
	
	private class StatusPhoneStateListener extends PhoneStateListener {
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			updateSignalStrength(signalStrength);			
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		phoneStateListener = new StatusPhoneStateListener();
		telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	}

	@Override
	public String getAppName() {
		return getString(R.string.app_name);
	}

	@Override
	protected void onResume() {
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
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
	}

	@Override
	protected void onDraw(Canvas canvas) {
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
