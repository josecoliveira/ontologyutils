import os
import glob
import pandas as pd
from scipy.stats import wilcoxon, ttest_1samp
from statsmodels.stats.multitest import multipletests


# Directory containing the CSV files (now inside analysis/data)
DATA_DIR = os.path.join(os.path.dirname(__file__), 'data')

# Find all CSV files in analysis/data
csv_files = sorted(glob.glob(os.path.join(DATA_DIR, '*.csv')))

results = []

for csv_file in csv_files:
    # Read only the first column
    data = pd.read_csv(csv_file, header=None, usecols=[0]).iloc[:, 0].values
    # Wilcoxon signed-rank test (one-sided, alternative: median > 0.5)
    try:
        w_stat, w_p = wilcoxon(data - 0.5, alternative='greater', zero_method='wilcox')
    except ValueError:
        w_stat, w_p = float('nan'), float('nan')
    # One-sample t-test (one-sided, alternative: mean > 0.5)
    t_stat, t_p_two_sided = ttest_1samp(data, 0.5)
    t_p = t_p_two_sided / 2 if t_stat > 0 else 1.0  # one-sided p-value
    results.append({
        'file': os.path.basename(csv_file),
        'test': 'Wilcoxon',
        'statistic': w_stat,
        'pvalue': w_p
    })
    results.append({
        'file': os.path.basename(csv_file),
        'test': 'T-test',
        'statistic': t_stat,
        'pvalue': t_p
    })

# Holm-Bonferroni correction
pvals = [r['pvalue'] for r in results]
reject, pvals_corrected, _, _ = multipletests(pvals, alpha=0.05, method='holm')

print(f"{'File':30} {'Test':10} {'Stat':>10} {'p-value':>10} {'p-corr':>10} {'Reject H0':>10}")
for i, r in enumerate(results):
    print(f"{r['file']:30} {r['test']:10} {r['statistic']:10.4g} {r['pvalue']:10.4g} {pvals_corrected[i]:10.4g} {str(reject[i]):>10}")
