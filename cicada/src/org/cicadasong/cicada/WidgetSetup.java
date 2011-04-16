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
import java.util.List;

import org.cicadasong.cicadalib.CicadaApp.AppType;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * This Activity presents the UI that allows the user to configure which installed widgets are
 * displayed on which parts on the widget screen.
 */

public class WidgetSetup extends Activity {
  
  public static final AppDescription NONE =
      new AppDescription("NONE", "NONE", "(None)", AppType.NONE);
  
  public static final int TOP_WIDGET = 0;
  public static final int MIDDLE_WIDGET = 1;
  public static final int BOTTOM_WIDGET = 2;
  
  private List<AppDescription> widgets;
  private AppDescription[] selections;
  
  private Spinner[] spinners;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.widget_setup);
    
    AppDatabase db = new AppDatabase(this);
    widgets = db.getWidgets();
    widgets.add(0, NONE);

    selections = db.getWidgetSetup().toArray(new AppDescription[]{});
    for (int i = 0; i < selections.length; i++) {
      if ((selections[i] == null) || (indexForApp(selections[i]) == 0)) {
        selections[i] = NONE;
      }
    }
    
    db.close();
    
    ArrayAdapter<AppDescription> widgetAdapter =
        new ArrayAdapter<AppDescription>(this, android.R.layout.simple_spinner_item, widgets);
    
    spinners = new Spinner[WidgetScreen.NUM_WIDGETS];
    for (int i = 0; i < spinners.length; i++) {
      Spinner spinner;
      switch (i) {
      case TOP_WIDGET:
      default:
        spinner = (Spinner) findViewById(R.id.top_widget_spinner);
        break;
      case MIDDLE_WIDGET:
        spinner = (Spinner) findViewById(R.id.middle_widget_spinner);
        break;
      case BOTTOM_WIDGET:
        spinner = (Spinner) findViewById(R.id.bottom_widget_spinner);
        break;
      }
      spinners[i] = spinner;
      spinner.setAdapter(widgetAdapter);
      final int spinnerId = i;
      spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
          widgetSelected(spinnerId, widgets.get(pos));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
      });
      
    }
    updateSpinners();
  }
  
  private void widgetSelected(int spinner, AppDescription app) {
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
    
    List<AppDescription> storeWidgets = new ArrayList<AppDescription>();
    for (int i = 0; i < selections.length; i++) {
      if (selections[i] == NONE) {
        storeWidgets.add(null);
      } else {
        storeWidgets.add(selections[i]);
      }
    }
    
    AppDatabase db = new AppDatabase(this);
    db.setWidgetSetup(storeWidgets);
    db.close();
  }
  
  private void updateSpinners() {
    for (int i = 0; i < spinners.length; i++) {
      spinners[i].setSelection(indexForApp(selections[i]));
    }
  }
  
  private int indexForApp(AppDescription app) {
    for (int i = 0; i < widgets.size(); i++) {
      if (widgets.get(i).equals(app)) {
        return i;
      }
    }
    return 0;
  }
}
