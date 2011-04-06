package org.cicadasong.cicada;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtil {
  public static final String SHARED_PREF_NAME = "CicadaPrefs";
  private static final String PREF_SCAN_COMPLETED = "ScanCompleted";
  
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
}
