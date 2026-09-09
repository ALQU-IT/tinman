#!/usr/bin/env python3
"""
Pixel-art bot for the Tin Man mod's textures.

Claude authors 16x16 textures as a palette plus a character grid; this renders that
to PNG. Deliberately not an image model: at 16x16 every pixel is a decision, and
diffusion output has to be downsampled into one, which smears the palette, softens
the alpha edge and loses the readability that makes a Minecraft texture work. A
character grid is pixel-exact by construction, reviewable in a diff, and hand-editable
afterwards without another API call.

    pixelart.py gen "glowing cyan ingot" --name voltite_ingot --kind item
    pixelart.py render specs/voltite_ingot.pix
    pixelart.py extract src/.../voltite_ore.png
    pixelart.py sheet

Only `gen` calls the API. Everything else is offline, so iterating on a generated
texture is free.
"""

from __future__ import annotations

import argparse
import json
import pathlib
import re
import struct
import sys
import zlib

MOD_TEXTURES = pathlib.Path("src/main/resources/assets/tinman/textures")
SPEC_DIR = pathlib.Path("tools/pixelart/specs")
MODEL = "claude-opus-5"

TRANSPARENT = "."

AUTH_HELP = (
    "No Anthropic credentials found. Either:\n"
    "  export ANTHROPIC_API_KEY=sk-ant-...\n"
    "or log in once with the Anthropic CLI:\n"
    "  ant auth login\n"
    "Only `gen` needs this; render, extract and sheet are offline."
)


# --------------------------------------------------------------------------- PNG

def write_png(path: pathlib.Path, rows: list[list[tuple[int, int, int, int]]]) -> None:
    """Writes RGBA rows as a PNG. No dependency on Pillow, which is not worth
    pulling in for what is a few hundred bytes of scanline and zlib."""
    height = len(rows)
    width = len(rows[0])
    raw = b"".join(b"\x00" + bytes(v for px in row for v in px) for row in rows)

    def chunk(tag: bytes, data: bytes) -> bytes:
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def read_png(path: pathlib.Path) -> list[list[tuple[int, int, int, int]]]:
    """Reads a PNG back to RGBA rows, undoing the five scanline filters."""
    data = path.read_bytes()
    pos, idat, width, height, depth, colour, palette, trns = 8, b"", 0, 0, 0, 0, None, None

    while pos < len(data):
        length = struct.unpack_from(">I", data, pos)[0]
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]

        if tag == b"IHDR":
            width, height, depth, colour = struct.unpack(">IIBB", body[:10])
        elif tag == b"IDAT":
            idat += body
        elif tag == b"PLTE":
            palette = body
        elif tag == b"tRNS":
            trns = body

        pos += 12 + length

    if depth != 8:
        raise SystemExit(f"{path}: only 8-bit PNGs are supported, got {depth}-bit")

    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colour]
    stride = width * channels
    raw = zlib.decompress(idat)
    rows, previous, at = [], bytearray(stride), 0

    for _ in range(height):
        filter_type = raw[at]
        at += 1
        line = bytearray(raw[at:at + stride])
        at += stride

        for i in range(stride):
            a = line[i - channels] if i >= channels else 0
            b = previous[i]
            c = previous[i - channels] if i >= channels else 0

            if filter_type == 1:
                line[i] = (line[i] + a) & 255
            elif filter_type == 2:
                line[i] = (line[i] + b) & 255
            elif filter_type == 3:
                line[i] = (line[i] + (a + b) // 2) & 255
            elif filter_type == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pred) & 255

        previous = line
        row = []

        for x in range(width):
            px = line[x * channels:(x + 1) * channels]

            if colour == 6:
                row.append(tuple(px))
            elif colour == 2:
                row.append((px[0], px[1], px[2], 255))
            elif colour == 4:
                row.append((px[0], px[0], px[0], px[1]))
            elif colour == 3:
                i = px[0]
                alpha = trns[i] if trns and i < len(trns) else 255
                row.append((palette[i * 3], palette[i * 3 + 1], palette[i * 3 + 2], alpha))
            else:
                row.append((px[0], px[0], px[0], 255))

        rows.append(row)

    return rows


# -------------------------------------------------------------------------- spec

def parse_hex(value: str) -> tuple[int, int, int, int]:
    text = value.lstrip("#")

    if len(text) == 6:
        text += "ff"

    if len(text) != 8:
        raise SystemExit(f"bad colour {value!r}: want #RRGGBB or #RRGGBBAA")

    return tuple(int(text[i:i + 2], 16) for i in (0, 2, 4, 6))


def to_hex(px: tuple[int, int, int, int]) -> str:
    r, g, b, a = px
    return "#%02X%02X%02X" % (r, g, b) + ("" if a == 255 else "%02X" % a)


