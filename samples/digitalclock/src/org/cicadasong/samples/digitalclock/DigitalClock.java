// Copyright (C) 2011 Cicada contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.package org.cicadasong.cicadalib;

package org.cicadasong.samples.digitalclock;

import org.cicadasong.cicadalib.CicadaApp;
import org.cicadasong.cicadalib.CicadaIntents.Button;
import org.cicadasong.cicadalib.CicadaIntents.ButtonEvent;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;

public class DigitalClock extends CicadaApp {
  public static final String TAG = DigitalClock.class.getSimpleName();

  public static final int TIME_UPDATE_INTERVAL_MSEC = 30000;
  
  private static final String FORMAT_12 = "%l:%M%p";
  private static final String FORMAT_24 = "%H:%M";
  
  private Time time;
  private Paint paint;
  private Runnable updateTimeTask;
  private Handler handler;
  private boolean is24Hour = false;
  
  @Override
  public String getAppName() {
    return getString(R.string.app_name);
  }

  @Override
  protected void onActivate(AppType mode) {
    Log.v(TAG, "Digital Clock activated in mode: " + mode);
    
    time = new Time();
    paint = new Paint();
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
    paint.setTextSize(16);

    if (updateTimeTask == null) {
      updateTimeTask = new Runnable() {
        @Override
        public void run() {
          if (!DigitalClock.this.isActive()) return;
          
          invalidate();
          handler.postDelayed(this, TIME_UPDATE_INTERVAL_MSEC);
        }
      };
    }
    if (handler == null) {
      handler = new Handler();
    }
    handler.removeCallbacks(updateTimeTask);
    handler.post(updateTimeTask);
  }

  @Override
  protected void onDeactivate() {
    Log.v(TAG, "Digital Clock deactivated");
    handler.removeCallbacks(updateTimeTask);
  }

  @Override
  protected void onButtonPress(ButtonEvent buttons) {
    if (buttons.hasOnlyButtonsPressed(Button.MIDDLE_RIGHT)) {
      is24Hour = !is24Hour;  // Simple time format toggle to demonstrate button handling.
      invalidate();  // Don't forget to redraw after a button event that changes the app state!
      vibrate(200, 0, 1);  // Just to demonstrate vibration
    }
  }
  
  protected void onDraw(Canvas canvas) {
    int x = canvas.getWidth() / 2;
    int y = (int) (canvas.getHeight() - paint.ascent()) / 2;
    
    time.set(System.currentTimeMillis());
    canvas.drawText(time.format(is24Hour ? FORMAT_24 : FORMAT_12), x, y, paint);
  }

}
