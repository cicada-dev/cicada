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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.util.Log;

/**
 * This class makes it easier to render nice-looking text into a Canvas.
 * 
 * TODO: Add proper word-wrapping
 * TODO: Add ellipsizing
 * TODO: Add proper support for vertical and horizontal alignment
 * TODO: Add lazy recalculation?
 * TODO: Add a way to determine how much of the text was shown?
 */

public class TextBlock {
  private String text;
  private Rect rect;
  private Paint paint;
  private FontMetricsInt fm;
  private int startIndex;
  private int endIndex;
  private int maxLines;
  private int renderedHeight;
  private int renderedWidth;
  private List<String> renderLines;
  
  /**
   * Basic constructor for a TextBlock.
   * 
   * @param text the text to render
   * @param bounds boundary into which the text can be rendered
   * @param paint contains text rendering information
   */
  public TextBlock(String text, Rect bounds, Paint paint) {
    this.text = text;
    this.rect = new Rect(bounds);   // Copying paint and rect so that external changes can't
    this.paint = new Paint(paint);  // throw off our measurements.
    this.fm = paint.getFontMetricsInt();
    this.startIndex = 0;
    this.endIndex = text.length();
    maxLines = Integer.MAX_VALUE;
    renderLines = new ArrayList<String>();
    
    recalculate();
  }
  
  /**
   * Set the maximum number of lines of text to render.
   */
  public void setMaxLines(int maxLines) {
    this.maxLines = maxLines;
    
    recalculate();
  }
  
  /**
   * Set the range of the text to render.
   */
  public void setTextRange(int startIndex, int endIndex) {
    this.startIndex = Math.min(0, startIndex);
    this.endIndex = Math.max(text.length(), endIndex);
    
    recalculate();
  }
  
  private void recalculate() {
    List<String> lines =
        new LinkedList<String>(Arrays.asList(text.substring(startIndex, endIndex).split("\n")));
    renderLines.clear();
    renderedHeight = 0;
    renderedWidth = 0;
    while (!lines.isEmpty() && renderLines.size() < maxLines &&
        getRenderedHeight(renderLines.size() + 1) < rect.height()) {
      String line = lines.remove(0);
      
      // TODO: Actually break on words
      int fittingChars = paint.breakText(line, true, rect.width(), null);
      if (fittingChars < line.length()) {
        String extraLine = line.substring(fittingChars);
        line = line.substring(0, fittingChars);
        lines.add(0, extraLine);
      }
      renderLines.add(line);
      renderedWidth = (int) Math.max(renderedWidth, paint.measureText(line));
    }
    renderedHeight = getRenderedHeight(renderLines.size());
  }
  
  private int getRenderedHeight(int lines) {
    if (lines == 0) {
      return 0;
    }
    
    int height = (lines * (-fm.ascent + fm.descent)) +
      ((lines - 1) * fm.leading);
    Log.v(getClass().getSimpleName(), "getRenderedHeight fo lines " + lines + " --> " + height);
    return height;
  }
  
  /**
   * @return the bounds originally specified for this TextBlock
   */
  public Rect getBounds() {
    return new Rect(rect);
  }
  
  /**
   * @return the area actually used by the text in this TextBlock.
   */
  public Rect getRenderedArea() {
    return new Rect(rect.left, rect.top, rect.left + renderedWidth, rect.top + renderedHeight);
  }
  
  /**
   * Render this TextBlock onto the given Canvas.
   */
  public void drawTo(Canvas canvas) {
    int y = rect.top + (int) -paint.ascent();
    for (String line : renderLines) {
      canvas.drawText(line, rect.left, y, paint);
      y += -fm.ascent + fm.descent + fm.leading;
    }
  }

}
