# Supertonic TTS — Desktop GUI

A small, self-contained tkinter app for turning a text or markdown file into
a WAV file using the [`supertonic`](https://pypi.org/project/supertonic/)
PyPI package. Everything — dependencies and the model itself — lives inside
this folder, so `gui-app/` can be copied/moved as one unit without touching
anything outside it (no global site-packages, no `~/.cache/`).

## Setup

```bash
python -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt   # Windows
# .venv/bin/python -m pip install -r requirements.txt          # macOS/Linux
```

## Run

```bash
.venv\Scripts\python.exe app.py   # Windows
# .venv/bin/python app.py          # macOS/Linux
```

No `.git lfs` / manual asset download needed — the first time you click
**Synthesize**, the `supertonic` package automatically downloads the
Supertonic 3 model (~400MB) into `gui-app/model/` and reuses it on every run
after that.

## Usage

1. Click **Browse...** next to "Input file" and pick a `.txt`, `.md`, or any
   plain-text file.
2. Choose a language (`en`, `ko`, `na` for language-agnostic, etc. — 31
   languages supported), a voice style (`F1`–`F5`), quality
   (5 = fast/low quality, 12 = slow/high quality, default 12), and speed
   (0.7 = slow, 2.0 = fast, default 1.5).
3. Optionally change the output folder (defaults to `gui-app/output/`).
4. Click **Synthesize**. The window stays responsive while it runs. When
   done, the WAV file is saved as `<input filename>.wav` in the output
   folder.

If a `.md` file is selected, its markdown syntax (`#` headings, `**bold**`,
`[links](url)`, code fences, list bullets, etc.) is stripped to plain text
before synthesis, so the voice doesn't read out symbols like `#` or `**`
literally.
