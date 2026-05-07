package com.example.monotoneCli;

import com.example.monotoneCli.cli.MusicPlayerCLI;

/**
 * Music Player CLI エントリポイント
 *
 * 前提条件:
 * - ffmpeg が PATH 上にインストールされていること
 * CachyOS: sudo pacman -S ffmpeg
 *
 * ビルドと実行:
 * ./mvnw package -q
 * java -jar target/music-player-1.0.0-SNAPSHOT.jar
 *
 * または直接:
 * ./mvnw compile exec:java -Dexec.mainClass=com.example.musicplayer.Main
 */
public class Main {
    public static void main(String[] args) {
        // ffmpeg の存在チェック
        if (!isFfmpegAvailable()) {
            System.err.println("エラー: ffmpeg が見つかりません。");
            System.err.println("インストールしてください: sudo pacman -S ffmpeg");
            System.exit(1);
        }

        MusicPlayerCLI cli = new MusicPlayerCLI();
        cli.run();
    }

    private static boolean isFfmpegAvailable() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}