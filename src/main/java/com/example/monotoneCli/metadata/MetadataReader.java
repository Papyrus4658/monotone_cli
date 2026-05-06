package com.example.monotoneCli.metadata;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import com.example.monotoneCli.model.Track;

public class MetadataReader {
    static {
        // jaudiotaggerの冗長なログを抑制
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    public static void read(Track track) {
        try {
            AudioFile audioFile = AudioFileIO.read(new File(track.getFilePath()));

            // 再生時間・フォーマット
            long durationSec = audioFile.getAudioHeader().getTrackLength();
            track.setDurationSeconds(durationSec);
            track.setFormat(audioFile.getAudioHeader().getFormat());

            // タグ（タイトル・アーティスト・アルバム）
            Tag tag = audioFile.getTag();

            if (tag != null) {
                String title = tag.getFirst(FieldKey.TITLE);
                String artist = tag.getFirst(FieldKey.ARTIST);
                String album = tag.getFirst(FieldKey.ALBUM);

                if (title != null && !title.isBlank()) {
                    track.setTitle(title);
                }

                if (artist != null && !artist.isBlank()) {
                    track.setArtist(artist);
                }

                if (album != null && !album.isBlank()) {
                    track.setAlbum(album);
                }
            }
        } catch (Exception e) {
            // メタデータ読み取り失敗時はデフォルト値のまま（無視）
        }
    }
}
