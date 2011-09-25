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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cicadasong.cicadalib.CicadaApp;
import org.cicadasong.cicadalib.CicadaApp.AppType;
import org.cicadasong.cicadalib.CicadaIntents;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * This Activity presents the UI that allows the user to configure buttons that instantly launch
 * an app from from the Idle and Widget screens.
 */

public class HotkeySetupActivity extends Activity {
  public static final String INTENT_HOTKEYS_CHANGED = "org.cicadasong.cicada.HOTKEYS_CHANGED";
  
  public static final AppDescription NONE =
      new AppDescription("NONE", "NONE", null, "(None)", AppType.NONE);
  
  public static final int MIDDLE_LEFT_BUTTON = 0;
  public static final int MIDDLE_RIGHT_BUTTON = 1;
  public static final int BOTTOM_RIGHT_BUTTON = 2;
  
  private static final byte[] BUTTON_MASK = {
    CicadaApp.WatchButton.MIDDLE_LEFT.value(),
    CicadaApp.WatchButton.MIDDLE_RIGHT.value(),
    CicadaApp.WatchButton.BOTTOM_RIGHT.value(),
  };
  
  public static final int NUM_CONFIGURABLE_BUTTONS = BUTTON_MASK.length;
  
  private List<AppDescription> apps;
  private AppDescription[] selections = new AppDescription[NUM_CONFIGURABLE_BUTTONS];
  private AppDescription[] lastDBselections = new AppDescription[NUM_CONFIGURABLE_BUTTONS];
  
  private Spinner[] spinners;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.hotkey_setup);
    
    AppDatabase db = new AppDatabase(this);
    apps = db.getApps();
    apps.add(0, NONE);
    
    Arrays.fill(selections, NONE);
    
    for (HotkeySetupEntry entry : db.getHotkeySetup()) {
      for (int i = 0; i < BUTTON_MASK.length; i++) {
        if (BUTTON_MASK[i] == entry.hotkeys) {
          selections[i] = entry.app;
          continue;
        }
      }
    }
    System.arraycopy(selections, 0, lastDBselections, 0, selections.length);
    
    db.close();
    
    ArrayAdapter<AppDescription> appsAdapter =
        new ArrayAdapter<AppDescription>(this, R.layout.spinner_item, apps);
    
    spinners = new Spinner[NUM_CONFIGURABLE_BUTTONS];
    for (int i = 0; i < spinners.length; i++) {
      Spinner spinner;
      switch (i) {
      case MIDDLE_LEFT_BUTTON:
      default:
        spinner = (Spinner) findViewById(R.id.middle_left_button_spinner);
        break;
      case MIDDLE_RIGHT_BUTTON:
        spinner = (Spinner) findViewById(R.id.middle_right_button_spinner);
        break;
      case BOTTOM_RIGHT_BUTTON:
        spinner = (Spinner) findViewById(R.id.bottom_right_button_spinner);
        break;
      }
      spinners[i] = spinner;
      spinner.setAdapter(appsAdapter);
      final int spinnerId = i;
      spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
          appSelected(spinnerId, apps.get(pos));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
      });
      
    }
    updateSpinners();
  }
  
  private void appSelected(int spinner, AppDescription app) {
    // App only mapped to one button at a time; might as well, right?
    for (int i = 0; i < selections.length; i++) {
      if (i == spinner) {
        continue;
      }
      if (selections[i].equals(app)) {
        selections[i] = NONE;
      }
    }
    selections[spinner] = app;
    updateSpinners();
    saveSetup();
  }

  private void saveSetup() {
    if (Arrays.equals(selections, lastDBselections)) {
      return;
    }
    
    System.arraycopy(selections, 0, lastDBselections, 0, selections.length);
    
    List<HotkeySetupEntry> entries = new ArrayList<HotkeySetupEntry>();
    for (int i = 0; i < selections.length; i++) {
      if (selections[i] != NONE) {
        entries.add(new HotkeySetupEntry(selections[i], BUTTON_MASK[i]));
      }
    }
    
    AppDatabase db = new AppDatabase(this);
    db.setHotkeySetup(entries);
    db.close();
    
    sendBroadcast(new Intent(INTENT_HOTKEYS_CHANGED));
  }
  
  private void updateSpinners() {
    for (int i = 0; i < spinners.length; i++) {
      spinners[i].setSelection(indexForApp(selections[i]));
    }
  }
  
  private int indexForApp(AppDescription app) {
    return Math.max(0, apps.indexOf(app));
  }
}
