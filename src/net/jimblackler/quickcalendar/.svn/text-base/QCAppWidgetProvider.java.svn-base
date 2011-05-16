package net.jimblackler.quickcalendar;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

import net.jimblackler.Utils.TimeConstants;
import net.jimblackler.Utils.TimeFormatter;
import net.jimblackler.Utils.TimeFormatters;
import net.jimblackler.Utils.TimeUtils;
import net.jimblackler.quickcalendar.Common.LookupException;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;

public abstract class QCAppWidgetProvider extends AppWidgetProvider {

  static final String ACTION_CALENDAR_APPWIDGET_UPDATE = 
      "net.jimblackler.quickcalendar.APPWIDGET_UPDATE";

  
  private static final int DAYS_TO_LOOKAHEAD_FOR_WIDGETS = 14;
  static int[] START_TIME_IDS = { R.id.start_time_now, R.id.start_time_next, R.id.start_time_next2,
      R.id.start_time_next3 };
  static int[] TITLE_IDS = { R.id.title_now, R.id.title_next, R.id.title_next2, R.id.title_next3 };

  @Override
  public void onDeleted(Context context, int[] gadgetIds) {
    super.onDeleted(context, gadgetIds);
  }

  @Override
  public void onDisabled(Context context) {
    super.onDisabled(context);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager widgetManager, int[] gadgetIds) {

    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

    boolean enhancedDates = preferences.getBoolean(context.getText(R.string.enhanced_dates_key)
        .toString(), true);

    boolean largeFonts = preferences.getBoolean(context.getText(R.string.large_widget_font_key)
        .toString(), false);

    // boolean largeFonts = isLargeFonts();

    TimeFormatter timeFormatter = TimeFormatters.getTimeFormatter(context.getText(
        R.string.time_language).toString(), DateFormat.is24HourFormat(context), enhancedDates);

    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.gadget_provider);

    boolean allDayInStatusBar = preferences.getBoolean(context.getText(
        R.string.all_day_in_status_bar_key).toString(), true);

    final int N = gadgetIds.length;
    for (int i = 0; i < N; i++) {
      int widgetId = gadgetIds[i];
      final Date date = new Date();
      final long nowUtc = date.getTime();

      final long numberEvents = getNumberEvents();

      try {
        List<ClientEvent> events = Common.calendarQuery(preferences, context.getContentResolver(),
            null, nowUtc, nowUtc + DAYS_TO_LOOKAHEAD_FOR_WIDGETS
                * TimeConstants.MILLISECONDS_PER_DAY, false, allDayInStatusBar);

        final int timezoneOffsetMinutes = date.getTimezoneOffset();
        final long offset = -timezoneOffsetMinutes * TimeConstants.MILLISECONDS_PER_MINUTE;
        final long nowLocal = TimeUtils.utcToLocal(nowUtc);

        int idx = 0;
        boolean futureEventSeen = false;

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

          final int titleViewId = TITLE_IDS[idx];
          views.setViewVisibility(titleViewId, View.VISIBLE);
          views.setTextViewText(titleViewId, (future ? "" : context.getResources().getText(
              R.string.now) + ": ") + event.getTitle());

          try {
            // v.setTextColor(R.id.Title, cursor.getInt(colorIdx));
            RemoteViews.class.getMethod("setTextColor", int.class, int.class).invoke(views,
                titleViewId, event.getColor());
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

          final int startTimeViewId = START_TIME_IDS[idx];
          views.setViewVisibility(startTimeViewId, View.VISIBLE);
          views.setTextViewText(startTimeViewId, Common.getNiceDuration(timeFormatter, nowLocal,
              event.getBeginUtc(), event.getAllDay() != 0, event.getEndUtc(), false));

          if (largeFonts) {
            views.setFloat(titleViewId, "setTextSize", 18);
            views.setFloat(startTimeViewId, "setTextSize", 12);
          } else {
            views.setFloat(titleViewId, "setTextSize", 14);
            views.setFloat(startTimeViewId, "setTextSize", 10);
          }

          idx++;
        }

      } catch (LookupException e) {
        e.printStackTrace();
      } catch (SQLiteException e) {
        e.printStackTrace();
      }

      final Intent intent = new Intent(context, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // needed?
      views.setOnClickPendingIntent(R.id.main_layout, PendingIntent.getActivity(context, 0, intent,
          PendingIntent.FLAG_UPDATE_CURRENT));

      widgetManager.updateAppWidget(widgetId, views);
    }

  }


  
  abstract boolean isLargeFonts();

  abstract long getNumberEvents();
}
