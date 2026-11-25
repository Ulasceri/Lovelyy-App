"""Generates every cat clip in clips.json with fal (image-to-video, start + end keyframe).

Usage:  FAL_KEY=... python generate.py keys.json out_dir [clip ...]
keys.json maps keyframe names (base, sleep, food, water, toy) to uploaded image URLs.
Every clip starts and ends on a keyframe, so loops are seamless and clips can be
chained at their boundaries without a visible jump.
"""
import json, os, sys
from concurrent.futures import ThreadPoolExecutor
import fal

spec = json.load(open(os.path.join(os.path.dirname(__file__), "clips.json")))
keys = json.load(open(sys.argv[1]))
out = sys.argv[2]
names = sys.argv[3:] or list(spec["clips"])


def one(name):
    c = spec["clips"][name]
    args = {
        "prompt": c["prompt"] + spec["suffix"],
        "image_url": keys[c["start"]],
        "tail_image_url": keys[c["end"]],
        "duration": c.get("duration", "5"),
        "negative_prompt": spec["negative_prompt"],
    }
    res = fal.run(spec["model"], args)
    url = next(fal.find_urls(res))
    path = fal.download(url, os.path.join(out, f"{name}.mp4"))
    print("ok", name, flush=True)
    return path


with ThreadPoolExecutor(8) as ex:
    for f in [ex.submit(one, n) for n in names]:
        try:
            f.result()
        except Exception as e:
            print("FAILED", e, flush=True)
