package com.example.monotoneCli.cli;

import com.example.monotoneCli.metadata.MetadataReader;
import com.example.monotoneCli.model.Playlist;
import com.example.monotoneCli.model.Track;
import com.example.monotoneCli.player.AudioPlayer;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * コマンドラインミュージックプレイヤーの UI 層。
 * ユーザー入力を解釈して AudioPlayer / Playlist を操作する。
 */
public class MusicPlayerCLI {
    // 対応拡張子
    private static final List<String> SUPPORTED_EXT = List.of(
            "flac", "m4a", "mp3", "ogg", "wav", "aiff", "aif", "opus", "wma", "alac");

    private final AudioPlayer player = new AudioPlayer();
    private final Playlist playlist = new Playlist();
    private final Scanner scanner = new Scanner(System.in);

    // ----------------------------------------------------------------
    // エントリポイント
    // ----------------------------------------------------------------

    public void run() {
        printBanner();
        printHelp();

        // 曲が自然に終わったら自動で次の曲へ
        player.setOnFinish(() -> {
            Track next = playlist.next();
            
            if (next != null) {
                System.out.println("\n>>> 次の曲: " + next.getDisplayTitle());
                player.play(next);
                printStatus();
            } else {
                System.out.println("\n>>> プレイリストの最後まで再生しました。");
            }
        });

        // メインループ
        while (true) {
            printPrompt();
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String arg = parts.length > 1 ? parts[1].trim() : "";

            switch (cmd) {
                case "play", "p" -> cmdPlay(arg);
                case "pause", "pa" -> cmdPause();
                case "stop", "s" -> cmdStop();
                case "next", "n" -> cmdNext();
                case "prev", "b" -> cmdPrev();
                case "jump", "j" -> cmdJump(arg);
                case "add", "a" -> cmdAdd(arg);
                case "list", "ls" -> cmdList();
                case "clear", "cl" -> cmdClear();
                case "volume", "vol" -> cmdVolume(arg);
                case "info", "i" -> cmdInfo();
                case "status", "st" -> printStatus();
                case "help", "h" -> printHelp();
                case "quit", "q", "exit" -> {
                    player.stop();
                    System.out.println("さようなら！");
                    return;
                }

                default -> System.out.println(
                        "不明なコマンド: " + cmd + "  (help または h でコマンド一覧を表示)");
            }
        }
    }

    // ----------------------------------------------------------------
    // コマンド実装
    // ----------------------------------------------------------------

    /** play [ファイル/フォルダパス] */
    private void cmdPlay(String arg) {
        if (!arg.isEmpty()) {
            // パスが指定されたら先に add する
            cmdAdd(arg);
        }

        if (playlist.isEmpty()) {
            System.out.println("プレイリストが空です。  add <ファイルまたはフォルダ> で追加してください。");
            return;
        }

        // ポーズ中なら再開
        if (player.isPaused()) {
            player.togglePause();
            System.out.println("▶ 再開: " + player.getCurrentTrack().getDisplayTitle());

            return;
        }

        Track track = playlist.current();

        if (track == null) {
            track = playlist.next();
        }

        if (track == null) {
            return;
        }

        System.out.println(
                "▶ 再生: " + track.getDisplayTitle() + "  [" + track.getDurationString() + "]");
        player.play(track);
    }

    /** pause / pa — ポーズ切り替え */
    private void cmdPause() {
        if (player.isStopped()) {
            System.out.println("再生中ではありません。");
            return;
        }

        player.togglePause();

        if (player.isPaused()) {
            System.out.println("⏸ ポーズ");
        } else {
            System.out.println("▶ 再開");
        }
    }

    /** stop / s */
    private void cmdStop() {
        player.stop();
        System.out.println("■ 停止しました。");
    }

    /** next / n */
    private void cmdNext() {
        Track next = playlist.next();

        if (next == null) {
            System.out.println("プレイリストが空です。");
            return;
        }

        System.out.println(
                "⏭ 次の曲: " + next.getDisplayTitle() + "  [" + next.getDurationString() + "]");
        player.play(next);
    }

    /** prev / b */
    private void cmdPrev() {
        Track prev = playlist.prev();

        if (prev == null) {
            System.out.println("プレイリストが空です。");
            return;
        }

        System.out.println(
                "⏮ 前の曲: " + prev.getDisplayTitle() + "  [" + prev.getDurationString() + "]");
        player.play(prev);
    }

    /** jump <番号> / j <番号> — 指定番号の曲へジャンプ */
    private void cmdJump(String arg) {
        if (arg.isEmpty()) {
            System.out.println("使い方: jump <番号>  (list で番号を確認)");
            return;
        }

        try {
            int idx = Integer.parseInt(arg) - 1; // 表示は 1 始まり
            Track track = playlist.jumpTo(idx);

            if (track == null) {
                System.out.println("番号 " + arg + " は存在しません。");
                return;
            }

            System.out.println(
                    "▶ ジャンプ: " + track.getDisplayTitle() + "  [" + track.getDurationString() + "]");
            player.play(track);
        } catch (NumberFormatException e) {
            System.out.println("番号を整数で指定してください。  例: jump 3");
        }
    }

    /** add <ファイルまたはフォルダ> / a <...> */
    private void cmdAdd(String arg) {
        if (arg.isEmpty()) {
            System.out.println("使い方: add <ファイルまたはフォルダのパス>");
            return;
        }

        File f = new File(arg);

        if (!f.exists()) {
            System.out.println("見つかりません: " + arg);
            return;
        }

        int before = playlist.size();
        addPath(f);
        int added = playlist.size() - before;
        System.out.println(added + " 曲を追加しました。  (合計: " + playlist.size() + " 曲)");
    }

