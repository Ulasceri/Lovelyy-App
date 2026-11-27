"""Generates the item clips in clips.json (cat using one of its items) with fal and encodes them.

Usage:  FAL_KEY=... python make_clips.py out_dir [clip ...]
Output: ../../android/app/src/main/assets/cat/<clip>.mp4 (720x720 H.264, 24 fps, no audio), raw clips in out_dir.

The video model takes reference images, which is what keeps the cat and the item identical to the
rest of the app: Image 1 is always the cat's base pose, Image 2 the item picture on white.
Every clip is pinned to a start and an end frame so clips chain without a jump:
  "base"            the cat's base pose (the item comes into the frame and leaves again)
  "placed"          base pose with the item picture composited on the rug (see "place")
  "keys/<name>.jpg" a stored still (cat sitting or asleep inside a bed)
"""
import json, os, subprocess, sys
from concurrent.futures import ThreadPoolExecutor
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..")
sys.path.insert(0, os.path.join(HERE, "..", "cat-animation"))
import fal  # noqa: E402

SIDE = 1024  # square reference images: four of them are included in the clip price
spec = json.load(open(os.path.join(HERE, "clips.json")))
out = sys.argv[1]
names = sys.argv[2:] or list(spec["clips"])
os.makedirs(out, exist_ok=True)


def base():
    path = os.path.join(ROOT, "android/app/src/main/res/drawable-nodpi/cat_base.webp")
    return Image.open(path).convert("RGBA").resize((SIDE, SIDE), Image.LANCZOS)


def item(name):
    return Image.open(os.path.join(ROOT, "assets/items", name + ".png")).convert("RGBA")


def save(im, name):
    path = os.path.join(out, name + ".png")
    im.convert("RGB").save(path)
    return path


def item_ref(name):
    """The item alone on white, as the model's reference for its design."""
    bg = Image.new("RGBA", (SIDE, SIDE), "white")
    bg.alpha_composite(item(name).resize((900, 900), Image.LANCZOS), (62, 62))
    return save(bg, name + "_ref")


def placed(name, place):
    """Base pose with the item standing on the rug and a soft contact shadow under it."""
    w, cx, by = place["width"], place["center_x"], place["base_y"]
    it = item(name)
    it = it.crop(it.getbbox())
    it = it.resize((w, int(it.height * w / it.width)), Image.LANCZOS)
    im = base()
    shadow = Image.new("RGBA", im.size, (0, 0, 0, 0))
    sw = int(w * 1.1)
    ImageDraw.Draw(shadow).ellipse(
        (cx - sw // 2, by - int(w * 0.09), cx + sw // 2, by + int(w * 0.07)), fill=(74, 59, 53, 90))
    im.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(10)))
    im.alpha_composite(it, (cx - w // 2, by - it.height))
    return save(im, name + "_placed")


def frame(kind, name, clip):
    if kind == "base":
        return save(base(), "base")
    if kind == "placed":
        return placed(clip["item"], clip["place"])
    return os.path.join(HERE, kind)


def encode(raw, name):
    """Same output as cat-animation/encode.sh: the last frame repeats the first, so it is dropped."""
    frames = int(subprocess.check_output(
        ["ffprobe", "-v", "error", "-count_frames", "-select_streams", "v:0",
         "-show_entries", "stream=nb_read_frames", "-of", "csv=p=0", raw]).split()[0])
    dst = os.path.join(ROOT, "android/app/src/main/assets/cat", name + ".mp4")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    subprocess.check_call(
        ["ffmpeg", "-v", "error", "-y", "-i", raw, "-an", "-vf",
         f"trim=end_frame={frames - 1},setpts=N/24/TB,scale=720:720:flags=lanczos,format=yuv420p",
         "-r", "24", "-c:v", "libx264", "-profile:v", "main", "-preset", "slow", "-crf", "21",
         "-g", "24", "-movflags", "+faststart", dst])


def one(name):
    clip = spec["clips"][name]
    refs = [fal.upload(save(base(), "base"))]
    if "item" in clip:
        refs.append(fal.upload(item_ref(clip["item"])))
    start = fal.upload(frame(clip["start"], name, clip))
    end = start if clip["end"] == clip["start"] else fal.upload(frame(clip["end"], name, clip))
    res = fal.run(spec["model"], {**spec["args"], "prompt": clip["prompt"],
                                  "reference_image_urls": refs, "image_url": start, "end_image_url": end})
    raw = fal.download(res["video"]["url"], os.path.join(out, name + ".mp4"))
    encode(raw, name)
    print("ok", name, "seed", res.get("seed"), flush=True)


with ThreadPoolExecutor(6) as ex:
    for f in [ex.submit(one, n) for n in names]:
        try:
            f.result()
        except Exception as e:
            print("FAILED", repr(e), flush=True)
