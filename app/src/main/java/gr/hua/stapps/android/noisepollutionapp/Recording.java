package gr.hua.stapps.android.noisepollutionapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recording {

    private Double decibels;
    private Double latitude;
    private Double longitude;
    private String date;
    private String time;
    private Integer gender;
    private Integer age;
    private Integer anthropogenic;
    private Integer natural;
    private Integer technological;
    private Integer perception;

    public Recording() {
        decibels = 0.0;
        latitude = 0.0;
        longitude = 0.0;
        date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        gender = 2;
        age = 0;
        anthropogenic = 0;
        natural = 0;
        technological = 0;
        perception = 0;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Double getDecibels() {
        return decibels;
    }

    public void setDecibels(Double decibels) {
        this.decibels = decibels;
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
