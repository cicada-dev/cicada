package com.hydraproductions.cicada.stopwatch;

/**
 * Stopwatch example
 * 
 * The timer runs on the phone, not the watch.  It'd be better if the timer value was kept on the watch and if there
 * was some way of updating the dispatch from the watch itself...
 * 
 * The display is updated every 5 seconds when the stopwatch is running (to save battery).
 * 
 * Buttons control the stopwatch as follows
 * Middle - start/stop
 * Bottom - reset
 * 
 * The example is based on the DigitalClock sample.
 */

import org.cicadasong.cicadalib.CicadaApp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;

public class StopWatch extends CicadaApp {

	public static final String TAG = StopWatch.class.getSimpleName();

	public static final int DISPLAY_UPDATE_INTERVAL_MSEC = 5000;
	private Paint paint;
	private Runnable updateDisplayTask;
	private Handler handler;

	private boolean indicator = true; // causes the ':''s to be drawn, or not.

	// TODO move stopwatch timer into new class, Stopwatch, with reset, continue
	// and getElapsed methods
	private enum State {
		STOPPED, RUNNING
	}

	private static long startTime = 0;
	private static long stopTime = 0;
	private static long previouslyElapsedTime = 0;
	private static State state = State.STOPPED;

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
		
		ensureUpdateDisplayTask();
		startUpdateDisplayTask();
	}
	
	private void ensureUpdateDisplayTask() {
		if (updateDisplayTask != null) {
			return;
		}
		
		updateDisplayTask = new Runnable() {
			@Override
			public void run() {
				if (!StopWatch.this.isActive())
					return;

				if (state != State.RUNNING) {
					return;
				}
				
				if (state == State.RUNNING) {
					indicator = !indicator;
				} else {
					indicator = true;
				}
				
				invalidate();
				handler.postDelayed(this, DISPLAY_UPDATE_INTERVAL_MSEC);
			}
		};
	}
	
	private void startUpdateDisplayTask() {
		if (handler == null) {
			handler = new Handler();
		}
		handler.removeCallbacks(updateDisplayTask);
		handler.postDelayed(updateDisplayTask, DISPLAY_UPDATE_INTERVAL_MSEC);
	}

	@Override
	protected void onPause() {
		handler.removeCallbacks(updateDisplayTask);
	}

	@Override
	protected void onButtonPress(WatchButton button) {
		boolean updateDisplay = false;
		switch (button) {
		case MIDDLE_RIGHT:
			updateDisplay = true;
			// handle state transitions
			if (state == State.STOPPED) {
				// continue
				previouslyElapsedTime = stopTime - startTime;
				startTime = System.currentTimeMillis();
				state = State.RUNNING;
			} else {
				// stop
				state = State.STOPPED;
				startTime = startTime - previouslyElapsedTime; 
				stopTime = System.currentTimeMillis();
				previouslyElapsedTime = 0;
			}
			break;
		case BOTTOM_RIGHT:
			updateDisplay = true;
			// reset
			state = State.STOPPED;
			startTime = 0;
			stopTime = 0;
			previouslyElapsedTime = 0;
			break;
		}
		if (updateDisplay) {
			invalidate();
		}
	}

	private long getElapsed() {
		if (state == State.RUNNING) {
			long now = System.currentTimeMillis();
			return (now - startTime) + previouslyElapsedTime;
		} else {
			return (stopTime - startTime) + previouslyElapsedTime;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int x = canvas.getWidth() / 2;
		int y = (int) (canvas.getHeight() - paint.ascent()) / 2;

		char seperator;
		if (indicator) {
			seperator = ':';
		} else {
			seperator = '.';
		}

		long elapsedMillis = getElapsed();
		short seconds = (short) (elapsedMillis / 1000);
		short mins = (short) (seconds / 60);
		short hours = (short) (mins / 60);

		String readout = String.format("%d%c%02d%c%02d", hours, seperator, mins % 60, seperator, seconds % 60);
		canvas.drawText(readout, x, y, paint);
	}
}
