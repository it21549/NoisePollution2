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
    private Integer Gender;
    private Integer Age;
    private Integer Anthropogenic;
    private Integer Natural;
    private Integer Technological;
    private Integer Perception;

    public Recording(Double averageDecibels, Double latitude, Double longitude, Integer gender, Integer age, Integer anthropogenic, Integer natural, Integer technological, Integer perception) {
        AverageDecibels = averageDecibels;
        Latitude = latitude;
        Longitude = longitude;
        currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        Gender = gender;
        Age = age;
        Anthropogenic = anthropogenic;
        Natural = natural;
        Technological = technological;
        Perception = perception;
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

    public Integer getGender() {
        return Gender;
    }

    public void setGender(Integer gender) {
        Gender = gender;
    }

    public Integer getAnthropogenic() {
        return Anthropogenic;
    }

    public void setAnthropogenic(Integer anthropogenic) {
        Anthropogenic = anthropogenic;
    }

    public Integer getNatural() {
        return Natural;
    }

    public void setNatural(Integer natural) {
        Natural = natural;
    }

    public Integer getTechnological() {
        return Technological;
    }

    public void setTechnological(Integer technological) {
        Technological = technological;
    }

    public Integer getPerception() {
        return Perception;
    }

    public void setPerception(Integer perception) {
        Perception = perception;
    }

    public Integer getAge() {
        return Age;
    }

    public void setAge(Integer age) {
        Age = age;
    }
}
