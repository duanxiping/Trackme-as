package com.wise.model;

public class LocationBuffer {
	
	public String ObjectID;
	public String Lat;
	public String Lon;
	public String GPSFlag;
	public String StatusDes;
	
	public String GPSTime;
	
	
	public String getGPSTime() {
		return GPSTime;
	}
	public void setGPSTime(String gPSTime) {
		GPSTime = gPSTime;
	}
	public String getObjectID() {
		return ObjectID;
	}
	public void setObjectID(String objectID) {
		ObjectID = objectID;
	}
	public String getLat() {
		return Lat;
	}
	public void setLat(String lat) {
		Lat = lat;
	}
	public String getLon() {
		return Lon;
	}
	public void setLon(String lon) {
		Lon = lon;
	}
	public String getGPSFlag() {
		return GPSFlag;
	}
	public void setGPSFlag(String gPSFlag) {
		GPSFlag = gPSFlag;
	}
	public String getStatusDes() {
		return StatusDes;
	}
	public void setStatusDes(String statusDes) {
		StatusDes = statusDes;
	}

}
