package com.example.monotoneCli.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Playlist {
    private final List<Track> tracks = new ArrayList<>();
    private int currentIndex = -1;

    public void add(Track track) {
        tracks.add(track);

        if (currentIndex < 0) {
            currentIndex = 0;
        }
    }

    public void clear() {
        tracks.clear();
        currentIndex = -1;
    }

    public Track current() {
        if (currentIndex < 0 || currentIndex >= tracks.size()) {
            return null;
        }

        return tracks.get(currentIndex);
    }

    public Track next() {
        if (tracks.isEmpty()) {
            return null;
        }

        currentIndex = (currentIndex + 1) % tracks.size();
        return current();
    }

    public Track prev() {
        if (tracks.isEmpty()) {
            return null;
        }

        currentIndex = (currentIndex - 1 + tracks.size()) % tracks.size();
        return current();
    }

    public Track jumpTo(int index) {
        if (index < 0 || index >= tracks.size()) {
            return null;
        }

        currentIndex = index;
        return current();
    }

    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    public int size() {
        return tracks.size();
    }

    public List<Track> getTracks() {
        return Collections.unmodifiableList(tracks);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
