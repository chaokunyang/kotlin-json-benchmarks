#!/usr/bin/env python3
"""Generate Kotlin JSON benchmark charts from JMH JSON results."""

from __future__ import annotations

import argparse
import json
import math
import re
from pathlib import Path
from typing import Any

import matplotlib.pyplot as plt
import numpy as np
from matplotlib.ticker import FuncFormatter, MaxNLocator

SERIALIZERS = ("fory", "kotlinx", "moshi", "jackson")
OPERATIONS = ("to", "from")
REPRESENTATIONS = ("string", "bytes")
LABELS = {
    "fory": "Fory JSON Kotlin",
    "kotlinx": "kotlinx.serialization",
    "moshi": "Moshi",
    "jackson": "Jackson Kotlin",
}
COLORS = {
    "fory": "#FF6F01",
    "kotlinx": "#7F52FF",
    "moshi": "#8C6D8A",
    "jackson": "#55BCC2",
}
CASE_LABELS = {"to": "Serialize", "from": "Deserialize"}
BENCHMARK_PATTERN = re.compile(
    r"(?:^|[.])(?P<serializer>fory|kotlinx|moshi|jackson)"
    r"(?P<operation>To|From)Json(?P<representation>Bytes|String)$"
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--json-file",
        default="results/benchmark_results.json",
        help="JMH JSON result file",
    )
    parser.add_argument(
        "--output-dir",
        default="results",
        help="Directory for generated charts",
    )
    parser.add_argument("--payload", choices=("users", "clients"),
                        help="Required when plotting the Users/Clients suite")
    parser.add_argument("--size-kb", type=int, default=1000,
                        help="Select the Users/Clients size parameter (default: 1000)")
    return parser.parse_args()


def load_results(path: Path) -> list[dict[str, Any]]:
    with path.open("r", encoding="utf-8") as source:
        payload = json.load(source)
    if not isinstance(payload, list):
        raise TypeError(f"Expected a JMH benchmark list in {path}")
    return payload


def ops_per_second(value: float, unit: str) -> float:
    multipliers = {
        "ops/s": 1,
        "ops/ms": 1_000,
        "ops/us": 1_000_000,
        "ops/ns": 1_000_000_000,
    }
    try:
        return value * multipliers[unit]
    except KeyError as error:
        raise ValueError(f"Unsupported JMH throughput unit: {unit}") from error


def collect_results(
    benchmarks: list[dict[str, Any]],
    payload: str | None = None,
    size_kb: int = 1000,
) -> dict[tuple[str, str], dict[str, tuple[float, float]]]:
    results = {
        (operation, representation): {}
        for representation in REPRESENTATIONS
        for operation in OPERATIONS
    }
    for benchmark in benchmarks:
        match = BENCHMARK_PATTERN.search(benchmark.get("benchmark", ""))
        if match is None:
            continue
        params = benchmark.get("params", {})
        if payload is None and "payload" in params:
            raise ValueError("Use --payload to select Users or Clients results")
        if payload is not None and (
            params.get("payload") != payload or str(params.get("sizeKb")) != str(size_kb)
        ):
            continue
        case = (
            match.group("operation").lower(),
            match.group("representation").lower(),
        )
        metric = benchmark["primaryMetric"]
        unit = metric["scoreUnit"]
        score = ops_per_second(float(metric["score"]), unit)
        error = ops_per_second(float(metric.get("scoreError", 0.0)), unit)
        if not math.isfinite(error):
            error = 0.0
        serializer = match.group("serializer")
        if serializer in results[case]:
            raise ValueError(f"Duplicate JMH result for {serializer} {case}")
        if not math.isfinite(score) or score <= 0:
            raise ValueError(f"Invalid throughput for {serializer} {case}: {score}")
        results[case][serializer] = (score, error)

    missing = [
        f"{serializer}{operation.title()}Json{representation.title()}"
        for representation in REPRESENTATIONS
        for operation in OPERATIONS
        for serializer in SERIALIZERS
        if serializer not in results[(operation, representation)]
    ]
    if missing:
        raise ValueError("Missing JMH benchmark results: " + ", ".join(missing))
    return results


def format_throughput(value: float, _position: float | None = None) -> str:
    if value >= 1_000_000:
        return f"{value / 1_000_000:.2f}".rstrip("0").rstrip(".") + "M"
    if value >= 1_000:
        return f"{value / 1_000:.2f}".rstrip("0").rstrip(".") + "K"
    return f"{value:.0f}"


def style_axis(axis: Any) -> None:
    axis.set_axisbelow(True)
    axis.grid(True, axis="y", color="#D9DEE7", linewidth=0.7)
    axis.grid(False, axis="x")
    axis.yaxis.set_major_locator(MaxNLocator(nbins=5, min_n_ticks=3))
    axis.yaxis.set_major_formatter(FuncFormatter(format_throughput))
    axis.tick_params(axis="both", width=0.8, length=3)
    for spine in axis.spines.values():
        spine.set_color("#8A939E")
        spine.set_linewidth(0.8)


def render_plot(
    results: dict[tuple[str, str], dict[str, tuple[float, float]]],
    representation: str,
    output: Path,
    model_title: str = "",
) -> None:
    figure, axes = plt.subplots(1, 2, figsize=(12.5, 5.2))
    x = np.arange(len(SERIALIZERS), dtype=float)

    for axis, operation in zip(axes, OPERATIONS):
        values = [results[(operation, representation)][name][0] for name in SERIALIZERS]
        errors = [results[(operation, representation)][name][1] for name in SERIALIZERS]
        bars = axis.bar(
            x,
            values,
            width=0.62,
            yerr=errors,
            capsize=2.5,
            color=[COLORS[name] for name in SERIALIZERS],
            edgecolor="white",
            linewidth=0.8,
        )
        axis.bar_label(
            bars,
            labels=[format_throughput(value) for value in values],
            padding=3,
            fontsize=8,
        )
        axis.set_ylim(0, max(value + error for value, error in zip(values, errors)) * 1.18)
        axis.set_xticks(x)
        axis.set_xticklabels([LABELS[name] for name in SERIALIZERS], rotation=12)
        axis.set_title(CASE_LABELS[operation], pad=10)
        style_axis(axis)

    axes[0].set_ylabel("Throughput (ops/sec)")
    representation_title = "String" if representation == "string" else "UTF-8 Bytes"
    figure.suptitle(
        f"Kotlin JSON {model_title}{representation_title} Serialization and Deserialization Throughput",
        y=0.98,
    )
    # Resolve formatter-dependent tick widths before calculating export margins.
    figure.canvas.draw()
    figure.tight_layout(rect=[0, 0, 1, 0.95], w_pad=2.4)
    figure.savefig(output, dpi=170, bbox_inches="tight", pad_inches=0.12)
    plt.close(figure)


def main() -> None:
    args = parse_args()
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    results = collect_results(load_results(Path(args.json_file)), args.payload, args.size_kb)
    prefix = f"{args.payload}_" if args.payload else ""
    title = f"{args.payload.title()} ({args.size_kb} KB) — " if args.payload else ""
    string_chart = output_dir / f"{prefix}string_throughput.png"
    bytes_chart = output_dir / f"{prefix}utf8_bytes_throughput.png"
    render_plot(results, "string", string_chart, title)
    render_plot(results, "bytes", bytes_chart, title)
    print(f"Generated {string_chart}")
    print(f"Generated {bytes_chart}")


if __name__ == "__main__":
    main()
