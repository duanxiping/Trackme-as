package com.wise.model;

import android.location.Location;

public class UIUpdataEvent {
	
	private String lat  = "";
	private String lon  = "";
	
	private String detail_location = "";

	public UIUpdataEvent(String detail_location, Location mLocation) {
         this.detail_location = detail_location;
         this.lon = String.valueOf(mLocation.getLongitude());
         this.lat = String.valueOf(mLocation.getLatitude());
    } 
	
	public UIUpdataEvent(String detail_location, String lat, String lon) {
        this.detail_location = detail_location;
        this.lon = lon;
        this.lat = lat;
   } 
	
	
	
	public String getDetailLocation() {
			return detail_location;
	}

	 
	public String getLat() {
		return lat;
    }

	public String getLon() {
		return lon;
	}
}
