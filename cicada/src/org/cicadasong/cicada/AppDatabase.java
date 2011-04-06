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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabase {
  private static final String DATABASE_NAME = "cicada.db";
  private static final int DATABASE_VERSION = 1;
  private static final String TABLE_NAME = "apps";
  private static final String APP_NAME = "name";
  private static final String PACKAGE_NAME = "package_name";
  private static final String CLASS_NAME = "class_name";
  private static final String APP_TYPE = "app_type";
  
  private SQLiteDatabase db;
  
  public AppDatabase(Context context) {
    db = new AppDatabaseHelper(context).getWritableDatabase();
  }
  
  public void close() {
    db.close();
  }
  
  public void addApp(AppDescription app) {
    ContentValues values = new ContentValues();
    values.put(APP_NAME, app.appName);
    values.put(PACKAGE_NAME, app.packageName);
    values.put(CLASS_NAME, app.className);
    values.put(APP_TYPE, app.modes.ordinal());
    db.insert(TABLE_NAME, null, values);
  }
  
  public void deleteAppsWithPackageName(String packageName) {
    db.delete(TABLE_NAME, PACKAGE_NAME + "=\"" + packageName + "\"", null);
  }
  
  public List<AppDescription> getApps() {
    ArrayList<AppDescription> apps = new ArrayList<AppDescription>();
    
    Cursor cursor = db.query(TABLE_NAME, new String[] {
            PACKAGE_NAME,
            CLASS_NAME,
            APP_NAME, 
                APP_TYPE}, 
                null, 
                null, 
                null, 
                null, 
                APP_NAME + " ASC");
    if (cursor.moveToFirst()) {
      do {
        AppType type = AppType.values()[cursor.getInt(3)];
        AppDescription app =
            new AppDescription(cursor.getString(0), cursor.getString(1), cursor.getString(2), type);
        apps.add(app);
      } while (cursor.moveToNext());
    }
    if (cursor != null & !cursor.isClosed()) {
      cursor.close();
    }
    
    return apps;
  }
  
  public static class AppDatabaseHelper extends SQLiteOpenHelper {

    AppDatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + TABLE_NAME +
          "(id INTEGER PRIMARY KEY AUTOINCREMENT, " + APP_NAME + " TEXT, " +
          PACKAGE_NAME + " TEXT, " + CLASS_NAME + " TEXT, " + APP_TYPE + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
      // TODO Auto-generated method stub
    }
  }
}
