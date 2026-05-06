# 🎵 Music Player CLI

FLAC・M4A・MP3・WAV などを再生できるコマンドラインミュージックプレイヤー。  
Java 26 + FFmpeg + jaudiotagger で実装。

---

## 前提条件

| 必要なもの | インストール方法 (CachyOS/Arch) |
|-----------|-------------------------------|
| JDK 26+   | `sudo pacman -S jdk-openjdk`  |
| FFmpeg    | `sudo pacman -S ffmpeg`       |
| Maven     | `./mvnw`（ラッパー同梱）     |

---

## ビルド

```bash
./mvnw package -q
```

`target/music-player-1.0.0-SNAPSHOT.jar` が生成されます。

---

## 起動

```bash
java -jar target/music-player-1.0.0-SNAPSHOT.jar
```

---

## コマンド一覧

| コマンド          | エイリアス | 説明                         |
|------------------|-----------|------------------------------|
| `add <パス>`     | `a`       | ファイルまたはフォルダを追加  |
| `play [パス]`    | `p`       | 再生開始（ポーズ中なら再開） |
| `pause`          | `pa`      | 一時停止 / 再開              |
| `stop`           | `s`       | 停止                         |
| `next`           | `n`       | 次の曲                       |
| `prev`           | `b`       | 前の曲                       |
| `jump <番号>`    | `j`       | 指定番号の曲へジャンプ       |
| `list`           | `ls`      | プレイリスト表示             |
| `clear`          | `cl`      | プレイリストをクリア         |
| `volume <0-100>` | `vol`     | 音量設定                     |
| `info`           | `i`       | 現在の曲の詳細情報           |
| `status`         | `st`      | 再生状態を表示               |
| `help`           | `h`       | ヘルプ表示                   |
| `quit`           | `q`       | 終了                         |

---

## 使用例

```
■ > add ~/Music/albums/MyAlbum
10 曲を追加しました。  (合計: 10 曲)

■ > play
▶ 再生: Artist - Track01  [03:42]

▶ [00:15 / 03:42] vol:100%  > pause
⏸ ポーズ

⏸ [00:15 / 03:42] vol:100%  > volume 70
🔊 音量: 70%

▶ [00:15 / 03:42] vol:70%  > list
┌─────────────────────────────────────────────────────┐
│ No.  タイトル                                  時間 │
├─────────────────────────────────────────────────────┤
│▶ 1   Artist - Track01                         03:42 │
│  2   Artist - Track02                         04:10 │
│  ...
```

---

## アーキテクチャ

```
Main
 └── MusicPlayerCLI         ← ユーザー入力・表示
      ├── AudioPlayer        ← 再生エンジン
      │    ├── decoderThread  ← FFmpeg stdout → BlockingQueue
      │    └── playbackThread ← BlockingQueue → SourceDataLine
      ├── Playlist           ← 曲リスト管理
      └── MetadataReader     ← jaudiotagger でタグ読み取り
```

### 対応フォーマット（FFmpegが対応していれば再生可能）

- **ロスレス**: FLAC, WAV, AIFF, ALAC (M4A)
- **非可逆**: MP3, AAC (M4A), OGG Vorbis, Opus, WMA

---

## 今後の拡張案

- [ ] シャッフル再生 (`shuffle`)
- [ ] リピートモード (`repeat`)
- [ ] シーク機能 (`seek <秒>`)
- [ ] プレイリストの保存・読み込み (`save` / `load`)
- [ ] TUI (テキストUI) の追加 (Lanterna ライブラリなど)
- [ ] JavaFX GUI 版への移行