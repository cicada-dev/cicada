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

package org.cicadasong.apollo;

import org.cicadasong.apollo.ApolloConfig.Button;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

/**
 * Defines the intent interface for communicating with Apollo.
 */

public class ApolloIntents {
  public static final String TAG = "ApolloIntents";
  
  public static final String INTENT_BUTTON_PRESS = "com.fossil.metawatch.BUTTON_PRESS";
  public static final String EXTRA_BUTTONS = "button";
  public static final String EXTRA_MODE = "mode";  // "application" or "idle"
  
  public static final String INTENT_PUSH_BITMAP =
      "com.fossil.metawatch.APPLICATION_UPDATE";
  public static final String EXTRA_BITMAP_ARRAY = "data";
  public static final String EXTRA_BITMAP_BUFFER = "buffer";
  
  public static final String INTENT_PUSH_TEXT =
      "com.smartmadsoft.openwatch.apollo.action.APPLICATION_MODE_TEXT";
  public static final String EXTRA_TEXT = "text";

  public static final String INTENT_VIBRATE = "com.smartmadsoft.openwatch.apollo.action.VIBRATE";
  public static final String EXTRA_VIBRATE_ON_MSEC = "on";
  public static final String EXTRA_VIBRATE_OFF_MSEC = "off";
  public static final String EXTRA_VIBRATE_NUM_CYCLES = "cycles";
  
  public static final String INTENT_START_APPLICATION_MODE =
      "com.fossil.metawatch.APPLICATION_START";
  
  public static final String INTENT_STOP_APPLICATION_MODE =
       "com.fossil.metawatch.APPLICATION_STOP";
  
  public static final String INTENT_IDLE_BUTTONS_OVERRIDE =
      "com.fossil.metawatch.IDLE_BUTTONS_OVERRIDE";
  public static final String EXTRA_OVERRIDE_BUTTONS = "buttons";
  
  private ApolloIntents() {
  }
  
  public static void pushBitmap(Context context, Bitmap bitmap) {
    Intent intent = new Intent(INTENT_PUSH_BITMAP);
    intent.putExtra(EXTRA_BITMAP_BUFFER, BitmapUtil.bitmapToBuffer(bitmap));
    context.sendBroadcast(intent);
  }
  
  /**
   * Prints the given text to the display, forcibly breaking the text at the edge of the screen
   * (with no attention to words).  setApplicationMode(true) may need to be called first for
   * the text to appear.
   * 
   * @param context The context for the intent
   * @param text The text to push to Apollo; can contain newlines.
   */
  public static void pushText(Context context, String text) {
    Intent intent = new Intent(INTENT_PUSH_TEXT);
    intent.putExtra(EXTRA_TEXT, text);
    context.sendBroadcast(intent);
  }
  
  public static void vibrate(Context context, int onMsec, int offMsec, int numCycles) {
    Intent intent = new Intent(INTENT_VIBRATE);
    intent.putExtra(EXTRA_VIBRATE_ON_MSEC, onMsec);
    intent.putExtra(EXTRA_VIBRATE_OFF_MSEC, offMsec);
    intent.putExtra(EXTRA_VIBRATE_NUM_CYCLES, numCycles);
    context.sendBroadcast(intent);
  }
  
  public static void setApplicationMode(Context context, boolean enabled) {
    Intent intent = new Intent(enabled ?
        INTENT_START_APPLICATION_MODE : INTENT_STOP_APPLICATION_MODE);
    context.sendBroadcast(intent);
  }
  
  public static class ButtonPress {
    private final byte pressedButtons;
    
    private ButtonPress(byte pressedButtons) {
      this.pressedButtons = pressedButtons;
    }
    
    public static ButtonPress parseIntent(Intent intent) {
      if (!intent.getAction().equals(INTENT_BUTTON_PRESS)) {
        return null;
      }
      if (!intent.hasExtra(EXTRA_BUTTONS)) {
        return null;
      }
      
      return new ButtonPress(intent.getByteExtra(EXTRA_BUTTONS, (byte) 0));
    }
    
    private static byte bitfieldFromButtons(Button... buttons) {
      byte bitfield = (byte) 0;
      for (Button button : buttons) {
        bitfield |= button.value();
      }
      return bitfield;
    }
    
    public boolean hasButtonsPressedNonExclusive(Button... buttons) {
      return (pressedButtons & bitfieldFromButtons(buttons)) != 0;
    }
    
    public boolean hasButtonsPressed(Button... buttons) {
      return pressedButtons == bitfieldFromButtons(buttons);
    }
  }
}
