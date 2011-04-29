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
import org.cicadasong.cicadalib.CicadaIntents;
import org.cicadasong.cicadalib.CicadaIntents.ButtonEvent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public class AppList extends CicadaApp {
  public static final AppDescription DESCRIPTION = new AppDescription(
    AppList.class.getPackage().getName(),
    AppList.class.getName(),
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
  protected void onActivate(AppType mode) {
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
        selectedIndex = Math.min(apps.size() - 1, selectedIndex);
        invalidate();
      }
    };
    registerReceiver(receiver, filter);
  }
  
  private void loadApps() {
    AppDatabase db = new AppDatabase(this);
    apps = db.getApps();
    db.close();
    apps.add(0, CicadaService.IDLE_SCREEN);
    apps.add(1, WidgetScreen.DESCRIPTION);
  }

  @Override
  protected void onDeactivate() {
    unregisterReceiver(receiver);
  }

  @Override
  protected void onButtonPress(ButtonEvent buttonEvent) {
    if (buttonEvent.hasButtonsPressed(CicadaIntents.Button.TOP_RIGHT)) {
      selectedIndex = Math.max(0, selectedIndex - 1);
      invalidate();
    } else if (buttonEvent.hasButtonsPressed(CicadaIntents.Button.BOTTOM_RIGHT)) {
      selectedIndex = Math.min(apps.size() - 1, selectedIndex + 1);
      invalidate();
    } else if (buttonEvent.hasButtonsPressed(CicadaIntents.Button.MIDDLE_RIGHT)) {
      launchSelectedApp();
    } else if (buttonEvent.hasButtonsPressed(CicadaIntents.Button.TOP_LEFT)) {
      // Most Cicada apps can't trap the top left button, but it's reserved for AppList so we can.
      selectedIndex = 0;
      invalidate();
    }
  }
  
  private void launchSelectedApp() {
    AppDescription app = apps.get(selectedIndex);
    
    Intent intent = new Intent(CicadaService.INTENT_LAUNCH_APP);
    intent.putExtra(CicadaService.EXTRA_APP_PACKAGE, app.packageName);
    intent.putExtra(CicadaService.EXTRA_APP_CLASS, app.className);
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
