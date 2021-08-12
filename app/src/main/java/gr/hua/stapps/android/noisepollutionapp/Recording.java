package gr.hua.stapps.android.noisepollutionapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recording {

    private Double AverageDecibels;
    private Double Latitude;
    private Double Longitude;
    private String currentDate;
    private String currentTime;

    public Recording(Double averageDecibels, Double latitude, Double longitude) {
        AverageDecibels = averageDecibels;
        Latitude = latitude;
        Longitude = longitude;
        currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public Double getAverageDecibels() {
        return AverageDecibels;
    }

    public void setAverageDecibels(Double averageDecibels) {
        AverageDecibels = averageDecibels;
    }

    public Double getLatitude() {
        return Latitude;
    }

    public void setLatitude(Double latitude) {
        Latitude = latitude;
    }

    public Double getLongitude() {
        return Longitude;
    }

    public void setLongitude(Double longitude) {
        Longitude = longitude;
    }

}
