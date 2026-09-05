"""Simple desktop GUI for Supertonic TTS.

Pick a .txt/.md/any text file, choose voice/language/quality/speed,
and synthesize it to a WAV file in an output folder.
"""

import re
import threading
import tkinter as tk
from pathlib import Path
from tkinter import filedialog, ttk

import numpy as np

AVAILABLE_LANGS = [
    "en", "ko", "ja", "ar", "bg", "cs", "da", "de", "el", "es", "et", "fi",
    "fr", "hi", "hr", "hu", "id", "it", "lt", "lv", "nl", "pl", "pt", "ro",
    "ru", "sk", "sl", "sv", "tr", "uk", "vi", "na",
]
VOICE_STYLES = ["F1", "F2", "F3", "F4", "F5"]
DEFAULT_OUTPUT_DIR = Path(__file__).parent / "output"
MODEL_DIR = Path(__file__).parent / "model"


def strip_markdown(text: str) -> str:
    """Strip common markdown syntax so it isn't read aloud literally."""
    # Code fences: keep inner text, drop the ``` markers and language tag
    text = re.sub(r"```[^\n]*\n(.*?)```", r"\1", text, flags=re.DOTALL)
    # Inline code: keep inner text, drop backticks
    text = re.sub(r"`([^`]+)`", r"\1", text)
    # Images: drop entirely (alt text is usually not meaningful when spoken)
    text = re.sub(r"!\[[^\]]*\]\([^)]*\)", "", text)
    # Links: keep the label, drop the URL
    text = re.sub(r"\[([^\]]+)\]\([^)]*\)", r"\1", text)
    # Headings: strip leading #'s
    text = re.sub(r"^\s{0,3}#{1,6}\s+", "", text, flags=re.MULTILINE)
    # List bullets: strip leading -, *, +, or "1." markers
    text = re.sub(r"^\s*([-*+]|\d+\.)\s+", "", text, flags=re.MULTILINE)
    # Emphasis markers: **bold**, *italic*, __bold__, _italic_, ~~strike~~
    text = re.sub(r"(\*\*|__)(.+?)\1", r"\2", text)
    text = re.sub(r"(\*|_|~~)(.+?)\1", r"\2", text)
    # Blockquote markers
    text = re.sub(r"^\s*>\s?", "", text, flags=re.MULTILINE)
    # Collapse runs of blank lines
    text = re.sub(r"\n\s*\n+", "\n\n", text)
    return text.strip()


def read_input_file(path: Path) -> str:
    text = path.read_text(encoding="utf-8", errors="replace")
    if path.suffix.lower() == ".md":
        text = strip_markdown(text)
    text = text.strip()
    if not text:
        raise ValueError("Selected file is empty (no readable text found).")
    return text


