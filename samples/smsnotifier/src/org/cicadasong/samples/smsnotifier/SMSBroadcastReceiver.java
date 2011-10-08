package org.cicadasong.samples.smsnotifier;

import org.cicadasong.cicadalib.CicadaNotification;
import org.cicadasong.cicadalib.CicadaNotificationManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SMSBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v(getClass().getSimpleName(), "Received intent: " + intent);
    
    if (intent.getAction().equals("com.google.android.apps.googlevoice.SMS_RECEIVED")) {
      String phoneNumber = intent.getExtras().getString("com.google.android.apps.googlevoice.PHONE_NUMBER");
      String text = intent.getExtras().getString("com.google.android.apps.googlevoice.TEXT");
      String msgText = "SMS from " + phoneNumber + ": " + text;
      Log.v(getClass().getSimpleName(), msgText);
      
      CicadaNotification note = CicadaNotification.createWithText(context, msgText);
      CicadaNotificationManager.startNotification(context, note);
    }
  }
}
