# Tools

## `cat-animation/`: the cat's clips (fal.ai)

Needs Python 3 and ffmpeg. The fal key is read from the `FAL_KEY` environment variable:
**never commit it**.

```bash
export FAL_KEY=...                       # PowerShell: $env:FAL_KEY="..."
cd tools/cat-animation
python fal.py fal-ai/nano-banana-pro '{"prompt": "...", "aspect_ratio": "1:1"}' out/char   # character
python fal.py fal-ai/nano-banana-pro/edit '{"prompt": "...", "image_urls": ["<base url>"]}' out/food
python generate.py keys.json raw/                 # all clips in clips.json (Kling, start+end frame)
python generate.py keys.json raw/ playing         # or just one
./encode.sh raw/                                  # -> android assets + poster/widget pictures
```

- Character: `fal-ai/nano-banana-pro`, soft 3D peach tabby, palette background (cream wall, mint rug).
- Key poses (`keys.json`: base, sleep, food, water, toy) are edits of the base image, so the cat is
  pixel-identical across them.
- Clips: `fal-ai/kling-video/v2.5-turbo/pro/image-to-video` with `image_url` = start pose and
  `tail_image_url` = end pose. Every clip starting and ending on a key pose is what makes the loops
  and transitions seamless in the app. Prompts live in `clips.json`.
- `encode.sh` drops each clip's last frame (it repeats the first), scales to 720², H.264 CRF 21.

## `item-art/`: Mırmır's items (fal.ai)

Needs Python 3 and Pillow, plus `FAL_KEY` as above. Output: `assets/items/*.png`, 686 px squares with
real alpha, one per item (`mama_*`, `su_*`, `oyuncak_*`, `yatak_*`).

```bash
cd tools/item-art
python make_items.py generate toys_beds out/        # one sheet from sheets.json
python make_items.py cut toys_beds out/             # -> ../../assets/items/<item>.png
python make_items.py generate food_drink out/ out/bowls.png   # extra refs pin scale to an earlier sheet
```

- Model: `bytedance/seedream/v5.5/edit` (image-to-image). Of the four models compared on 2026-11-27
  (Seedream 5.5, Meta Muse Image, Flux 3, MiniMax H3) it matched the cat's matte clay look best and is
  the only one that returns a transparent background. About $0.08 per sheet.
- **Always generate a whole sheet with the cat as reference** (`style-ref.png`). Text-to-image, or one
  item per call, gives every item its own design language; items rendered together share light,
  material and scale. None of these models has a seed, so references are what keeps the set consistent.
- A Plus model must differ from the free one in silhouette, not by a decal (a bowl with a heart on it
  reads as the same bowl).
- Nothing in an item picture may be couple-specific (no initials or names).
- The committed set was picked from four sheets (bowls, toys and beds, food and drink alternatives,
  scratching posts); `sheets.json` lists the prompts of the items that were kept.