class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Supertonic TTS")
        self.resizable(False, False)

        self.input_path_var = tk.StringVar()
        self.lang_var = tk.StringVar(value="en")
        self.voice_var = tk.StringVar(value="F1")
        self.steps_var = tk.StringVar(value="12")
        self.speed_var = tk.StringVar(value="1.5")
        self.output_dir_var = tk.StringVar(value=str(DEFAULT_OUTPUT_DIR))
        self.status_var = tk.StringVar(value="Ready.")

        self._build_widgets()

    def _build_widgets(self):
        pad = {"padx": 8, "pady": 6}

        row = 0
        ttk.Label(self, text="Input file:").grid(row=row, column=0, sticky="w", **pad)
        ttk.Entry(self, textvariable=self.input_path_var, width=45, state="readonly").grid(
            row=row, column=1, **pad
        )
        ttk.Button(self, text="Browse...", command=self.browse_input).grid(
            row=row, column=2, **pad
        )

        row += 1
        ttk.Label(self, text="Language:").grid(row=row, column=0, sticky="w", **pad)
        ttk.Combobox(
            self, textvariable=self.lang_var, values=AVAILABLE_LANGS, width=10, state="readonly"
        ).grid(row=row, column=1, sticky="w", **pad)

        row += 1
        ttk.Label(self, text="Voice style:").grid(row=row, column=0, sticky="w", **pad)
        ttk.Combobox(
            self, textvariable=self.voice_var, values=VOICE_STYLES, width=10, state="readonly"
        ).grid(row=row, column=1, sticky="w", **pad)

        row += 1
        ttk.Label(self, text="Quality (5-12):").grid(row=row, column=0, sticky="w", **pad)
        ttk.Spinbox(self, from_=5, to=12, textvariable=self.steps_var, width=8).grid(
            row=row, column=1, sticky="w", **pad
        )

        row += 1
        ttk.Label(self, text="Speed (0.7-2.0):").grid(row=row, column=0, sticky="w", **pad)
        ttk.Spinbox(
            self, from_=0.7, to=2.0, increment=0.05, textvariable=self.speed_var, width=8
        ).grid(row=row, column=1, sticky="w", **pad)

        row += 1
        ttk.Label(self, text="Output folder:").grid(row=row, column=0, sticky="w", **pad)
        ttk.Entry(self, textvariable=self.output_dir_var, width=45).grid(row=row, column=1, **pad)
        ttk.Button(self, text="Browse...", command=self.browse_output).grid(
            row=row, column=2, **pad
        )

        row += 1
        self.synthesize_btn = ttk.Button(self, text="Synthesize", command=self.on_synthesize)
        self.synthesize_btn.grid(row=row, column=0, columnspan=3, pady=(10, 4))

        row += 1
        self.progress = ttk.Progressbar(self, mode="indeterminate", length=300)
        self.progress.grid(row=row, column=0, columnspan=3, padx=8, pady=(0, 4))
        self.progress.grid_remove()

        row += 1
        self.status_label = ttk.Label(self, textvariable=self.status_var, wraplength=380)
        self.status_label.grid(row=row, column=0, columnspan=3, padx=8, pady=(0, 10))

    def browse_input(self):
        path = filedialog.askopenfilename(
            title="Select a text file",
            filetypes=[("Text/Markdown", "*.txt *.md"), ("All files", "*.*")],
        )
        if path:
            self.input_path_var.set(path)

    def browse_output(self):
        path = filedialog.askdirectory(title="Select output folder")
        if path:
            self.output_dir_var.set(path)

    def on_synthesize(self):
        input_path = self.input_path_var.get().strip()
        if not input_path:
            self.status_var.set("Error: please select an input file first.")
            self.status_label.config(foreground="red")
            return
        input_path = Path(input_path)
        if not input_path.exists():
            self.status_var.set("Error: input file no longer exists.")
            self.status_label.config(foreground="red")
            return

        output_dir = Path(self.output_dir_var.get().strip() or DEFAULT_OUTPUT_DIR)
        try:
            output_dir.mkdir(parents=True, exist_ok=True)
        except OSError as e:
            self.status_var.set(f"Error: cannot create output folder ({e}).")
            self.status_label.config(foreground="red")
            return

        lang = self.lang_var.get()
        voice = self.voice_var.get()
        try:
            steps = int(self.steps_var.get())
            speed = float(self.speed_var.get())
        except ValueError:
            self.status_var.set("Error: quality and speed must be numbers.")
            self.status_label.config(foreground="red")
            return

        self.synthesize_btn.config(state="disabled")
        self.status_label.config(foreground="black")
        self.status_var.set("Loading model (first run may download ~400MB)...")
        self.progress.grid()
        self.progress.config(mode="indeterminate")
        self.progress.start(10)

        thread = threading.Thread(
            target=self._worker,
            args=(input_path, lang, voice, steps, speed, output_dir),
            daemon=True,
        )
        thread.start()

    def _worker(self, input_path: Path, lang: str, voice: str, steps: int, speed: float, output_dir: Path):
        try:
            text = read_input_file(input_path)

            from supertonic import TTS
            from supertonic.utils import chunk_text

            tts = TTS(model_dir=MODEL_DIR, auto_download=True)
            style = tts.get_voice_style(voice_name=voice)

            # Chunk ourselves (rather than handing the whole text to tts.synthesize()
            # in one opaque call) so we can report real per-chunk progress — the
            # package exposes no per-denoising-step callback, only a verbose stdout flag.
            max_len = 120 if lang in ("ko", "ja") else 300
            chunks = chunk_text(text, max_len)
            total_chunks = len(chunks)

            self.after(0, self._start_chunk_progress, total_chunks)

            silence_duration = 0.3
            silence = np.zeros((1, int(silence_duration * tts.sample_rate)), dtype=np.float32)
            wav_cat = None
            dur_total = 0.0
            for i, chunk in enumerate(chunks):
                wav, duration = tts.synthesize(
                    text=chunk,
                    voice_style=style,
                    lang=lang,
                    total_steps=steps,
                    speed=speed,
                )
                dur_value = float(duration[0]) if hasattr(duration, "__len__") else float(duration)
                if wav_cat is None:
                    wav_cat = wav
                    dur_total = dur_value
                else:
                    wav_cat = np.concatenate([wav_cat, silence, wav], axis=1)
                    dur_total += dur_value + silence_duration
                self.after(0, self._update_chunk_progress, i + 1, total_chunks)

            out_path = output_dir / f"{input_path.stem}.wav"
            tts.save_audio(wav_cat, str(out_path))

            self.after(0, self._on_done, out_path, dur_total)
        except Exception as e:
            self.after(0, self._on_error, str(e))

    def _start_chunk_progress(self, total_chunks: int):
        self.progress.stop()
        self.progress.config(mode="determinate", maximum=total_chunks, value=0)
        self.status_var.set(f"Synthesizing chunk 0/{total_chunks}...")

    def _update_chunk_progress(self, done: int, total_chunks: int):
        self.progress.config(value=done)
        self.status_var.set(f"Synthesizing chunk {done}/{total_chunks}...")

    def _on_done(self, out_path: Path, duration: float):
        self.progress.stop()
        self.progress.grid_remove()
        self.synthesize_btn.config(state="normal")
        self.status_label.config(foreground="green")
        self.status_var.set(f"Done. Saved to {out_path} ({duration:.2f}s of audio).")

    def _on_error(self, message: str):
        self.progress.stop()
        self.progress.grid_remove()
        self.synthesize_btn.config(state="normal")
        self.status_label.config(foreground="red")
        self.status_var.set(f"Error: {message}")


def main():
    App().mainloop()


if __name__ == "__main__":
    main()
