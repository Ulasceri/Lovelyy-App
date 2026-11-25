#!/usr/bin/env bash
# Turns the raw fal clips into the app's videos: 720x720 H.264, 24 fps, no audio.
# The last frame of every raw clip repeats the first (start = end keyframe), so it is
# dropped: looping or chaining clips then never shows the same frame twice.
# Also exports the poster and widget pictures.
# Usage: tools/cat-animation/encode.sh RAW_DIR
set -euo pipefail
RAW="$1"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/android/app/src/main/assets/cat"
RES="$ROOT/android/app/src/main/res/drawable-nodpi"
mkdir -p "$OUT" "$RES"
for f in "$RAW"/*.mp4; do
  n=$(basename "$f" .mp4)
  frames=$(ffprobe -v error -count_frames -select_streams v:0 -show_entries stream=nb_read_frames -of csv=p=0 "$f")
  ffmpeg -v error -y -i "$f" -an \
    -vf "trim=end_frame=$((frames - 1)),setpts=N/24/TB,scale=720:720:flags=lanczos,format=yuv420p" \
    -r 24 -c:v libx264 -profile:v main -preset slow -crf 21 -g 24 -movflags +faststart "$OUT/$n.mp4"
  echo "$n: $(du -h "$OUT/$n.mp4" | cut -f1)"
done

# still(name, clip, second): one frame as a WebP picture.
still() {
  ffmpeg -v error -y -ss "$3" -i "$RAW/$2.mp4" -frames:v 1 -vf "scale=$4:$4:flags=lanczos" -c:v libwebp -quality 82 "$RES/$1.webp"
}
still cat_base idle 0 720
still cat_sleep sleeping 0 720
still cat_w_idle idle 0 360
still cat_w_happy happy 2.6 360
still cat_w_hungry hungry 2.7 360
still cat_w_thirsty thirsty 0.6 360
still cat_w_sleepy sleepy 1.8 360
still cat_w_sleeping sleeping 0 360
still cat_w_love wants_love 2.4 360
