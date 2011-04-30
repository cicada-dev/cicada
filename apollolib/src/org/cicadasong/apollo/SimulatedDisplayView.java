package org.cicadasong.apollo;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

public class SimulatedDisplayView extends View {
  private Bitmap bitmap;

  public SimulatedDisplayView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void clearDisplay() {
    bitmap = null;
    invalidate();
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
