package com.wise.model;

import android.location.Location;

public class FiveMinutesEvent {

	private Location location;

	public Location getLocation() {
		return location;
	}

	public FiveMinutesEvent(Location mLocation){
		this.location = mLocation;
	}
}
