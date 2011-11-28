package org.cicadasong.samples.tubestatus;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
  public static final String PREFS_NAME = "TubeStatusPreferences";
  
  private Preferences() {}
  
  public static SharedPreferences getSharedPreferences(Context context) {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }
  
  public static String getPreferredLine(Context context) {
    SharedPreferences prefs = getSharedPreferences(context);
    return prefs.getString("PreferredLine", context.getString(R.string.preferred_line));
  }

  /**
   * @return value in minutes
   */
  public static int getUpdateFrequency(Context context) {
    SharedPreferences prefs = getSharedPreferences(context);
    int frequency = Integer.parseInt(context.getString(R.string.update_frequency));
    try {
      frequency = Integer.parseInt(prefs.getString("UpdateFrequency", Integer.toString(frequency)));
    } catch (NumberFormatException e) {
    }
    return frequency;
  }
}
