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
// limitations under the License.package org.cicadasong.cicadalib;

package org.cicadasong.cicadalib;

import android.content.Context;
import android.content.Intent;

public class CicadaIntents {
  private CicadaIntents() {
  }
  
  public static final String PACKAGE_PREFIX = "org.cicadasong.cicada.";
  
  public static final String INTENT_ACTIVATE_APP = PACKAGE_PREFIX + "ACTIVATE_APP";
  public static final String INTENT_DEACTIVATE_APP = PACKAGE_PREFIX + "DEACTIVATE_APP";
  
  public static final String EXTRA_APP_MODE = "mode";
  public static final String EXTRA_APP_NAME = "name";
  public static final String EXTRA_SESSION_ID = "session_id";
  
  public static final String INTENT_PUSH_CANVAS = "org.cicadasong.cicada.PUSH_CANVAS";
  public static final String EXTRA_BUFFER = "buffer";
  
  public static final String INTENT_VIBRATE = PACKAGE_PREFIX + "VIBRATE";
  public static final String EXTRA_VIBRATE_ON_MSEC = "on";
  public static final String EXTRA_VIBRATE_OFF_MSEC = "off";
  public static final String EXTRA_VIBRATE_NUM_CYCLES = "cycles";
  
  public static final String INTENT_BUTTON_EVENT = "org.cicadasong.cicada.BUTTON_EVENT";
  public static final String EXTRA_BUTTONS = "buttons";
  
  public enum Button {
    TOP_RIGHT    ((byte) 1),
    MIDDLE_RIGHT ((byte) 2),
    BOTTOM_RIGHT ((byte) 4),
    TOP_LEFT     ((byte) 64),
    MIDDLE_LEFT  ((byte) 32),
    BOTTOM_LEFT  ((byte) 8);
    
    private final byte value;
    
    private Button(byte value) {
      this.value = value;
    }
    
    public byte value() {
      return value;
    }
  }
  
  public static class ButtonEvent {
    private final byte pressedButtons;
    
    private ButtonEvent(byte pressedButtons) {
      this.pressedButtons = pressedButtons;
    }
    
    public static ButtonEvent parseIntent(Intent intent) {
      if (!intent.getAction().equals(INTENT_BUTTON_EVENT) || !intent.hasExtra(EXTRA_BUTTONS)) {
        return null;
      }
      
      return new ButtonEvent(intent.getByteExtra(EXTRA_BUTTONS, (byte) 0));
    }
    
    public static void sendIntent(Context context, Button... buttons) {
      Intent intent = new Intent(INTENT_BUTTON_EVENT);
      intent.putExtra(EXTRA_BUTTONS, bitfieldFromButtons(buttons));
      context.sendBroadcast(intent);
    }
    
    public static void sendIntent(Context context, byte buttons) {
      Intent intent = new Intent(INTENT_BUTTON_EVENT);
      intent.putExtra(EXTRA_BUTTONS, buttons);
      context.sendBroadcast(intent);
    }

    private static byte bitfieldFromButtons(Button... buttons) {
      byte bitfield = (byte) 0;
      for (Button button : buttons) {
        bitfield |= button.value();
      }
      return bitfield;
    }
    
    /**
     * Returns true if the given buttons are pressed in this event.  (Other buttons may also
     * be pressed at the same time.)  Returns false if some of the given buttons are not
     * current pressed.
     */
    public boolean hasButtonsPressedNonExclusive(Button... buttons) {
      return (pressedButtons & bitfieldFromButtons(buttons)) != 0;
    }
    
    /**
     * Returns true if the given buttons are pressed in the current event.  This method will return
     * false if buttons not named in the parameter are also pressed; if you don't care about the
     * state of the other buttons, use hasButtonsPressedNonExclusive() instead.
     */
    public boolean hasButtonsPressed(Button... buttons) {
      return pressedButtons == bitfieldFromButtons(buttons);
    }
  }
}
