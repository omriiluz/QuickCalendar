package net.jimblackler.quickcalendar;

public class ClientEvent {

	private String title;
	private String description;
	private String location;
	private long beginUtc;
	private long endUtc;
	private int allDay;
	private long eventId;
	private int color;
	private String id;
	private long startDay;
	private int startMinute;
	private String calendarPrefix;

	public ClientEvent(String calendarPrefix, String title, String description,
			String location, long beginUtc, long endUtc, int allDay,
			long eventId, int color, String id, long startDay, int startMinute) {
		this.calendarPrefix = calendarPrefix;
		this.title = title;
		this.description = description;
		this.location = location;
		this.beginUtc = beginUtc;
		this.endUtc = endUtc;
		this.allDay = allDay;
		this.eventId = eventId;
		this.color = color;
		this.id = id;
		this.startDay = startDay;
		this.startMinute = startMinute;
	}

	public String getCalendarPrefix() {
		return calendarPrefix;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getLocation() {
		return location;
	}

	public long getBeginUtc() {
		return beginUtc;
	}

	public long getStartDay() {
		return startDay;
	}

	public int getStartMinute() {
		return startMinute;
	}

	public long getEndUtc() {
		return endUtc;
	}

	public int getAllDay() {
		return allDay;
	}

	public long getEventId() {
		return eventId;
	}

	public int getColor() {
		return color;
	}

	public String getId() {
		return id;
	}

}
