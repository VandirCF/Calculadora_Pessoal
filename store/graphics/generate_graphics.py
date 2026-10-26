"""Generates the store graphics from the same original artwork as the launcher icon
(app/src/main/res/drawable/ic_launcher_foreground.xml). Requires Pillow: pip install pillow.

    python store/graphics/generate_graphics.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.dirname(os.path.abspath(__file__))
BG = (0x0F, 0x4C, 0x5C)
BODY = (0xF4, 0xF7, 0xF8)
KEY = (0x5B, 0x8E, 0x9B)
ENTER = (0xFF, 0xB7, 0x03)

# (x, y, w, h, radius, color) in the 108x108 viewport of the vector drawable
SHAPES = [
    (32, 24, 44, 60, 6, BODY),
    (35, 29, 38, 14, 2, BG),
    *[(35 + 13 * col, 48 + 11 * row, 12, 8, 2, KEY) for row in range(2) for col in range(3)],
    (35, 70, 12, 8, 2, KEY),
    (48, 70, 25, 8, 2, ENTER),
]


def draw_icon(canvas: Image.Image, left: int, top: int, size: int, with_background: bool) -> None:
    scale = size / 108.0
    draw = ImageDraw.Draw(canvas)
    if with_background:
        draw.rounded_rectangle([left, top, left + size, top + size], radius=int(size * 0.22), fill=BG)
    for x, y, w, h, r, color in SHAPES:
        draw.rounded_rectangle(
            [left + x * scale, top + y * scale, left + (x + w) * scale, top + (y + h) * scale],
            radius=r * scale, fill=color
        )
    # ENTER arrow
    arrow = [(58, 73), (67, 73), (67, 74.6), (61.8, 74.6), (63.6, 76.4), (62.5, 77.5), (59, 74)]
    draw.polygon([(left + px * scale, top + py * scale) for px, py in arrow], fill=BG)


def font(size: int):
    for name in ("arialbd.ttf", "arial.ttf", "DejaVuSans-Bold.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


# 512x512 store icon (the store applies its own mask, so the square is full-bleed)
icon = Image.new("RGB", (512, 512), BG)
draw_icon(icon, 0, 0, 512, with_background=False)
icon.save(os.path.join(OUT, "icon-512.png"))

# 1024x500 feature graphic
feature = Image.new("RGB", (1024, 500), BG)
draw_icon(feature, 70, 70, 360, with_background=True)
d = ImageDraw.Draw(feature)
d.text((480, 150), "Calculadora", font=font(70), fill=BODY)
d.text((480, 235), "Financeira RPN", font=font(70), fill=ENTER)
d.text((480, 340), "Basic · Scientific · Financial", font=font(34), fill=(0xBF, 0xD8, 0xDE))
d.text((480, 390), "100% offline", font=font(34), fill=(0xBF, 0xD8, 0xDE))
feature.save(os.path.join(OUT, "feature-1024x500.png"))
print("done")
