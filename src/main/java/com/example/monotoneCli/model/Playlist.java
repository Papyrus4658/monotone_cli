package com.example.monotoneCli.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 曲のリストと現在位置を管理するプレイリスト
 */
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

    /** 現在のトラックを返す（なければ null） */
    public Track current() {
        if (currentIndex < 0 || currentIndex >= tracks.size()) {
            return null;
        }

        return tracks.get(currentIndex);
    }

    /** 次のトラックに進んで返す（ループ） */
    public Track next() {
        if (tracks.isEmpty()) {
            return null;
        }

        currentIndex = (currentIndex + 1) % tracks.size();

        return current();
    }

    /** 前のトラックに戻って返す（ループ） */
    public Track prev() {
        if (tracks.isEmpty()) {
            return null;
        }

        currentIndex = (currentIndex - 1 + tracks.size()) % tracks.size();

        return current();
    }

    /** インデックスを指定して移動 */
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

    public int getCurrentIndex() {
        return currentIndex;
    }

    public List<Track> getTracks() {
        return Collections.unmodifiableList(tracks);
    }
}