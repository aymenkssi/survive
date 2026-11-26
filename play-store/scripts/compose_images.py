#!/usr/bin/env python3
"""Compose les images de la fiche Google Play à partir des captures brutes (play-store/build/raw/).

Produit dans play-store/images/ :
  icon-512.png                       icône de la fiche (512 × 512)
  feature-graphic-<lang>.png         bannière (1024 × 500)
  screenshots-<lang>/NN-*.png        8 captures légendées (1080 × 1920)
Police : DejaVu Sans (paquet fonts-dejavu-core sur Ubuntu / Debian).
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "..", "build", "raw")
OUT = os.path.join(HERE, "..", "images")
FONT_DIRS = ["/usr/share/fonts/truetype/dejavu", "/usr/share/fonts/dejavu", "/Library/Fonts", "C:/Windows/Fonts"]

GREEN_TOP, GREEN_BOTTOM = (36, 66, 45), (14, 28, 19)
CREAM, GOLD = (242, 232, 207), (242, 201, 76)

CAPTIONS = {
    "fr": {
        "01-home": "Tout pour survivre,\nmême sans réseau",
        "02-map": "Cartes hors ligne\net position GPS",
        "03-guide": "73 fiches claires,\npas à pas",
        "04-cpr": "Massage cardiaque :\nle bon rythme",
        "05-morse": "Morse : émettre,\nécouter, traduire",
        "06-maps": "La carte de votre région,\ntéléchargée une fois",
        "07-checklist": "Kit d'urgence\net listes à cocher",
        "08-night": "Mode nuit rouge,\ndiscret dans le noir",
    },
    "en": {
        "01-home": "Everything to survive,\neven with no network",
        "02-map": "Offline maps\nwith GPS position",
        "03-guide": "73 clear,\nstep-by-step guides",
        "04-cpr": "CPR metronome:\nthe right rhythm",
        "05-morse": "Morse: send,\nlisten, translate",
        "06-maps": "Your region's map,\ndownloaded once",
        "07-checklist": "Emergency kit\nand checklists",
        "08-night": "Night red mode,\ndiscreet in the dark",
    },
}
BANNER = {
    "fr": "Le guide de survie qui marche\nsans réseau, sans Internet",
    "en": "The survival guide that works\nwith no network, no Internet",
}


def font(bold, size):
    name = "DejaVuSans-Bold.ttf" if bold else "DejaVuSans.ttf"
    for d in FONT_DIRS:
        p = os.path.join(d, name)
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    sys.exit(f"Police {name} introuvable : installer fonts-dejavu-core")


def gradient(w, h):
    top = Image.new("RGB", (w, h), GREEN_TOP)
    bottom = Image.new("RGB", (w, h), GREEN_BOTTOM)
    mask = Image.linear_gradient("L").resize((w, h))
    return Image.composite(bottom, top, mask)


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, img.width - 1, img.height - 1), radius, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def paste_with_shadow(canvas, img, xy, radius, blur=24, offset=12):
    pad = blur * 2
    shadow = Image.new("RGBA", (img.width + pad * 2, img.height + pad * 2), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((pad, pad, pad + img.width, pad + img.height), radius, fill=(0, 0, 0, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    canvas.alpha_composite(shadow, (xy[0] - pad, xy[1] - pad + offset))
    canvas.alpha_composite(rounded(img, radius), xy)


def screenshot(lang, name, caption):
    w, h, top = 1080, 1920, 380
    canvas = gradient(w, h).convert("RGBA")
    ImageDraw.Draw(canvas).multiline_text((w // 2, 200), caption, font=font(True, 68), fill=CREAM,
                                          anchor="mm", align="center", spacing=18)
    shot = Image.open(os.path.join(RAW, lang, name + ".png")).convert("RGB")
    scale = (h - top - 40) / shot.height
    shot = shot.resize((int(shot.width * scale), int(shot.height * scale)), Image.LANCZOS)
    paste_with_shadow(canvas, shot, ((w - shot.width) // 2, top), 36)
    return canvas.convert("RGB")


def banner(lang, ic):
    w, h = 1024, 500
    canvas = gradient(w, h).convert("RGBA")
    d = ImageDraw.Draw(canvas)
    paste_with_shadow(canvas, ic.resize((200, 200), Image.LANCZOS), (56, 150), 44, blur=16, offset=8)
    d.text((290, 128), "Survivor", font=font(True, 84), fill=CREAM)
    d.text((294, 228), "End of the World", font=font(True, 36), fill=GOLD)
    d.multiline_text((294, 288), BANNER[lang], font=font(False, 24), fill=CREAM, spacing=8)
    # Aperçu de la carte hors ligne, à droite.
    shot = Image.open(os.path.join(RAW, lang, "02-map.png")).convert("RGB")
    shot = shot.resize((220, int(220 * shot.height / shot.width)), Image.LANCZOS).crop((0, 0, 220, 400))
    paste_with_shadow(canvas, shot, (w - 220 - 44, 50), 22, blur=16, offset=8)
    return canvas.convert("RGB")


def main():
    if not os.path.isdir(RAW):
        sys.exit("Captures brutes absentes : lancer d'abord StoreImagesTest (voir play-store/README.md)")
    os.makedirs(OUT, exist_ok=True)
    ic = Image.open(os.path.join(RAW, "icon-512.png")).convert("RGB")
    ic.save(os.path.join(OUT, "icon-512.png"), optimize=True)
    for lang, caps in CAPTIONS.items():
        folder = os.path.join(OUT, f"screenshots-{lang}")
        os.makedirs(folder, exist_ok=True)
        for name, caption in caps.items():
            screenshot(lang, name, caption).save(os.path.join(folder, name + ".png"), optimize=True)
        banner(lang, ic).save(os.path.join(OUT, f"feature-graphic-{lang}.png"), optimize=True)
    for root, _, files in sorted(os.walk(OUT)):
        for f in sorted(files):
            p = os.path.join(root, f)
            print(os.path.relpath(p, OUT), Image.open(p).size)


if __name__ == "__main__":
    main()
