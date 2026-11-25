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
