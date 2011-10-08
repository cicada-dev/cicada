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

package org.cicadasong.samples.smsnotifier;

import org.cicadasong.cicadalib.CicadaNotification;
import org.cicadasong.cicadalib.CicadaNotificationManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("com.google.android.apps.googlevoice.SMS_RECEIVED")) {
      // Processes SMSes arriving via the Google Voice service
      String phoneNumber =
          intent.getExtras().getString("com.google.android.apps.googlevoice.PHONE_NUMBER");
      String text = intent.getExtras().getString("com.google.android.apps.googlevoice.TEXT");
      handleTextMessage(context, phoneNumber, text);
    } else if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
      // Process SMSes from normal telephony system
      Object[] pdus = (Object[]) intent.getExtras().get("pdus");
      String phoneNumber = null;
      String text = "";
      for (int i = pdus.length - 1; i >= 0; i--) {
        SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[i]);
        String newPhoneNumber = message.getDisplayOriginatingAddress();
        if (phoneNumber == null) {
          phoneNumber = newPhoneNumber;
        } else if (!newPhoneNumber.equals(phoneNumber)) {
          break;
        }
        
        text = message.getDisplayMessageBody() + text;
      }
      handleTextMessage(context, phoneNumber, text);
    }
  }

  private void handleTextMessage(Context context, String phoneNumber, String text) {
    String msgText = "SMS from " + phoneNumber + ": " + text;
    Log.v(getClass().getSimpleName(), msgText);
    
    CicadaNotification note = CicadaNotification.createWithText(context, msgText);
    CicadaNotificationManager.startNotification(context, note);
  }
}
