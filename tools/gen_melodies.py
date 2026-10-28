"""Original ambient loops for YogaApp. Not copied from any recording."""
import math
import struct
import wave
from pathlib import Path

SR = 22050
DUR = 16.0
N = int(SR * DUR)
OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "melodies"


def midi(note: int) -> float:
    return 440.0 * (2 ** ((note - 69) / 12.0))


def wrap_freq(freq: float) -> float:
    # Integer number of cycles over the loop so the join does not click.
    return round(freq * DUR) / DUR


def render(events, drones):
    buf = [0.0] * N
    for freq, vol in drones:
        f = wrap_freq(freq)
        for i in range(N):
            t = i / SR
            trem = 0.82 + 0.18 * math.sin(2 * math.pi * (4.0 / DUR) * t)
            buf[i] += math.sin(2 * math.pi * f * t) * vol * trem
            buf[i] += 0.25 * math.sin(2 * math.pi * f * 2 * t) * vol * trem
    for note, start, length, vol in events:
        f = midi(note)
        a = int(start * SR)
        b = min(N, a + int(length * SR))
        count = max(1, b - a)
        for i in range(a, b):
            local = (i - a) / count
            env = min(1.0, local / 0.08) * min(1.0, (1.0 - local) / 0.18)
            t = i / SR
            sample = math.sin(2 * math.pi * f * t)
            sample += 0.28 * math.sin(2 * math.pi * f * 2 * t)
            sample += 0.08 * math.sin(2 * math.pi * f * 3 * t)
            buf[i] += sample * env * vol
    peak = max(abs(s) for s in buf) or 1.0
    fade = int(0.03 * SR)
    pcm = bytearray()
    for i, s in enumerate(buf):
        edge = 1.0
        if i < fade:
            edge = i / fade
        elif i > N - fade:
            edge = (N - i) / fade
        v = max(-1.0, min(1.0, s / peak * 0.86 * edge))
        pcm += struct.pack("<h", int(v * 32767))
    return pcm


def write(name: str, pcm: bytes) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    with wave.open(str(path), "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm)
    print(path, len(pcm))


def main() -> None:
    # C major pentatonic, slow.
    morning = [
        (64, 0.4, 1.1, 0.22),
        (67, 1.6, 1.1, 0.2),
        (69, 2.8, 1.4, 0.22),
        (67, 4.4, 1.0, 0.18),
        (64, 5.6, 1.2, 0.2),
        (62, 7.0, 1.3, 0.18),
        (64, 8.6, 1.2, 0.22),
        (67, 10.0, 1.4, 0.2),
        (72, 11.6, 1.2, 0.16),
        (69, 13.0, 1.6, 0.18),
        (64, 14.6, 1.2, 0.16),
    ]
    write("morning.wav", render(morning, [(midi(48), 0.16), (midi(55), 0.1)]))

    # Long tones, breath-like swells.
    breath = [
        (60, 0.2, 3.6, 0.2),
        (64, 3.8, 3.6, 0.18),
        (67, 7.4, 3.8, 0.18),
        (64, 11.2, 4.2, 0.16),
    ]
    write("breath.wav", render(breath, [(midi(48), 0.2), (midi(55), 0.12)]))

    # Light arpeggio.
    river = []
    pattern = [72, 76, 79, 76, 74, 72, 67, 72]
    t = 0.2
    for step in range(28):
        river.append((pattern[step % len(pattern)], t, 0.48, 0.16))
        t += 0.55
    write("river.wav", render(river, [(midi(55), 0.12), (midi(48), 0.1)]))

    # A minor pentatonic, quieter.
    evening = [
        (69, 0.5, 1.6, 0.18),
        (72, 2.2, 1.4, 0.16),
        (74, 3.8, 1.6, 0.16),
        (76, 5.6, 1.8, 0.15),
        (74, 7.6, 1.4, 0.15),
        (72, 9.2, 1.5, 0.16),
        (69, 10.8, 1.8, 0.18),
        (65, 12.6, 1.6, 0.14),
        (69, 14.2, 1.5, 0.15),
    ]
    write("evening.wav", render(evening, [(midi(45), 0.18), (midi(52), 0.1)]))


if __name__ == "__main__":
    main()
