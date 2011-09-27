package org.cicadasong.cicada;

import java.io.IOException;
import java.util.UUID;

import org.cicadasong.cicada.MetaWatchConnection.Mode;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class DeviceService extends Service {
  public static final String TAG = DeviceService.class.getSimpleName();

  public static final int MESSAGETYPE_ACTIVATE = 1;
  public static final int MESSAGETYPE_DEACTIVATE = 2;
  public static final int MESSAGETYPE_BUTTON_EVENT = 3;
  
  // The "well-known" UUID that the Android docs recommend using for SPP.
  public static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  private BluetoothSocket socket;
  private boolean connected;
  private MetaWatchConnection connection;
  private MetaWatchConnection.Listener listener;
  
  private final DeviceServiceBinder binder = new DeviceServiceBinder();
  
  public class DeviceServiceBinder extends Binder {
    public DeviceService getService() {
      return DeviceService.this;
    }
  }
  
  @Override
  public IBinder onBind(Intent intent) {
    Log.v(TAG, "onBind service");
    return binder;
  }
  
  @Override
  public void onCreate() {
    Log.v(TAG, "onCreate service");
    connectToDevice();

    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.v(TAG, "onStartCommand");
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public void onDestroy() {
    disconnectFromDevice();
    Log.v(TAG, "onDestroy service");
    super.onDestroy();
  }
  
  private void sendConnectionNotice() {
    Log.v(TAG, "Connected to device");
    
  }
  
  private void sendDisconnectionNotice() {
    Log.v(TAG, "Disconnected from device");
    
  }
  
  private void sendButtonPress() {
    
  }
  
  public void vibrate(int timeOnMs, int timeOffMs, int cycles) {
    connection.vibrate(timeOnMs, timeOffMs, cycles);
  }
  
  public void updateScreen(byte[] buffer, Mode mode) {
    if (connection != null) {
      connection.updateScreen(buffer, mode);
    }
  }
  
  public void setMode(Mode mode) {
    if (connection != null) {
      connection.updateDisplayToMode(mode);
    }
  }
  
  private void connectToDevice() {
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice device;
    String mac = PrefUtil.getWatchMAC(this);
    try {
      device = adapter.getRemoteDevice(mac);
    } catch (IllegalArgumentException e) {
      // TODO: Bad MAC address, tell the user
      Log.v(TAG, "MAC address not valid: " + mac);
      return;
    }
    try {
      socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
      socket.connect();
      connection = new MetaWatchConnection(socket, listener);
      sendConnectionNotice();
      connection.configureWatchMode(Mode.APPLICATION, 255, false);
      connection.vibrate(500, 0, 1);  // 01 0c 23 00 01 f4 01 00 00 01 ee 00 
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
  private void disconnectFromDevice() {
    if (connection != null) {
      connection.setNativeClockVisible(true);
      connection.flush();
    }
    
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    sendDisconnectionNotice();
  }
  
  public void setListener(MetaWatchConnection.Listener listener) {
    this.listener = listener;
    if (connection != null) {
      connection.setListener(listener);
    }
  }

  /**
   * Handler of incoming messages from CicadaService.
   */
  class IncomingHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch(msg.what){
      case MESSAGETYPE_ACTIVATE:
//        if (!isActive) {
//          sessionId = msg.arg1;
//          activate(AppType.values()[msg.arg2]);
//        }
        break;

      case MESSAGETYPE_DEACTIVATE:
//        if (isActive) {
//          deactivate();
//        }
        break;
        
      case MESSAGETYPE_BUTTON_EVENT:
//        ButtonEvent event = new ButtonEvent((byte) msg.arg1);
//        onButtonPress(event);
        break;

      default:
        super.handleMessage(msg);
        break;
      }
    }
  }


}
