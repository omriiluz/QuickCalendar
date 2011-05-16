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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.RemoteViews;
import net.jimblackler.Utils.TimeConstants;
import net.jimblackler.Utils.TimeFormatter;
import net.jimblackler.Utils.TimeFormatters;
import net.jimblackler.Utils.TimeUtils;
import net.jimblackler.quickcalendar.Common.LookupException;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

public class RefreshService extends Service {

  public class RefreshBinder extends Binder {
    RefreshService getService() {
      return RefreshService.this;
    }
  }

  private static final int MAXIMUM_NOTIFY_EVENTS = 2;

  private final IBinder binder = new RefreshBinder();

  private final Handler handler = new Handler();

  private final Runnable refresher = new Runnable() {
    public void run() {
      refresh();
    }

  };

  private BroadcastReceiver screenOffReceiver;

  private boolean screenOn = true;

  private BroadcastReceiver screenOnReceiver;

  @Override
  public IBinder onBind(Intent arg0) {
    Log.i(RefreshService.class.getName(), "onBind");
    return binder;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    screenOnReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {
        Log.i("RefreshService", "Screen on");
        refresh();
        screenOn = true;
      }
    };

    registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));

    screenOffReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {
        Log.i("RefreshService", "Screen off");
        screenOn = false;
      }
    };

    registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

    getContentResolver().registerContentObserver(Uri.parse("content://calendar/events"), true,
        new ContentObserver(new Handler()) {
          @Override
          public void onChange(boolean selfChange) {
            Log.i("Refresh service", "Calendar changed");
            refresh();
          }
        });

    refresh();

  }

  @Override
  public void onDestroy() {
    Log.i(RefreshService.class.getName(), "onDestroy");
    if (screenOnReceiver != null) {
      unregisterReceiver(screenOnReceiver);
    }
    if (screenOffReceiver != null) {
      unregisterReceiver(screenOffReceiver);
    }
    handler.removeCallbacks(refresher);
  }

  @Override
  public boolean onUnbind(Intent arg) {
    Log.i(RefreshService.class.getName(), "onUnbind");
    return true;
  }

  public void refresh() {

    Log.i("Refresh Service", "Refreshing");
    refreshNotificationIcons();

    refreshWidgets();

    scheduleNext();

  }

  private void refreshNotificationIcons() {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    boolean enhancedDates = preferences.getBoolean(getText(R.string.enhanced_dates_key).toString(),
        true);

    TimeFormatter timeFormatter = TimeFormatters.getTimeFormatter(getText(R.string.time_language)
        .toString(), DateFormat.is24HourFormat(this), enhancedDates);

    Log.i(RefreshService.class.getName(), "Refreshed");

    boolean hideIcons = preferences.getBoolean(getText(R.string.hide_icons_key).toString(), true);

    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    final int numberEvents = Integer
        .parseInt(preferences.getString(getText(R.string.number_events_key).toString(), getText(
            R.string.number_events_default).toString()));

    boolean allDayInStatusBar = preferences.getBoolean(getText(R.string.all_day_in_status_bar_key)
        .toString(), true);

    final Date date = new Date();
    final long nowUtc = date.getTime();

    try {
      List<ClientEvent> events = Common.calendarQuery(preferences, getContentResolver(), null,
          nowUtc, nowUtc
              + (preferences.getLong(getResources().getText(R.string.events_to_alert_key)
                  .toString(), Long.parseLong(getResources().getText(
                  R.string.events_to_alert_default).toString()))), false, allDayInStatusBar);

      final int timezoneOffsetMinutes = date.getTimezoneOffset();
      final long offset = -timezoneOffsetMinutes * TimeConstants.MILLISECONDS_PER_MINUTE;
      final long nowLocal = TimeUtils.utcToLocal(nowUtc);

      int idx = 0;
      boolean futureEventSeen = false;

      {
        for (ClientEvent event : events) {

          if (idx == numberEvents) {
            break;
          }

          long begin = event.getBeginUtc();
          if (event.getAllDay() == 0) {
            begin += offset;
          }
          final boolean future = begin > nowLocal;

          if (!futureEventSeen) {
            if (future) {
              futureEventSeen = true;
            } else {
              if (idx > 0) {
                idx--;
              } // A very hacky way of including just one 'now' event, the
              // last one before future events
            }

          }

          Notification notification = new Notification();
          notification.icon = hideIcons ? R.drawable.onebyone
              : (future ? R.drawable.calendar_notify_later : R.drawable.calendar_notify_now);
          notification.flags |= Notification.FLAG_ONGOING_EVENT;
          notification.when = nowLocal + (hideIcons ? TimeConstants.MILLISECONDS_PER_WEEK : 0)
              - idx;
          // fake old date causes reverse normal setting

          notification.contentView = new RemoteViews("net.jimblackler.quickcalendar",
              future ? R.layout.notification : R.layout.notification_now);

          final RemoteViews v = notification.contentView;
          v.setTextViewText(R.id.Title, event.getTitle());
          try {
            // v.setTextColor(R.id.Title, cursor.getInt(colorIdx));
            RemoteViews.class.getMethod("setTextColor", int.class, int.class).invoke(v, R.id.Title,
                event.getColor());
          } catch (SecurityException e) {
            e.printStackTrace();
          } catch (IllegalArgumentException e) {
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          } catch (InvocationTargetException e) {
            e.printStackTrace();
          } catch (NoSuchMethodException e) {
          }

          v.setTextViewText(R.id.StartTime, Common.getNiceDuration(timeFormatter, nowLocal, event
              .getBeginUtc(), event.getAllDay() != 0, event.getEndUtc(), true));

          notification.contentIntent = Common.getPendingIntent(this, event.getId(), 2 + idx);
          notificationManager.notify(idx, notification);

          idx++;
        }

        while (idx <= MAXIMUM_NOTIFY_EVENTS) {
          notificationManager.cancel(idx);
          idx++;
        }
      }
    } catch (LookupException e) {
      e.printStackTrace();
    }
  }

  /**
   * Note: The point of doing this here is to allow user control over the update frequency
   */
  private void refreshWidgets() {

    AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
    refreshWidgets(widgetManager, QCGadgetProvider.class);
    refreshWidgets(widgetManager, QCAppWidgetProvider2.class);
    refreshWidgets(widgetManager, QCAppWidgetProvider2DH.class);
    refreshWidgets(widgetManager, QCAppWidgetProvider3DH.class);
  }

  private void refreshWidgets(AppWidgetManager widgetManager, Class<?> class1) {
    try {
      int[] gadgetIds = widgetManager.getAppWidgetIds(new ComponentName(this, class1));
      if (gadgetIds.length > 0) {
        AppWidgetProvider provider = (AppWidgetProvider) class1.getConstructor().newInstance();
        provider.onUpdate(this, widgetManager, gadgetIds);
      }

    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } finally {
    }
  }

  private void scheduleNext() {
    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    if (screenOn) {
      handler.postDelayed(refresher, preferences.getLong(getResources().getText(
          R.string.refresh_frequency_key).toString(), Long.parseLong(getResources().getText(
          R.string.refresh_frequency_default).toString())));
    }
  }
  
 

}
