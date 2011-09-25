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
  private static final String APPS_TABLE_NAME = "apps";
  private static final String WIDGET_SETUP_TABLE_NAME = "widget_setup";
  private static final String HOTKEY_SETUP_TABLE_NAME = "hotkey_setup";
  private static final String APP_NAME = "name";
  private static final String PACKAGE_NAME = "package_name";
  private static final String CLASS_NAME = "class_name";
  private static final String APP_TYPE = "app_type";
  private static final String COL_WIDGET_INDEX = "widget_index";
  private static final String COL_HOTKEYS = "hotkeys";
  
  private static final String WHERE_WIDGET = String.format("%s = %d OR %s = %d",
      APP_TYPE, AppType.WIDGET.ordinal(), APP_TYPE, AppType.WIDGET_AND_APP.ordinal());
  private static final String WHERE_APP = String.format("%s = %d OR %s = %d",
          APP_TYPE, AppType.APP.ordinal(), APP_TYPE, AppType.WIDGET_AND_APP.ordinal());
  
  private static final int NUM_WIDGETS_ON_SCREEN = 3;
  
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
    db.insert(APPS_TABLE_NAME, null, values);
  }
  
  public int deleteAppsWithPackageName(String packageName) {
    return db.delete(APPS_TABLE_NAME, PACKAGE_NAME + "=\"" + packageName + "\"", null);
  }
  
  public List<AppDescription> getApps() {
    return getApps(WHERE_APP);
  }
  
  public List<AppDescription> getWidgets() {
    return getApps(WHERE_WIDGET);
  }
  
  private List<AppDescription> getApps(String whereClause) {
    ArrayList<AppDescription> apps = new ArrayList<AppDescription>();
    
    Cursor cursor = db.query(APPS_TABLE_NAME, new String[] {
            PACKAGE_NAME,
            CLASS_NAME,
            APP_NAME, 
                APP_TYPE}, 
                whereClause, 
                null, 
                null, 
                null, 
                APP_NAME + " ASC");
    if (cursor.moveToFirst()) {
      do {
        AppType type = AppType.values()[cursor.getInt(3)];
        AppDescription app = new AppDescription(
            cursor.getString(0), cursor.getString(1), null, cursor.getString(2), type);
        apps.add(app);
      } while (cursor.moveToNext());
    }
    if (cursor != null & !cursor.isClosed()) {
      cursor.close();
    }
    
    return apps;
  }
  
  public List<AppDescription> getWidgetSetup() {
    ArrayList<AppDescription> widgets = new ArrayList<AppDescription>(NUM_WIDGETS_ON_SCREEN);
    for (int i = 0; i < NUM_WIDGETS_ON_SCREEN; i++) {
      widgets.add(null);
    }
    
    Cursor cursor = db.query(WIDGET_SETUP_TABLE_NAME,
        new String[] { PACKAGE_NAME, CLASS_NAME, APP_NAME, APP_TYPE, COL_WIDGET_INDEX }, 
        null, 
        null, 
        null, 
        null, 
        COL_WIDGET_INDEX + " ASC");
    if (cursor.moveToFirst()) {
      do {
        AppType type = AppType.values()[cursor.getInt(3)];
        AppDescription app = new AppDescription(
            cursor.getString(0), cursor.getString(1), null, cursor.getString(2), type);
        widgets.set(cursor.getInt(4), app);
      } while (cursor.moveToNext());
    }
    if (cursor != null & !cursor.isClosed()) {
      cursor.close();
    }
    
    return widgets;
  }
  
  public void setWidgetSetup(List<AppDescription> widgets) {
    // Clear out the old values
    db.delete(WIDGET_SETUP_TABLE_NAME, null, null);
    
    for (int i = 0; i < widgets.size(); i++) {
      AppDescription app = widgets.get(i);
      if (app == null) {
        continue;
      }

      ContentValues values = new ContentValues();
      values.put(APP_NAME, app.appName);
      values.put(PACKAGE_NAME, app.packageName);
      values.put(CLASS_NAME, app.className);
      values.put(APP_TYPE, app.modes.ordinal());
      values.put(COL_WIDGET_INDEX, i);
      db.insert(WIDGET_SETUP_TABLE_NAME, null, values);
    }
  }

  public List<HotkeySetupEntry> getHotkeySetup() {
    ArrayList<HotkeySetupEntry> entries = new ArrayList<HotkeySetupEntry>();
    
    Cursor cursor = db.query(HOTKEY_SETUP_TABLE_NAME,
        new String[] { PACKAGE_NAME, CLASS_NAME, APP_NAME, APP_TYPE, COL_HOTKEYS }, 
        null, 
        null, 
        null, 
        null, 
        COL_HOTKEYS + " ASC");
    if (cursor.moveToFirst()) {
      do {
        AppType type = AppType.values()[cursor.getInt(3)];
        AppDescription app = new AppDescription(
            cursor.getString(0), cursor.getString(1), null, cursor.getString(2), type);
        entries.add(new HotkeySetupEntry(app, (byte) cursor.getInt(4)));
      } while (cursor.moveToNext());
    }
    if (cursor != null & !cursor.isClosed()) {
      cursor.close();
    }
    
    return entries;
  }
  
  public void setHotkeySetup(List<HotkeySetupEntry> hotkeyEntries) {
    // Clear out the old values
    db.delete(HOTKEY_SETUP_TABLE_NAME, null, null);
    
    for (HotkeySetupEntry entry : hotkeyEntries) {
      if (entry.app == null) {
        continue;
      }

      ContentValues values = new ContentValues();
      values.put(APP_NAME, entry.app.appName);
      values.put(PACKAGE_NAME, entry.app.packageName);
      values.put(CLASS_NAME, entry.app.className);
      values.put(APP_TYPE, entry.app.modes.ordinal());
      values.put(COL_HOTKEYS, entry.hotkeys);
      db.insert(HOTKEY_SETUP_TABLE_NAME, null, values);
    }
  }

  public static class AppDatabaseHelper extends SQLiteOpenHelper {

    AppDatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("CREATE TABLE " + APPS_TABLE_NAME +
          "(id INTEGER PRIMARY KEY AUTOINCREMENT, " + APP_NAME + " TEXT, " +
          PACKAGE_NAME + " TEXT, " + CLASS_NAME + " TEXT, " + APP_TYPE + " INTEGER)");
      
      db.execSQL("CREATE TABLE " + WIDGET_SETUP_TABLE_NAME +
          "(" + COL_WIDGET_INDEX + " INTEGER PRIMARY KEY, " + APP_NAME + " TEXT, " +
          PACKAGE_NAME + " TEXT, " + CLASS_NAME + " TEXT, " + APP_TYPE + " INTEGER)");

      db.execSQL("CREATE TABLE " + HOTKEY_SETUP_TABLE_NAME +
              "(" + COL_HOTKEYS + " INTEGER PRIMARY KEY, " + APP_NAME + " TEXT, " +
              PACKAGE_NAME + " TEXT, " + CLASS_NAME + " TEXT, " + APP_TYPE + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
      // TODO Auto-generated method stub
    }
  }
}
