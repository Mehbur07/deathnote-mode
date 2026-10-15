"""Generates the Death Note pixel-art textures (32x32) and the mod icon (128x128).

Usage: python -I gen_textures.py <resources_dir> [preview_dir]
No third-party dependencies: PNGs are encoded with zlib + struct.
"""
import math
import struct
import sys
import zlib
from pathlib import Path

SIZE = 32


def write_png(path, pixels, size):
    """pixels: dict[(x, y)] -> (r, g, b, a); missing pixels are transparent."""
    raw = bytearray()
    for y in range(size):
        raw.append(0)  # filter: none
        for x in range(size):
            raw.extend(pixels.get((x, y), (0, 0, 0, 0)))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def hexc(value, alpha=255):
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


def add_outline(pixels, color):
    """Draws a 1px outline around the opaque shape (outside it), like vanilla item sprites."""
    filled = set(pixels)
    for x in range(SIZE):
        for y in range(SIZE):
            if (x, y) in filled:
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                pixels[(x, y)] = color


# 3x5 pixel font for the cover title.
FONT = {
    "D": ["110", "101", "101", "101", "110"],
    "E": ["111", "100", "110", "100", "111"],
    "A": ["010", "101", "111", "101", "101"],
    "T": ["111", "010", "010", "010", "010"],
    "H": ["101", "101", "111", "101", "101"],
    "N": ["1001", "1101", "1011", "1001", "1001"],
    "O": ["111", "101", "101", "101", "111"],
}


def draw_text(pixels, text, x0, y0, color, shadow):
    glyph_pixels = []
    x = x0
    for ch in text:
        for row, bits in enumerate(FONT[ch]):
            for col, bit in enumerate(bits):
                if bit == "1":
                    glyph_pixels.append((x + col, y0 + row))
        x += len(FONT[ch][0]) + 1
    glyph_set = set(glyph_pixels)
    for gx, gy in glyph_pixels:
        if (gx + 1, gy + 1) not in glyph_set:
            pixels[(gx + 1, gy + 1)] = shadow
    for p in glyph_pixels:
        pixels[p] = color


def text_width(text):
    return sum(len(FONT[ch][0]) + 1 for ch in text) - 1


def make_book():
    px = {}
    cream, cream_shade = hexc("#e6dcc4"), hexc("#bfb398")
    cover, cover_dark, cover_light = hexc("#1b1b1d"), hexc("#0c0c0d"), hexc("#303033")
    gold, gold_shadow, gold_dim = hexc("#f5c542"), hexc("#6e4f0c"), hexc("#a8801f")

    # Page block peeking out on the right and bottom.
    for x in range(6, 29):
        for y in range(4, 30):
            shade = cream_shade if (x == 28 and y % 2 == 0) or (y == 29 and x % 2 == 0) else cream
            px[(x, y)] = shade

    # Cover with spine and edge lighting.
    for x in range(4, 28):
        for y in range(2, 29):
            px[(x, y)] = cover
    for y in range(2, 29):
        px[(4, y)] = cover_dark
        px[(5, y)] = cover_dark
        px[(6, y)] = cover_light
        px[(27, y)] = cover_dark
    for x in range(7, 27):
        px[(x, 2)] = cover_light
        px[(x, 28)] = cover_dark

    # Golden title, centred on the cover area right of the spine.
    area_left, area_right = 7, 26
    for text, y in (("DEATH", 9), ("NOTE", 16)):
        x0 = area_left + (area_right - area_left + 1 - text_width(text)) // 2
        draw_text(px, text, x0, y, gold, gold_shadow)

    # Thin ornamental rule under the title.
    for x in range(11, 23):
        px[(x, 23)] = gold_dim
    px[(16, 24)] = gold_dim

    add_outline(px, hexc("#050505"))
    return px


def make_quill():
    px = {}
    nib = (4.5, 27.5)
    tip = (27.5, 3.5)
    dx, dy = tip[0] - nib[0], tip[1] - nib[1]
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length  # along the quill
    nx, ny = -uy, ux  # perpendicular

    gold_light, gold, gold_mid, gold_dark = hexc("#fff1a8"), hexc("#f2c12e"), hexc("#d79d18"), hexc("#9c6a0c")
    ink, steel = hexc("#1a1208"), hexc("#b8860b")

    for x in range(SIZE):
        for y in range(SIZE):
            cx, cy = x + 0.5 - nib[0], y + 0.5 - nib[1]
            t = (cx * ux + cy * uy) / length  # 0 at nib, 1 at tip
            s = cx * nx + cy * ny  # signed distance from the shaft
            if t < 0.0 or t > 1.0:
                continue

            # Metal nib with an ink-dipped point.
            if t < 0.22:
                half = 0.5 + 1.4 * (t / 0.22)
                if abs(s) <= half:
                    if t < 0.07:
                        px[(x, y)] = ink
                    elif abs(s) < 0.45 and t < 0.16:
                        px[(x, y)] = ink  # nib slit
                    else:
                        px[(x, y)] = steel if s > 0 else gold_mid
                continue

            # Shaft (rachis).
            if abs(s) <= 0.55:
                px[(x, y)] = gold_light
                continue

            # Vane: narrow and asymmetric, rising quickly near the base and tapering to a long point.
            v = (t - 0.32) / 0.68
            if v < 0.0:
                continue
            profile = min(1.0, v / 0.22) ** 0.8 * (1.0 - v) ** 0.45
            width = (4.3 if s > 0 else 2.3) * profile
            if abs(s) > width:
                continue
            # Feather notches: small gaps along the outer edge of the wide side.
            along = t * length
            if s > 0 and abs(s) > width - 1.6 and int(along) % 6 == 0 and 0.15 < v < 0.85:
                continue
            barb = (along * 1.1 + abs(s) * 0.9) % 3.0
            if abs(s) > width - 1.0:
                color = gold_dark
            elif barb < 1.0:
                color = gold_mid
            else:
                color = gold
            px[(x, y)] = color

    add_outline(px, hexc("#3d2a05"))
    return px


def upscale(pixels, factor):
    return {(x * factor + i, y * factor + j): c for (x, y), c in pixels.items() for i in range(factor) for j in range(factor)}


def main():
    resources = Path(sys.argv[1])
    preview = Path(sys.argv[2]) if len(sys.argv) > 2 else None

    book, quill = make_book(), make_quill()
    tex = resources / "assets" / "deathnote" / "textures" / "item"
    write_png(tex / "death_note_book.png", book, SIZE)
    write_png(tex / "death_note_quill.png", quill, SIZE)
    write_png(resources / "assets" / "deathnote" / "icon.png", upscale(book, 4), SIZE * 4)

    if preview:
        write_png(preview / "book_x8.png", upscale(book, 8), SIZE * 8)
        write_png(preview / "quill_x8.png", upscale(quill, 8), SIZE * 8)
    print("textures written")


if __name__ == "__main__":
    main()
