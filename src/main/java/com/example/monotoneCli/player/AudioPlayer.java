package com.example.monotoneCli.player;

import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.example.monotoneCli.model.Track;

public class AudioPlayer {
    // 出力フォーマット（44100Hz、16bit、ステレオ、リトルエンディアン）
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNELS = 2;
    private static final int SAMPLE_SIZE_BIT = 16;
    private static final int CHUNK_SIZE = 8192;

    // デコーダ → 再生スレッド間のキュー（終端マーカー用センチネル）
    private static final byte[] SENTINEL = new byte[0];
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>(256);

    private SourceDataLine line;
    private Process ffmpegProcess;
    private Thread decoderThread;
    private Thread playbackThread;

    // 状態フラグ（volatileで可視性確保）
    private volatile boolean stopped = true;
    private volatile boolean paused = false;
    private volatile float volume = 1.0f; // 0.0 ~ 1.0

    // 経過時間計測
    private long playStartTime;
    private long pausedTotal;
    private long pauseBegin;

    private Track currentTrack;
    private Runnable onFinish;

    // ----------------------------------------------------------------
    // 公開 API
    // ----------------------------------------------------------------

    public synchronized void play(Track track) {
        stop();
        currentTrack = track;
        audioQueue.clear();

        try {
            // --- FFmpeg プロセス起動 ---
            // -i <入力> : 入力ファイル
            // -vn : 映像ストリームを無視
            // -f s16le : 出力形式 = 符号付き 16bit リトルエンディアン PCM
            // -ar 44100 : サンプルレート
            // -ac 2 : チャンネル数
            // pipe:1 : stdout に出力
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i",
                    track.getFilePath(),
                    "-vn",
                    "-f",
                    "s16le",
                    "-ar", String.valueOf(SAMPLE_RATE),
                    "-ac", String.valueOf(CHANNELS),
                    "pipe:1");

            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            ffmpegProcess = pb.start();

            // --- javax.sound.sampled 初期化 ---
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    SAMPLE_RATE,
                    SAMPLE_SIZE_BIT,
                    CHANNELS,
                    CHANNELS * (SAMPLE_SIZE_BIT / 8),
                    SAMPLE_RATE,
                    false // little endian
            );

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, CHUNK_SIZE * 4);
            applyVolumeToLine();
            line.start();

            // 状態リセット
            stopped = false;
            paused = false;
            playStartTime = System.currentTimeMillis();
            pausedTotal = 0;

            // デコーダースレッド
            InputStream ffOut = ffmpegProcess.getInputStream();

            decoderThread = new Thread(() -> {
                try {
                    byte[] buf = new byte[CHUNK_SIZE];
                    int n;
                    while (!stopped && (n = ffOut.read(buf)) != -1) {
                        audioQueue.put(Arrays.copyOf(buf, n));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception ignored) {
                } finally {
                    try {
                        audioQueue.put(SENTINEL);
                    } catch (InterruptedException ignored) {
                    }
                }
            }, "decoder");

            // 再生スレッド]
            playbackThread = new Thread(() -> {
                try {
                    while (!stopped) {
                        if (paused) {
                            Thread.sleep(20);
                            continue;
                        }

                        byte[] chunk = audioQueue.poll(200, TimeUnit.MILLISECONDS);
                        if (chunk == null) {
                            continue;
                        }

                        if (chunk == SENTINEL) {
                            break;
                        }

                        line.write(chunk, 0, chunk.length);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 停止コマンドでなければバッファを最後まで書き出す
                    if (!stopped) {
                        line.drain();
                    }

                    line.stop();
                    line.close();
                    stopped = true;

                    // 曲が自然に終わった場合のコールバック
                    if (!Thread.currentThread().isInterrupted() && onFinish != null) {
                        onFinish.run();
                    }
                }
            }, "playback");

            decoderThread.setDaemon(true);
            playbackThread.setDaemon(true);
            decoderThread.start();
            playbackThread.start();
        } catch (LineUnavailableException e) {
            System.err.println("[エラー] オーディオデバイスを開けませんでした: " + e.getMessage());
            stop();
        } catch (Exception e) {
            System.err.println("[エラー] 再生開始に失敗しました: " + e.getMessage());
            stop();
        }
    }

    /**
     * 一時停止/再生を切り替える
     */
    public void togglePause() {
        if (stopped) {
            return;
        }

        paused = !paused;

        if (paused) {
            pauseBegin = System.currentTimeMillis();
        } else {
            pausedTotal += System.currentTimeMillis() - pauseBegin;
        }
    }

    /**
     * 再生を停止してリソースを解放する
     */
    public synchronized void stop() {
        stopped = true;
        paused = false;

        if (ffmpegProcess != null) {
            ffmpegProcess.destroyForcibly();
            ffmpegProcess = null;
        }

        audioQueue.clear();

        if (decoderThread != null) {
            decoderThread.interrupt();
        }

        if (playbackThread != null) {
            playbackThread.interrupt();
        }
    }

    public void setVolume(float v) {
        volume = Math.max(0.0f, Math.min(1.0f, v));
        applyVolumeToLine();
    }

    // ----------------------------------------------------------------
    // 内部ヘルパー
    // ----------------------------------------------------------------

    /**
     * SourceDataLine に現在の volume 値を適用する
     */
    private void applyVolumeToLine() {
        if (line == null || !line.isOpen()) {
            return;
        }

        try {
            FloatControl fc = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

            if (volume <= 0.0f) {
                fc.setValue(fc.getMinimum());
            } else {
                // 線形比率 → dB 変換 (20 * log10(v))
                float dB = (float) (20.0 * Math.log10(volume));
                fc.setValue(Math.max(fc.getMinimum(), Math.min(fc.getMaximum(), dB)));
            }
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // MASTER_GAIN 非対応のデバイスでは無限
        }
    }

    // ----------------------------------------------------------------
    // 状態取得
    // ----------------------------------------------------------------

    /** 再生中かどうか */
    public boolean isPlaying() {
        return !stopped && !paused;
    }

    /** ポーズ中かどうか */
    public boolean isPaused() {
        return !stopped && paused;
    }

    /** 停止中かどうか */
    public boolean isStopped() {
        return stopped;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public float getVolume() {
        return volume;
    }

    /** 現在の再生経過秒数（ポーズ時間を除く） */
    public long getElapsedSeconds() {
        if (stopped) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - playStartTime - pausedTotal;
        if (paused) {
            elapsed -= (now - pauseBegin);
        }

        return Math.max(0, elapsed / 1000);
    }

    /** 再生終了時に呼び出されるコールバックを設定する */
    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }
}
