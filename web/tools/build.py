#!/usr/bin/env python3
"""Generates web/public/*.html from the page bodies in web/pages/. Run: python3 web/tools/build.py"""
import pathlib, re
root = pathlib.Path(__file__).resolve().parent.parent
CONTACT = (root / "CONTACT_EMAIL").read_text().strip()
NAV = {
 "tr": [("/gizlilik.html","Gizlilik Politikası"),("/kullanim-sartlari.html","Kullanım Şartları"),("/hesap-silme.html","Hesap Silme"),("/en/privacy.html","English")],
 "en": [("/en/privacy.html","Privacy Policy"),("/en/terms.html","Terms of Use"),("/en/delete-account.html","Delete Account"),("/gizlilik.html","Türkçe")],
}
for src in sorted((root/"pages").rglob("*.html")):
    rel = src.relative_to(root/"pages")
    lang = "en" if rel.parts[0] == "en" else "tr"
    body = src.read_text().replace("{{CONTACT}}", CONTACT)
    title = re.search(r"<h1>(.*?)</h1>", body).group(1)
    nav = "".join(f'<a href="{h}">{t}</a>' for h,t in NAV[lang])
    html = f'''<!doctype html><html lang="{lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{title} · Lovelyy</title><link rel="stylesheet" href="/style.css"></head><body>
<header><div class="in"><a href="/">Lovelyy 💛</a><nav>{nav}</nav></div></header><main>
{body}
</main><footer>© Lovelyy · {CONTACT}</footer></body></html>'''
    out = root/"public"/rel
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(html)
    print("wrote", out.relative_to(root))
