package www.ontologyutils.repair.powerindex;

import java.util.*;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAxiom;

import www.ontologyutils.toolbox.Utils;

/**
 * Computes an approximate Shapley inconsistency value for an axiom using Monte
 * Carlo sampling. This trades precision for speed by sampling permutations
 * instead of enumerating all subsets.
 */
public class ShapleyInconsistencyValueApproximate implements PowerIndex {
    private static final int DEFAULT_APPROXIMATION_SAMPLES = 735;
    private static final long DEFAULT_APPROXIMATION_SEED = 13L;

    private final Map<Set<OWLAxiom>, Integer> drasticCache;
    private final int approximationSamples;
    private final long approximationSeed;

    /**
     * Creates a new approximate Shapley inconsistency value computer with default
     * parameters (4096 samples, seed 13).
     */
    public ShapleyInconsistencyValueApproximate() {
        this(DEFAULT_APPROXIMATION_SAMPLES, DEFAULT_APPROXIMATION_SEED);
    }

    /**
     * Creates a new approximate Shapley inconsistency value computer with custom
     * sampling parameters.
     *
     * @param approximationSamples
     *            The number of permutations to sample (must be positive).
     * @param approximationSeed
     *            The base seed for deterministic sampling.
     */
    public ShapleyInconsistencyValueApproximate(int approximationSamples, long approximationSeed) {
        if (approximationSamples <= 0) {
            throw new IllegalArgumentException("Approximation samples must be positive: " + approximationSamples);
        }
        this.drasticCache = new HashMap<>();
        this.approximationSamples = approximationSamples;
        this.approximationSeed = approximationSeed;
    }

    @Override
    public double computeScore(Set<OWLAxiom> axioms, OWLAxiom targetAxiom) {
        Set<OWLAxiom> universe = new HashSet<>(axioms);
        universe.add(targetAxiom);
        return approximateShapleyInconsistencyValue(universe, targetAxiom);
    }

    /**
     * Batch scoring for multiple targets. This shares permutations across all
     * targets while preserving the semantics that each target is evaluated in
     * the context of {@code axioms} (i.e. scoring each target as if it were
     * added alone to the base set).
     */
    public Map<OWLAxiom, Double> computeScores(Set<OWLAxiom> axioms, Set<OWLAxiom> targets) {
        if (targets == null || targets.isEmpty()) {
            return Map.of();
        }

        Set<OWLAxiom> universe = new HashSet<>(axioms);
        universe.addAll(targets);

        // deterministic ordering
        List<OWLAxiom> permutation = universe.stream().sorted().collect(Collectors.toCollection(ArrayList::new));

        Map<OWLAxiom, Double> totals = new HashMap<>();
        for (OWLAxiom t : targets) {
            totals.put(t, 0.0d);
        }

        Random random = new Random(seedFor(universe, targets));

        for (int i = 0; i < approximationSamples; i++) {
            Collections.shuffle(permutation, random);

            Set<OWLAxiom> prefixBase = new HashSet<>();
            for (OWLAxiom current : permutation) {
                if (targets.contains(current)) {
                    Set<OWLAxiom> prefixWith = new HashSet<>(prefixBase);
                    prefixWith.add(current);
                    double marginal = drasticInconsistencyValue(prefixWith) - drasticInconsistencyValue(prefixBase);
                    totals.merge(current, marginal, Double::sum);
                } else {
                    prefixBase.add(current);
                }
            }
        }

        // average
        Map<OWLAxiom, Double> averaged = new HashMap<>();
        for (Map.Entry<OWLAxiom, Double> e : totals.entrySet()) {
            averaged.put(e.getKey(), e.getValue() / approximationSamples);
        }
        return averaged;
    }

    /**
     * Adaptive scoring that samples in chunks and stops when the ranking of
     * targets stabilizes (or when maxSamples is reached).
     *
     * This returns the latest averaged scores for all targets.
     */
    public Map<OWLAxiom, Double> computeScoresAdaptive(Set<OWLAxiom> axioms, Set<OWLAxiom> targets,
            int chunkSize, int maxSamples) {
        return computeScoresAdaptive(axioms, targets, chunkSize, maxSamples, true);
    }

