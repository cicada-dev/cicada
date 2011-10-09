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

package org.cicadasong.cicada;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.cicadasong.apollo.ApolloConfig;
import org.cicadasong.apollo.BitmapUtil;
import org.cicadasong.cicadalib.CicadaNotification;
import org.cicadasong.cicadalib.TextBlock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public class NotificationRenderer {
  private static final int TEXT_MARGIN = 3;
  
  private Paint metawatch11px;
  private Paint metawatch7px;
  private Paint metawatch5px;

  public NotificationRenderer(Context context) {
    createFontPainters(context);
  }
  
  private void createFontPainters(Context context) {
    Typeface fontMetawatch11px =
        Typeface.createFromAsset(context.getAssets(), "fonts/metawatch_16pt_11pxl_proto1.ttf");
    metawatch11px = new Paint();
    metawatch11px.setTypeface(fontMetawatch11px);
    metawatch11px.setTextSize(16);
    
    Typeface fontMetawatch7px =
        Typeface.createFromAsset(context.getAssets(), "fonts/metawatch_8pt_7pxl_CAPS_proto1.ttf");
    metawatch7px = new Paint();
    metawatch7px.setTypeface(fontMetawatch7px);
    metawatch7px.setTextSize(8);
    
    Typeface fontMetawatch5px =
        Typeface.createFromAsset(context.getAssets(), "fonts/metawatch_8pt_5pxl_CAPS_proto1.ttf");
    metawatch5px = new Paint();
    metawatch5px.setTypeface(fontMetawatch5px);
    metawatch5px.setTextSize(8);
  }
  
  public Bitmap renderNotification(CicadaNotification note) {
    Bitmap result = Bitmap.createBitmap(
            ApolloConfig.DISPLAY_WIDTH, ApolloConfig.DISPLAY_HEIGHT, Bitmap.Config.RGB_565);
    Canvas canvas = new Canvas(result);
    canvas.drawColor(Color.WHITE);
    if (note.getScreenBuffer() != null) {
      Bitmap notificationBitmap = BitmapUtil.bufferToBitmap(note.getScreenBuffer());
      canvas.drawBitmap(notificationBitmap, 0, 0, new Paint());
    }
    
    if (note.getText() != null && note.getText().length() > 0) {
      // TODO: Get text size from notification?
      renderText(canvas, note.getText(), 0, note.getBodyRect());
    }
    
    return result;
  }
  
  private void renderText(Canvas canvas, String text, int fontSize, Rect bounds) {
    if (bounds == null) {
      bounds = new Rect(0, 0, ApolloConfig.DISPLAY_WIDTH, ApolloConfig.DISPLAY_HEIGHT);
    }
    bounds.inset(3, 3);
    
    // TODO: Use fontSize and adjust font size to fit text?
    Paint textPaint = metawatch11px;
    
    TextBlock bodyBlock = new TextBlock(text, bounds, textPaint);
    bodyBlock.drawTo(canvas);
  }

}
