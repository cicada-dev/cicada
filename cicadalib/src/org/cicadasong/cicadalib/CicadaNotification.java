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

import org.cicadasong.apollo.BitmapUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;

/**
 * This class represents a notification that an app wants to send to the watch.  It can be as
 * simple as a text screen, or as complicated as a text string fit into a template image,
 * with a vibration pattern.
 */

public class CicadaNotification {
  private String body;
  private int id;
  private String packageName;
  private String className;
  private byte[] screenBuffer;
  private Rect bodyRect;
  
  private CicadaNotification() {
  }
  
  public static CicadaNotification createWithText(Context context, String text) {
    CicadaNotification note = new CicadaNotification();
    note.packageName = context.getPackageName();
    note.className = context.getClass().getName();
    note.body = text;
    return note;
  }
  
  public static CicadaNotification createWithBitmap(Context context, Bitmap bitmap) {
    CicadaNotification note = new CicadaNotification();
    note.packageName = context.getPackageName();
    note.className = context.getClass().getName();
    note.screenBuffer = BitmapUtil.bitmapToBuffer(bitmap);
    return note;
  }
  
  public static CicadaNotification createWithTextAndTemplate(
      Context context, String text, Bitmap bitmap, Rect textRect) {
    CicadaNotification note = new CicadaNotification();
    note.packageName = context.getPackageName();
    note.className = context.getClass().getName();
    note.body = text;
    note.bodyRect = new Rect(textRect);
    note.screenBuffer = BitmapUtil.bitmapToBuffer(bitmap);
    return note;
  }
  
  public static CicadaNotification createFromBundle(Bundle bundle) {
    CicadaNotification note = new CicadaNotification();
    note.id = bundle.getInt(CicadaIntents.EXTRA_NOTIFICATION_ID, 0);
    note.body = bundle.getString(CicadaIntents.EXTRA_NOTIFICATION_BODY);
    note.bodyRect = rectFromArray(bundle.getIntArray(CicadaIntents.EXTRA_NOTIFICATION_BODY_RECT));
    note.packageName = bundle.getString(CicadaIntents.EXTRA_PACKAGE_NAME);
    note.className = bundle.getString(CicadaIntents.EXTRA_CLASS_NAME);
    note.screenBuffer = bundle.getByteArray(CicadaIntents.EXTRA_BUFFER);
    
    return note;
  }
  
  public Bundle toBundle() {
    Bundle bundle = new Bundle();
    bundle.putInt(CicadaIntents.EXTRA_NOTIFICATION_ID, id);
    
    if (isNonEmpty(body)) {
      bundle.putString(CicadaIntents.EXTRA_NOTIFICATION_BODY, body);
    }
    
    if (isNonEmpty(packageName)) {
      bundle.putString(CicadaIntents.EXTRA_PACKAGE_NAME, packageName);
    }
    
    if (isNonEmpty(className)) {
      bundle.putString(CicadaIntents.EXTRA_CLASS_NAME, className);
    }
    
    if (bodyRect != null) {
      bundle.putIntArray(CicadaIntents.EXTRA_NOTIFICATION_BODY_RECT, rectToArray(bodyRect));
    }
    
    if (screenBuffer != null) {
      bundle.putByteArray(CicadaIntents.EXTRA_BUFFER, screenBuffer);
    }
    
    return bundle;
  }
  
  public String getPackageName() {
    return packageName;
  }
  
  public String getClassName() {
    return className;
  }
  
  public String getText() {
    return body;
  }
  
  public byte[] getScreenBuffer() {
    return screenBuffer;
  }
  
  public Rect getBodyRect() {
    return bodyRect;
  }

  private static boolean isNonEmpty(String string) {
    return string != null && string.length() > 0;
  }
  
  private static Rect rectFromArray(int[] bounds) {
    if (bounds == null || bounds.length < 4) {
      return null;
    }
    return new Rect(bounds[0], bounds[1], bounds[2], bounds[3]);
  }
  
  private static int[] rectToArray(Rect rect) {
    return new int[]{rect.left, rect.top, rect.right, rect.bottom};
  }
}
