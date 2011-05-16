package net.jimblackler.quickcalendar;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class QCWidgetConfigure extends Activity {

	int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.widget_configure);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}

		Button button = (Button)findViewById(R.id.TwoSlotButton);
		button.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				
				QCWidgetConfigure widgetConfigure = QCWidgetConfigure.this;
				widgetConfigure.setResult(RESULT_OK);
				widgetConfigure.finish();
			}});
	}

}
