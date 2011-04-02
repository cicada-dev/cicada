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
  public static final String INTENT_IDLE_BUTTON_PRESS =
      "com.smartmadsoft.openwatch.apollo.command.IDLE_BUTTON_PRESS";
  public static final String EXTRA_BUTTONS = "button";
  
  public static final String INTENT_PUSH_BITMAP =
      "com.smartmadsoft.openwatch.apollo.action.APPLICATION_MODE_GRAPHICS";
  public static final String EXTRA_BITMAP_ARRAY = "data";
  public static final String EXTRA_BITMAP_BUFFER = "buffer";

  public static final String INTENT_VIBRATE = "com.smartmadsoft.openwatch.apollo.action.VIBRATE";
  public static final String EXTRA_VIBRATE_ON_MSEC = "on";
  public static final String EXTRA_VIBRATE_OFF_MSEC = "off";
  public static final String EXTRA_VIBRATE_NUM_CYCLES = "cycles";
  
  private ApolloIntents() {
  }
  
  public static void pushBitmap(Context context, Bitmap bitmap) {
    Intent intent = new Intent(INTENT_PUSH_BITMAP);
    intent.putExtra(EXTRA_BITMAP_BUFFER, BitmapUtil.bitmapToBuffer(bitmap));
    context.sendBroadcast(intent);
  }
  
  public static void vibrate(Context context, int onMsec, int offMsec, int numCycles) {
    Intent intent = new Intent(INTENT_VIBRATE);
    intent.putExtra(EXTRA_VIBRATE_ON_MSEC, onMsec);
    intent.putExtra(EXTRA_VIBRATE_OFF_MSEC, offMsec);
    intent.putExtra(EXTRA_VIBRATE_NUM_CYCLES, numCycles);
    context.sendBroadcast(intent);
  }
  
  public static class IdleButtonPress {
    private final byte pressedButtons;
    
    private IdleButtonPress(byte pressedButtons) {
      this.pressedButtons = pressedButtons;
    }
    
    public static IdleButtonPress parseIntent(Intent intent) {
      if (!intent.getAction().equals(INTENT_IDLE_BUTTON_PRESS) || !intent.hasExtra(EXTRA_BUTTONS)) {
        return null;
      }
      
      return new IdleButtonPress(intent.getByteExtra(EXTRA_BUTTONS, (byte) 0));
    }
    
    private static byte bitfieldFromButtons(Button... buttons) {
      byte bitfield = (byte) 0;
      for (Button button : buttons) {
        bitfield |= button.value();
      }
      return bitfield;
    }
    
    public boolean hasButtonsPressed(Button... buttons) {
      return (pressedButtons & bitfieldFromButtons(buttons)) != 0;
    }
    
    public boolean hasOnlyButtonsPressed(Button... buttons) {
      return pressedButtons == bitfieldFromButtons(buttons);
    }
  }
}
