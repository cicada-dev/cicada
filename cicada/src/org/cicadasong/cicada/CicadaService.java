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

import java.util.HashMap;
import java.util.Map;

import org.cicadasong.apollo.ApolloConfig;
import org.cicadasong.apollo.ApolloIntents;
import org.cicadasong.apollo.ApolloIntents.ButtonPress;
import org.cicadasong.cicadalib.CicadaApp;
import org.cicadasong.cicadalib.CicadaApp.AppType;
import org.cicadasong.cicadalib.CicadaIntents;
import org.cicadasong.cicadalib.CicadaIntents.ButtonEvent;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class CicadaService extends Service {
  public static final String INTENT_LAUNCH_APP = "org.cicadasong.cicada.LAUNCH_APP";
  public static final String INTENT_SERVICE_STARTED = "org.cicadasong.cicada.SERVICE_STARTED";
  public static final String INTENT_SERVICE_STOPPED = "org.cicadasong.cicada.SERVICE_STOPPED";
  public static final String EXTRA_APP_PACKAGE = "app_package";
  public static final String EXTRA_APP_CLASS = "app_class";
  public static final String EXTRA_APP_NAME = "app_name";
  
  public static final boolean USE_DEVICE_SERVICE = true;
  
  // Special AppDescription, since idle screen isn't a real app.
  public static final AppDescription IDLE_SCREEN =
      new AppDescription("IDLE_SCREEN", "IDLE_SCREEN", "Idle Screen", AppType.APP);
  
  private BroadcastReceiver receiver;
  private AppDescription activeApp;
  private AppConnection activeConnection;
  private int sessionId = 1;
  private WidgetScreen widgetScreen = new WidgetScreen();
  private static boolean isRunning = false;
  private Map<Byte, AppDescription> hotkeys = new HashMap<Byte, AppDescription>();
  private DeviceServiceConnection deviceServiceConnection;
  private MetaWatchConnection.Listener connectionListener;
  
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
    filter.addAction(ApolloIntents.INTENT_BUTTON_PRESS);
    filter.addAction(HotkeySetupActivity.INTENT_HOTKEYS_CHANGED);
    filter.addAction(WidgetSetup.INTENT_WIDGETS_CHANGED);
    filter.addAction(INTENT_LAUNCH_APP);
    
    receiver = createBroadcastReceiver();
    registerReceiver(receiver, filter);

    loadHotkeys();
    
    Log.v(Cicada.TAG, "Cicada Service Started");
    isRunning = true;
    sendBroadcast(new Intent(INTENT_SERVICE_STARTED));

    super.onCreate();
  }
  
  private MetaWatchConnection.Listener createConnectionListener() {
    return new MetaWatchConnection.Listener() {
      @Override
      public void buttonPressed(ButtonPress event) {
        handleButtonPress(event);
      }
    };
  }
  
  public static boolean isRunning() {
    return isRunning;
  }
  
  private void loadHotkeys() {
    AppDatabase db = new AppDatabase(this);
    hotkeys.clear();
    for (HotkeySetupEntry entry : db.getHotkeySetup()) {
      hotkeys.put(entry.hotkeys, entry.app);
    }
    db.close();
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
          
          if (USE_DEVICE_SERVICE && (deviceServiceConnection != null)) {
            deviceServiceConnection.getService().updateScreen(
                    buffer, MetaWatchConnection.MODE_APPLICATION);
          } else {
            Intent newIntent = new Intent(ApolloIntents.INTENT_PUSH_BITMAP);
            newIntent.putExtra(ApolloIntents.EXTRA_BITMAP_BUFFER, buffer);
            sendBroadcast(newIntent);
          }
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
          
          if (USE_DEVICE_SERVICE && (deviceServiceConnection != null)) {
            deviceServiceConnection.getService().vibrate(onMillis, offMillis, numCycles);
          } else {
            Intent newIntent = new Intent(ApolloIntents.INTENT_VIBRATE);
            newIntent.putExtra(ApolloIntents.EXTRA_VIBRATE_ON_MSEC, onMillis);
            newIntent.putExtra(ApolloIntents.EXTRA_VIBRATE_OFF_MSEC, offMillis);
            newIntent.putExtra(ApolloIntents.EXTRA_VIBRATE_NUM_CYCLES, numCycles);
            sendBroadcast(newIntent);
          }
        } else if (intent.getAction().equals(ApolloIntents.INTENT_BUTTON_PRESS)) {
          ButtonPress buttonPress = ApolloIntents.ButtonPress.parseIntent(intent);
          handleButtonPress(buttonPress);
        } else if (intent.getAction().equals(INTENT_LAUNCH_APP)) {
          String packageName = intent.getExtras().getString(EXTRA_APP_PACKAGE);
          String className = intent.getExtras().getString(EXTRA_APP_CLASS);
          String appName = intent.getExtras().getString(EXTRA_APP_NAME);
          AppDescription app = new AppDescription(packageName, className, appName, AppType.APP);
          switchToApp(app);
        } else if (intent.getAction().equals(HotkeySetupActivity.INTENT_HOTKEYS_CHANGED)) {
          loadHotkeys();
        } else if (intent.getAction().equals(WidgetSetup.INTENT_WIDGETS_CHANGED)) {
          if (WidgetScreen.DESCRIPTION.equals(activeApp)) {
            deactivateWidgets();
            activateWidgets();
          }
        }
      }

    };
  }

  protected void handleButtonPress(ButtonPress buttonPress) {
    byte buttons = buttonPress.getButton();
    Log.v(Cicada.TAG, "Got button press: " + buttons);
    if (buttonPress != null) {
      if (buttonPress.hasButtonsPressed(ApolloConfig.Button.TOP_LEFT)) {
        if (!AppList.DESCRIPTION.equals(activeApp)) {
          switchToApp(AppList.DESCRIPTION);
        } else {
          activeConnection.sendButtonEvent(buttons);
        }
      } else if (IDLE_SCREEN.equals(activeApp) ||
          WidgetScreen.DESCRIPTION.equals(activeApp)) {
        if (hotkeys.containsKey(buttons)) {
          switchToApp(hotkeys.get(buttons));
        }
      } else if (activeApp != null) {
        activeConnection.sendButtonEvent(buttons);
      }
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    startForeground();
    
    if (USE_DEVICE_SERVICE) {
      deviceServiceConnection = new DeviceServiceConnection();
      deviceServiceConnection.connect();
    }
    
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

  private void startForeground() {
    String notificationTitle = getString(R.string.notification_title);
    String notificationBody = getString(R.string.notification_body);

    Notification notification = new Notification(R.drawable.icon, notificationTitle, 0);

    // The PendingIntent to launch our activity if the user selects this notification
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, Cicada.class), 0);

    // Set the info for the views that show in the notification panel.
    notification.setLatestEventInfo(
        getApplicationContext(), notificationTitle, notificationBody, contentIntent);
    notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
    startForeground(1, notification);
  }
  
  @Override
  public void onDestroy() {
    Log.v(Cicada.TAG, "Cicada Service Destroyed");
    
    if (deviceServiceConnection != null) {
      deviceServiceConnection.disconnect();
      deviceServiceConnection = null;
    }
    
    if (activeApp != null) {
      deactivateApp(activeApp);
    }
    
    unregisterReceiver(receiver);
    ApolloIntents.setApplicationMode(this, false);
    
    stopForeground(true);
    
    isRunning = false;
    sendBroadcast(new Intent(INTENT_SERVICE_STOPPED));
    
    super.onDestroy();
  }
  
  private void incrementSessionId() {
    sessionId = (sessionId + 1) % Short.MAX_VALUE;
    if (sessionId == 0) {
      sessionId++;  // Make sure 0 is never a valid session ID, since that's the uninitialized value
    }
  }
  
  private void switchToApp(AppDescription app) {
    if (app.equals(activeApp)) {
      return;
    }
    
    if (activeApp != null) {
      deactivateApp(activeApp);
    }
    incrementSessionId();
    activateApp(app, AppType.APP);
  }
  
  private void activateApp(AppDescription app, AppType mode) {
    Log.v(Cicada.TAG,
        "Activating app: " + app.appName + " (" + app.packageName + "/" + app.className + ")");
    activeApp = app;
    
    if (IDLE_SCREEN.equals(app)) {
      Log.v(Cicada.TAG, "Switching to idle screen...");
      ApolloIntents.pushText(this, getResources().getString(R.string.waiting_for_idle_screen));
      ApolloIntents.setApplicationMode(this, false);
      return;
    } else if (WidgetScreen.DESCRIPTION.equals(app)) {
      activateWidgets();
      return;
    }
    
    activeConnection = new AppConnection(app, sessionId, AppType.APP);
    activeConnection.connect();
  }
  
  private void deactivateApp(AppDescription app) {
    Log.v(Cicada.TAG, "Dectivating app: " + app.appName);
    activeApp = null;

    if (IDLE_SCREEN.equals(app)) {
      ApolloIntents.setApplicationMode(this, true);
      return;
    } else if (WidgetScreen.DESCRIPTION.equals(app)) {
      deactivateWidgets();
      return;
    }
    
    activeConnection.deactivateApp();
    activeConnection.disconnect();
    activeConnection = null;
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
        AppConnection connection = new AppConnection(app, sessionId, AppType.WIDGET);
        widgetScreen.widgetConnections[i] = connection;
        connection.connect();
        incrementSessionId();
      }
    }
    
    if (!hasWidgets) {
      ApolloIntents.pushText(this, getResources().getString(R.string.no_widgets_set_up));
    }
  }
  
  private void deactivateWidgets() {
    for (int i = 0; i < widgetScreen.widgets.length; i++) {
      AppConnection connection = widgetScreen.widgetConnections[i];
      if (connection != null) {
        connection.deactivateApp();
        connection.disconnect();
        widgetScreen.widgetConnections[i] = null;
      }
    }
    
    widgetScreen.clearSessionIds();
  }
  
  private void appDisconnectedUnexpectedly(AppConnection connection) {
    Log.v(Cicada.TAG, "App disconnected unexpectedly: " + connection.getApp());
    
    // The active app died, bump back to app list.
    if (connection.getApp().equals(activeApp)) {
      activeApp = null;
      activeConnection = null;
      switchToApp(AppList.DESCRIPTION);
    }
  }
  
  public class DeviceServiceConnection implements ServiceConnection {
    private boolean connected;
    private boolean requestedDisconnect;
    private DeviceService service;

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      Log.v(Cicada.TAG, "DeviceService bound");
      service = ((DeviceService.DeviceServiceBinder) binder).getService();
      connectionListener = createConnectionListener();
      service.setListener(connectionListener);
      connected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.v(Cicada.TAG, "DeviceService disconnected");
      connected = false;
    }
    
    public void connect() {
      Log.v(Cicada.TAG, "Attempting to bind device service");
      requestedDisconnect = false;
      Intent bindingIntent = new Intent(getApplicationContext(), DeviceService.class);
      bindService(bindingIntent, this, Context.BIND_AUTO_CREATE);
    }
    
    public void disconnect() {
      requestedDisconnect = true;
      unbindService(this);
    }
    
    public DeviceService getService() {
      return service;
    }
  }

  public class AppConnection implements ServiceConnection {
    private boolean connected;
    private boolean requestedDisconnect;
    private Messenger messenger;
    private final AppDescription app;
    private int sessionId;
    private AppType mode;
    
    public AppConnection(AppDescription app, int sessionId, AppType mode) {
      this.app = app;
      this.sessionId = sessionId;
      this.mode = mode;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.v(Cicada.TAG, "Successfully connected to service for app: " + app);
      this.messenger = new Messenger(service);
      connected = true;
      activateApp();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      connected = false;
      if (!requestedDisconnect) {
        appDisconnectedUnexpectedly(this);
      }
    }
    
    public void connect() {
      Log.v(Cicada.TAG, "Attempting to bind service for app: " + app);
      requestedDisconnect = false;
      Intent bindingIntent = new Intent();
      bindingIntent.setComponent(app.getComponentName());
      bindService(bindingIntent, this, Context.BIND_AUTO_CREATE);
    }
    
    public void disconnect() {
      requestedDisconnect = true;
      unbindService(this);
    }
    
    public AppDescription getApp() {
      return app;
    }
    
    public void activateApp() {
      Message activateMessage =
          Message.obtain(null, CicadaApp.MESSAGETYPE_ACTIVATE, sessionId, mode.ordinal());
      sendMessage(activateMessage);
    }
    
    public void deactivateApp() {
      sessionId = 0;
      Message deactivateMessage = Message.obtain(null, CicadaApp.MESSAGETYPE_DEACTIVATE);
      sendMessage(deactivateMessage);
    }
    
    public void sendButtonEvent(byte buttons) {
      Message buttonMessage =
        Message.obtain(null, CicadaApp.MESSAGETYPE_BUTTON_EVENT, buttons, 0);
      sendMessage(buttonMessage);
    }
    
    private void sendMessage(Message message) {
      if (!connected) {
        Log.w(Cicada.TAG,
            "Attempted to send message " + message + " to app " + app + " when disconnected");
        return;
      }

      try {
        messenger.send(message);
      } catch (RemoteException e) {
        Log.w(Cicada.TAG,
            "RemoteException when sending message " + message + " to app " + app + ":" + e);
      }
    }
  }
}
