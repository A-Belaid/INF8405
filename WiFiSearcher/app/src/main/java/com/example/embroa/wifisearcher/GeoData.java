package com.example.embroa.wifisearcher;

/**
 * Created by utilisateur on 2018-01-29.
 */

public class GeoData {
    String country;
    String city;
    String countryCode;
    double latitude;
    double longitude;
    String region;
    String timezone;
    String isp;

    public GeoData() {}

    public String getCountry() {return country;}

    public String getCity() {return city;}

    public String getCountryCode() {return countryCode;}

    public double getLatitude() {return latitude;}

    public String getLatitudeStr() {return Double.toString(latitude);}

    public double getLongitude() {return longitude;}

    public String getLongitudeStr() {return Double.toString(longitude);}

    public String getRegion() {return region;}

    public String getTimezone() {return timezone;}

    public String getISP() {return isp;}

    public void setCountry(String country) {this.country = country;}

    public void setCity(String city) {this.city = city;}

    public void setCountryCode(String code) {countryCode = code;}

    public void setLatitude(double lat) {latitude = lat;}

    public void setLongitude(double lon) {longitude = lon;}

    public void setRegion(String region) {this.region = region;}

    public void setTimezone(String tmz){timezone = tmz;}

    public void setIsp(String isp) {this.isp = isp;}
}
