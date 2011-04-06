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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.util.Log;

public class PackageUtil {
  public static final String APP_TYPE_METADATA_KEY = "org.cicadasong.cicada.apptype";
  public static final String APP_LIST_PREF = "KnownApps";

  private PackageUtil() {
  }
  
  public static List<AppDescription> getAppsFromPackage(PackageManager pm, String packageName) {
    ArrayList<AppDescription> apps = new ArrayList<AppDescription>();
    
    try {
      PackageInfo pi = pm.getPackageInfo(packageName,
          PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
      if (pi == null || pi.services == null) {
        return apps;
      }
      for (ServiceInfo serviceInfo : pi.services) {
        Bundle metadata = serviceInfo.metaData;
        if (metadata != null) {
          String appTypeString = serviceInfo.metaData.getString(APP_TYPE_METADATA_KEY);
          if (appTypeString != null) {
            if (!serviceInfo.exported) {
              Log.w(Cicada.TAG, "Found service \"" + serviceInfo.name + "\", but it doesn't have" +
                  " android:exported=\"true\" in its definition, so Cicada can't use it.");
            } else {
              AppType mode = AppType.APP;
              try {
                mode = AppType.valueOf(appTypeString);
              } catch (Exception e) {
                Log.w(Cicada.TAG, "Couldn't recognize app type \"" + appTypeString + "\" for " +
                    "service \"" + serviceInfo.name + "\", assuming " + mode.name());
              }
              AppDescription app = new AppDescription(packageName, serviceInfo.name,
                  serviceInfo.loadLabel(pm).toString(), mode);
              apps.add(app);
            }
          }
        }
      }
    } catch (NameNotFoundException e) {
      Log.w(Cicada.TAG, "PackageManager couldn't find package named: " + packageName);
    }
    
    return apps;
  }

}
