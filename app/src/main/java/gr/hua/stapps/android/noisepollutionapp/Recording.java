package gr.hua.stapps.android.noisepollutionapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recording {

    private Double averageDecibels;
    private Double latitude;
    private Double longitude;
    private String currentDate;
    private String currentTime;
    private Integer gender;
    private Integer age;
    private Integer anthropogenic;
    private Integer natural;
    private Integer technological;
    private Integer perception;

    public Recording(Double averageDecibels, Double latitude, Double longitude, Integer gender, Integer age, Integer anthropogenic, Integer natural, Integer technological, Integer perception) {
        this.averageDecibels = averageDecibels;
        this.latitude = latitude;
        this.longitude = longitude;
        //currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        this.gender = gender;
        this.age = age;
        this.anthropogenic = anthropogenic;
        this.natural = natural;
        this.technological = technological;
        this.perception = perception;
    }
    public Recording() {
        averageDecibels = 0.0;
        latitude = 0.0;
        longitude = 0.0;
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        gender = 2;
        age = 0;
        anthropogenic = 0;
        natural = 0;
        technological = 0;
        perception = 0;
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
        return averageDecibels;
    }

    public void setAverageDecibels(Double averageDecibels) {
        this.averageDecibels = averageDecibels;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Integer getAnthropogenic() {
        return anthropogenic;
    }

    public void setAnthropogenic(Integer anthropogenic) {
        this.anthropogenic = anthropogenic;
    }

    public Integer getNatural() {
        return natural;
    }

    public void setNatural(Integer natural) {
        this.natural = natural;
    }

    public Integer getTechnological() {
        return technological;
    }

    public void setTechnological(Integer technological) {
        this.technological = technological;
    }

    public Integer getPerception() {
        return perception;
    }

    public void setPerception(Integer perception) {
        this.perception = perception;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