def parse_spec(text: str) -> list[list[tuple[int, int, int, int]]]:
    """A spec is `<char> = <colour>` lines, then the grid. '.' is always transparent."""
    palette = {TRANSPARENT: (0, 0, 0, 0)}
    grid: list[str] = []

    for line in text.splitlines():
        stripped = line.rstrip()

        if not stripped or stripped.lstrip().startswith("#") and "=" not in stripped:
            continue

        match = re.match(r"^\s*(\S)\s*=\s*(\S+)\s*$", stripped)

        if match and not grid:
            palette[match.group(1)] = parse_hex(match.group(2))
        else:
            grid.append(stripped.strip())

    if not grid:
        raise SystemExit("spec has no grid rows")

    width = len(grid[0])

    for y, row in enumerate(grid):
        if len(row) != width:
            raise SystemExit(f"row {y} is {len(row)} wide, expected {width}")

        for char in row:
            if char not in palette:
                raise SystemExit(f"row {y} uses {char!r}, which the palette does not define")

    return [[palette[c] for c in row] for row in grid]


def build_spec(rows: list[list[tuple[int, int, int, int]]], name: str) -> str:
    """The inverse: turns a PNG back into an editable spec."""
    seen: dict[tuple[int, int, int, int], str] = {(0, 0, 0, 0): TRANSPARENT}
    alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    for row in rows:
        for px in row:
            if px[3] == 0:
                continue
            if px not in seen:
                if len(seen) > len(alphabet):
                    raise SystemExit("more than 62 distinct colours; not a pixel-art texture")
                seen[px] = alphabet[len(seen) - 1]

    lines = [f"# {name}"]
    lines += [f"{char} = {to_hex(px)}" for px, char in seen.items() if px[3] != 0]
    lines.append("")
    lines += ["".join(seen[px] for px in row) for row in rows]
    return "\n".join(lines) + "\n"


# --------------------------------------------------------------------------- api

SYSTEM = """\
You draw textures for a Minecraft mod. You reply with a palette and a character grid; \
another program renders it to PNG, so what you write is the final art.

Rules that make a texture read at this size:
- Exactly {size} rows of exactly {size} characters.
- Use '.' for fully transparent. Items need transparency around the shape; blocks fill \
the whole square edge to edge.
- 4 to 7 colours. More reads as mush at 16 pixels; each colour must earn its place.
- Light from the top-left: lighter on the top and left faces, darker on the bottom and \
right, one or two mid tones between.
- Block textures must tile: the left edge has to sit against the right edge, and the top \
against the bottom, without a seam.
- Shape first. A silhouette that is readable in one colour is worth more than detail.
- Dither sparingly, only to blend two adjacent tones.
"""


def generate(description: str, size: int, kind: str, palette_from: pathlib.Path | None) -> str:
    try:
        import anthropic
    except ImportError:
        raise SystemExit("`gen` needs the SDK: pip install anthropic")

    constraint = ""

    if palette_from:
        colours = sorted({to_hex(px) for row in read_png(palette_from) for px in row if px[3]})
        constraint = ("\nUse only these colours, so it matches the set it belongs to: "
                      + ", ".join(colours) + "\n")

    try:
        client = anthropic.Anthropic()
    except Exception:
        raise SystemExit(AUTH_HELP)

    try:
        response = client.messages.create(
        model=MODEL,
        max_tokens=8000,
        system=SYSTEM.format(size=size),
        messages=[{
            "role": "user",
            "content": (f"Draw a {size}x{size} Minecraft {kind} texture: {description}."
                        + constraint),
        }],
        # Guarantees the grid parses. Free-form text would need a retry loop for
        # every stray fence or comment; the schema makes that class of error impossible.
        output_config={
            "format": {
                "type": "json_schema",
                "schema": {
                    "type": "object",
                    "properties": {
                        "notes": {
                            "type": "string",
                            "description": "One line on what was drawn and why.",
                        },
                        "palette": {
                            "type": "object",
                            "description": "Single character -> #RRGGBB or #RRGGBBAA. Do not "
                                           "include '.', which is always transparent.",
                            "additionalProperties": {"type": "string"},
                        },
                        "rows": {
                            "type": "array",
                            "description": f"Exactly {size} strings of exactly {size} characters.",
                            "items": {"type": "string"},
                        },
                    },
                    "required": ["notes", "palette", "rows"],
                    "additionalProperties": False,
                },
            }
        },
        )
    except TypeError as error:
        if "authentication" in str(error).lower():
            raise SystemExit(AUTH_HELP)
        raise
    except anthropic.APIStatusError as error:
        raise SystemExit(f"API error {error.status_code}: {error.message}")
    except anthropic.APIConnectionError as error:
        raise SystemExit(f"could not reach the API: {error}")

    data = json.loads(next(b.text for b in response.content if b.type == "text"))
    print(f"  {data['notes']}", file=sys.stderr)
    return spec_from_payload(data, description)


