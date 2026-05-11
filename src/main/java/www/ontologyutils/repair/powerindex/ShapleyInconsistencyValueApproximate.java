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

