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

import android.graphics.Bitmap;
import android.graphics.Color;

public class BitmapUtil {
  
  private static final byte WHITE_PIXEL_BIT = 0x0;
  private static final byte BLACK_PIXEL_BIT = 0x1;
  
  private BitmapUtil() {
  }
  
  public static int[] bitmapToPixelArray(Bitmap bitmap) {
    int pixels[] = new int[ApolloConfig.DISPLAY_WIDTH * ApolloConfig.DISPLAY_HEIGHT];
    for (int x = 0; x < ApolloConfig.DISPLAY_WIDTH; x++) {
      for (int y = 0; y < ApolloConfig.DISPLAY_HEIGHT; y++) {
        int color = Color.BLACK;
        if (x < bitmap.getWidth() && y < bitmap.getHeight()) {
          color = bitmap.getPixel(x, y);
        }
        color = (Math.abs(Color.BLACK - color) < Math.abs(Color.WHITE - color)) ?
            Color.BLACK : Color.WHITE;
        pixels[(y * ApolloConfig.DISPLAY_WIDTH) + x] = color;
      }
    }
    return pixels;
  }
  
  public static byte[] bitmapToBuffer(Bitmap bitmap) {
    byte buffer[] = new byte[(ApolloConfig.DISPLAY_WIDTH * ApolloConfig.DISPLAY_HEIGHT) / 8];
    for (int x = 0; x < ApolloConfig.DISPLAY_WIDTH; x++) {
      for (int y = 0; y < ApolloConfig.DISPLAY_HEIGHT; y++) {
        int color = Color.BLACK;
        if (x < bitmap.getWidth() && y < bitmap.getHeight()) {
          color = bitmap.getPixel(x, y);
        }
        byte pixelBit = (Math.abs(Color.BLACK - color) < Math.abs(Color.WHITE - color)) ?
            BLACK_PIXEL_BIT : WHITE_PIXEL_BIT;
        int pixelIndex = (y * ApolloConfig.DISPLAY_WIDTH) + x;
        int byteIndex = pixelIndex / 8;
        buffer[byteIndex] = (byte) (buffer[byteIndex] | (pixelBit << (pixelIndex % 8)));
      }
    }
    return buffer;
  }

}
