"""Generates Mırmır's items (bowls, toys, beds) with fal.ai and cuts them into transparent PNGs.

Why sheets: items rendered together in ONE image share light, material and scale, so the set looks
like one family. Rendering each item alone (or text-to-image without the cat as a reference) gives a
different design language per item. The cat picture is always the first reference image.

    export FAL_KEY=...                                  # never commit it
    python make_items.py generate bowls out/            # one sheet from sheets.json -> out/bowls.png
    python make_items.py cut bowls out/ ../../assets/items/   # split it into one PNG per item

Needs Pillow. Uses the queue client in ../cat-animation/fal.py.
"""
import json, os, sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "cat-animation"))
import fal  # noqa: E402

HERE = os.path.dirname(__file__)
MODEL = "bytedance/seedream/v5.5/edit"  # the only model of the four tried that returns real alpha
SIDE = 686      # every item PNG is a square of this size, item centered, scale kept
ALPHA_MIN = 12  # alpha below this counts as empty when finding an item's bounding box


def load(name):
    with open(os.path.join(HERE, "sheets.json"), encoding="utf-8") as f:
        return json.load(f)[name]


def generate(name, out_dir, extra_refs=()):
    """extra_refs: local images (an earlier sheet, a sketch sheet) that pin shapes or scale."""
    sheet = load(name)
    listing = " ".join(f"({i + 1}) {d}." for i, d in enumerate(sheet["items"].values()))
    prompt = (
        "Image 1 is the style reference (a soft 3D clay cat). Any further images show items of the same "
        "set for material, color and scale. "
        f"Create a sheet of {len(sheet['items'])} cat items in a grid of {sheet['cols']} columns by "
        f"{sheet['rows']} rows. Left to right, top row first: {listing} "
        "Every object is a 3D object in exactly the same soft matte clay material, lighting and pastel "
        "peach, mint, cream palette as image 1, all seen from the same slightly top-down three-quarter "
        "angle and at the same scale. Clear empty space between objects so none touch. Isolated objects "
        "on a fully transparent background: no floor, no rug, no backdrop, no cast shadows, no cat, "
        "no text, no letters, no numbers, no grid lines."
    )
    refs = [fal.upload(os.path.join(HERE, "style-ref.png"))] + [fal.upload(p) for p in extra_refs]
    res = fal.run(MODEL, {
        "prompt": prompt, "image_urls": refs, "num_images": 1,
        "image_size": {"width": sheet["width"], "height": sheet["height"]},
        "background": "transparent", "output_format": "png",
    })
    path = os.path.join(out_dir, name + ".png")
    fal.download(next(fal.find_urls(res)), path)
    print(path)


def cut(name, out_dir, dest):
    from PIL import Image
    sheet = load(name)
    im = Image.open(os.path.join(out_dir, name + ".png")).convert("RGBA")
    cw, ch = im.width // sheet["cols"], im.height // sheet["rows"]
    os.makedirs(dest, exist_ok=True)
    for i, item in enumerate(sheet["items"]):
        x, y = (i % sheet["cols"]) * cw, (i // sheet["cols"]) * ch
        cell = im.crop((x, y, x + cw, y + ch))
        box = cell.getchannel("A").point(lambda v: 255 if v > ALPHA_MIN else 0).getbbox()
        if box is None:
            sys.exit(f"{item}: empty cell, regenerate the sheet")
        if box[0] == 0 or box[1] == 0 or box[2] == cw or box[3] == ch:
            print(f"warning: {item} touches its cell edge, check the cut")
        piece = cell.crop(box)
        if max(piece.size) > SIDE:
            piece.thumbnail((SIDE, SIDE), Image.LANCZOS)
        out = Image.new("RGBA", (SIDE, SIDE), (0, 0, 0, 0))
        out.alpha_composite(piece, ((SIDE - piece.width) // 2, (SIDE - piece.height) // 2))
        out.save(os.path.join(dest, item + ".png"))
        print(item, piece.size)


if __name__ == "__main__":
    if len(sys.argv) < 4 or sys.argv[1] not in ("generate", "cut"):
        sys.exit(__doc__)
    if sys.argv[1] == "generate":
        generate(sys.argv[2], sys.argv[3], sys.argv[4:])
    else:
        cut(sys.argv[2], sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else os.path.join(HERE, "..", "..", "assets", "items"))
