package org.cicadasong.samples.webimageplayer;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class WebImagePlayerSettingsActivity extends PreferenceActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getPreferenceManager().setSharedPreferencesName(Preferences.PREFS_NAME);
    getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
    addPreferencesFromResource(R.xml.preferences);
  }
}