    /** list / ls — プレイリスト表示 */
    private void cmdList() {
        if (playlist.isEmpty()) {
            System.out.println("プレイリストは空です。");
            return;
        }

        System.out.println();
        System.out.println(
                "┌─────────────────────────────────────────────────────┐");
        System.out.printf(
                "│ %-4s %-38s %6s │%n", "No.", "タイトル", "時間");
        System.out.println(
                "├─────────────────────────────────────────────────────┤");

        var tracks = playlist.getTracks();
        int cur = playlist.getCurrentIndex();

        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            String marker = (i == cur) ? "▶" : " ";
            String title = truncate(t.getDisplayTitle(), 38);
            System.out.printf(
                    "│%s %-3d %-38s %6s │%n", marker, i + 1, title, t.getDurationString());
        }

        System.out.println(
                "└─────────────────────────────────────────────────────┘");
    }

    /** clear / cl — プレイリストをクリア */
    private void cmdClear() {
        player.stop();
        playlist.clear();
        System.out.println("プレイリストをクリアしました。");
    }

    /** volume <0-100> / vol <0-100> */
    private void cmdVolume(String arg) {
        if (arg.isEmpty()) {
            System.out.printf("現在の音量: %d%%%n", Math.round(player.getVolume() * 100));
            return;
        }

        try {
            int pct = Integer.parseInt(arg);

            if (pct < 0 || pct > 100) {
                throw new NumberFormatException();
            }

            player.setVolume(pct / 100.0f);
            System.out.printf("🔊 音量: %d%%%n", pct);
        } catch (NumberFormatException e) {
            System.out.println("0 〜 100 の整数で指定してください。  例: volume 80");
        }
    }

    /** info / i — 現在のトラック詳細 */
    private void cmdInfo() {
        Track t = player.getCurrentTrack();

        if (t == null || player.isStopped()) {
            System.out.println("再生中のトラックがありません。");
            return;
        }

        System.out.println();
        System.out.println("  タイトル : " + t.getTitle());
        System.out.println("  アーティスト: " + t.getArtist());
        System.out.println("  アルバム : " + t.getAlbum());
        System.out.println("  時間    : " + t.getDurationString());
        System.out.println("  フォーマット: " + (t.getFormat() != null ? t.getFormat() : "不明"));
        System.out.println("  ファイル : " + t.getFilePath());
    }

    // ----------------------------------------------------------------
    // ヘルパー
    // ----------------------------------------------------------------

    /** ファイル/ディレクトリを再帰的にたどってトラックを追加 */
    private void addPath(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children == null) {
                return;
            }

            Arrays.sort(children, Comparator.comparing(File::getName));

            for (File child : children) {
                addPath(child);
            }
        } else if (isSupportedFile(f)) {
            Track track = new Track(f.getAbsolutePath());
            MetadataReader.read(track); // メタデータ取得（失敗しても続行）
            playlist.add(track);
        }
    }

    private boolean isSupportedFile(File f) {
        String name = f.getName().toLowerCase();
        return SUPPORTED_EXT.stream().anyMatch(ext -> name.endsWith("." + ext));
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }

        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }

    /** プロンプト（ステータス付き） */
    private void printPrompt() {
        if (!player.isStopped()) {
            Track t = player.getCurrentTrack();
            long elapsed = player.getElapsedSeconds();
            long total = t != null ? t.getDurationSeconds() : 0;
            String state = player.isPaused() ? "⏸" : "▶";
            System.out.printf(
                    "%s [%s / %s] vol:%d%%  > ",
                    state,
                    formatTime(elapsed),
                    formatTime(total),
                    Math.round(player.getVolume() * 100));
        } else {
            System.out.print("■ > ");
        }
    }

    /** 現在のステータスを1行で表示 */
    private void printStatus() {
        if (player.isStopped()) {
            System.out.println("■ 停止中");
            return;
        }

        Track t = player.getCurrentTrack();
        long elapsed = player.getElapsedSeconds();
        String state = player.isPaused() ? "⏸ ポーズ" : "▶ 再生中";
        System.out.printf(
                "%s: %s  [%s / %s]  vol:%d%%%n",
                state,
                t != null ? t.getDisplayTitle() : "?",
                formatTime(elapsed),
                t != null ? t.getDurationString() : "--:--",
                Math.round(player.getVolume() * 100));
    }

    private String formatTime(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private void printBanner() {
        System.out.println("""
                ╔═══════════════════════════════════╗
                ║     🎵  Music Player CLI  🎵      ║
                ║   FLAC / M4A / MP3 / WAV など     ║
                ╚═══════════════════════════════════╝
                """);
    }

    private void printHelp() {
        System.out.println("""
                コマンド一覧:
                  add  <パス>    音楽ファイルまたはフォルダを追加  (エイリアス: a)
                  play [パス]    再生開始（ポーズ中なら再開）      (エイリアス: p)
                  pause          一時停止 / 再開                   (エイリアス: pa)
                  stop           停止                              (エイリアス: s)
                  next           次の曲                            (エイリアス: n)
                  prev           前の曲                            (エイリアス: b)
                  jump <番号>    指定番号の曲へジャンプ            (エイリアス: j)
                  list           プレイリスト表示                  (エイリアス: ls)
                  clear          プレイリストをクリア              (エイリアス: cl)
                  volume <0-100> 音量設定                          (エイリアス: vol)
                  info           現在の曲の詳細情報                (エイリアス: i)
                  status         再生状態を表示                    (エイリアス: st)
                  help           このヘルプを表示                  (エイリアス: h)
                  quit           終了                              (エイリアス: q)
                """);
    }
}