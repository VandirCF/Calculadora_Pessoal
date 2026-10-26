"""Turns the logo screenshots supplied by the project owner into transparent PNGs for the app.

The two source files have an opaque white background. This script removes only the white that
is connected to the image border (so white letters and shapes inside the shields are kept),
trims the light edge fringe, crops to the content and writes:

    app/src/main/res/drawable-nodpi/logo_flamengo.png
    app/src/main/res/drawable-nodpi/logo_cavaliers.png

Usage:  python tools/prepare_logos.py <flamengo_source.png> <cavaliers_source.png>
Requires Pillow (pip install pillow). The logos are not downloaded or generated: they are the files
the owner provided.
"""
import os
import sys
from collections import deque

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')


def is_background(px, threshold=232):
    return px[0] >= threshold and px[1] >= threshold and px[2] >= threshold


def make_transparent(src_path, dst_path):
    img = Image.open(src_path).convert('RGBA')
    w, h = img.size
    px = img.load()
    seen = [[False] * h for _ in range(w)]
    queue = deque()
    for x in range(w):
        queue.extend([(x, 0), (x, h - 1)])
    for y in range(h):
        queue.extend([(0, y), (w - 1, y)])
    while queue:
        x, y = queue.popleft()
        if x < 0 or y < 0 or x >= w or y >= h or seen[x][y]:
            continue
        if not is_background(px[x, y]):
            continue
        seen[x][y] = True
        px[x, y] = (255, 255, 255, 0)
        queue.extend([(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)])

    # light fringe left by anti-aliasing: make bright pixels next to transparent ones transparent too
    for _ in range(2):
        fringe = []
        for x in range(w):
            for y in range(h):
                r, g, b, a = px[x, y]
                if a == 0:
                    continue
                if (r + g + b) / 3 < 185:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] == 0:
                        fringe.append((x, y))
                        break
        for x, y in fringe:
            px[x, y] = (255, 255, 255, 0)

    box = img.getbbox()
    if box:
        img = img.crop(box)
    img.save(dst_path)
    return img.size


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    os.makedirs(OUT, exist_ok=True)
    print('flamengo', make_transparent(sys.argv[1], os.path.join(OUT, 'logo_flamengo.png')))
    print('cavaliers', make_transparent(sys.argv[2], os.path.join(OUT, 'logo_cavaliers.png')))
