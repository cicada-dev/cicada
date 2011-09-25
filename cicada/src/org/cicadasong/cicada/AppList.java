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

import java.util.List;

import org.cicadasong.cicadalib.CicadaApp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public class AppList extends CicadaApp {
  private static String PREF_APP_LIST_SELECTED_INDEX = "AppListSelectedIndex";
  
  public static final AppDescription DESCRIPTION = new AppDescription(
    AppList.class.getPackage().getName(),
    AppList.class.getName(),
    null,
    "App List",
    AppType.APP);
  
  private static final int LEFT_MARGIN = 2;
  
  private List<AppDescription> apps;
  private int selectedIndex;
  private Paint paint;
  private BroadcastReceiver receiver;
  
  @Override
  public String getAppName() {
    return getResources().getString(R.string.app_list_service_name);
  }

  @Override
  public void onCreate() {
    loadSelectedIndex();
    
    super.onCreate();
  }

  @Override
  public void onDestroy() {
    saveSelectedIndex();

    super.onDestroy();
  }

  private void loadSelectedIndex() {
    SharedPreferences prefs = getSharedPreferences(PrefUtil.SHARED_PREF_NAME, Context.MODE_PRIVATE);
    selectedIndex = prefs.getInt(PREF_APP_LIST_SELECTED_INDEX, 0);
  }
  
  private void saveSelectedIndex() {
    SharedPreferences.Editor prefEdit =
        getSharedPreferences(PrefUtil.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
    prefEdit.putInt(PREF_APP_LIST_SELECTED_INDEX, selectedIndex);
    prefEdit.commit();
  }

  @Override
  protected void onResume() {
    paint = new Paint();
    paint.setTypeface(Typeface.DEFAULT);
    
    if (apps == null) {
      loadApps();
    }
    
    IntentFilter filter = new IntentFilter();
    filter.addAction(PackageMonitor.INTENT_APPS_CHANGED);
    
    receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        loadApps();
        changeSelection(selectedIndex);  // Counting on sanitization/redraw here
      }
    };
    registerReceiver(receiver, filter);
  }
  
  private void loadApps() {
    AppDatabase db = new AppDatabase(this);
    apps = db.getApps();
    db.close();
    apps.add(0, WidgetScreen.DESCRIPTION);
    if (!CicadaService.USE_DEVICE_SERVICE) {
      apps.add(0, CicadaService.IDLE_SCREEN);
    }
  }

  @Override
  protected void onPause() {
    unregisterReceiver(receiver);
  }

  @Override
  protected void onButtonPress(WatchButton button) {
    switch (button) {
      case TOP_RIGHT:
        changeSelection(selectedIndex - 1);
        break;
        
      case BOTTOM_RIGHT:
        changeSelection(selectedIndex + 1);
        break;
        
      case MIDDLE_RIGHT:
        launchSelectedApp();
        break;
        
      case TOP_LEFT:
        // Most Cicada apps can't trap the top left button, but it's reserved for AppList so we can.
        changeSelection(0);
        break;
    }
  }
  
  private void changeSelection(int newIndex) {
    selectedIndex = newIndex;
    selectedIndex = Math.min(apps.size() - 1, selectedIndex);
    selectedIndex = Math.max(0, selectedIndex);
    invalidate();
  }
  
  private void launchSelectedApp() {
    AppDescription app = apps.get(selectedIndex);
    
    Intent intent = new Intent(CicadaService.INTENT_LAUNCH_APP);
    intent.putExtra(CicadaService.EXTRA_APP_PACKAGE, app.packageName);
    intent.putExtra(CicadaService.EXTRA_APP_CLASS, app.className);
    intent.putExtra(CicadaService.EXTRA_APP_SETTINGS_CLASS, app.settingsActivityClassName);
    intent.putExtra(CicadaService.EXTRA_APP_NAME, app.appName);
    sendBroadcast(intent);
  }
  
  protected void onDraw(Canvas canvas) {
    float y = -paint.ascent();

    int startIndex = Math.max(0, selectedIndex - 2);
    for (int i = startIndex; (i < apps.size()) && (i < startIndex + 7); i++) {
      if (i == selectedIndex) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(new Rect(0,
            (int)(y + paint.ascent()), canvas.getWidth(), (int)(y + paint.descent() + 1)), paint);
      }
      paint.setColor(i == selectedIndex ? Color.WHITE : Color.BLACK);
      canvas.drawText(apps.get(i).appName, LEFT_MARGIN, y, paint);
      y += paint.getFontSpacing();
    }
  }

}
