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

package org.cicadasong.samples.nextbuses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cicadasong.cicadalib.CicadaApp;
import org.cicadasong.cicadalib.CicadaIntents.ButtonEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

/**
 * An example app that displays the next several bus times at a given stop.  At present, the
 * stop is hard-coded; a real app would offer a phone-side setup UI to pick the stop, and/or
 * possibly autodetect the nearest stop(s) based on the phone's current location.
 */
public class NextBuses extends CicadaApp {
  public static final String TAG = NextBuses.class.getSimpleName();

  // Update every 2 minutes
  public static final int STATUS_UPDATE_INTERVAL_MSEC = 2 * 60 * 1000;

  private String stopName = "3rd St & Howard St";
  private Runnable updateStatusTask;
  private Handler handler;
  private PredictionSet predictions = null;
  private boolean inInitialFetch = true;
  
  // Paint objects for different fonts
  private Paint metawatch11px;
  private Paint metawatch7px;
  private Paint metawatch5px;
  private Paint default10pt;
  
  @Override
  public void onCreate() {
    createFontPainters();
  
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
    Log.v(TAG, "Next Buses activated in mode: " + getCurrentMode());

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
    // Set font for heading
    Paint paint = isWidget() ? metawatch5px : metawatch7px;

    int x = 2;
    int y = 2 + (int) -paint.ascent();
    
    canvas.drawText("Next buses at...", x, y, paint);
    
    y += (int) paint.descent() + 2;

    // Set font for stop name
    paint = isWidget() ? metawatch7px : metawatch11px;
    y += (int) -paint.ascent();
    
    canvas.drawText(stopName, x, y, paint);
    
    y += (int) paint.descent() + 2;

    // Set font for "body"
    paint = isWidget() ? metawatch5px : default10pt;
    y += (int) -paint.ascent();
    
    if (inInitialFetch) {
      canvas.drawText("Fetching...", x, y, paint);
    } else if (predictions == null) {
      canvas.drawText("Network Error", x, y, paint);
    } else if (isWidget()) {
      String singleLineResult = "";
      List<Prediction> allPredictions = predictions.getAllPredictions();
      StringBuilder resultBuilder = new StringBuilder();
      for (Prediction prediction : allPredictions) {
        if (resultBuilder.length() > 0) {
          resultBuilder.append(" ");
        }
        resultBuilder.append(prediction.route);
        resultBuilder.append("~");
        resultBuilder.append(prediction.minutes);
        resultBuilder.append("m");
      }
      singleLineResult = resultBuilder.toString();
      canvas.drawText(singleLineResult, x, y, paint);
    } else {
      // We're in app mode, so we have more screen space to work with
      for (String route : predictions.getRoutes()) {
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append(route);
        resultBuilder.append(":");
        for (Prediction prediction : predictions.getPredictionsForRoute(route)) {
          resultBuilder.append(" ");
          resultBuilder.append(prediction.minutes);
          resultBuilder.append("m");
        }
        canvas.drawText(resultBuilder.toString(), x, y, paint);
        y += paint.getFontSpacing();
      }
    }
  }

  private void processStatusUpdate(PredictionSet newPredictions) {
    if (!isActive()) {
      return;
    }
    
    inInitialFetch = false;
    predictions = newPredictions;

    invalidate();

    handler.postDelayed(updateStatusTask, STATUS_UPDATE_INTERVAL_MSEC);
  }

  private class GetTimesTask extends AsyncTask<Void, Void, PredictionSet> {
    private static final String STOP_URL =
        "http://proximobus.appspot.com/agencies/sf-muni/stops/13128/predictions.json";
    @Override
    protected void onPostExecute(PredictionSet result) {
      processStatusUpdate(result);
    }
    
    @Override
    protected PredictionSet doInBackground(Void... params) {
      HttpURLConnection connection = null;
      PredictionSet resultSet = null;
      try {
        URL url = new URL(STOP_URL);
        connection = (HttpURLConnection) url.openConnection();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          String response = convertStreamToString(connection.getInputStream());
          try {
            resultSet = new PredictionSet();
            JSONObject responseObj = new JSONObject(response);
            JSONArray predArray = responseObj.getJSONArray("items");
            for (int i = 0; i < predArray.length(); i++) {
              JSONObject predObj = predArray.getJSONObject(i);
              resultSet.addPrediction(predObj.getString("route_id"), predObj.getInt("minutes"));
            }
            
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

      return resultSet;
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
  
  private static class Prediction {
    public String route;
    public int minutes;
  }
  
  private static class PredictionSet {
    List<Prediction> predictions = new ArrayList<Prediction>();
    
    public void addPrediction(String route, int minutes) {
      Prediction prediction = new Prediction();
      prediction.route = route;
      prediction.minutes = minutes;
      predictions.add(prediction);
    }
    
    public List<String> getRoutes() {
      Set<String> routeSet = new HashSet<String>();
      for (Prediction prediction : predictions) {
        routeSet.add(prediction.route);
      }
      List<String> result = new ArrayList<String>(routeSet);
      Collections.sort(result);
      return result;
    }
    
    public List<Prediction> getAllPredictions() {
      return predictions;
    }
    
    public List<Prediction> getPredictionsForRoute(String route) {
      List<Prediction> result = new ArrayList<Prediction>();
      for (Prediction prediction : predictions) {
        if (prediction.route.equals(route)) {
          result.add(prediction);
        }
      }
      return result;
    }
  }
}
