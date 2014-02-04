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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cicadasong.cicadalib.CicadaApp;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
 * 
 * It uses the tubeupdates.com site to get the information which currently scrapes information from TFL.
 * Api documentation is here: http://tubeupdates.com/documentation/
 */
public class TubeStatus extends CicadaApp implements OnSharedPreferenceChangeListener {
  public static final String TAG = TubeStatus.class.getSimpleName();

  protected enum TubeLine {
    ALL("All lines", "all"),
    TUBE("Tube only", "tube"), // excludes DLR and Overground
    OVERGROUND("Overground", "overground"),
    DLR("DLR", "docklands"),

    BAKERLOO("Bakerloo", "bakerloo"),
    CENTRAL("Central", "central"),
    CIRCLE("Circle", "circle"),
    DISTRICT("District", "district"),
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

    protected static List<TubeLine> allLines = new ArrayList<TubeLine>(Arrays.asList(
      // wildcards
      TubeLine.ALL,
      TubeLine.TUBE,
      
      // sprawling transport systems with seperate lines
      TubeLine.OVERGROUND,
      TubeLine.DLR,
      
      // alphabetically ordered list of tube lines
      TubeLine.BAKERLOO,
      TubeLine.CENTRAL,
      TubeLine.CIRCLE,
      TubeLine.DISTRICT,
      TubeLine.HAMMERSMITH_AND_CITY,
      TubeLine.JUBILEE,
      TubeLine.METROPOLITAN,
      TubeLine.NORTHERN,
      TubeLine.PICCADILLY,
      TubeLine.VICTORIA,
      TubeLine.WATERLOO_AND_CITY
    ));
    
    public static int indexOf(String lineIdentifier) {
      for (int index = 0; index < allLines.size(); index++) {
        if (allLines.get(index).lineIdentifier.compareToIgnoreCase(lineIdentifier) == 0) {
          return index;
        }
      }
      return -1;
    }
  }
  
  
  private int selectionIndex;
  private String status;
  private Runnable updateStatusTask;
  private Handler handler;
  private int updateIntervalMsec;
  
  private static final String STATUS_GOOD = "good service";
  private static final String STATUS_MINOR_DELAYS = "minor delays";
  private static final String STATUS_SEVERE_DELAYS = "severe delays";
  private static final String STATUS_PLANNED_CLOSURE = "planned closure";
  private static final String STATUS_PART_CLOSURE = "part closure";
  private static final String STATUS_PART_SUSPENDED = "part suspended";
  private static final String STATUS_SUSPENDED = "suspended";
  private static final String STATUS_OTHER = "other"; // not returned via API, used only as map key
    
  private static final Map <String, String> messageToCodeMap = createMessageToCodeMap();
  private static Map<String, String> createMessageToCodeMap() {
    Map<String, String> result = new HashMap<String, String>();
    result.put(STATUS_GOOD, "OK");
    result.put(STATUS_MINOR_DELAYS, "MD");
    result.put(STATUS_SEVERE_DELAYS, "SD");
    result.put(STATUS_PLANNED_CLOSURE, "PLC");
    result.put(STATUS_PART_CLOSURE, "PC");
    result.put(STATUS_PART_SUSPENDED, "PS");
    result.put(STATUS_SUSPENDED, "S");
    result.put(STATUS_OTHER, "?");
    return Collections.unmodifiableMap(result);
  }
  
  @Override
  public void onCreate() {
    Preferences.getSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    readPreferences();
    refreshStatus();
  }
  
  private void readPreferences() {
    Log.v(TAG, "Reading preferences");
    updateSelection(TubeLine.indexOf(Preferences.getPreferredLine(this)));
    updateIntervalMsec = Preferences.getUpdateFrequency(this) * 60 * 1000;
  }
  
