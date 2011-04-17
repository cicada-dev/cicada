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

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

public class NotificationAccessibilityService extends AccessibilityService {
  public static final String TAG = Notifications.TAG;

  @Override
  protected void onServiceConnected() {
    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
    setServiceInfo(info);

    super.onServiceConnected();
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    Log.v(TAG, "onAccessibilityEvent " + event);
    if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
      return;
    }
    
    if (event.getParcelableData() instanceof Notification) {
      Notification notification = (Notification) event.getParcelableData();
      String titleText = null;
      String bodyText = null;

      // This is an ugly hack to pull more interesting text out of the notification, but it often
      // works.
      RemoteViews contentView = notification.contentView;
      ViewGroup notificationView = (ViewGroup) contentView.apply(this, new LinearLayout(this));
      if (notificationView != null) {
        titleText = getTitleText(notificationView);
        bodyText = getBodyText(notificationView);
      }
      
      // We're just blasting this out on a broadcast intent, maybe we should find a more
      // discreet way.
      Intent intent = new Intent(Notifications.INTENT_NOTIFICATION);
      intent.putExtra(Notifications.EXTRA_TICKER_TEXT, notification.tickerText);
      intent.putExtra(Notifications.EXTRA_TITLE_TEXT, titleText);
      intent.putExtra(Notifications.EXTRA_BODY_TEXT, bodyText);
      intent.putExtra(Notifications.EXTRA_ICON_ID, notification.icon);
      intent.putExtra(Notifications.EXTRA_PACKAGE, event.getPackageName());
      sendBroadcast(intent);
    }
  }
  
  private String getTitleText(ViewGroup contentView) {
    return getText(contentView, new int[]{ 0, 1 });  // Empirically determined
  }
  
  private String getBodyText(ViewGroup contentView) {
    return getText(contentView, new int[]{ 1, 0 });
  }
 
  private String getText(ViewGroup contentView, int[] path) {
    View view = getView(contentView, path);
    if (!(view instanceof TextView)) {
      return null;
    }
    
    return ((TextView) view).getText().toString();
  }
  
  private View getView(ViewGroup root, int[] path) {
    ViewGroup current = root;
    for (int i = 0; i < path.length; i++) {
      if (current.getChildCount() <= path[i]) {
        return null;
      }
      View child = current.getChildAt(path[i]);
      if (i == path.length - 1) {
        return child;
      }
      if (!(child instanceof ViewGroup)) {
        return null;
      }
      current = (ViewGroup) child;
    }
    return null;
  }

  @Override
  public void onInterrupt() {
  }

}
