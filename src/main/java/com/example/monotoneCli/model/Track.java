package com.example.monotoneCli.model;

import java.io.File;

public class Track {
    private final String filePath;
    private String title;
    private String artist;
    private String album;
    private long durationSeconds;
    private String format;

    public Track(String filePath) {
        this.filePath = filePath;
        String fileName = new File(filePath).getName();
        int dot = fileName.lastIndexOf('.');
        this.title = (dot > 0) ? fileName.substring(0, dot) : fileName;
        this.artist = "不明";
        this.album = "不明";
    }

    public String getDisplayTitle() {
        if (!"不明".equals(artist)) {
            return artist + " - " + title;
        }

        return title;
    }

    /** 再生時間を mm:ss 形式で返す */
    public String getDurationString() {
        if (durationSeconds <= 0) {
            return "--:--";
        }

        long m = durationSeconds / 60;
        long s = durationSeconds % 60;

        return String.format("%02d:%02d", m, s);
    }

    public String getFilePath() {
        return filePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
