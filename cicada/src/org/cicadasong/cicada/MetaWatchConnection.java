package org.cicadasong.cicada;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.cicadasong.apollo.ApolloIntents;
import org.cicadasong.apollo.ApolloIntents.ButtonPress;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class MetaWatchConnection {
  public static final String TAG = MetaWatchConnection.class.getSimpleName();
  
  public static final int MAX_8_BIT_VALUE = 255;
  public static final int MAX_16_BIT_VALUE = 65535;
  public static final byte OPTION_BITS_UNUSED = 0x00;
  public static final int PACKET_WAIT_MILLIS = 25;
  
  public enum Mode {
    IDLE         (0x00),
    APPLICATION  (0x01),
    NOTIFICATION (0x02);
    
    private byte value;
    
    private Mode(int newValue) {
      value = (byte) newValue;
    }
    
    public byte getValue(Mode mode) {
      return mode.value;
    }
    
    public static Mode valueOf(byte mode) {
      for (Mode possibleMode : Mode.values()) {
        if (possibleMode.value == mode) {
          return possibleMode;
        }
      }
      return Mode.IDLE;
    }
  }
  
  // MetaWatch protocol message codes
  private static final byte MSG_GET_DEVICE_TYPE                    = 0x01;
  private static final byte MSG_GET_DEVICE_TYPE_RESPONSE           = 0x02;
  private static final byte MSG_GET_INFORMATION_STRING             = 0x03;
  private static final byte MSG_GET_INFORMATION_TYPE_RESPONSE      = 0x04;
  private static final byte MSG_ADVANCE_WATCH_HANDS                = 0x20;
  private static final byte MSG_SET_VIBRATE_MODE                   = 0x23;
  private static final byte MSG_STATUS_CHANGE_EVENT                = 0x33;
  private static final byte MSG_BUTTON_EVENT_MESSAGE               = 0x34;
  private static final byte MSG_WRITE_BUFFER                       = 0x40;
  private static final byte MSG_CONFIGURE_WATCH_MODE               = 0x41;
  private static final byte MSG_CONFIGURE_IDLE_BUFFER_SIZE         = 0x42;
  private static final byte MSG_UPDATE_DISPLAY                     = 0x43;
  private static final byte MSG_LOAD_TEMPLATE                      = 0x44;
  private static final byte MSG_ENABLE_BUTTON                      = 0x46;
  private static final byte MSG_DISABLE_BUTTON                     = 0x47;
  private static final byte MSG_READ_BUTTON_CONFIGURATION          = 0x48;
  private static final byte MSG_READ_BUTTON_CONFIGURATION_RESPONSE = 0x49;
  private static final byte MSG_BATTERY_CONFIGURATION              = 0x53;
  private static final byte MSG_LOW_BATTERY_WARNING                = 0x54;
  private static final byte MSG_LOW_BATTERY_BLUETOOTH_OFF          = 0x55;
  private static final byte MSG_READ_BATTERY_VOLTAGE_MESSAGE       = 0x56;
  private static final byte MSG_READ_BATTERY_VOLTAGE_RESPONSE      = 0x57;
  private static final byte MSG_ACCELEROMETER               = (byte) 0xEA;  // ???
  
  private BluetoothSocket socket;
  private Listener listener;
  private volatile boolean connected = true;
  private Handler remoteEventHandler;
  
  private byte[][] screenBuffers = new byte[3][(96 * 96) / 8];
  private boolean[] bufferCleared = new boolean[3];

  private ConcurrentLinkedQueue<byte[]> messageQueue = new ConcurrentLinkedQueue<byte[]>();
  private Thread senderThread = new Thread() {
    public void run() {
      while (connected) {
        while (!messageQueue.isEmpty()) {
          byte[] packet = makePacket(messageQueue.remove());
          
          Log.v(DeviceService.TAG, "Sending message: " + hexString(packet));
          
          try {
            socket.getOutputStream().write(packet);
            socket.getOutputStream().flush();
            sleep(PACKET_WAIT_MILLIS);
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (InterruptedException e) {
            
          }
        }
        
        try {
          synchronized (messageQueue) {
            messageQueue.wait();
          }
        } catch (InterruptedException e) {
          
        }
      }
    }
  };
  
  private Thread readerThread = new Thread() {
    public void run() {
      while (connected) {
        try {
          final byte[] message = new byte[32];
          InputStream inputStream = socket.getInputStream();
          int bytesRead = inputStream.read(message, 0, 2);
          if (bytesRead == 2) {
            inputStream.read(message, 2, Math.min(message.length - 2, message[1]));
            remoteEventHandler.post(new Runnable() {
              @Override
              public void run() {
                handleRemoteMessage(message);
              }
            });
          }
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          connected = false;
        }
      }
    }
  };
  
  public MetaWatchConnection(BluetoothSocket newSocket, Listener newListener) {
    socket = newSocket;
    listener = newListener;
    remoteEventHandler = new Handler();
    senderThread.start();
    readerThread.start();
  }
  
  public void setListener(Listener newListener) {
    listener = newListener;
  }
  
  private byte MSB(int value) {
    return (byte) (value / 256);
  }
  
  private byte LSB(int value) {
    return (byte) (value % 256);
  }
  
  private void handleRemoteMessage(byte[] message) {
    Log.v(DeviceService.TAG, "Read message: " + hexString(message));
    switch (message[2]) {
      case MSG_BUTTON_EVENT_MESSAGE:
        Log.v(DeviceService.TAG, "Button press!");
        byte button = message[4];
        if (listener != null) {
          // TODO: Make this less insane!
          Intent intent = new Intent(ApolloIntents.INTENT_BUTTON_PRESS);
          intent.putExtra(ApolloIntents.EXTRA_BUTTONS, button);
          listener.buttonPressed(ApolloIntents.ButtonPress.parseIntent(intent));
        }
      break;
      
      case MSG_STATUS_CHANGE_EVENT:
        Mode mode = Mode.valueOf(message[3]);
        byte event = message[4];
        Log.v(DeviceService.TAG, String.format("Status changed: %s %02x", mode, event));
        if (event == 0x01) {
          if (listener != null) {
            listener.modeChanged(mode);
          }
        } else if (event == 0x02) {
          if (listener != null) {
            listener.displayTimedOut(mode);
          }
        } else {
          Log.w(TAG, String.format("Unknown status change event code: %02x", event));
        }
    }
  }
  
  public void setNativeClockVisible(boolean isVisible) {
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    message.write(MSG_CONFIGURE_IDLE_BUFFER_SIZE);
    message.write(OPTION_BITS_UNUSED);
    message.write(isVisible ? (byte) 0x00 : (byte) 0x01);
    send(message.toByteArray());
  }
  
  public void configureWatchMode(Mode mode, int displayTimeoutSec, boolean invertDisplay) {
    displayTimeoutSec = Math.min(displayTimeoutSec, MAX_8_BIT_VALUE);
    
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    message.write(MSG_CONFIGURE_WATCH_MODE);
    message.write(mode.value);
    message.write(displayTimeoutSec);
    message.write(invertDisplay ? 0x01 : 0x00);
    send(message.toByteArray());
  }

  public void vibrate(int timeOnMs, int timeOffMs, int cycles) {
    timeOnMs = Math.min(timeOnMs, MAX_16_BIT_VALUE);
    timeOffMs = Math.min(timeOffMs, MAX_16_BIT_VALUE);
    cycles = Math.min(cycles, MAX_8_BIT_VALUE);
    
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    message.write(MSG_SET_VIBRATE_MODE);
    message.write(OPTION_BITS_UNUSED);
    message.write(0x01);  // enable vibration
    message.write(LSB(timeOnMs));
    message.write(MSB(timeOnMs));
    message.write(LSB(timeOffMs));
    message.write(MSB(timeOffMs));
    message.write((byte) cycles);
    send(message.toByteArray());
  }
  
  public void updateScreen(byte[] buffer, Mode mode) {
    boolean screenChanged = false;
    if (!bufferCleared[mode.value]) {
      screenChanged = true;
      writeSolidColorToBuffer(mode, (byte) 0x00);
      bufferCleared[mode.value] = true;
    }
    
    for (int row = 0; row < 96; row += 2) {
      boolean different = false;
      for (int rowOffset = 0; rowOffset < 2; rowOffset++) {
        int rowStart = ((row + rowOffset) * 12);
        for (int i = 0; i < 12; i++) {
          if (buffer[rowStart + i] != screenBuffers[mode.value][rowStart + i]) {
            different = true;
            screenBuffers[mode.value][rowStart + i] = buffer[rowStart + i];
          }
        }
      }
      if (different) {
        screenChanged = true;
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        message.write(MSG_WRITE_BUFFER);
        message.write(mode.value);
        for (int rowOffset = 0; rowOffset < 2; rowOffset++) {
          message.write(row + rowOffset);  // row index
          int rowStart = ((row + rowOffset) * 12);
          for (int i = 0; i < 12; i++) {
            message.write(buffer[rowStart + i]);
          }
        }
        send(message.toByteArray());
      }
    }
    
    // TODO: Right now I'm special-casing the NOTIFICATION mode because we generally fall out of
    //       it quickly.  The *correct* way to do this would be checking if the mode is not the
    //       mode that the watch is in right now.  However, the state messages that we're getting
    //       back from the watch don't seem to give us accurate mode information.
    if (screenChanged || mode == Mode.NOTIFICATION) {
      updateDisplayToMode(mode);
    }
  }

  public void updateDisplayToMode(Mode mode) {
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    message.write(MSG_UPDATE_DISPLAY);
    message.write(mode.value | 0x10);
    send(message.toByteArray());
  }
  
  public void writeSolidColorToBuffer(Mode mode, byte color) {
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    message.write(MSG_LOAD_TEMPLATE);
    message.write(mode.value);
    message.write(color == 0x00 ? 0x00 : 0x01);
    send(message.toByteArray());
  }
  
  private void addCRC(byte[] packet) {
    short crc = (short) 0xFFFF;
    int crcIndex = packet.length - 2;
    for (int i = 0; i < crcIndex; i++) {
      byte currentByte = packet[i];
      for (int bit = 7; bit >= 0; bit--) {
        boolean a = ((crc >> 15 & 1) == 1);
        boolean b = ((currentByte >> (7 - bit) & 1) == 1);
        crc <<= 1;
        if (a ^ b) {
          crc ^= 0x1021;
        }
      }
    }
    int finalCRC = crc - 0xFFFF0000;
    packet[crcIndex] = LSB(finalCRC);
    packet[crcIndex + 1] = MSB(finalCRC);
  }
  
  private String hexString(byte[] bytes) {
    StringBuffer buffer = new StringBuffer();
    for(int i = 0; i < bytes.length; i++) {
      buffer.append(String.format("%02x ", bytes[i]));
    }
    return buffer.toString();
  }
  
  public void flush() {
    connected = false;
  }
  
  private void send(byte[] message) {
    boolean wasEmpty = messageQueue.isEmpty();
    messageQueue.add(message);
    if (wasEmpty) {
      synchronized (messageQueue) {
        messageQueue.notify();
      }
    }
  }

  private byte[] makePacket(byte[] message) {
    byte length = (byte) (message.length + 4);  // prefix and CRC
    byte[] packet = new byte[length];
    packet[0] = 0x01;
    packet[1] = length;
    System.arraycopy(message, 0, packet, 2, message.length);
    
    addCRC(packet);
    return packet;
  }
  
  public interface Listener {
    public void buttonPressed(ButtonPress press);
    public void modeChanged(Mode mode);
    public void displayTimedOut(Mode mode);
  }
  
  
}
