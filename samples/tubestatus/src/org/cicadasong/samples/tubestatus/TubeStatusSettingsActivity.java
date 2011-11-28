package org.cicadasong.samples.tubestatus;

import org.cicadasong.samples.tubestatus.TubeStatus.TubeLine;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class TubeStatusSettingsActivity extends PreferenceActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getPreferenceManager().setSharedPreferencesName(Preferences.PREFS_NAME);
    getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
    addPreferencesFromResource(R.xml.preferences);
    updatePreferredLineList();
  }
  
  private void updatePreferredLineList() {
    ListPreference p = (ListPreference) findPreference("PreferredLine");
    int lineCount = TubeLine.allLines.size();
    CharSequence[] titles = new CharSequence[lineCount];
    CharSequence[] values = new CharSequence[lineCount];
    for (int index = 0; index < lineCount; index++) {
      titles[index] = TubeLine.allLines.get(index).name;
      values[index] = TubeLine.allLines.get(index).lineIdentifier;
    }
    p.setEntries(titles);
    p.setEntryValues(values);
  }
}
