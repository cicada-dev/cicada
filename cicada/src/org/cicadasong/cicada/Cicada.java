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

import org.cicadasong.apollo.ApolloIntents;
import org.cicadasong.apollo.SimulatedDisplayView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Cicada extends Activity {
  public static final String TAG = Cicada.class.getSimpleName();
    
  private BroadcastReceiver receiver;
  private ToggleButton serviceToggle;
  SimulatedDisplayView display;
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    display = (SimulatedDisplayView) findViewById(R.id.display);
    
    serviceToggle = (ToggleButton) findViewById(R.id.service_toggle);
    serviceToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
          if (!checkSettingsAndStart()) {
            serviceToggle.setChecked(false);  // Settings in a bad state
          }
        } else {
          stopService(new Intent(getBaseContext(), CicadaService.class));
        }
      }
    });
    updateServiceToggleState();
    
    if (!CicadaService.isRunning()) {
      checkSettingsAndStart();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    
    tearDownBroadcastReceiver();
  }

  @Override
  protected void onResume() {
    super.onResume();
    
    setUpBroadcastReceiver();
  }
  
  private boolean checkSettingsAndStart() {
    String mac = PrefUtil.getWatchMAC(this);
    if (mac.length() == 0) {
      Toast toast = Toast.makeText(this, getString(R.string.error_mac), Toast.LENGTH_LONG);
      toast.show();
      return false;
    }
    
    startService(new Intent(getBaseContext(), CicadaService.class));
    
    return true;
  }
  
  private void updateServiceToggleState() {
    serviceToggle.setChecked(CicadaService.isRunning());
  }
  
  private void setUpBroadcastReceiver() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(CicadaService.INTENT_SERVICE_STARTED);
    filter.addAction(CicadaService.INTENT_SERVICE_STOPPED);
    filter.addAction(ApolloIntents.INTENT_PUSH_BITMAP);
    filter.addAction(ApolloIntents.INTENT_PUSH_TEXT);
    
    receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.v(Cicada.TAG, "Received intent: " + intent);
        
        if (intent.getAction().equals(ApolloIntents.INTENT_PUSH_BITMAP)) {
          display.setByteBuffer(intent.getByteArrayExtra(ApolloIntents.EXTRA_BITMAP_BUFFER));
        } else if (intent.getAction().equals(ApolloIntents.INTENT_PUSH_TEXT)) {
          display.setText(intent.getStringExtra(ApolloIntents.EXTRA_TEXT));
        } else {
          updateServiceToggleState();
          if (!CicadaService.isRunning()) {
            display.clearDisplay();
          }
        }
      }
    };

    registerReceiver(receiver, filter);
  }
  
  private void tearDownBroadcastReceiver() {
    if (receiver != null) {
      unregisterReceiver(receiver);
      receiver = null;
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
    
    case R.id.menu_item_widget_setup:
      launchWidgetSetup();
      return true;

    case R.id.menu_item_hotkey_setup:
      launchHotkeySetup();
      return true;

    case R.id.menu_item_settings:
      launchSettings();
      return true;

    default:
      return super.onOptionsItemSelected(item);
    }
  }
  
  private void launchWidgetSetup() {
    Intent intent = new Intent(this, WidgetSetup.class);
    startActivity(intent);
  }
  
  private void launchHotkeySetup() {
    Intent intent = new Intent(this, HotkeySetupActivity.class);
    startActivity(intent);
  }
  
  private void launchSettings() {
    Intent intent = new Intent(this, CicadaSettingsActivity.class);
    startActivity(intent);
  }

}