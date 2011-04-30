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
 * An example app that fetches the current status of a particular London Underground line.
 * If this was a real app, it would also provide a phone-side setup screen to pick the train
 * lines to show.
 */
public class NextBuses extends CicadaApp {
  public static final String TAG = NextBuses.class.getSimpleName();

  // Update every 2 minutes
  public static final int STATUS_UPDATE_INTERVAL_MSEC = 2 * 60 * 1000;

  private String stopName = "3rd St & Howard St";
  private String status = "Fetching...";
  private Runnable updateStatusTask;
  private Handler handler;

  @Override
  protected void onActivate(AppType mode) {
    Log.v(TAG, "Tube Status activated in mode: " + mode);

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
  protected void onDeactivate() {
    handler.removeCallbacks(updateStatusTask);
  }

  @Override
  protected void onButtonPress(ButtonEvent buttonEvent) {
    Log.v(TAG, "Tube Status deactivated");
  }

  protected void onDraw(Canvas canvas) {
    Paint paint = new Paint();
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
    
    // We've centered the output vertically, so it works with the reduced canvas height in
    // widget mode.
    int y = canvas.getHeight() / 2;
    int x = canvas.getWidth() / 2;

    paint.setTypeface(Typeface.DEFAULT);
    paint.setTextSize(11);
    canvas.drawText(stopName, x, y - paint.descent() - 1, paint);

    paint.setTextAlign(Paint.Align.LEFT);
    x = 2;
    paint.setTextSize(10);
    canvas.drawText(status, x, y + (int)-paint.ascent() + 1, paint);
  }

  private void processStatusUpdate(String newStatus) {
    if (!isActive()) {
      return;
    }

    if (newStatus.equalsIgnoreCase("Good Service")) {
      status = "Good Service";
    } else if (newStatus.equalsIgnoreCase("Minor Delays")) {
      status = "Minor Delays";
    } else if (newStatus.equalsIgnoreCase("Part Closure")) {
      status = "Part Closure";
    } else if (newStatus.equalsIgnoreCase("Suspended")) {
      status = "Suspended";
    } else {
      status = newStatus;
    }

    invalidate();

    handler.postDelayed(updateStatusTask, STATUS_UPDATE_INTERVAL_MSEC);
  }

  private class GetTimesTask extends AsyncTask<Void, Void, String> {
    private static final String STOP_URL =
        "http://proximobus.appspot.com/agencies/sf-muni/stops/13128/predictions.json";
    protected void onPostExecute(String result) {
      processStatusUpdate(result);
    }
    @Override
    protected String doInBackground(Void... params) {
      String result = "Network Error";
      HttpURLConnection connection = null;
      PredictionSet resultSet = new PredictionSet();
      try {
        URL url = new URL(STOP_URL);
        connection = (HttpURLConnection) url.openConnection();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          String response = convertStreamToString(connection.getInputStream());
          try {
            JSONObject responseObj = new JSONObject(response);
            JSONArray predArray = responseObj.getJSONArray("items");
            for (int i = 0; i < predArray.length(); i++) {
              JSONObject predObj = predArray.getJSONObject(i);
              resultSet.addPrediction(predObj.getString("route_id"), predObj.getInt("minutes"));
            }
            
            List<Prediction> allPredictions = resultSet.getAllPredictions();
            StringBuilder resultBuilder = new StringBuilder();
            for (Prediction prediction : allPredictions) {
              if (resultBuilder.length() > 0) {
                resultBuilder.append(" ");
              }
              resultBuilder.append(prediction.route);
              resultBuilder.append(":");
              resultBuilder.append(prediction.minutes);
            }
            result = resultBuilder.toString();
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

      return result;
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
