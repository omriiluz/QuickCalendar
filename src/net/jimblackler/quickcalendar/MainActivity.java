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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jimblackler.Utils.TimeConstants;
import net.jimblackler.Utils.TimeFormatter;
import net.jimblackler.Utils.TimeFormatters;
import net.jimblackler.Utils.TimeUtils;
import net.jimblackler.quickcalendar.Common.LookupException;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends ListActivity {

  private RefreshService mService;

  private String queryString = null;
  private long startUtc = 0;
  private long endUtc = 0;
  private boolean searchAll = false;

  private String focusInstanceId;

  private TimeFormatter timeFormatter;

  private List<ClientEvent> events = new ArrayList<ClientEvent>();
  
  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {

    Intent serviceIntent = new Intent(this, RefreshService.class);
    startService(serviceIntent);
    bindService(serviceIntent, new ServiceConnection() {

      public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((RefreshService.RefreshBinder) service).getService();
      }

      public void onServiceDisconnected(ComponentName name) {
        mService = null;
      }
    }, Context.BIND_AUTO_CREATE);

    setContentView(R.layout.main);

    PackageManager pm = getPackageManager();
    pm.setComponentEnabledSetting(new ComponentName(this, RefreshService.class),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    registerForContextMenu(getListView());
    
    super.onCreate(savedInstanceState);
  }
  
  @Override
  protected void onDestroy() {
  
    super.onDestroy();
  }

  @Override
  protected void onStart() {

    tf_hour = DateFormat.is24HourFormat(this);

    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    boolean enhancedDates = preferences.getBoolean(getText(R.string.enhanced_dates_key).toString(),
        true);

    timeFormatter = TimeFormatters.getTimeFormatter(getText(R.string.time_language).toString(),
        tf_hour, enhancedDates);

    final Intent intent = getIntent();

    final String queryAction = intent.getAction();
    if (Intent.ACTION_SEARCH.equals(queryAction)) {
      queryString = intent.getStringExtra(SearchManager.QUERY);
      startUtc = intent.getLongExtra("start_date", 0);
      endUtc = intent.getLongExtra("end_date", 0);
      searchAll = intent.getBooleanExtra("search_all_calendars", false);
      this.setTitle(getText(R.string.results_for) + " \"" + queryString + "\"");

    } else {
      focusInstanceId = intent.getStringExtra("instanceId");
      Log.i("MainActivity", "FocusInstanceId:" + focusInstanceId);
    }

    refresh();

    super.onStart();
  }

  Timer timer = null;

  List<Integer> listToDbIdx;
  List<Boolean> isHeader;

  boolean coloredCalendars;
  boolean headers;
  boolean tf_hour;

  private void refresh() {

    final View progressContainer = findViewById(R.id.ProgressIndicatorContainer);

    progressContainer.setVisibility(View.VISIBLE);
    getListView().setVisibility(View.GONE);

    final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    coloredCalendars = preferences.getBoolean(getText(R.string.colored_calendars_key).toString(),
        true);

    headers = preferences.getBoolean(getText(R.string.headers_key).toString(), true);

    if (queryString == null) {
      startUtc = new Date().getTime();

      endUtc = startUtc
          + (preferences.getLong(getResources().getText(R.string.events_to_list_key).toString(),
              Long.parseLong(getResources().getText(R.string.events_to_list_default).toString())));

      setTitle("Quick Calendar");
    }

    new Thread(new Runnable() {

      public void run() {

        try {

          List<ClientEvent> newEvents = Common.calendarQuery(preferences, getContentResolver(), queryString, startUtc,
              endUtc, searchAll, true);

          listToDbIdx = new ArrayList<Integer>();
          isHeader = new ArrayList<Boolean>();
          events = newEvents;
          
          int focusIdx = -1;

          long prevDayBeginLocal = 0;
          int count = 0;

          for (ClientEvent event : events) {
            if (event.getId().equals(focusInstanceId)) {
              focusIdx = listToDbIdx.size();
            }

            if (headers) {
              final long beginLocal;
              final long beginUtc = event.getBeginUtc();
              if (event.getAllDay() != 0) {
                beginLocal = beginUtc; // Utc == local conceptually for
                // all day events
              } else {
                beginLocal = TimeUtils.utcToLocal(beginUtc);
              }
              final long dayBeginLocal = beginLocal / TimeConstants.MILLISECONDS_PER_DAY;
              if (dayBeginLocal != prevDayBeginLocal) {
                // header
                listToDbIdx.add(count);
                isHeader.add(true);
              }
              prevDayBeginLocal = dayBeginLocal;

            }
            listToDbIdx.add(count);
            isHeader.add(false);
            count++;
          }
          
          final int toFocusIdx = focusIdx;
          
          
          runOnUiThread(new Thread() {
            @Override
            public void run() {
              if (events.isEmpty()) {
                if (queryString == null) {
                  findViewById(R.id.no_events_advice).setVisibility(View.VISIBLE);
                  findViewById(R.id.no_search_results).setVisibility(View.GONE);
                } else {
                  findViewById(R.id.no_events_advice).setVisibility(View.GONE);
                  findViewById(R.id.no_search_results).setVisibility(View.VISIBLE);
                }
              } else {
                findViewById(R.id.no_events_advice).setVisibility(View.GONE);
                findViewById(R.id.no_search_results).setVisibility(View.GONE);

                
                final LayoutInflater inflater = (LayoutInflater) MainActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                final long nowLocal = TimeUtils.utcToLocal(new Date().getTime());

                
                setListAdapter(new BaseAdapter() {

                  public int getCount() {
                    return listToDbIdx.size();

                  }

                  public Object getItem(int position) {
                    return events.get(listToDbIdx.get(position));
                  }

                  public long getItemId(int position) {
                    return position;
                  }

                  public View getView(int position, View convertView, ViewGroup parent) {
                    ClientEvent event = events.get(listToDbIdx.get(position));
                    View v;

                    final long beginLocal;
                    final long endLocal;
                    final long beginUtc = event.getBeginUtc();
                    if (event.getAllDay() != 0) {
                      beginLocal = beginUtc; // Utc == local conceptually for
                      // all day events
                      endLocal = beginLocal + TimeConstants.MILLISECONDS_PER_DAY;
                    } else {
                      beginLocal = TimeUtils.utcToLocal(beginUtc);
                      endLocal = TimeUtils.utcToLocal(endUtc);
                    }
                    if (isHeader.get(position)) {

                      if (convertView != null && convertView.getTag().equals(R.layout.header)) {
                        v = convertView;
                      } else {
                        v = inflater.inflate(R.layout.header, parent, false);
                        v.setTag(R.layout.header);
                      }

                      ((TextView) v.findViewById(R.id.DayName)).setText(timeFormatter
                          .relativeDayName(beginLocal, false, nowLocal));
                    } else {
                      int layoutId;
                      if (headers) {
                        layoutId = R.layout.list_entry_categorized;
                      } else {
                        layoutId = R.layout.list_entry;
                      }

                      if (convertView != null && convertView.getTag().equals(layoutId)) {
                        v = convertView;
                      } else {
                        v = inflater.inflate(layoutId, parent, false);
                        v.setTag(layoutId);
                      }

                      final TextView titleTextView = (TextView) v.findViewById(R.id.Title);
                      if (coloredCalendars) {
                        titleTextView.setTextColor(event.getColor());
                      }

                      boolean now = (nowLocal >= beginLocal && nowLocal <= endLocal);

                      final String title = event.getTitle();
                      titleTextView.setText(title);

                      String text;
                      boolean allDay = (event.getAllDay() != 0);
                      final TextView textTextView = ((TextView) v
                          .findViewById(tf_hour || !headers ? R.id.StartTime : R.id.StartTime12));
                      if (headers) {
                        if (allDay) {
                          text = ""; // 'all day'
                        } else {
                          text = timeFormatter.timeOfDayString(beginLocal);
                          textTextView.setVisibility(View.VISIBLE);
                        }
                      } else {
                        text = Common.getNiceDuration(timeFormatter, nowLocal, beginUtc, allDay,
                            event.getEndUtc(), true);
                        textTextView.setVisibility(View.VISIBLE);
                      }

                      textTextView.setText(text
                          + (now ? " (" + getResources().getText(R.string.now) + ")" : ""));
                    }
                    return v;
                  }
                });

               
                getListView().setSelection(toFocusIdx);
                
                
              }
              progressContainer.setVisibility(View.GONE);
              getListView().setVisibility(View.VISIBLE);

            }

          });

          getListView().setTextFilterEnabled(true);

        } catch (final LookupException e) {
          e.printStackTrace();
          runOnUiThread(new Thread() {

            @Override
            public void run() {
              progressContainer.setVisibility(View.GONE);
              final AlertDialog alert = new AlertDialog.Builder(MainActivity.this).create();
              alert.setMessage(e.getLocalizedMessage());
              alert.setButton("Quit", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                  MainActivity.this.finish();
                }
              });
              alert.show();

            }

          });

          
          
        }

      }
    }).start();

  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    if (!isHeader.get(position)) {
      launchEvent(Intent.ACTION_VIEW, position);
    }
  }

  private void launchEvent(String action, int position) {
    ClientEvent event = events.get(listToDbIdx.get(position));
    String confNumber;
    
    try {
    	Pattern p = Pattern.compile("Blackberry: ([0-9\\-\\+\\.()]+) ?x ?([0-9]+)");
    	Matcher m = p.matcher(event.getDescription());
    	if (m.find()) {
    		confNumber = "tel:"+m.group(1)+","+m.group(2)+"#";
    		Uri callUri = Uri.parse(confNumber);
    		startActivity(new Intent(Intent.ACTION_CALL, callUri));
    	} else {
    		Intent intent = new Intent(action);
    	    Uri eventUri = ContentUris.withAppendedId(Uri.parse(event.getCalendarPrefix() + "/events"),
    	        event.getEventId());
    	    intent.setData(eventUri);
    	    intent.putExtra("beginTime", event.getBeginUtc());
    	    intent.putExtra("endTime", event.getEndUtc());
    	}
    }
    catch (Exception e) {
    	Log.e(getLocalClassName(), e.getMessage());
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    final int position = info.position;

    if (!isHeader.get(position)) {
      {
        MenuItem menuItem = menu.add(getText(R.string.edit));
        menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

          public boolean onMenuItemClick(MenuItem item) {
            launchEvent(Intent.ACTION_EDIT, position);
            return false;
          }
        });
      }

      {
        MenuItem menuItem = menu.add(getText(R.string.view));
        menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

          public boolean onMenuItemClick(MenuItem item) {
            launchEvent(Intent.ACTION_VIEW, position);
            return false;
          }
        });
      }
      
    }
    {
      MenuItem menuItem = menu.add(getText(R.string.new_event));
      menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

        public boolean onMenuItemClick(MenuItem item) {
          ClientEvent event = events.get(listToDbIdx.get(position));
          Intent intent = new Intent(Intent.ACTION_EDIT);
          intent.setType("vnd.android.cursor.item/event");

          final long beginUtc = event.getBeginUtc();
          long begin = beginUtc - beginUtc % TimeConstants.MILLISECONDS_PER_HOUR;// TimeConstants.MILLISECONDS_PER_DAY;
          intent.putExtra("beginTime", begin);
          intent.putExtra("endTime", begin + TimeConstants.MILLISECONDS_PER_HOUR);
          intent.putExtra("allDay", (beginUtc % TimeConstants.MILLISECONDS_PER_DAY) == 0/* true */);

          startActivity(intent);
          return false;
        }
      });

    }
    super.onCreateContextMenu(menu, v, menuInfo);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {

    return super.onContextItemSelected(item);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    {
      MenuItem menuItem = menu.add(getText(R.string.refresh));
      menuItem.setIcon(R.drawable.ic_menu_refresh);
      menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

        public boolean onMenuItemClick(MenuItem item) {
          refresh();
          return false;
        }
      });
    }

    {
      MenuItem menuItem = menu.add(getText(R.string.search));
      menuItem.setIcon(R.drawable.ic_menu_search);
      menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
          Intent intent = new Intent(MainActivity.this, Search.class);
          startActivity(intent);
          return false;
        }
      });
    }

    {
      MenuItem menuItem = menu.add(getText(R.string.about));
      menuItem.setIcon(R.drawable.ic_menu_info_details);
      menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        public boolean onMenuItemClick(MenuItem item) {
          Intent intent = new Intent(MainActivity.this, About.class);
          startActivity(intent);
          return false;
        }
      });
    }

    {
      MenuItem menuItem = menu.add(getText(R.string.preferences));
      menuItem.setIcon(R.drawable.ic_menu_preferences);
      menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

        public boolean onMenuItemClick(MenuItem item) {

          Intent intent = new Intent(MainActivity.this, Preferences.class);
          startActivity(intent);
          return false;
        }
      });
    }

    return true;

  }

  @Override
  public boolean onSearchRequested() {
    Intent intent = new Intent(MainActivity.this, Search.class);
    startActivity(intent);
    return false;
  }
}