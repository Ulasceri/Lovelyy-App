"""Minimal fal.ai queue client (no dependencies).

The API key is read from the FAL_KEY environment variable. Never commit it.

    export FAL_KEY=...            # PowerShell: $env:FAL_KEY="..."
    python fal.py fal-ai/nano-banana-pro '{"prompt": "..."}' out_dir
"""
import json, os, sys, time, urllib.request

QUEUE = "https://queue.fal.run"


def _req(method, url, body=None):
    key = os.environ.get("FAL_KEY")
    if not key:
        sys.exit("FAL_KEY is not set")
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, data=data, method=method, headers={
        "Authorization": f"Key {key}", "Content-Type": "application/json"})
    with urllib.request.urlopen(r, timeout=120) as resp:
        return json.loads(resp.read() or b"{}")


def submit(model, args):
    return _req("POST", f"{QUEUE}/{model}", args)


def wait(sub, poll=3, timeout=1800):
    t0 = time.time()
    while time.time() - t0 < timeout:
        st = _req("GET", sub["status_url"])
        if st.get("status") == "COMPLETED":
            return _req("GET", sub["response_url"])
        if st.get("status") not in ("IN_QUEUE", "IN_PROGRESS"):
            raise RuntimeError(st)
        time.sleep(poll)
    raise TimeoutError(sub)


def run(model, args):
    return wait(submit(model, args))


def upload(path):
    """Uploads a local file to fal storage and returns its URL."""
    import mimetypes
    key = os.environ["FAL_KEY"]
    ctype = mimetypes.guess_type(path)[0] or "application/octet-stream"
    init = urllib.request.Request(
        "https://rest.alpha.fal.ai/storage/upload/initiate?storage_type=fal-cdn-v3",
        data=json.dumps({"content_type": ctype, "file_name": os.path.basename(path)}).encode(),
        method="POST", headers={"Authorization": f"Key {key}", "Content-Type": "application/json"})
    with urllib.request.urlopen(init, timeout=60) as r:
        info = json.loads(r.read())
    with open(path, "rb") as f:
        put = urllib.request.Request(info["upload_url"], data=f.read(), method="PUT",
                                     headers={"Content-Type": ctype})
    urllib.request.urlopen(put, timeout=300).read()
    return info["file_url"]


def download(url, path):
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    urllib.request.urlretrieve(url, path)
    return path


def find_urls(obj):
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k == "url" and isinstance(v, str):
                yield v
            else:
                yield from find_urls(v)
    elif isinstance(obj, list):
        for v in obj:
            yield from find_urls(v)


if __name__ == "__main__":
    model, args, out = sys.argv[1], json.loads(sys.argv[2]), sys.argv[3]
    res = run(model, args)
    os.makedirs(out, exist_ok=True)
    json.dump(res, open(os.path.join(out, "result.json"), "w"), indent=2)
    for i, u in enumerate(find_urls(res)):
        ext = os.path.splitext(u.split("?")[0])[1] or ".bin"
        print(download(u, os.path.join(out, f"{i}{ext}")))
