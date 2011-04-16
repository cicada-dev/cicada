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

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public class AppList extends CicadaApp {
  
  private List<AppDescription> apps;
  private int selectedIndex;
  private Paint paint;
  
  @Override
  public String getAppName() {
    return getResources().getString(R.string.app_list_service_name);
  }

  @Override
  public void onActivate(AppType mode) {
    paint = new Paint();
    paint.setTypeface(Typeface.DEFAULT);
    
    if (apps == null) {
      loadApps();
    }
  }
  
  private void loadApps() {
    AppDatabase db = new AppDatabase(this);
    apps = db.getApps();
    db.close();
    apps.add(0, CicadaService.IDLE_SCREEN);
    apps.add(1, WidgetScreen.DESCRIPTION);
  }

  @Override
  public void onDeactivate() {
  }

  @Override
  public void onButtonPress(ButtonEvent buttonEvent) {
    if (buttonEvent.hasOnlyButtonsPressed(CicadaIntents.Button.TOP_RIGHT)) {
      selectedIndex = Math.max(0, selectedIndex - 1);
      invalidate();
    } else if (buttonEvent.hasOnlyButtonsPressed(CicadaIntents.Button.BOTTOM_RIGHT)) {
      selectedIndex = Math.min(apps.size() - 1, selectedIndex + 1);
      invalidate();
    } else if (buttonEvent.hasOnlyButtonsPressed(CicadaIntents.Button.MIDDLE_RIGHT)) {
      launchSelectedApp();
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
      canvas.drawText(apps.get(i).appName, 0, y, paint);
      y += paint.getFontSpacing();
    }
  }

}
