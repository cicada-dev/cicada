package org.cicadasong.apollo;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class SimulatedDisplayView extends View {
  private Bitmap bitmap;
  private Bitmap textBitmap;
  private Canvas textCanvas;
  private Paint textPaint;
  

  public SimulatedDisplayView(Context context, AttributeSet attrs) {
    super(context, attrs);
    
    // For this to work, the project using this view has to symlink to the apollolib fonts directory
    // from within its assets directory (or copy the relevant font into the project).
    Typeface fontMetawatch11px =
        Typeface.createFromAsset(context.getAssets(), "fonts/metawatch_16pt_11pxl_proto1.ttf");
    textPaint = new Paint();
    textPaint.setTypeface(fontMetawatch11px);
    textPaint.setTextSize(16);
  }

  public void clearDisplay() {
    bitmap = null;
    invalidate();
  }
  
  public void setText(String text) {
    if (textCanvas == null) {
      textBitmap = Bitmap.createBitmap(
          ApolloConfig.DISPLAY_WIDTH, ApolloConfig.DISPLAY_HEIGHT, Bitmap.Config.RGB_565);
      textCanvas = new Canvas(textBitmap);
    }
    
    textCanvas.drawColor(Color.WHITE);
    
    // Here we try to simulate the text-display behavior of the core Apollo app.
    int x = 3;
    int y = (int) -textPaint.ascent() + 1;
    List<String> lines = new LinkedList<String>(Arrays.asList(text.split("\n")));
    while(lines.size() > 0) {
      String line = lines.remove(0);
      
      int fittingChars = textPaint.breakText(line, true, ApolloConfig.DISPLAY_WIDTH - x, null);
      if (fittingChars < line.length()) {
        String extraLine = line.substring(fittingChars);
        line = line.substring(0, fittingChars);
        lines.add(0, extraLine);
      }
      
      textCanvas.drawText(line, x, y, textPaint);
      y += textPaint.getFontSpacing() - 1;
      
    }
    
    // Normally we'd have to crush this to monochrome, but we're using a bitmap font.
    setBitmap(textBitmap);
  }
  
  public void setByteBuffer(byte[] buffer) {
    if (buffer != null) {
      setBitmap(BitmapUtil.bufferToBitmap(buffer));
    }
  }
  
	public void setBitmap(Bitmap srcBitmap) {
	  if (bitmap == null) {
	    bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.RGB_565);
	  }
	  
	  int srcWidth = srcBitmap.getWidth();
	  int srcHeight = srcBitmap.getHeight();
	  int destWidth = bitmap.getWidth();
	  int destHeight = bitmap.getHeight();
	  
	  for (int y = 0; y < destHeight; y++) {
	    for (int x = 0; x < destWidth; x++) {
	      bitmap.setPixel(x, y, srcBitmap.getPixel((x * srcWidth)/destWidth, (y * srcHeight)/destHeight));
	    }
	  
	  }
	  invalidate();
	}
	public void onDraw(Canvas canvas) {
	  if (bitmap != null) {
	    canvas.drawColor(Color.BLACK);
	    canvas.drawBitmap(bitmap, 0, 0, null);
	  } else {
	    canvas.drawColor(Color.GRAY);
	  }
	}
}
