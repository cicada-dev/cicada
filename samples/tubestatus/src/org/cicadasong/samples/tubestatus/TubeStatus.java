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

package org.cicadasong.samples.tubestatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cicadasong.cicadalib.CicadaApp;
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
public class TubeStatus extends CicadaApp {
  public static final String TAG = TubeStatus.class.getSimpleName();

  // Update every 15 minutes so we don't barrage the server.
  public static final int STATUS_UPDATE_INTERVAL_MSEC = 15 * 60 * 1000;

  private enum TubeLine {
    ALL("All lines", "all"),
    TUBE("Tube only", "tube"), // excludes DLR and Overground
    OVERGROUND("Overground", "overground"),

    BAKERLOO("Bakerloo", "bakerloo"),
    CENTRAL("Central", "central"),
    CIRCLE("Circle", "circle"),
    DISTRICT("District", "district"),
    DLR("DLR", "docklands"),
    HAMMERSMITH_AND_CITY("Hammersmith & City", "hammersmithcity"),
    JUBILEE("Jubilee", "jubilee"),
    METROPOLITAN("Metropolitan", "metropolitan"),
    NORTHERN("Northern", "northern"),
    PICCADILLY("Piccadilly", "piccadilly"),
    VICTORIA("Victoria", "victoria"),
    WATERLOO_AND_CITY("Waterloo & City", "waterloocity");

    public String name;
    public String lineIdentifier;

    TubeLine(String name, String lineIdentifier) {
      this.name = name;
      this.lineIdentifier = lineIdentifier;
    }
  }
  
  // wildcards followed by alphabetically ordered list
  private List<TubeLine> allLines = new ArrayList<TubeLine>(Arrays.asList(
    TubeLine.ALL,
    TubeLine.TUBE,
    TubeLine.OVERGROUND,
    TubeLine.BAKERLOO,
    TubeLine.CENTRAL,
    TubeLine.CIRCLE,
    TubeLine.DISTRICT,
    TubeLine.DLR,
    TubeLine.HAMMERSMITH_AND_CITY,
    TubeLine.JUBILEE,
    TubeLine.METROPOLITAN,
    TubeLine.NORTHERN,
    TubeLine.PICCADILLY,
    TubeLine.VICTORIA,
    TubeLine.WATERLOO_AND_CITY
  ));
  
  private int selectionIndex;
  private TubeLine selectedLine;
  private String status;
  private Runnable updateStatusTask;
  private Handler handler;

  @Override
  public void onCreate() {
    updateSelection(0);
    refreshStatus();
  }
  
  private void updateSelection(int index) {
    if (index < 0) {
      index = allLines.size() - 1;
    }
    if (index >= allLines.size()) {
      index = 0;
    }
    selectionIndex = index;
    selectedLine = allLines.get(selectionIndex);
  }

  @Override
  protected void onResume() {
    Log.v(TAG, "Tube Status activated in mode: " + getCurrentMode());

    if (updateStatusTask == null) {
      updateStatusTask = new Runnable() {

        @Override
        public void run() {
          if (!isActive()) return;

          (new GetStatusTask()).execute();
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
		switch (button) {
			case TOP_RIGHT:
				updateSelection(selectionIndex - 1);
				refreshStatus();
				break;
			case MIDDLE_RIGHT:
				handler.post(updateStatusTask);
				refreshStatus();
				break;
			case BOTTOM_RIGHT:
				updateSelection(selectionIndex + 1);
				refreshStatus();
				break;
		}
  }

  protected void refreshStatus() {
    status = "Fetching...";
    if (!isActive()) {
      return;
    }
    if (handler != null) {
      handler.post(updateStatusTask);
    }
    invalidate();
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
    canvas.drawText(selectedLine.name, x, y - paint.descent() - 1, paint);

    paint.setTextSize(14);
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

  private class GetStatusTask extends AsyncTask<Void, Void, String> {
    private static final String TUBE_STATUS_URL =
        "http://api.tubeupdates.com/?method=get.status&lines={lineIdentifier}&return=status";
    protected void onPostExecute(String result) {
      processStatusUpdate(result);
    }
    @Override
    protected String doInBackground(Void... params) {
      String result = "Network Error";
      HttpURLConnection connection = null;
      try {
        URL url = new URL(TUBE_STATUS_URL.replace("{lineIdentifier}", selectedLine.lineIdentifier));
        connection = (HttpURLConnection) url.openConnection();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          String response = convertStreamToString(connection.getInputStream());
          try {
            // Expected response format is something like:
            // {
            //   "response": {
            //     "lines": [{
            //       "name": "Victoria",
            //       "id": "victoria",
            //       "status": "good service",
            //       "messages": [],
            //       "status_starts": "Wed, 23 Jun 2010 10:09:04 +0100",
            //       "status_ends": "",
            //       "status_requested": "Wed, 23 Jun 2010 17:27:17 +0100"
            //     }]
            // }
            JSONObject responseObj = new JSONObject(response);
            result = responseObj.getJSONObject("response").getJSONArray("lines")
                .getJSONObject(0).getString("status");
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
}
