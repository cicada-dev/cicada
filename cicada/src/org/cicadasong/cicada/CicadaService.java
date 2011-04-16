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

package org.cicadasong.cicada;

import org.cicadasong.apollo.ApolloConfig;
import org.cicadasong.apollo.ApolloIntents;
import org.cicadasong.apollo.ApolloIntents.ButtonPress;
import org.cicadasong.cicadalib.CicadaApp.AppType;
import org.cicadasong.cicadalib.CicadaIntents;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class CicadaService extends Service {
  public static final String INTENT_LAUNCH_APP = "org.cicadasong.cicada.LAUNCH_APP";
  public static final String EXTRA_APP_PACKAGE = "app_package";
  public static final String EXTRA_APP_CLASS = "app_class";
  public static final String EXTRA_APP_NAME = "app_name";
  
  // Special AppDescription, since idle screen isn't a real app.
  public static final AppDescription IDLE_SCREEN =
      new AppDescription("IDLE_SCREEN", "IDLE_SCREEN", "Idle Screen", AppType.APP);
  
  private BroadcastReceiver receiver;
  private AppDescription activeApp;
  private int sessionId = 1;
  private WidgetScreen widgetScreen = new WidgetScreen();

  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void onCreate() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(CicadaIntents.INTENT_PUSH_CANVAS);
    filter.addAction(CicadaIntents.INTENT_VIBRATE);
    filter.addAction(ApolloIntents.INTENT_IDLE_BUTTON_PRESS);
    filter.addAction(ApolloIntents.INTENT_APP_BUTTON_PRESS);
    filter.addAction(INTENT_LAUNCH_APP);
    
    receiver = createBroadcastReceiver();
    registerReceiver(receiver, filter);
    
    super.onCreate();
  }

  private BroadcastReceiver createBroadcastReceiver() {
    return new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.v(Cicada.TAG, "Received intent: " + intent);
        if (intent.getAction().equals(CicadaIntents.INTENT_PUSH_CANVAS)) {
          byte[] buffer = intent.getByteArrayExtra(CicadaIntents.EXTRA_BUFFER);
          int senderSessionId = intent.getIntExtra(CicadaIntents.EXTRA_SESSION_ID, 0);
          
          if (widgetScreen.hasSessionId(senderSessionId)) {
            buffer = widgetScreen.updateScreenBuffer(buffer, senderSessionId);
          } else if (senderSessionId != sessionId) {
            Log.e(Cicada.TAG, "App tried to push screen using expired sessionId " + senderSessionId
                + " -- the current sessionId is " + sessionId);
            return;
          }
          
          Intent newIntent = new Intent(ApolloIntents.INTENT_PUSH_BITMAP);
          newIntent.putExtra(ApolloIntents.EXTRA_BITMAP_BUFFER, buffer);
          sendBroadcast(newIntent);
        } else if (intent.getAction().equals(CicadaIntents.INTENT_VIBRATE)) {
          int senderSessionId = intent.getIntExtra(CicadaIntents.EXTRA_SESSION_ID, 0);
          if (senderSessionId != sessionId) {
            Log.e(Cicada.TAG, "App tried to vibrate using expired sessionId " + senderSessionId
                + " -- the current sessionId is " + sessionId);
            return;
          }
          
          int onMillis = intent.getIntExtra(CicadaIntents.EXTRA_VIBRATE_ON_MSEC, 0);
          int offMillis = intent.getIntExtra(CicadaIntents.EXTRA_VIBRATE_OFF_MSEC, 0);
          int numCycles = intent.getIntExtra(CicadaIntents.EXTRA_VIBRATE_NUM_CYCLES, 0);
          
          Intent newIntent = new Intent(ApolloIntents.INTENT_VIBRATE);
          newIntent.putExtra(ApolloIntents.EXTRA_VIBRATE_ON_MSEC, onMillis);
          newIntent.putExtra(ApolloIntents.EXTRA_VIBRATE_OFF_MSEC, offMillis);
          newIntent.putExtra(ApolloIntents.EXTRA_VIBRATE_NUM_CYCLES, numCycles);
          sendBroadcast(newIntent);
        } else if (intent.getAction().equals(ApolloIntents.INTENT_IDLE_BUTTON_PRESS) ||
             intent.getAction().equals(ApolloIntents.INTENT_APP_BUTTON_PRESS)) {
          ButtonPress buttonPress = ApolloIntents.ButtonPress.parseIntent(intent);
          if (buttonPress != null) {
            if (buttonPress.hasOnlyButtonsPressed(ApolloConfig.Button.TOP_LEFT)) {
              switchToApp(AppList.DESCRIPTION);
            } else if (!activeApp.className.equals(WidgetScreen.DESCRIPTION.className)) {
              CicadaIntents.ButtonEvent.sendIntent(getApplicationContext(),
                  intent.getByteExtra(ApolloIntents.EXTRA_BUTTONS, (byte) 0));
            }
          }
        } else if (intent.getAction().equals(INTENT_LAUNCH_APP)) {
          String packageName = intent.getExtras().getString(EXTRA_APP_PACKAGE);
          String className = intent.getExtras().getString(EXTRA_APP_CLASS);
          String appName = intent.getExtras().getString(EXTRA_APP_NAME);
          AppDescription app = new AppDescription(packageName, className, appName, AppType.APP);
          switchToApp(app);
        }
      }
    };
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.v(Cicada.TAG, "Cicada Service Started");
    
    if (activeApp != null) {
      deactivateApp(activeApp);
    }
    
    if (!PrefUtil.getAppScanCompleted(this)) {
      Log.v(Cicada.TAG, "Scanning for Cicada apps...");
      AppScanner scanner = new AppScanner(this, new AppScanner.Listener() {
        @Override
        public void scanFinished() {
          Log.v(Cicada.TAG, "App scan complete");
          PrefUtil.setAppScanCompleted(CicadaService.this, true);
          switchToApp(AppList.DESCRIPTION);
        }
      });
      scanner.execute((Void) null);
    } else {
      switchToApp(AppList.DESCRIPTION);
    }
    
    ApolloIntents.setApplicationMode(this, true);
    ApolloIntents.pushText(this, getResources().getString(R.string.welcome_on_screen));
    ApolloIntents.vibrate(this, 300, 0, 1);
    
    return super.onStartCommand(intent, flags, startId);
  }
  
  @Override
  public void onDestroy() {
    Log.v(Cicada.TAG, "Cicada Service Destroyed");
    
    deactivateApp(activeApp);
    
    unregisterReceiver(receiver);
    ApolloIntents.setApplicationMode(this, false);
    
    super.onDestroy();
  }
  
  private void incrementSessionId() {
    sessionId = (sessionId + 1) % Short.MAX_VALUE;
    if (sessionId == 0) {
      sessionId++;  // Make sure 0 is never a valid session ID, since that's the uninitialized value
    }
  }
  
  private void switchToApp(AppDescription app) {
    if (activeApp != null) {
      deactivateApp(activeApp);
    }
    incrementSessionId();
    activateApp(app, AppType.APP);
  }
  
  private void activateApp(AppDescription app, AppType mode) {
    Log.v(Cicada.TAG, "Activating app: " + app.appName);
    activeApp = app;
    
    if (IDLE_SCREEN.className.equals(app.className)) {
      Log.v(Cicada.TAG, "Switching to idle screen...");
      ApolloIntents.pushText(this, getResources().getString(R.string.waiting_for_idle_screen));
      ApolloIntents.setApplicationMode(this, false);
      return;
    } else if (WidgetScreen.DESCRIPTION.className.equals(app.className)) {
      activateWidgets();
      return;
    }
    
    sendActivationIntent(app, sessionId, AppType.APP);
  }
  
  private void deactivateApp(AppDescription app) {
    Log.v(Cicada.TAG, "Dectivating app: " + app.appName);
    activeApp = null;

    if (IDLE_SCREEN.className.equals(app.className)) {
      ApolloIntents.setApplicationMode(this, true);
      return;
    } else if (WidgetScreen.DESCRIPTION.className.equals(app.className)) {
      deactivateWidgets();
      return;
    }
    
    sendDeactivationIntent(app);
  }

  private void activateWidgets() {
    AppDatabase db = new AppDatabase(this);
    widgetScreen.widgets = db.getWidgetSetup().toArray(new AppDescription[]{});
    db.close();
    
    widgetScreen.clearBuffer();
    boolean hasWidgets = false;
    
    for (int i = 0; i < widgetScreen.widgets.length; i++) {
      AppDescription app = widgetScreen.widgets[i];
      if (app != null) {
        hasWidgets = true;
        widgetScreen.putSessionId(i, sessionId);
        sendActivationIntent(app, sessionId, AppType.WIDGET);
        incrementSessionId();
      }
    }
    
    if (!hasWidgets) {
      ApolloIntents.pushText(this, getResources().getString(R.string.no_widgets_set_up));
    }
  }
  
  private void deactivateWidgets() {
    for (int i = 0; i < widgetScreen.widgets.length; i++) {
      AppDescription app = widgetScreen.widgets[i];
      if (app != null) {
        sendDeactivationIntent(app);
      }
    }
    
    widgetScreen.clearSessionIds();
  }

  private void sendActivationIntent(AppDescription app, int appSessionId, AppType mode) {
    Intent serviceIntent = new Intent();
    serviceIntent.setComponent(new ComponentName(app.packageName, app.className));
    serviceIntent.setAction(CicadaIntents.INTENT_ACTIVATE_APP);
    serviceIntent.putExtra(CicadaIntents.EXTRA_SESSION_ID, appSessionId);
    serviceIntent.putExtra(CicadaIntents.EXTRA_APP_MODE, mode.name());
    startService(serviceIntent);
  }
  
  private void sendDeactivationIntent(AppDescription app) {
    Intent serviceIntent = new Intent();
    serviceIntent.setComponent(new ComponentName(app.packageName, app.className));
    serviceIntent.setAction(CicadaIntents.INTENT_DEACTIVATE_APP);
    stopService(serviceIntent);
  }

}
