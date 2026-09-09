# pixelart

A bot that draws the mod's textures. Lives outside the mod — nothing here ships in the jar.

```
pixelart.py gen "glowing cyan power cell" --name voltite_core --kind item
pixelart.py render specs/voltite_core.pix          # spec -> PNG, offline
pixelart.py extract .../voltite_ore.png            # PNG -> spec, to hand-edit
pixelart.py sheet                                  # zoomed contact sheet of all 40 textures
```

## Why a character grid and not an image model

At 16×16 every pixel is a decision. Diffusion output has to be downsampled into that
grid, which smears the palette, softens the alpha edge, and loses the readability that
makes a Minecraft texture work at inventory size. Claude writing a palette and a grid of
characters is pixel-exact by construction, and it is the same skill as drawing the art —
just expressed in a form that survives the trip.

The side effects are worth as much as the art:

- **It diffs.** A texture change shows up in review as changed characters, not as an
  opaque binary blob.
- **It is hand-editable.** `render` never calls the API, so nudging one pixel of a
  generated texture is free and instant.
- **It round-trips.** `extract` turns any existing PNG back into a spec, so hand-made
  textures can be tweaked with the same tools. Verified lossless: extract → render
  reproduces the original file's pixels exactly.

## The spec format

```
# voltite_ingot
a = #AAFAFF          <- 6 or 8 hex digits; 8 keeps alpha
b = #3FE0E8
c = #188C9E

................    <- '.' is always transparent
...aaaaaaaaaa...
...bababababa...
```

Everything before the first grid row is palette; everything after is the grid. Rows must
all be the same width, and every character must be in the palette — `render` refuses
rather than guessing, which is how a heredoc that swallowed its terminator got caught
instead of shipping as a 20-wide texture.

## Authentication

Only `gen` needs credentials — `export ANTHROPIC_API_KEY=...`, or `ant auth login` once.
It uses `claude-opus-5` with structured outputs, so the grid always parses; there is no
retry loop for stray prose or code fences.

## Matching an existing set

`--palette some_texture.png` extracts that file's colours and constrains the drawing to
them, so a new item lands in the same palette as the ones beside it.
