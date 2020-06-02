package ru.Artak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Strava {
    private final int ID = 46301;
    private final String SECRET = "671832cb5403c96630b8a3facc66d9953b06fd1a";
    private String refresh_token;
    private String access_token;
    private Athlete athlete;

    public Strava() {
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

    @Override
    public String toString() {
        return "Strava{" +
                "athlete=" + athlete +
                '}';
    }
}