def spec_from_payload(data: dict, description: str) -> str:
    """Turns the model's JSON into spec text. Separate from the call so it can be
    exercised without spending one."""
    lines = [f"# {description}"]
    lines += [f"{char} = {colour}" for char, colour in data["palette"].items()]
    lines.append("")
    lines += data["rows"]
    return "\n".join(lines) + "\n"


# --------------------------------------------------------------------------- cli

def cmd_gen(args: argparse.Namespace) -> None:
    spec_text = generate(args.description, args.size, args.kind,
                         pathlib.Path(args.palette) if args.palette else None)
    rows = parse_spec(spec_text)

    if len(rows) != args.size or len(rows[0]) != args.size:
        raise SystemExit(f"model returned {len(rows[0])}x{len(rows)}, wanted {args.size} square")

    spec_path = SPEC_DIR / f"{args.name}.pix"
    spec_path.parent.mkdir(parents=True, exist_ok=True)
    spec_path.write_text(spec_text)

    out = pathlib.Path(args.out) if args.out else MOD_TEXTURES / args.kind_dir / f"{args.name}.png"
    write_png(out, rows)
    print(f"{spec_path}\n{out}")


def cmd_render(args: argparse.Namespace) -> None:
    for spec in args.specs:
        path = pathlib.Path(spec)
        rows = parse_spec(path.read_text())
        out = (pathlib.Path(args.out) if args.out
               else MOD_TEXTURES / "item" / f"{path.stem}.png")
        write_png(out, rows)
        print(out)


def cmd_extract(args: argparse.Namespace) -> None:
    for png in args.pngs:
        path = pathlib.Path(png)
        spec_path = SPEC_DIR / f"{path.stem}.pix"
        spec_path.parent.mkdir(parents=True, exist_ok=True)
        spec_path.write_text(build_spec(read_png(path), path.stem))
        print(spec_path)


def cmd_sheet(args: argparse.Namespace) -> None:
    """A zoomed contact sheet of every texture, since 16x16 files are unreviewable
    in a file browser."""
    cards = []

    for png in sorted(MOD_TEXTURES.rglob("*.png")):
        rows = read_png(png)
        cells = "".join(
            f'<i style="background:rgba({r},{g},{b},{a / 255:.2f})"></i>'
            for row in rows for r, g, b, a in row
        )
        cards.append(
            f'<figure><div class=grid style="--n:{len(rows[0])}">{cells}</div>'
            f'<figcaption>{png.relative_to(MOD_TEXTURES)}<br><small>{len(rows[0])}x{len(rows)}'
            f'</small></figcaption></figure>'
        )

    out = pathlib.Path(args.out)
    out.write_text(
        "<meta charset=utf-8><title>Tin Man textures</title><style>"
        "body{background:#15181c;color:#cfd8dd;font:13px system-ui;margin:24px;}"
        "main{display:flex;flex-wrap:wrap;gap:20px}figure{margin:0;text-align:center}"
        ".grid{display:grid;grid-template-columns:repeat(var(--n),8px);"
        "width:calc(var(--n)*8px);image-rendering:pixelated;"
        "background:repeating-conic-gradient(#2a2f35 0 25%,#22262b 0 50%) 0 0/12px 12px;"
        "border:1px solid #333}"
        "i{width:8px;height:8px;display:block}figcaption{margin-top:6px;color:#8b979e}"
        "</style><main>" + "".join(cards) + "</main>\n"
    )
    print(f"{out}  ({len(cards)} textures)")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="cmd", required=True)

    gen = sub.add_parser("gen", help="draw a new texture with Claude (the only command that calls the API)")
    gen.add_argument("description")
    gen.add_argument("--name", required=True, help="file name, without .png")
    gen.add_argument("--kind", default="item", choices=["item", "block"])
    gen.add_argument("--size", type=int, default=16)
    gen.add_argument("--palette", help="an existing PNG whose colours to reuse, for a matching set")
    gen.add_argument("--out", help="output PNG path (default: the mod's textures folder)")
    gen.set_defaults(func=cmd_gen)

    render = sub.add_parser("render", help="spec -> PNG, offline")
    render.add_argument("specs", nargs="+")
    render.add_argument("--out")
    render.set_defaults(func=cmd_render)

    extract = sub.add_parser("extract", help="PNG -> spec, to hand-edit an existing texture")
    extract.add_argument("pngs", nargs="+")
    extract.set_defaults(func=cmd_extract)

    sheet = sub.add_parser("sheet", help="zoomed contact sheet of every mod texture")
    sheet.add_argument("--out", default="tools/pixelart/sheet.html")
    sheet.set_defaults(func=cmd_sheet)

    args = parser.parse_args()
    args.kind_dir = getattr(args, "kind", "item")
    args.func(args)


if __name__ == "__main__":
    main()
