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
// limitations under the License.package org.cicadasong.cicada;

package org.cicadasong.cicada;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtil {
  public static final String SHARED_PREF_NAME = "CicadaPrefs";
  private static final String PREF_SCAN_COMPLETED = "ScanCompleted";
  private static final String PREF_WATCH_MAC = "WatchMAC";
  
  private PrefUtil() {
  }
  
  public static boolean getAppScanCompleted(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    return prefs.getBoolean(PREF_SCAN_COMPLETED, false);
  }
  
  public static void setAppScanCompleted(Context context, boolean value) {
    SharedPreferences.Editor prefEdit =
        context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
    prefEdit.putBoolean(PREF_SCAN_COMPLETED, value);
    prefEdit.commit();
  }
  
  public static String getWatchMAC(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    return prefs.getString(PREF_WATCH_MAC, "").toUpperCase();
  }
}
