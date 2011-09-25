package org.cicadasong.cicada;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class CicadaSettingsActivity extends PreferenceActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getPreferenceManager().setSharedPreferencesName(PrefUtil.SHARED_PREF_NAME);
    getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
    addPreferencesFromResource(R.xml.preferences);
  }
}
