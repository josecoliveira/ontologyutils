"""Simple IIC analysis by confidence interval.

Reads CSVs from analysis/data, treats each header as a comparison, and produces
one table with per-ontology rows plus one overall row.

Decision rule:
Reject H0 if the lower bound of the 95% confidence interval is greater than 0.5.
"""

from __future__ import annotations

import glob
import math
import os
import re
from typing import Tuple

import numpy as np
import pandas as pd
from scipy import stats


DATA_DIR = os.path.join(os.path.dirname(__file__), "data/shapley-shapley")
OUT_DIR = os.path.join(os.path.dirname(__file__), "output/shapley-shapley")
REPORT_PATH = os.path.join(OUT_DIR, "iic_report.md")
CSV_OUT_PATH = os.path.join(OUT_DIR, "iic_summary.csv")
COMPARISON_ORDER = [
    ("iic_power_vs_random", "vs random"),
    ("iic_power_vs_not_in_largest_mcs", "vs non_in_largest_mcs"),
    ("iic_power_vs_weakening", "vs weakening"),
]


def strip_trailing_run_id(name: str) -> str:
    return re.sub(r"-\d+$", "", name)


def mean_std_n_ci(arr: np.ndarray, conf: float = 0.95) -> Tuple[float, float, int, float, float]:
    arr = np.asarray(arr, dtype=np.float64)
    arr = arr[~np.isnan(arr)]
    n = arr.size
    if n == 0:
        return float("nan"), float("nan"), 0, float("nan"), float("nan")

    mean = float(np.mean(arr))
    std = float(np.std(arr, ddof=1)) if n > 1 else 0.0
    if n == 1:
        return mean, std, 1, mean, mean

    se = std / math.sqrt(n)
    alpha = 1.0 - conf
    tcrit = stats.t.ppf(1.0 - alpha / 2.0, n - 1)
    ci_low = mean - tcrit * se
    ci_high = mean + tcrit * se
    return mean, std, n, ci_low, ci_high


def read_comparison_rows(path: str) -> list[dict]:
    ontology = strip_trailing_run_id(os.path.splitext(os.path.basename(path))[0])
    df = pd.read_csv(path)
    if df.empty:
        return []

    rows = []
    for column in df.columns:
        values = pd.to_numeric(df[column], errors="coerce").dropna().to_numpy(dtype=float)
        for value in values:
            rows.append({"ontology": ontology, "comparison": str(column), "value": float(value)})
    return rows


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    csv_paths = sorted(glob.glob(os.path.join(DATA_DIR, "*.csv")))
    if not csv_paths:
        raise SystemExit(f"No CSV files found in {DATA_DIR}")

    rows = []
    for path in csv_paths:
        rows.extend(read_comparison_rows(path))

    if not rows:
        raise SystemExit("No numeric data parsed from the CSV files.")

    df = pd.DataFrame(rows)

    summary_rows = []
    for (ontology, comparison), group in df.groupby(["ontology", "comparison"], sort=True):
        values = group["value"].to_numpy(dtype=float)
        mean, std, n, ci_low, ci_high = mean_std_n_ci(values)
        summary_rows.append({
            "ontology": ontology,
            "comparison": comparison,
            "mean": mean,
            "sd": std,
            "n": n,
            "ci_low": ci_low,
            "ci_high": ci_high,
            "reject_h0": bool(ci_low > 0.5),
        })
        print(f"{ontology} & {mean:.2f} [{ci_low:.2f}; {ci_high:.2f}]")

    for comparison_key, _ in COMPARISON_ORDER:
        comp_values = df.loc[df["comparison"] == comparison_key, "value"].to_numpy(dtype=float)
        overall_mean, overall_sd, overall_n, overall_ci_low, overall_ci_high = mean_std_n_ci(comp_values)
        summary_rows.append({
            "ontology": "overall",
            "comparison": comparison_key,
            "mean": overall_mean,
            "sd": overall_sd,
            "n": overall_n,
            "ci_low": overall_ci_low,
            "ci_high": overall_ci_high,
            "reject_h0": bool(overall_ci_low > 0.5),
        })

    summary_df = pd.DataFrame(summary_rows)
    summary_df.to_csv(CSV_OUT_PATH, index=False)

    report_lines = [
        "# IIC Summary",
        "",
        "Decision rule: reject H0 when the lower bound of the 95% CI is greater than 0.5.",
        "",
    ]

    for comparison_key, label in COMPARISON_ORDER:
        table_df = summary_df[summary_df["comparison"] == comparison_key].copy()
        if table_df.empty:
            continue
        table_df["comparison"] = label
        table_df.loc[table_df["ontology"] == "overall", "ontology"] = "overall"
        report_lines.append(f"## {label}")
        report_lines.append(table_df.to_markdown(index=False))
        report_lines.append("")

    with open(REPORT_PATH, "w", encoding="utf-8") as handle:
        handle.write("\n".join(report_lines))

    print("Wrote:", CSV_OUT_PATH, REPORT_PATH)

    # Build a LaTeX table with four columns: ontology, Removal, MCS, Weakening.
    # Map comparison keys to column headings.
    comp_to_col = {
        "iic_power_vs_random": "Removal",
        "iic_power_vs_not_in_largest_mcs": "MCS",
        "iic_power_vs_weakening": "Weakening",
    }

    # Preserve ordering and ensure 'overall' is last
    ontologies = sorted([o for o in summary_df["ontology"].unique() if o != "overall"]) 
    if "overall" in summary_df["ontology"].unique():
        ontologies.append("overall")

    tex_lines = [
        "% Auto-generated IIC summary table",
        "\\begin{table}[ht]",
        "  \\centering",
        "  \\caption{IIC results: mean and 95\\% confidence intervals}",
        "  \\begin{tabular}{lccc}",
        "    \\toprule",
        "    Ontology name & Removal & MCS & Weakening \\\\",
        "    \\midrule",
    ]

    def fmt_cell(row: pd.Series) -> str:
        try:
            mean = float(row["mean"])
            lo = float(row["ci_low"])
            hi = float(row["ci_high"])
            return f"{mean:.2f} [{lo:.2f}; {hi:.2f}]"
        except Exception:
            return ""

    for ont in ontologies:
        cells = []
        for comp_key, _label in COMPARISON_ORDER:
            match = summary_df[(summary_df["ontology"] == ont) & (summary_df["comparison"] == comp_key)]
            if not match.empty:
                cells.append(fmt_cell(match.iloc[0]))
            else:
                cells.append("")
        tex_lines.append(f"    {ont} & {cells[0]} & {cells[1]} & {cells[2]} \\\\")

    tex_lines.extend([
        "    \\bottomrule",
        "  \\end{tabular}",
        "\\end{table}",
    ])

    tex_path = os.path.join(OUT_DIR, "iic_summary.tex")
    with open(tex_path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(tex_lines))
    print("Wrote LaTeX table:", tex_path)


if __name__ == "__main__":
    main()
