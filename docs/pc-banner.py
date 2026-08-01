#!/usr/bin/env python3
"""Generate the Parallel Collectors README banner (dark + light) as SVG."""

W, H = 1600, 560
M = 96                      # side margin
CY = 352                    # diagram centre line
LANE_DY = 56                # lane spacing
LABEL_Y = 250

SANS = "-apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif"
MONO = "'SF Mono', ui-monospace, Menlo, Monaco, 'Courier New', monospace"

THEMES = {
    "dark": dict(
        bg="#0D1117",
        title="#F2F6FD",
        text="#C7D3E4",
        muted="#7D8DA6",
        blue="#3B82F6",
        blue_fill="rgba(59,130,246,0.10)",
        blue_stroke="rgba(59,130,246,0.55)",
        blue_text="#BFD5FF",
        amber="#F59E0B",
        amber_fill="rgba(245,158,11,0.10)",
        amber_stroke="rgba(245,158,11,0.55)",
        amber_text="#FBD38D",
        rail="#28323F",
        chip_stroke="#212B3A",
    ),
    "light": dict(
        bg="#FFFFFF",
        title="#0B1220",
        text="#33415C",
        muted="#5B6B84",
        blue="#2563EB",
        blue_fill="rgba(37,99,235,0.07)",
        blue_stroke="rgba(37,99,235,0.40)",
        blue_text="#1D4ED8",
        amber="#D97706",
        amber_fill="rgba(217,119,6,0.09)",
        amber_stroke="rgba(217,119,6,0.42)",
        amber_text="#B45309",
        rail="#E4E9F0",
        chip_stroke="#E1E7EF",
    ),
}

# --- diagram geometry -------------------------------------------------------
EL_N, EL_S, EL_GAP = 5, 28, 48          # stream elements: count, size, pitch
EL_X0 = M
COLL_X, COLL_W, BOX_H = 346, 290, 84
LANE_X0, LANE_X1 = 720, 1080            # lane rail span
BAR_H = 36
BAR_W = (300, 210, 258)                 # deliberately unequal: tasks differ in duration
RES_X, RES_W = 1164, 340

CHIPS = ("parallel()", "parallelToStream()", "parallelBy()", "parallelToStreamBy()")
CHIP_FS, CHIP_H, CHIP_Y, CHIP_PAD, CHIP_GAP = 16, 36, 480, 16, 14

TAGLINE = "Concurrency for the Java Stream API — without the parallel-stream compromises."


def mono_w(text, fs):
    return len(text) * fs * 0.6


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def curve(x0, y0, x1, y1):
    """Horizontal-tangent S-curve between two points."""
    dx = (x1 - x0) * 0.5
    return f"M {x0} {y0} C {x0 + dx} {y0}, {x1 - dx} {y1}, {x1} {y1}"


