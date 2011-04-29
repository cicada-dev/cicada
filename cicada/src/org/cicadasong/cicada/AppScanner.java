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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

public class AppScanner extends AsyncTask<Void, Void, Void> {

  public interface Listener {
    public abstract void scanFinished();
  }
  
  private Context context;
  private Listener listener;
  
  public AppScanner(Context context, Listener listener) {
    this.context = context;
    this.listener = listener;
  }
  
  @Override
  protected Void doInBackground(Void... arg0) {
    PackageManager pm = context.getPackageManager();
    List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_SERVICES);
    AppDatabase db = new AppDatabase(context);
    for (PackageInfo pi : packages) {
      List<AppDescription> apps =
        PackageUtil.getCicadaAppsFromPackage(context.getPackageManager(), pi.packageName);
      db.deleteAppsWithPackageName(pi.packageName);
      for(AppDescription app : apps) {
        Log.v(Cicada.TAG, "AppScanner found Cicada app: " + app.appName);
        db.addApp(app);
      }
    }
    db.close();
    
    return null;
  }

  @Override
  protected void onPostExecute(Void result) {
    listener.scanFinished();
  }

}