    /**
     * Adaptive scoring with explicit ranking direction.
     *
     * @param ascending
     *            true for smaller scores being better (weakenings), false for
     *            larger scores being better (bad axioms).
     */
    public Map<OWLAxiom, Double> computeScoresAdaptive(Set<OWLAxiom> axioms, Set<OWLAxiom> targets,
            int chunkSize, int maxSamples, boolean ascending) {
        if (targets == null || targets.isEmpty()) {
            return Map.of();
        }
        if (chunkSize <= 0 || maxSamples <= 0) {
            throw new IllegalArgumentException("chunkSize and maxSamples must be positive");
        }

        Set<OWLAxiom> universe = new HashSet<>(axioms);
        universe.addAll(targets);
        List<OWLAxiom> permutation = universe.stream().sorted().collect(Collectors.toCollection(ArrayList::new));

        Map<OWLAxiom, Double> totals = new HashMap<>();
        for (OWLAxiom t : targets) {
            totals.put(t, 0.0d);
        }

        Random random = new Random(seedFor(universe, targets));
        int used = 0;
        List<OWLAxiom> previousRanking = null;

        while (used < maxSamples) {
            int batch = Math.min(chunkSize, maxSamples - used);
            for (int i = 0; i < batch; i++) {
                Collections.shuffle(permutation, random);

                Set<OWLAxiom> prefixBase = new HashSet<>();
                for (OWLAxiom current : permutation) {
                    if (targets.contains(current)) {
                        Set<OWLAxiom> prefixWith = new HashSet<>(prefixBase);
                        prefixWith.add(current);
                        double marginal = drasticInconsistencyValue(prefixWith) - drasticInconsistencyValue(prefixBase);
                        totals.merge(current, marginal, Double::sum);
                    } else {
                        prefixBase.add(current);
                    }
                }
            }
            used += batch;

            // compute averaged scores so far
            Map<OWLAxiom, Double> averaged = new HashMap<>();
            for (Map.Entry<OWLAxiom, Double> e : totals.entrySet()) {
                averaged.put(e.getKey(), e.getValue() / (double) used);
            }

            // compute ranking using the requested direction.
            Comparator<Map.Entry<OWLAxiom, Double>> comparator = Comparator
                    .<Map.Entry<OWLAxiom, Double>>comparingDouble(Map.Entry::getValue)
                    .thenComparing(e -> Utils.prettyPrintAxiomDL(e.getKey()));
            if (!ascending) {
                comparator = comparator.reversed();
            }
            List<OWLAxiom> ranking = averaged.entrySet().stream()
                    .sorted(comparator)
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (previousRanking != null && ranking.equals(previousRanking)) {
                return averaged;
            }
            previousRanking = ranking;
        }

        // final averaged
        Map<OWLAxiom, Double> finalAveraged = new HashMap<>();
        for (Map.Entry<OWLAxiom, Double> e : totals.entrySet()) {
            finalAveraged.put(e.getKey(), e.getValue() / (double) used);
        }
        return finalAveraged;
    }

    private double approximateShapleyInconsistencyValue(Set<OWLAxiom> universe, OWLAxiom axiom) {
        // Sort once so seeded sampling is deterministic across JVM runs.
        List<OWLAxiom> permutation = universe.stream().sorted().collect(Collectors.toCollection(ArrayList::new));
        double totalMarginal = 0.0d;
        Random random = new Random(seedFor(universe, axiom));

        for (int i = 0; i < approximationSamples; i++) {
            Collections.shuffle(permutation, random);

            Set<OWLAxiom> prefix = new HashSet<>();
            for (OWLAxiom current : permutation) {
                if (current.equals(axiom)) {
                    Set<OWLAxiom> prefixWithAxiom = new HashSet<>(prefix);
                    prefixWithAxiom.add(axiom);
                    totalMarginal += drasticInconsistencyValue(prefixWithAxiom) - drasticInconsistencyValue(prefix);
                    break;
                }
                prefix.add(current);
            }
        }

        return totalMarginal / approximationSamples;
    }

    private long seedFor(Set<OWLAxiom> universe, OWLAxiom axiom) {
        long seed = approximationSeed;
        seed = 31L * seed + canonicalize(universe).hashCode();
        seed = 31L * seed + axiom.hashCode();
        return seed;
    }

    private long seedFor(Set<OWLAxiom> universe, Set<OWLAxiom> targets) {
        long seed = approximationSeed;
        seed = 31L * seed + canonicalize(universe).hashCode();
        seed = 31L * seed + canonicalize(targets).hashCode();
        return seed;
    }

    private static String canonicalize(Set<OWLAxiom> axioms) {
        return axioms.stream().sorted().map(Object::toString).collect(Collectors.joining("|"));
    }

    private int drasticInconsistencyValue(Set<OWLAxiom> subset) {
        Set<OWLAxiom> key = Set.copyOf(subset);
        Integer cached = drasticCache.get(key);
        if (cached != null) {
            return cached;
        }
        int value = Utils.isConsistent(subset) ? 0 : 1;
        drasticCache.put(key, value);
        return value;
    }
}

