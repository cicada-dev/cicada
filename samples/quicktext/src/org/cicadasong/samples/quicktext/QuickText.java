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
// limitations under the License.

package org.cicadasong.samples.quicktext;

import org.cicadasong.cicadalib.CicadaApp;
import org.cicadasong.cicadalib.CicadaIntents.Button;
import org.cicadasong.cicadalib.CicadaIntents.ButtonEvent;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.telephony.SmsManager;

public class QuickText extends CicadaApp {
  public static String TAG = QuickText.class.getSimpleName();
  
  public static String SHARED_PREF_NAME = "QuickTextPreferences";
  public static String PREF_RECIPIENT_NAME = "RecipientName";
  public static String PREF_RECIPIENT_NUMBER = "RecipientNumber";
  public static String PREF_MESSAGE = "Message";

  public static final String ACTION_SMS_SENT = "org.cicadasong.samples.quicktext.SENT_SMS";
  public static final String ACTION_SETUP_UPDATE = "org.cicadasong.samples.quicktext.SETUP_UPDATE";

  private String name = "";
  private String number = "";
  private String message = "";
  
  private enum State {
    NEED_CONFIGURATION,
    READY_TO_SEND,
    SENDING,
    GOT_SEND_RESULT,
  };
  
  private State state = State.READY_TO_SEND;
  private String result;
  BroadcastReceiver smsIntentReceiver;
  BroadcastReceiver setupUpdateIntentReceiver;
  
  @Override
  public void onActivate(AppType mode) {
    registerIntentHandler();
    loadSetup();
    backToReady();
  }

  @Override
  public void onDeactivate() {
    unregisterIntentHandler();
  }
  
  private void registerIntentHandler() {
    smsIntentReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        switch (getResultCode()) {
        case Activity.RESULT_OK:
          result = "Message sent!";
          break;
        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
          result = "Error.";
          break;
        case SmsManager.RESULT_ERROR_NO_SERVICE:
          result = "Error: No service.";
          break;
        case SmsManager.RESULT_ERROR_NULL_PDU:
          result = "Error: Null PDU.";
          break;
        case SmsManager.RESULT_ERROR_RADIO_OFF:
          result = "Error: Radio off.";
          break;
        }
        state = State.GOT_SEND_RESULT;
        draw();
      }
    };
    registerReceiver(smsIntentReceiver, new IntentFilter(ACTION_SMS_SENT));

    setupUpdateIntentReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        loadSetup();
        backToReady();
      }
    };
    registerReceiver(setupUpdateIntentReceiver, new IntentFilter(ACTION_SETUP_UPDATE));
  }
  
  private void unregisterIntentHandler() {
    unregisterReceiver(smsIntentReceiver);
    unregisterReceiver(setupUpdateIntentReceiver);
  }

  @Override
  public void onButtonPress(ButtonEvent buttonEvent) {
    if (buttonEvent.hasOnlyButtonsPressed(Button.BOTTOM_RIGHT)) {
      if (state == State.READY_TO_SEND) {
        sendText();
      } else if (state == State.GOT_SEND_RESULT) {
        backToReady();
      } else if (state == State.NEED_CONFIGURATION) {
        launchSetup();
      }
    }
  }
  
  private void sendText() {
    SmsManager manager = SmsManager.getDefault();
    PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SMS_SENT), 0);
    manager.sendTextMessage(number, null, message, sentIntent, null);
    state = State.SENDING;
    draw();
  }
  
  private void launchSetup() {
    Intent setupIntent = new Intent(getBaseContext(), QuickTextSetup.class);
    setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(setupIntent);
  }
  
  private void loadSetup() {
    SharedPreferences prefs =
      getSharedPreferences(QuickText.SHARED_PREF_NAME, Context.MODE_PRIVATE);
    name = prefs.getString(QuickText.PREF_RECIPIENT_NAME, "");
    number = prefs.getString(QuickText.PREF_RECIPIENT_NUMBER, "");
    message = prefs.getString(QuickText.PREF_MESSAGE, "");
  }
  
  private void backToReady() {
    if (name.length() > 0 && number.length() > 0 && message.length() > 0) {
      state = State.READY_TO_SEND;
    } else {
      state = State.NEED_CONFIGURATION;
    }
    draw();
  }
  
  private void draw() {
    Canvas canvas = getCanvas();
    
    canvas.drawColor(Color.WHITE);

    Paint paint = new Paint();
    paint.setTypeface(Typeface.DEFAULT);
    paint.setTextSize(12);
    
    paint.setTextAlign(Paint.Align.LEFT);
    int y = 2;
    int x = 2;
    
    canvas.drawText("To: " + name, x, y + -paint.ascent(), paint);
    y += -paint.ascent() + paint.descent();
    
    canvas.drawText("Message:", x, y + -paint.ascent(), paint);
    y += -paint.ascent() + paint.descent();
    
    canvas.drawText(message, x, y + -paint.ascent(), paint);
    
    // Roughly align with the lower right button
    paint.setTextAlign(Paint.Align.RIGHT);
    y = canvas.getHeight() - 4;
    x = canvas.getWidth() - 2;
    
    if (state == State.READY_TO_SEND) {
      canvas.drawText("SEND SMS >>", x, y, paint);
    } else if (state == State.SENDING) {
      canvas.drawText("Sending...", x, y, paint);
    } else if (state == State.GOT_SEND_RESULT) {
      canvas.drawText(result, x, y, paint);
    } else if (state == State.NEED_CONFIGURATION) {
      canvas.drawText("SETUP >>", x, y, paint);
    }
    
    pushCanvas();
  }

}
