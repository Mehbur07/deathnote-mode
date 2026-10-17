"""Generates the Death Note pixel-art textures (32x32), the mod icon (128x128) and tools/deathnote.ico.

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
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png_bytes(pixels, size))


def png_bytes(pixels, size):
    raw = bytearray()
    for y in range(size):
        raw.append(0)  # filter: none
        for x in range(size):
            raw.extend(pixels.get((x, y), (0, 0, 0, 0)))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")


def resample(pixels, src_size, dst_size):
    """Area-average resample (alpha-weighted) for downscaling; nearest neighbour when upscaling."""
    if dst_size >= src_size:
        factor = dst_size // src_size
        return {(x * factor + i, y * factor + j): c for (x, y), c in pixels.items() for i in range(factor) for j in range(factor)}
    out = {}
    for ty in range(dst_size):
        y0, y1 = ty * src_size // dst_size, max(ty * src_size // dst_size + 1, (ty + 1) * src_size // dst_size)
        for tx in range(dst_size):
            x0, x1 = tx * src_size // dst_size, max(tx * src_size // dst_size + 1, (tx + 1) * src_size // dst_size)
            r = g = b = a = 0
            count = 0
            for sy in range(y0, y1):
                for sx in range(x0, x1):
                    pr, pg, pb, pa = pixels.get((sx, sy), (0, 0, 0, 0))
                    r, g, b, a = r + pr * pa, g + pg * pa, b + pb * pa, a + pa
                    count += 1
            if a > 0:
                out[(tx, ty)] = (round(r / a), round(g / a), round(b / a), round(a / count))
    return out


def write_ico(path, pixels, src_size, sizes=(16, 24, 32, 48, 64, 128, 256)):
    """Windows .ico with PNG-compressed entries (supported since Windows Vista)."""
    images = [(s, png_bytes(resample(pixels, src_size, s), s)) for s in sizes]
    header = struct.pack("<HHH", 0, 1, len(images))
    offset = 6 + 16 * len(images)
    entries, data = b"", b""
    for s, png in images:
        dim = 0 if s >= 256 else s  # 0 means 256 in the ICO directory
        entries += struct.pack("<BBBBHHII", dim, dim, 0, 0, 1, 32, len(png), offset + len(data))
        data += png
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(header + entries + data)


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


# 5x7 pixel font for the 128x128 mod logo.
LOGO_FONT = {
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
}
LOGO_SIZE = 128


def shade(color, delta):
    r, g, b, a = color
    return (max(0, min(255, r + delta)), max(0, min(255, g + delta)), max(0, min(255, b + delta)), a)


def noise(x, y):
    """Deterministic per-pixel noise in [-1, 1] for a subtle leather grain."""
    h = (x * 374761393 + y * 668265263) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h & 0xFF) / 127.5) - 1.0


LOGO_LETTER_GAP = 2


def draw_logo_text(px, text, x0, y0, scale, gradient, shadow, shadow_offset):
    """Draws scaled 5x7 glyphs with a vertical gold gradient (one colour per glyph row) and a drop shadow."""
    glyph_pixels = []
    x = x0
    for ch in text:
        for row, bits in enumerate(LOGO_FONT[ch]):
            for col, bit in enumerate(bits):
                if bit == "1":
                    for i in range(scale):
                        for j in range(scale):
                            glyph_pixels.append((x + col * scale + i, y0 + row * scale + j, row * scale + j))
        x += 5 * scale + LOGO_LETTER_GAP
    covered = {(gx, gy) for gx, gy, _ in glyph_pixels}
    for gx, gy, _ in glyph_pixels:
        for d in range(1, shadow_offset + 1):
            p = (gx + d, gy + d)
            if p not in covered:
                px[p] = shadow
    height = 7 * scale
    for gx, gy, local_y in glyph_pixels:
        t = local_y / (height - 1)
        index = min(len(gradient) - 1, int(t * len(gradient)))
        px[(gx, gy)] = gradient[index]


def logo_text_width(text, scale):
    return len(text) * (5 * scale + LOGO_LETTER_GAP) - LOGO_LETTER_GAP


def make_logo():
    """128x128 mod icon: black leather notebook with a gold frame and a large gold DEATH NOTE title."""
    px = {}
    size = LOGO_SIZE
    cream, cream_line = hexc("#e9dfc6"), hexc("#bcae8f")
    leather = hexc("#1a1a1c")
    spine, spine_light, stitch = hexc("#0b0b0c"), hexc("#262629"), hexc("#5a4a22")
    gold_frame, gold_frame_dark = hexc("#d9a628"), hexc("#7a5810")
    gradient = [hexc("#fff3b0"), hexc("#ffe066"), hexc("#f7cc3c"), hexc("#efb92a"), hexc("#d99a1c"), hexc("#b97c12")]
    text_shadow = hexc("#2a1d04")

    cover_l, cover_t, cover_r, cover_b = 8, 8, 117, 115

    # Page block (right and bottom edges) with page lines.
    for x in range(cover_l + 6, cover_r + 5):
        for y in range(cover_t + 4, cover_b + 5):
            on_edge_line = (x > cover_r and y % 3 == 0) or (y > cover_b and x % 3 == 0)
            px[(x, y)] = cream_line if on_edge_line else cream

    # Leather cover with grain and edge lighting.
    for x in range(cover_l, cover_r + 1):
        for y in range(cover_t, cover_b + 1):
            px[(x, y)] = shade(leather, int(noise(x, y) * 4))
    for x in range(cover_l, cover_r + 1):
        px[(x, cover_t)] = shade(leather, 26)
        px[(x, cover_t + 1)] = shade(leather, 14)
        px[(x, cover_b)] = shade(leather, -12)
    for y in range(cover_t, cover_b + 1):
        px[(cover_r, y)] = shade(leather, -12)

    # Spine with gold stitching.
    for x in range(cover_l, cover_l + 9):
        for y in range(cover_t, cover_b + 1):
            px[(x, y)] = shade(spine, int(noise(x, y) * 3))
    for y in range(cover_t, cover_b + 1):
        px[(cover_l + 9, y)] = spine_light
        if y % 6 < 3:
            px[(cover_l + 4, y)] = stitch

    # Double gold frame on the cover.
    fl, ft, fr, fb = cover_l + 12, cover_t + 6, cover_r - 4, cover_b - 6
    for inset, color in ((0, gold_frame), (1, gold_frame_dark), (3, gold_frame_dark)):
        for x in range(fl + inset, fr - inset + 1):
            px[(x, ft + inset)] = color
            px[(x, fb - inset)] = color
        for y in range(ft + inset, fb - inset + 1):
            px[(fl + inset, y)] = color
            px[(fr - inset, y)] = color
    # Corner ornaments.
    for cx, cy in ((fl, ft), (fr, ft), (fl, fb), (fr, fb)):
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                if abs(dx) + abs(dy) <= 2:
                    px[(cx + dx, cy + dy)] = gold_frame

    # Title, centred inside the frame.
    scale = 3
    inner_l, inner_r = fl + 4, fr - 4
    for text, y in (("DEATH", ft + 18), ("NOTE", ft + 46)):
        x0 = inner_l + (inner_r - inner_l + 1 - logo_text_width(text, scale)) // 2
        draw_logo_text(px, text, x0, y, scale, gradient, text_shadow, 2)

    # Ornamental rule with a diamond under the title.
    rule_y = ft + 80
    mid = (inner_l + inner_r) // 2
    for x in range(mid - 26, mid + 27):
        px[(x, rule_y)] = gold_frame
    for dx in range(-3, 4):
        for dy in range(-3, 4):
            if abs(dx) + abs(dy) <= 3:
                px[(mid + dx, rule_y + dy)] = gold_frame if abs(dx) + abs(dy) < 3 else gold_frame_dark

    # Dark outline around the whole notebook.
    filled = set(px)
    for x in range(size):
        for y in range(size):
            if (x, y) in filled:
                continue
            if any((x + dx, y + dy) in filled for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[(x, y)] = hexc("#050505")
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
    logo = make_logo()
    write_png(resources / "assets" / "deathnote" / "icon.png", logo, LOGO_SIZE)
    # Windows icon for desktop shortcuts (not packaged in the mod jar).
    write_ico(Path(__file__).resolve().parent / "deathnote.ico", logo, LOGO_SIZE)

    if preview:
        write_png(preview / "book_x8.png", upscale(book, 8), SIZE * 8)
        write_png(preview / "quill_x8.png", upscale(quill, 8), SIZE * 8)
        write_png(preview / "logo_x3.png", upscale(logo, 3), LOGO_SIZE * 3)
    print("textures written")


if __name__ == "__main__":
    main()
