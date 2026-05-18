# IIC Summary

Decision rule: reject H0 when the lower bound of the 95% CI is greater than 0.5.

## vs random
| ontology        | comparison   |     mean |        sd |   n |   ci_low |   ci_high | reject_h0   |
|:----------------|:-------------|---------:|----------:|----:|---------:|----------:|:------------|
| iic-bctt-ss     | vs random    | 0.940582 | 0.114527  | 100 | 0.917857 |  0.963306 | True        |
| iic-co-wheat-ss | vs random    | 0.989762 | 0.0703218 | 100 | 0.975809 |  1.00372  | True        |
| iic-hom-ss      | vs random    | 0.946358 | 0.0912326 | 100 | 0.928255 |  0.96446  | True        |
| iic-pe-ss       | vs random    | 0.973846 | 0.109267  | 100 | 0.952165 |  0.995527 | True        |
| iic-taxrank-ss  | vs random    | 0.991943 | 0.05005   | 100 | 0.982012 |  1.00187  | True        |
| overall         | vs random    | 0.968498 | 0.0925275 | 500 | 0.960368 |  0.976628 | True        |

## vs non_in_largest_mcs
| ontology        | comparison            |     mean |       sd |   n |   ci_low |   ci_high | reject_h0   |
|:----------------|:----------------------|---------:|---------:|----:|---------:|----------:|:------------|
| iic-bctt-ss     | vs non_in_largest_mcs | 0.903884 | 0.148584 | 100 | 0.874402 |  0.933366 | True        |
| iic-co-wheat-ss | vs non_in_largest_mcs | 0.619094 | 0.213    | 100 | 0.57683  |  0.661357 | True        |
| iic-hom-ss      | vs non_in_largest_mcs | 0.874173 | 0.169983 | 100 | 0.840444 |  0.907901 | True        |
| iic-pe-ss       | vs non_in_largest_mcs | 0.721921 | 0.246591 | 100 | 0.672992 |  0.77085  | True        |
| iic-taxrank-ss  | vs non_in_largest_mcs | 0.802881 | 0.277793 | 100 | 0.747761 |  0.858001 | True        |
| overall         | vs non_in_largest_mcs | 0.78439  | 0.239354 | 500 | 0.763359 |  0.805421 | True        |

## vs weakening
| ontology        | comparison   |     mean |       sd |   n |   ci_low |   ci_high | reject_h0   |
|:----------------|:-------------|---------:|---------:|----:|---------:|----------:|:------------|
| iic-bctt-ss     | vs weakening | 0.713511 | 0.275457 | 100 | 0.658854 |  0.768168 | True        |
| iic-co-wheat-ss | vs weakening | 0.668516 | 0.235972 | 100 | 0.621694 |  0.715338 | True        |
| iic-hom-ss      | vs weakening | 0.823743 | 0.229913 | 100 | 0.778124 |  0.869363 | True        |
| iic-pe-ss       | vs weakening | 0.682764 | 0.239698 | 100 | 0.635203 |  0.730326 | True        |
| iic-taxrank-ss  | vs weakening | 0.840935 | 0.29428  | 100 | 0.782543 |  0.899326 | True        |
| overall         | vs weakening | 0.745894 | 0.265333 | 500 | 0.72258  |  0.769208 | True        |
