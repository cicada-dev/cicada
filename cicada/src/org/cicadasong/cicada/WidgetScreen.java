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

import java.util.Arrays;

import org.cicadasong.cicadalib.CicadaApp.AppType;

public class WidgetScreen {
  public static final AppDescription DESCRIPTION =
      new AppDescription("WIDGET_SCREEN", "WIDGET_SCREEN", "Widget Screen", AppType.APP);
  
  public static final int NUM_WIDGETS = 3;
  
  private int BUFFER_LENGTH = (96 * (96 / NUM_WIDGETS)) / 8;
  private byte[] screenBuffer = new byte[96 * 96 / 8];
  private int[] sessionIds = new int[3];
  public AppDescription widgets[] = new AppDescription[] { null, null, null };
  
  public void putSessionId(int index, int id) {
    sessionIds[index] = id;
  }
  
  public void clearSessionIds() {
    sessionIds = new int[NUM_WIDGETS];
  }
  
  public boolean hasSessionId(int id) {
    for (int i = 0; i < sessionIds.length; i++) {
      if (id == sessionIds[i]) {
        return true;
      }
    }
    return false;
  }
  
  public void clearBuffer() {
    Arrays.fill(screenBuffer, (byte) 0);
  }
  
  /**
   * Updates the widget screen buffer using the widgetBuffer sent by a widget.
   * 
   * @param widgetBuffer the buffer sent by a widget
   * @param sessionId the sessionId of the sender; used to determine which part of the screen to
   *        update
   * @return the updated widget screen buffer
   */
  public byte[] updateScreenBuffer(byte[] widgetBuffer, int sessionId) {
    for (int i = 0; i < widgets.length; i++) {
      if ((widgets[i] != null) && (sessionId == sessionIds[i])) {
        System.arraycopy(widgetBuffer, 0, screenBuffer, i * BUFFER_LENGTH, BUFFER_LENGTH);
        break;
      }
    }
    
    return screenBuffer;
  }
}
