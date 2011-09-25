package org.cicadasong.samples.bitly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.cicadasong.cicadalib.CicadaApp;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class Bitly extends CicadaApp {
  public static final String TAG = Bitly.class.getSimpleName();

  // Update every 2 minutes
  public static final int STATUS_UPDATE_INTERVAL_MSEC = 15 * 60 * 1000;

  public static final String LOGIN = "";  // Your bitly login here
  public static final String API_KEY = "";  // Your bitly API key
  public static final String HASH = "owYORw";  // Link hash (without domain name)
  public static final String TITLE = "";  // TODO: Get this from the API
  private Runnable updateStatusTask;
  private Handler handler;
  private Stats stats = null;
  private boolean inInitialFetch = true;
  
  // Paint objects for different fonts
  private Paint metawatch11px;
  private Paint metawatch7px;
  private Paint metawatch5px;
  private Paint default10pt;
  private Bitmap logo;
  
  @Override
  public void onCreate() {
    createFontPainters();
    logo = BitmapFactory.decodeResource(getResources(), R.drawable.bitly);
  
    super.onCreate();
  }

  private void createFontPainters() {
    Typeface fontMetawatch11px =
        Typeface.createFromAsset(getAssets(), "fonts/metawatch_16pt_11pxl_proto1.ttf");
    metawatch11px = new Paint();
    metawatch11px.setTypeface(fontMetawatch11px);
    metawatch11px.setTextSize(16);
    
    Typeface fontMetawatch7px =
        Typeface.createFromAsset(getAssets(), "fonts/metawatch_8pt_7pxl_CAPS_proto1.ttf");
    metawatch7px = new Paint();
    metawatch7px.setTypeface(fontMetawatch7px);
    metawatch7px.setTextSize(8);
    
    Typeface fontMetawatch5px =
        Typeface.createFromAsset(getAssets(), "fonts/metawatch_8pt_5pxl_CAPS_proto1.ttf");
    metawatch5px = new Paint();
    metawatch5px.setTypeface(fontMetawatch5px);
    metawatch5px.setTextSize(8);
    
    default10pt = new Paint();
    default10pt.setTextSize(10);
  }

  @Override
  protected void onResume() {
    Log.v(TAG, "Bitly activated in mode: " + getCurrentMode());

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
  protected void onButtonPress(WatchButton button) {
  }

  protected void onDraw(Canvas canvas) {
    // Set font for heading
    Paint paint = isWidget() ? metawatch5px : metawatch7px;

    int x = 2;
    int y = 2 + (int) -paint.ascent();
    
    y += (int) paint.descent();

    // Set font for stop name
    paint = isWidget() ? metawatch5px : metawatch7px;
    y += (int) -paint.ascent();
    
    canvas.drawText(TITLE, x, y, paint);
    
    y += (int) paint.descent() + 2;

    // Set font for "body"
    paint = isWidget() ? metawatch7px : metawatch11px;
    paint = metawatch11px;
    y -= 1;
    canvas.drawBitmap(logo, 1, y, paint);
    x += logo.getWidth() + 2;
    y += (int) -paint.ascent();
    
    if (inInitialFetch) {
      canvas.drawText("Fetching...", x, y, paint);
    } else if (stats == null) {
      canvas.drawText("Network Error", x, y, paint);
    } else {
      String singleLineResult = "" + stats.globalClicks;
      canvas.drawText(singleLineResult, x, y, paint);
      
      if (stats.clicksByDay != null) {
        StringBuilder resultBuilder = new StringBuilder();
        int max = 0;
        for (int foo : stats.clicksByDay) {
          max = Math.max(max, foo);
          resultBuilder.append(foo);
          resultBuilder.append(", ");
        }
        Rect bounds = new Rect();
        paint.getTextBounds(singleLineResult, 0, singleLineResult.length(), bounds);
        x += bounds.width() + 2;
        y += 2;
        int height = (int) -paint.ascent();
        int width = (96 - x) / stats.clicksByDay.length;
        for (int hits : stats.clicksByDay) {
          int barHeight = (int) (((float)hits/(float)max) * height);
          canvas.drawRect(x, y -barHeight, x+width, y, paint);
          x += width;
        }
      }
    }
  }

  private void processStatusUpdate(Stats newStats) {
    if (!isActive()) {
      return;
    }
    
    inInitialFetch = false;
    stats = newStats;

    invalidate();

    handler.postDelayed(updateStatusTask, STATUS_UPDATE_INTERVAL_MSEC);
  }

  private class GetTimesTask extends AsyncTask<Void, Void, Stats> {
    @Override
    protected void onPostExecute(Stats result) {
      processStatusUpdate(result);
    }
    
    @Override
    protected Stats doInBackground(Void... params) {
      HttpURLConnection connection = null;
      Stats statResult = null;
      try {
        String urlString = "http://api.bitly.com/v3/clicks?login=" +
            LOGIN + "&apiKey=" + API_KEY + "&hash=" + HASH;
        URL url = new URL(urlString);
        connection = (HttpURLConnection) url.openConnection();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          String response = convertStreamToString(connection.getInputStream());
          try {
            statResult = new Stats();
            JSONObject responseObj = new JSONObject(response);
            JSONObject dataObj = responseObj.getJSONObject("data");
            JSONObject clickObj = dataObj.getJSONArray("clicks").getJSONObject(0);
            statResult.yourClicks = clickObj.getInt("user_clicks");
            statResult.globalClicks = clickObj.getInt("global_clicks");
            Log.v(TAG,
                "Got click results " + statResult.yourClicks + "/" + statResult.globalClicks);
          } catch (JSONException e) {
            Log.e(TAG, "Error decoding response: " + response);
          }
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

      if (statResult != null) {
      try {
        String urlString = "http://api.bitly.com/v3/clicks_by_day?login=" +
            LOGIN + "&apiKey=" + API_KEY + "&hash=" + HASH;
        URL url = new URL(urlString);
        connection = (HttpURLConnection) url.openConnection();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          String response = convertStreamToString(connection.getInputStream());
          try {
            JSONObject responseObj = new JSONObject(response);
            JSONObject dataObj = responseObj.getJSONObject("data");
            JSONObject clickObj = dataObj.getJSONArray("clicks_by_day").getJSONObject(0);
            JSONArray clicksArray = clickObj.getJSONArray("clicks");
            statResult.clicksByDay = new int[clicksArray.length()];
            for (int i = 0; i < clicksArray.length(); i++) {
              int clicks = clicksArray.getJSONObject(i).getInt("clicks");
              statResult.clicksByDay[clicksArray.length() - 1 - i] = clicks;
            }
            
            statResult.yourClicks = clickObj.getInt("user_clicks");
            statResult.globalClicks = clickObj.getInt("global_clicks");
            Log.v(TAG,
                "Got click results " + statResult.yourClicks + "/" + statResult.globalClicks);
          } catch (JSONException e) {
            Log.e(TAG, "Error decoding response: " + response);
          }
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
      }
      
      return statResult;
    }
  }

  private static String convertStreamToString(InputStream is) {
    /*
     * To convert the InputStream to String we use the BufferedReader.readLine()
     * method. We iterate until the BufferedReader return null which means
     * there's no more data to read. Each line will appended to a StringBuilder
     * and returned as String.
     */
    BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
    StringBuilder sb = new StringBuilder();

    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        sb.append(line + "\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return sb.toString();
  }
  
  private static class Stats {
    public String title;
    public int yourClicks;
    public int globalClicks;
    public int[] clicksByDay;
  }
}
