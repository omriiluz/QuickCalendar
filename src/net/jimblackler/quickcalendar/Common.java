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

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import net.jimblackler.Utils.TimeFormatter;
import net.jimblackler.Utils.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Common {

  public static class LookupException extends Exception {

    private static final long serialVersionUID = 1L;

    public LookupException(String detailMessage) {
      super(detailMessage);
    }
  }

  static List<ClientEvent> calendarQuery(final SharedPreferences preferences,
      final ContentResolver contentResolver, final String searchString, final long startUtc,
      final long endUtc, boolean searchAll, boolean includeAllDayEvents) throws LookupException {

    // TODO: generalize this content with that in Preferences
    String[] calendarSetUrls = { "content://calendar", "content://com.android.calendar", "content://calendarEx" };
    int[] preferenceIds = {0, 0, 1};
    
    List<ClientEvent> events = new ArrayList<ClientEvent>();

    boolean anyService = false;
    
    for (int sourceIdx = 0; sourceIdx != calendarSetUrls.length; sourceIdx++) {

      String calendarSetUrl = calendarSetUrls[sourceIdx];
      StringBuilder where = new StringBuilder();

      if (!searchAll) {
        // Get selected calendars query
        final Cursor cursor = contentResolver.query(Uri.parse(calendarSetUrl + "/calendars"),
            new String[] { "_id", "selected" }, null, null, "displayName");

        if (cursor == null) {
          continue;
        }
        anyService = true;
        
        final String initialSeperator = "(";
        String seperator = initialSeperator;

        while (cursor.moveToNext()) {

          final String rawKey = cursor.getString(0);
          final String key = getCalendarKey(preferenceIds[sourceIdx], rawKey);
          if (preferences.getBoolean(key, !cursor.getString(1).equals("0"))) {
            where.append(seperator);
            where.append("Calendars._id=").append(rawKey);
            seperator = " OR ";
          }
        }
        if (seperator == initialSeperator) {
          continue; // Do nothing .. no calendars selected
        } else {
          where.append(") AND ");
        }
      }

      final long startLocal = TimeUtils.utcToLocal(startUtc);
      final long endLocal = TimeUtils.utcToLocal(endUtc);

      where.append("(");
      if (includeAllDayEvents) {
        where.append("(allDay == 1 AND ").append(startLocal).append(" < end AND ").append(endLocal)
            .append(" >= begin) OR ");
      }
      where.append("(allDay == 0 AND ").append(startUtc).append(" < end AND ").append(endUtc)
          .append(" >= begin)");
      where.append(")");

      if (searchString != null) {
        where.append("AND (title LIKE ");
        DatabaseUtils.appendEscapedSQLString(where, "%" + searchString + "%");
        
        where.append(" OR description LIKE ");
        DatabaseUtils.appendEscapedSQLString(where, "%" + searchString + "%");
        
        where.append(" OR eventLocation LIKE ");
        DatabaseUtils.appendEscapedSQLString(where, "%" + searchString + "%");

        where.append(" ) ");
      }

      Uri.Builder builder = Uri.parse(calendarSetUrl + "/instances/when").buildUpon();
      ContentUris.appendId(builder, Math.min(startUtc, startLocal));
      ContentUris.appendId(builder, Math.max(endUtc, endLocal));
      Uri build = builder.build();

      Cursor cursor = contentResolver.query(build, new String[] { "title", "begin", "end",
          "allDay", "event_id", "color", "_id", "startDay", "startMinute", "description"}, where.toString(),
          null, null);

      if (cursor != null) {
        anyService = true;
        while (cursor.moveToNext()) {
          events.add(new ClientEvent(calendarSetUrl, cursor.getString(0), cursor.getString(9), cursor.getLong(1),
              cursor.getLong(2), cursor.getInt(3), cursor.getLong(4), cursor.getInt(5),
              cursor.getString(6), cursor.getLong(7), cursor.getInt(8)));
          String s=cursor.getString(9);
        }
      }
    }
    
    if (!anyService) { 
      throw new LookupException("Calendar service could not be found");
    }
    
    Collections.sort(events, new Comparator<ClientEvent>() {

      public int compare(ClientEvent object1, ClientEvent object2) {
        // "startDay ASC, startMinute ASC"
        int compare = new Long(object1.getStartDay()).compareTo(object2.getStartDay());
        if (compare == 0) {
          compare = new Integer(object1.getStartMinute()).compareTo(object2.getStartMinute());
        }
        return compare;
      }
    });
    return events;

  }

  static String getNiceDuration(TimeFormatter timeFormatter, final long nowLocal, long beginUtc,
      final boolean allDayEvent, final long endUtc, boolean in) {

    if (allDayEvent) {
      final long beginLocal = beginUtc; // Utc == local conceptually for all day events
      final long endLocal = endUtc;
      return timeFormatter.dailyRelativeStart(beginLocal, endLocal, nowLocal);
    } else {
      final long beginLocal = TimeUtils.utcToLocal(beginUtc);
      final long endLocal = TimeUtils.utcToLocal(endUtc);
      return timeFormatter.relativeStart(beginLocal, endLocal, nowLocal, in);
    }
  }

  public static PendingIntent getPendingIntent(Context context, String idxString, int requestCode) {
    final Intent intent = new Intent(context, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // needed?
    intent.putExtra("instanceId", idxString);
    return PendingIntent.getActivity(context, requestCode, intent,
        PendingIntent.FLAG_CANCEL_CURRENT
    /* PendingIntent.FLAG_UPDATE_CURRENT */);

  }

  public static String getCalendarKey(int sourceIdx, String idString) {
    if (sourceIdx == 0) {
      return "include_calendar_" + idString; // BACKWARDS COMPATIBILITY FOR 40,000+ USERS
    } else {
      return "include_calendar_" + idString + "_" + sourceIdx;
    }
  }

}
