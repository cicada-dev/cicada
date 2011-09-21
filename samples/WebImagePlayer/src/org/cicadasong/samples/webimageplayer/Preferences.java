package org.cicadasong.samples.webimageplayer;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
  public static final String PREFS_NAME = "WebImagePlayerPreferences";
  
  private Preferences() {}
  
  public static SharedPreferences getSharedPreferences(Context context) {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }
  
  public static String getImageUrl(Context context) {
    SharedPreferences prefs = getSharedPreferences(context);
    return prefs.getString("ImageUrl", context.getString(R.string.default_image_url));
  }

  public static int getUpdateFrequency(Context context) {
    SharedPreferences prefs = getSharedPreferences(context);
    int frequency = Integer.parseInt(context.getString(R.string.default_update_frequency));
    try {
      frequency = Integer.parseInt(prefs.getString("UpdateFrequency", Integer.toString(frequency)));
    } catch (NumberFormatException e) {
    }
    return frequency;
  }
  
  public static boolean getDithering(Context context) {
    SharedPreferences prefs = getSharedPreferences(context);
    return prefs.getBoolean("UseDithering", false);
  }
}
