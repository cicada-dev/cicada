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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Monitors the adding and removal of packages to maintain the list of Cicada apps.
 */

public class PackageMonitor extends BroadcastReceiver {
  public static final String APP_TYPE_METADATA_KEY = "org.cicadasong.cicada.apptype";
  
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v(Cicada.TAG, "Package monitor received intent: " + intent);
    
    if (intent.getData() == null) {
      return;
    }
    
    String packageName = intent.getData().getSchemeSpecificPart();
    
    if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
      if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
        AppDatabase db = new AppDatabase(context);
        db.deleteAppsWithPackageName(packageName);
        db.close();
      }
    } else {
      List<AppDescription> apps =
          PackageUtil.getAppsFromPackage(context.getPackageManager(), packageName);
      if (apps.size() > 0) {
        AppDatabase db = new AppDatabase(context);
        db.deleteAppsWithPackageName(apps.get(0).packageName);
        
        for (AppDescription app : apps) {
          Log.v(Cicada.TAG, "Found Cicada app: " + app.appName);
          db.addApp(app);
        }
        
        db.close();
      }
    }
  }
}
