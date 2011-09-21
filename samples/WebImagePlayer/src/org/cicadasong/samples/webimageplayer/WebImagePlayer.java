package org.cicadasong.samples.webimageplayer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.cicadasong.cicadalib.CicadaApp;
import org.cicadasong.cicadalib.CicadaIntents.ButtonEvent;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class WebImagePlayer extends CicadaApp implements OnSharedPreferenceChangeListener {
  public static final String TAG = WebImagePlayer.class.getSimpleName();

  private Bitmap downloadedImage = null;
  
  private Runnable updateStatusTask;
  private Handler handler;
  private Paint paint;
  private String imageUrl;
  private int updateIntervalMsec;
  private boolean dither;
  
  @Override
  public void onCreate() {
    paint = new Paint();
  
    super.onCreate();
  }

  @Override
  protected void onResume() {
    Log.v(TAG, "WebImagePlayer activated in mode: " + getCurrentMode());

    Preferences.getSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    readPreferences();
    if (updateStatusTask == null) {
      updateStatusTask = new Runnable() {

        @Override
        public void run() {
          if (!isActive()) return;

          (new GetTimesTask()).execute(imageUrl);
        }
      };
    }
    if (handler == null) {
      handler = new Handler();
    }
    handler.removeCallbacks(updateStatusTask);
    handler.post(updateStatusTask);
  }

  private void readPreferences() {
    imageUrl = Preferences.getImageUrl(this);
    updateIntervalMsec = Preferences.getUpdateFrequency(this) * 1000;
    dither = Preferences.getDithering(this);
  }

  @Override
  protected void onPause() {
    Preferences.getSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    handler.removeCallbacks(updateStatusTask);
  }

  @Override
  protected void onButtonPress(ButtonEvent buttonEvent) {
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    readPreferences();
    handler.removeCallbacks(updateStatusTask);
    handler.post(updateStatusTask);
  }

  protected void onDraw(Canvas canvas) {
    if (downloadedImage != null) {
      Rect src = new Rect(0, 0, downloadedImage.getWidth(), downloadedImage.getHeight());
      Rect canvasBounds = canvas.getClipBounds();
      Rect dst = new Rect(canvasBounds);
      
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
      
      if (!dither) {
        canvas.drawBitmap(downloadedImage, src, dst, paint);
      } else {
        Bitmap scaledBitmap = Bitmap.createBitmap(
            canvasBounds.width(), canvasBounds.height(), Bitmap.Config.ARGB_8888);
        Canvas scaledCanvas = new Canvas(scaledBitmap);
        
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        scaledCanvas.drawBitmap(downloadedImage, src, dst, paint);
        
        // TODO: Implement dithering
        
        canvas.drawBitmap(scaledBitmap, 0, 0, paint);
      }
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

    handler.postDelayed(updateStatusTask, updateIntervalMsec);
  }

  private class GetTimesTask extends AsyncTask<String, Void, Bitmap> {
    @Override
    protected void onPostExecute(Bitmap result) {
      processStatusUpdate(result);
    }
    
    @Override
    protected Bitmap doInBackground(String... params) {
      HttpURLConnection connection = null;
      Bitmap bitmap = null;
      String imageUrlString = params[0];
      try {
        URL url = new URL(imageUrlString);
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

      Log.v(TAG, "Image fetch for URL " + imageUrlString + " yielded " + bitmap);
      return bitmap;
    }
  }
}
