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
    // Wrap around across top or bottom
    if (selectedIndex < 0) {
    	selectedIndex = apps.size() - 1;
    } else if (selectedIndex >= apps.size()) {
    	selectedIndex = 0;
    }
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
  
  private int lastStartIndex = -1;
  private int lastSelectedRow = -1;
  private final int listSize = 7;
  
  protected void onDraw(Canvas canvas) {
    float y = -paint.ascent();

    int startIndex = 0;
    if (selectedIndex < lastStartIndex) {
    	startIndex = selectedIndex - (int)(listSize/2); // Put our item in the centre of the list if scrolling upwards
    } else if (selectedIndex > (lastStartIndex + listSize -1)) {
    		startIndex = selectedIndex - (int)(listSize/2); 	// Put our item in the centre of the list if scrolling downwards
    }
    if ((startIndex + listSize) >= apps.size()) {
    	startIndex = apps.size() - listSize;		// If there would be blank lines then adjust so that doesn't happen if we can
    }
    if (startIndex < 0) {
    	startIndex = 0;			// If we're off the top then fix it
    }
    
    // The block below was designed so that instead of repainting the whole screen 
    // we would just repaint the two lines that had changed.
    // Unfortunately that didn't work because the other lines all went blank
    // if (startIndex == lastStartIndex) {
    if (false) {
    	// If the start indexes are the same then the screen content won't change
    	// So there's no need to repaint the whole screen we just need to move the highlight
    	// Turn off the previous highlight
    	y = -paint.ascent() + (lastSelectedRow - 1) * paint.getFontSpacing();
    	paint.setColor(Color.WHITE);
    	canvas.drawRect(new Rect(0,
                (int)(y + paint.ascent()), canvas.getWidth(), (int)(y + paint.descent() + 1)), paint);
    	paint.setColor(Color.BLACK);
        canvas.drawText(apps.get(startIndex + lastSelectedRow -1).appName, LEFT_MARGIN, y, paint);
    	// Turn on the new highlight
    	lastSelectedRow = selectedIndex - startIndex + 1;
    	y = -paint.ascent() + (lastSelectedRow - 1) * paint.getFontSpacing();
    	paint.setColor(Color.BLACK);
    	canvas.drawRect(new Rect(0,
                (int)(y + paint.ascent()), canvas.getWidth(), (int)(y + paint.descent() + 1)), paint);
    	paint.setColor(Color.WHITE);
        canvas.drawText(apps.get(startIndex + lastSelectedRow -1).appName, LEFT_MARGIN, y, paint);
    }
    else {
    	lastStartIndex = startIndex;
    	 
    	for (int i = startIndex; (i < apps.size()) && (i < startIndex + 7); i++) {
    		if (i == selectedIndex) {
    			paint.setColor(Color.BLACK);
    			canvas.drawRect(new Rect(0,
    					(int)(y + paint.ascent()), canvas.getWidth(), (int)(y + paint.descent() + 1)), paint);
    			lastSelectedRow = selectedIndex - startIndex + 1;
    		}
    		paint.setColor(i == selectedIndex ? Color.WHITE : Color.BLACK);
    		canvas.drawText(apps.get(i).appName, LEFT_MARGIN, y, paint);
    		y += paint.getFontSpacing();
    	}
    }
  }

}
