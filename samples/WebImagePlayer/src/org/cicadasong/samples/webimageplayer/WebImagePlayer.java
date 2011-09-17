package org.cicadasong.samples.webimageplayer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.cicadasong.cicadalib.CicadaApp;
import org.cicadasong.cicadalib.CicadaIntents.ButtonEvent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class WebImagePlayer extends CicadaApp {
  public static final String TAG = WebImagePlayer.class.getSimpleName();

  // Update interval and image URL
  // TODO: Make these preferences so that they can be easily tweaked.
  public static final int STATUS_UPDATE_INTERVAL_MSEC = 600 * 1000;
  public static final String IMAGE_URL = "https://github.com/" +
      "cicada-dev/cicada/raw/master/samples/hellocicada/res/drawable-hdpi/cicada.png";

  private Bitmap downloadedImage = null;
  
  private Runnable updateStatusTask;
  private Handler handler;
  private Paint paint;
  
  @Override
  public void onCreate() {
    paint = new Paint();
  
    super.onCreate();
  }

  @Override
  protected void onResume() {
    Log.v(TAG, "WebImagePlayer activated in mode: " + getCurrentMode());

    if (updateStatusTask == null) {
      updateStatusTask = new Runnable() {

        @Override
        public void run() {
          if (!isActive()) return;

          (new GetTimesTask()).execute();
        }
      };
    }
    if (handler == null) {
      handler = new Handler();
    }
    handler.removeCallbacks(updateStatusTask);
    handler.post(updateStatusTask);
  }

  @Override
  protected void onPause() {
    handler.removeCallbacks(updateStatusTask);
  }

  @Override
  protected void onButtonPress(ButtonEvent buttonEvent) {
  }

  protected void onDraw(Canvas canvas) {
    if (downloadedImage != null) {
      Rect src = new Rect(0, 0, downloadedImage.getWidth(), downloadedImage.getHeight());
      Rect canvasBounds = canvas.getClipBounds();
      Rect dst = canvasBounds;
      
      // Scale & center image to fit the display
      boolean tall = downloadedImage.getWidth() < downloadedImage.getHeight();
      if (tall) {
        int width = (int) (canvasBounds.width() *
            (float)downloadedImage.getWidth() / (float)downloadedImage.getHeight());
        dst.left = (canvasBounds.width() - width) / 2;
        dst.right = dst.left + width;
      } else {
        int height = (int) (canvasBounds.height() *
            (float)downloadedImage.getHeight() / (float)downloadedImage.getWidth());
        dst.top = (canvasBounds.height() - height) / 2;
        dst.bottom = dst.top + height;
      }
      canvas.drawBitmap(downloadedImage, src, dst, paint);
    }
    return;
  }

  private void processStatusUpdate(Bitmap bitmap) {
    if (!isActive()) {
      return;
    }
    
    if (bitmap != null) {
      downloadedImage = bitmap;
      // TODO: Only send to the watch if the image differs, either here or upstream.
      invalidate();
    }

    handler.postDelayed(updateStatusTask, STATUS_UPDATE_INTERVAL_MSEC);
  }

  private class GetTimesTask extends AsyncTask<Void, Void, Bitmap> {
    @Override
    protected void onPostExecute(Bitmap result) {
      processStatusUpdate(result);
    }
    
    @Override
    protected Bitmap doInBackground(Void... params) {
      HttpURLConnection connection = null;
      Bitmap bitmap = null;
      try {
        URL url = new URL(IMAGE_URL);
        connection = (HttpURLConnection) url.openConnection();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          bitmap = BitmapFactory.decodeStream(connection.getInputStream());
        }
      } catch (MalformedURLException e) {
        Log.e(TAG, "Malformed request URL: " + e);
      } catch (IOException e) {
        Log.e(TAG, "Connection error");
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
      }

      Log.v(TAG, "Image fetch for URL " + IMAGE_URL + " yielded " + bitmap);
      return bitmap;
    }
  }
}
