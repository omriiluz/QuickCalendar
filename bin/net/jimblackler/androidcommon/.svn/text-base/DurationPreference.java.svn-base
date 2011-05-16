package net.jimblackler.androidcommon;

import net.jimblackler.Utils.TimeConstants;
import net.jimblackler.quickcalendar.R;
import net.jimblackler.quickcalendar.R.id;
import net.jimblackler.quickcalendar.R.layout;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Spinner;

public class DurationPreference extends DialogPreference {

	private NumberPicker picker;
	private long value;

	public DurationPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.preference_dialog_duration);
	}

	private static final NumberPicker.Formatter NUMBER_FORMATTER = new NumberPicker.Formatter() {
		final StringBuilder mBuilder = new StringBuilder();
		final java.util.Formatter mFmt = new java.util.Formatter(mBuilder);
		final Object[] mArgs = new Object[1];

		public String toString(int value) {
			mArgs[0] = value;
			mBuilder.delete(0, mBuilder.length());
			mFmt.format("%d", mArgs);
			return mFmt.toString();
		}
	};

	private Spinner durationList = null;

	@Override
	protected View onCreateDialogView() {
		View view = super.onCreateDialogView();

		durationList = (Spinner) view.findViewById(R.id.choose_list);
		picker = (NumberPicker) view.findViewById(R.id.NumberPicker);
		picker.setFormatter(NUMBER_FORMATTER);
		picker.setRange(0, 999);
		picker.setSpeed(100);
		return view;
	}

  @Override
  public void onDismiss(DialogInterface dialog) {
    value = getDurationMultiplier(durationList.getSelectedItemPosition()) * picker.getCurrent();
    super.onDismiss(dialog);
  }

  private long getDurationMultiplier(int selectedItemPosition) {
		final long[] durationValues = new long[] { TimeConstants.MILLISECONDS_PER_MINUTE,
				TimeConstants.MILLISECONDS_PER_HOUR, TimeConstants.MILLISECONDS_PER_DAY,
				TimeConstants.MILLISECONDS_PER_WEEK };

		return durationValues[selectedItemPosition];
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		int size = durationList.getCount();
		for (int count = size - 1; count >= 0; count--) {
			long durationMultiplier = getDurationMultiplier(count);
			if (value >= durationMultiplier) {
				long remainder = value % durationMultiplier;
				if (remainder == 0) {
					durationList.setSelection(count);
					break;
				}
			}
		}
		picker.setCurrent((int) (value / getDurationMultiplier(durationList.getSelectedItemPosition())));
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return Long.parseLong(a.getString(index));
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			setValue(getPersistedLong(0));
		} else {
			setValue(((Number) defaultValue).longValue());
		}
	}

	public void setValue(long value) {
		persistLong(value);

		notifyDependencyChange(false);
		this.value = value;
	}

	public long getValue() {
		return value;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		long value = getValue();

		if (positiveResult) {
			if (callChangeListener(value)) {
				setValue(value);
			}
		}
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (isPersistent()) {
			// No need to save instance state since it's persistent
			return superState;
		}

		final SavedState myState = new SavedState(superState);
		myState.ticks = getValue();
		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (!state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());
		setValue(myState.ticks);
	}

	private static class SavedState extends BaseSavedState {
		long ticks;

		public SavedState(Parcel source) {
			super(source);
			ticks = source.readLong();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeLong(ticks);
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

}