  private void updateSelection(int index) {
    if (index < 0) {
      index = TubeLine.allLines.size() - 1;
    }
    if (index >= TubeLine.allLines.size()) {
      index = 0;
    }
    selectionIndex = index;
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
    Preferences.getSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
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
      handler.removeCallbacks(updateStatusTask); // ensure that any fetch that is already in process doesn't deliver updates to the UI after this new fetch
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
    canvas.drawText(TubeLine.allLines.get(selectionIndex).name, x, y - paint.descent() - 1, paint);

    paint.setTextSize(11); // TODO dynamically adjust font size depending on length of status string?
    canvas.drawText(status, x, y + (int)-paint.ascent() + 1, paint);
  }
  
  private void processStatusUpdate(String newStatus) {
    if (!isActive()) {
      return;
    }

    status = newStatus;

    invalidate();

    handler.postDelayed(updateStatusTask, updateIntervalMsec);
  }

  private class GetStatusTask extends AsyncTask<Void, Void, String> {
    private static final String TUBE_STATUS_URL =
        "http://api.tubeupdates.com/?method=get.status&lines={lineIdentifier}&return=status";
    protected void onPostExecute(String result) {
      processStatusUpdate(result);
    }
            
    protected String determineOverallStatus(JSONObject responseObj) throws JSONException {
      
      Map<String, Integer> counterMap = new HashMap<String, Integer>();
      counterMap.put(STATUS_GOOD, 0);
      counterMap.put(STATUS_MINOR_DELAYS, 0);
      counterMap.put(STATUS_SEVERE_DELAYS, 0);
      counterMap.put(STATUS_PLANNED_CLOSURE, 0);
      counterMap.put(STATUS_PART_CLOSURE, 0);
      counterMap.put(STATUS_PART_SUSPENDED, 0);
      counterMap.put(STATUS_SUSPENDED, 0);
      counterMap.put(STATUS_OTHER, 0);
      
      JSONArray lines = responseObj.getJSONObject("response").getJSONArray("lines");
      for (int index = 0; index < lines.length() ; index++) {
        JSONObject line = lines.getJSONObject(index);
        Log.d(TAG, "Line: " + line.toString());
        String status = line.getString("status");
        
        String[] statusMessages = status.split(",");
        for (String statusMessage : statusMessages) {
          if (messageToCodeMap.containsKey(statusMessage)) {
            counterMap.put(statusMessage, counterMap.get(statusMessage) + 1);
          } else {
            counterMap.put(STATUS_OTHER, counterMap.get(STATUS_OTHER) + 1);
          }
        }
      }
      
      ArrayList<String> codes = new ArrayList<String>();
      
      for (Map.Entry<String, Integer> entry : counterMap.entrySet()) {
        if (entry.getValue() == 0) {
          continue;
        }
        String codeAndCount = String.format("%s:%d", messageToCodeMap.get(entry.getKey()), entry.getValue());
        codes.add(codeAndCount);
      }
      return TextUtils.join("/", codes);
    }
    
    protected String determineSingleLineStatus(JSONObject responseObj) throws JSONException {
      String status = responseObj.getJSONObject("response").getJSONArray("lines")
          .getJSONObject(0).getString("status");
      
      String[] statusMessages = status.split(",");
      ArrayList<String> codes = new ArrayList<String>();
      for (String statusMessage : statusMessages) {
        if (messageToCodeMap.containsKey(statusMessage)) {
          codes.add(messageToCodeMap.get(statusMessage));
        } else {
          return "Unknown";
        }
      }
      return TextUtils.join("/", codes);
    }
    
    @Override
    protected String doInBackground(Void... params) {
      String result = "Network Error";
      HttpURLConnection connection = null;
      try {
        URL url = new URL(TUBE_STATUS_URL.replace("{lineIdentifier}", TubeLine.allLines.get(selectionIndex).lineIdentifier));
        connection = (HttpURLConnection) url.openConnection();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
          String response = convertStreamToString(connection.getInputStream());
          Log.d(TAG, response);
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
            
            int lineCount = responseObj.getJSONObject("response").getJSONArray("lines").length();
            if (lineCount > 1) {
              result = determineOverallStatus(responseObj);
            } else {
              result = determineSingleLineStatus(responseObj);
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

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    readPreferences();
    handler.removeCallbacks(updateStatusTask);
    handler.post(updateStatusTask);
  }
}