def build(t):
    c = THEMES[t]
    o = []
    add = o.append

    add(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
        f'viewBox="0 0 {W} {H}" font-family="{SANS}">')
    add('<defs>')
    for name, col in (("ah-blue", c["blue"]), ("ah-amber", c["amber"])):
        add(f'<marker id="{name}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" '
            f'markerHeight="6" orient="auto"><path d="M0 0 L10 5 L0 10 z" fill="{col}"/></marker>')
    add('</defs>')
    add(f'<rect width="{W}" height="{H}" fill="{c["bg"]}"/>')

    # --- masthead ---
    add(f'<text x="{M}" y="108" font-size="58" font-weight="700" letter-spacing="-1.4" '
        f'fill="{c["title"]}">Parallel Collectors</text>')
    add(f'<text x="{M}" y="152" font-size="22" fill="{c["text"]}">{esc(TAGLINE)}</text>')

    # --- section labels ---
    labels = ((M, "STREAM"), (COLL_X, "PARALLEL COLLECTOR"),
              (LANE_X0, "CONCURRENT TASKS · VIRTUAL THREADS"), (RES_X, "SINGLE RESULT"))
    for x, s in labels:
        add(f'<text x="{x}" y="{LABEL_Y}" font-family="{MONO}" font-size="12" '
            f'letter-spacing="1.7" fill="{c["muted"]}">{s}</text>')

    # --- stream: elements riding a rail into the collector ---
    el_end = EL_X0 + (EL_N - 1) * EL_GAP + EL_S
    add(f'<line x1="{EL_X0}" y1="{CY}" x2="{COLL_X - 4}" y2="{CY}" stroke="{c["blue"]}" '
        f'stroke-width="2" opacity="0.55" marker-end="url(#ah-blue)"/>')
    for i in range(EL_N):
        add(f'<rect x="{EL_X0 + i * EL_GAP}" y="{CY - EL_S // 2}" width="{EL_S}" height="{EL_S}" '
            f'rx="8" fill="{c["blue"]}" opacity="{0.5 + i * 0.125:.3f}"/>')

    # --- collector box ---
    add(f'<rect x="{COLL_X}" y="{CY - BOX_H // 2}" width="{COLL_W}" height="{BOX_H}" rx="14" '
        f'fill="{c["blue_fill"]}" stroke="{c["blue_stroke"]}" stroke-width="1.5"/>')
    add(f'<text x="{COLL_X + COLL_W // 2}" y="{CY + 7}" text-anchor="middle" font-family="{MONO}" '
        f'font-size="19" fill="{c["blue_text"]}">collect(parallel(…))</text>')

    lanes = (CY - LANE_DY, CY, CY + LANE_DY)

    # --- fan-out ---
    for y in lanes:
        add(f'<path d="{curve(COLL_X + COLL_W, CY, LANE_X0 - 6, y)}" fill="none" '
            f'stroke="{c["blue"]}" stroke-width="2" opacity="0.55" marker-end="url(#ah-blue)"/>')

    # --- lanes: rail = the join barrier, bar = actual work ---
    for y, bw in zip(lanes, BAR_W):
        add(f'<line x1="{LANE_X0}" y1="{y}" x2="{LANE_X1}" y2="{y}" stroke="{c["rail"]}" '
            f'stroke-width="4" stroke-linecap="round"/>')
        add(f'<rect x="{LANE_X0}" y="{y - BAR_H // 2}" width="{bw}" height="{BAR_H}" rx="10" '
            f'fill="{c["amber"]}"/>')

    # --- fan-in: lanes converge, then a single arrow into the result ---
    join = RES_X - 44
    for y in lanes:
        add(f'<path d="{curve(LANE_X1, y, join, CY)}" fill="none" stroke="{c["amber"]}" '
            f'stroke-width="2" opacity="0.6"/>')
    add(f'<line x1="{join}" y1="{CY}" x2="{RES_X - 6}" y2="{CY}" stroke="{c["amber"]}" '
        f'stroke-width="2" opacity="0.6" marker-end="url(#ah-amber)"/>')

    # --- result box ---
    add(f'<rect x="{RES_X}" y="{CY - BOX_H // 2}" width="{RES_W}" height="{BOX_H}" rx="14" '
        f'fill="{c["amber_fill"]}" stroke="{c["amber_stroke"]}" stroke-width="1.5"/>')
    add(f'<text x="{RES_X + RES_W // 2}" y="{CY + 6}" text-anchor="middle" font-family="{MONO}" '
        f'font-size="17" fill="{c["amber_text"]}">{esc("CompletableFuture<List<R>>")}</text>')

    # --- API chips ---
    x = M
    for s in CHIPS:
        w = mono_w(s, CHIP_FS) + CHIP_PAD * 2
        add(f'<rect x="{x}" y="{CHIP_Y}" width="{w:.0f}" height="{CHIP_H}" rx="10" fill="none" '
            f'stroke="{c["chip_stroke"]}" stroke-width="1.5"/>')
        add(f'<text x="{x + w / 2:.0f}" y="{CHIP_Y + CHIP_H / 2 + 6:.0f}" text-anchor="middle" '
            f'font-family="{MONO}" font-size="{CHIP_FS}" fill="{c["muted"]}">{s}</text>')
        x += w + CHIP_GAP

    add('</svg>')
    return "\n".join(o)


if __name__ == "__main__":
    import sys
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    for t in THEMES:
        p = f"{out}/pc-{t}.svg"
        with open(p, "w") as f:
            f.write(build(t))
        print(p)
