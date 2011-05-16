/*
 * Copyright (C) 2009 Jim Blackler jimblackler@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.jimblackler.quickcalendar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class Preferences extends PreferenceActivity {

  private RefreshService mService;
  private ServiceConnection serviceConnection;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);

    PreferenceScreen prefSet = getPreferenceScreen();
    PreferenceScreen calendarsSelect = (PreferenceScreen) prefSet
        .findPreference("calendars_select");

    doCalendarPreferences("content://calendar/calendars", 0, calendarsSelect);
    doCalendarPreferences("content://com.android.calendar/calendars", 0, calendarsSelect);
    doCalendarPreferences("content://calendarEx/calendars", 1, calendarsSelect);
    
    // Get service for refresh
    Intent serviceIntent = new Intent(this, RefreshService.class);
    startService(serviceIntent);
    serviceConnection = new ServiceConnection() {

      public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((RefreshService.RefreshBinder) service).getService();
      }

      public void onServiceDisconnected(ComponentName name) {
        mService = null;
      }
    };
    bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
  }

  private void doCalendarPreferences(String source, int sourceIdx,
      PreferenceScreen preferenceScreen) {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    final Editor edit = preferences.edit();

    final Cursor cursor = getContentResolver().query(Uri.parse(source),
        (new String[] { "_id", "displayName", "selected" }), null, null, "displayName");

    if (cursor != null) {
      while (cursor.moveToNext()) {
        final String key = Common.getCalendarKey(sourceIdx, cursor.getString(0));
        if (!preferences.contains(key)) {
          final boolean selected = !cursor.getString(2).equals("0");
          edit.putBoolean(key, selected);
        }
      }
      edit.commit();
      cursor.moveToPosition(-1);

      while (cursor.moveToNext()) {
        final CheckBoxPreference preference = new CheckBoxPreference(this);
        preference.setTitle(cursor.getString(1));
        preference.setKey(Common.getCalendarKey(sourceIdx, cursor.getString(0)));
        preferenceScreen.addPreference(preference);
      }
    }

  }

  @Override
  protected void onStop() {

    if (mService == null) {
      Log.i("Preferences", "Could not refresh");
    } else {
      mService.refresh();
    }
 
    unbindService(serviceConnection);
    
    super.onStop();
  }

 
}
