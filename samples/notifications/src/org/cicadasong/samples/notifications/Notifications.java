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
// limitations under the License.package org.cicadasong.cicada;

package org.cicadasong.samples.notifications;

import java.util.ArrayList;
import java.util.List;

import org.cicadasong.cicadalib.CicadaApp;
import org.cicadasong.cicadalib.CicadaIntents.ButtonEvent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

/**
 * This class tries to show a reasonable interpretation of the phone's status bar notifications,
 * using the Android AccessibilityService.
 */
public class Notifications extends CicadaApp {
  public static final String TAG = Notifications.class.getSimpleName();
  
  public static final String INTENT_NOTIFICATION =
      "com.cicadasong.samples.notifications.NOTIFICATION";
  public static final String EXTRA_TICKER_TEXT = "ticker_text";
  public static final String EXTRA_ICON_ID = "icon_id";
  public static final String EXTRA_PACKAGE = "package";
  public static final String EXTRA_TITLE_TEXT = "title_text";
  public static final String EXTRA_BODY_TEXT = "body_text";
  public static final String EXTRA_NOTIFICATION_ID = "notification_id";
  
  private static final int ICON_SIZE = 16;
  private static final int ICON_MARGIN = 2;
  
  private static final int MAX_ALERTS = 6;

  private Paint paint;
  private BroadcastReceiver receiver;
  
  private class Alert {
    int resource;
    Drawable drawable;
    String tickerText;
    String titleText;
    String bodyText;
    String packageName;
    
    // This is intended to identify updates to a previous notification, but I haven't figured
    // out how to populate this yet.
    int notificationId; 
    
    // Instead of just glomming the strings together, I think we really want some kind of thing
    // that can be customized to suit the app's notification habits.
    public String getText() {
      StringBuilder builder = new StringBuilder();
      if (titleText != null && titleText.length() > 0) {
        builder.append(titleText);
      }
      if (bodyText != null && bodyText.length() > 0) {
        if (builder.length() > 0) {
          builder.append(" ");
        }
        builder.append(bodyText);
      }
      if (tickerText != null && tickerText.length() > 0) {
        if (builder.length() > 0) {
          builder.append(" ");
        }
        builder.append(tickerText);
      }
      return builder.toString();
    }
  }
  
  private List<Alert> alerts = new ArrayList<Alert>();
  
  @Override
  public void onCreate() {
    paint = new Paint();
    paint.setTextSize(12);
    
    super.onCreate();
  }

  @Override
  protected void onActivate(AppType mode) {
    receiver = new BroadcastReceiver() {
      
      @Override
      public void onReceive(Context context, Intent intent) {
        Alert alert = new Alert();
        alert.tickerText = intent.getStringExtra(EXTRA_TICKER_TEXT);
        alert.titleText = intent.getStringExtra(EXTRA_TITLE_TEXT);
        alert.bodyText = intent.getStringExtra(EXTRA_BODY_TEXT);
        alert.resource = intent.getIntExtra(EXTRA_ICON_ID, 0);
        alert.packageName = intent.getStringExtra(EXTRA_PACKAGE);
        alert.notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
        
        loadDrawableForAlert(alert);
        addAlertToList(alert);
        
        invalidate();
      }

    };
    IntentFilter filter = new IntentFilter();
    filter.addAction(INTENT_NOTIFICATION);
    registerReceiver(receiver, filter);
  }

  @Override
  protected void onDeactivate() {
    unregisterReceiver(receiver);
  }

  @Override
  protected void onButtonPress(ButtonEvent buttonEvent) {
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (alerts.size() == 0) {
      int x = canvas.getWidth() / 2;
      int y = (int) (canvas.getHeight() - paint.ascent()) / 2;
      paint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText(getString(R.string.no_notifications), x, y, paint);
    } else {
      paint.setTextAlign(Paint.Align.LEFT);

      int x = 0;
      int y = 0;
      for (Alert alert : alerts) {
        Drawable d = alert.drawable;
        if (d != null) {
          // This is problematic; many icons are gray and they don't show up!
          d.setBounds(x + ICON_MARGIN, y, x + ICON_MARGIN + ICON_SIZE, y + ICON_SIZE);
          d.draw(canvas);
        }
        
        canvas.drawText(
            alert.getText(), x + ICON_SIZE + ICON_MARGIN * 2, y + -paint.ascent() + 1, paint);
        y += ICON_SIZE + ICON_MARGIN;
        
        if (y > canvas.getHeight()) {
          break;
        }
      }
    }
  }
  
  private void addAlertToList(Alert alert) {
    List<Alert> newAlerts = new ArrayList<Alert>();
    newAlerts.add(alert);
    for (Alert oldAlert: alerts) {
      if (alert.notificationId != 0 && alert.notificationId == oldAlert.notificationId) {
        continue;
      }
      newAlerts.add(oldAlert);
      if (newAlerts.size() >= MAX_ALERTS) {
        break;
      }
    }
    alerts = newAlerts;
  }

  private void loadDrawableForAlert(Alert alert) {
    try {
      alert.drawable = getResources().getDrawable(alert.resource);
    } catch (Resources.NotFoundException e) {
      // Not a system resource
    }
    if (alert.drawable == null) {
      alert.drawable = getPackageManager().getDrawable(alert.packageName, alert.resource, null);
    }
  }

}
