package org.cicadasong.cicada;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.cicadasong.apollo.ApolloConfig;
import org.cicadasong.apollo.ApolloIntents;
import org.cicadasong.apollo.ApolloIntents.ButtonPress;
import org.cicadasong.cicadalib.CicadaIntents.ButtonEvent;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class MetaWatchConnection {
  public static final String TAG = MetaWatchConnection.class.getSimpleName();
  
  public static final int MAX_16_BIT_VALUE = 65535;
  public static final byte OPTION_BITS_UNUSED = 0x00;
  public static final int PACKET_WAIT_MILLIS = 25;
  
  public static final byte MODE_IDLE = 0x00;
  public static final byte MODE_APPLICATION = 0x01;
  public static final byte MODE_NOTIFICATION = 0x02;
  
  
  public static final byte MSG_SET_VIBRATE_MODE = 0x23;
  public static final byte MSG_WRITE_BUFFER = 0x40;
  public static final byte MSG_CONFIGURE_IDLE_BUFFER_SIZE = 0x42;
  public static final byte MSG_UPDATE_DISPLAY = 0x43;
  public static final byte MSG_LOAD_TEMPLATE = 0x44;
  
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
      case 0x34:
        Log.v(DeviceService.TAG, "Button press!");
        byte button = message[4];
        if (listener != null) {
          // TODO: Make this less insane!
          Intent intent = new Intent(ApolloIntents.INTENT_BUTTON_PRESS);
          intent.putExtra(ApolloIntents.EXTRA_BUTTONS, button);
          listener.buttonPressed(ApolloIntents.ButtonPress.parseIntent(intent));
        }
      break;
    }
  }
  
  public void setNativeClockVisible(boolean isVisible) {
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    message.write(MSG_CONFIGURE_IDLE_BUFFER_SIZE);
    message.write(OPTION_BITS_UNUSED);
    message.write(isVisible ? (byte) 0x00 : (byte) 0x01);
    send(message.toByteArray());
  }
  
  public void vibrate(int timeOnMs, int timeOffMs, int cycles) {
    timeOnMs = Math.min(timeOnMs, MAX_16_BIT_VALUE);
    timeOffMs = Math.min(timeOffMs, MAX_16_BIT_VALUE);
    cycles = Math.min(cycles, 255);
    
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
  
  public void updateScreen(byte[] buffer, byte mode) {
    boolean screenChanged = false;
    if (!bufferCleared[mode]) {
      screenChanged = true;
      writeSolidColorToBuffer(mode, (byte) 0x00);
      bufferCleared[mode] = true;
    }
    
    for (int row = 0; row < 96; row += 2) {
      boolean different = false;
      for (int rowOffset = 0; rowOffset < 2; rowOffset++) {
        int rowStart = ((row + rowOffset) * 12);
        for (int i = 0; i < 12; i++) {
          if (buffer[rowStart + i] != screenBuffers[mode][rowStart + i]) {
            different = true;
            screenBuffers[mode][rowStart + i] = buffer[rowStart + i];
          }
        }
      }
      if (different) {
        screenChanged = true;
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        message.write(MSG_WRITE_BUFFER);
        message.write(mode);
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
    
    if (screenChanged) {
      ByteArrayOutputStream message = new ByteArrayOutputStream();
      message.write(MSG_UPDATE_DISPLAY);
      message.write(mode | 0x10);
      send(message.toByteArray());
    }
  }
  
  public void writeSolidColorToBuffer(byte mode, byte color) {
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    message.write(MSG_LOAD_TEMPLATE);
    message.write(mode);
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
  }
  
  
}
