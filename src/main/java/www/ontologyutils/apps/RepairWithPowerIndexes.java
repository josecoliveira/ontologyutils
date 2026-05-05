package www.ontologyutils.apps;

import java.util.*;

import www.ontologyutils.refinement.AxiomWeakener;
import www.ontologyutils.repair.*;
import www.ontologyutils.repair.OntologyRepairRemoval.BadAxiomStrategy;
import www.ontologyutils.repair.OntologyRepairWeakening.RefOntologyStrategy;
import www.ontologyutils.repair.powerindex.PowerIndex;
import www.ontologyutils.repair.powerindex.ShapleyInconsistencyValueApproximate;
import www.ontologyutils.repair.powerindex.ShapleyInconsistencyValueExact;
import www.ontologyutils.toolbox.Ontology;

/**
 * Repair the given ontology using power indexes (like Shapley values) to select
 * bad axioms and weaker replacements.
 */
public class RepairWithPowerIndexes extends RepairApp {
    private boolean coherence = false;
    private RefOntologyStrategy refOntologyStrategy = RefOntologyStrategy.ONE_MCS;
    private BadAxiomStrategy badAxiomStrategy = BadAxiomStrategy.IN_SOME_MUS;
    private int weakeningFlags = AxiomWeakener.FLAG_DEFAULT;
    private boolean enhanceRef = false;
    private String powerIndexType = "exact";

    @Override
    protected List<Option<?>> appOptions() {
        var options = new ArrayList<Option<?>>();
        options.addAll(super.appOptions());
        options.add(OptionType.FLAG.create("coherence", b -> coherence = true, "make the ontology coherent"));
        options.add(OptionType.FLAG.create("fast", b -> {
            refOntologyStrategy = RefOntologyStrategy.ONE_MCS;
            badAxiomStrategy = BadAxiomStrategy.IN_ONE_MUS;
        }, "use fast methods for selection"));
        options.add(OptionType.options(
                Map.of("intersect", RefOntologyStrategy.INTERSECTION_OF_MCS,
                        "intersect-of-some", RefOntologyStrategy.INTERSECTION_OF_SOME_MCS,
                        "largest", RefOntologyStrategy.LARGEST_MCS,
                        "any", RefOntologyStrategy.ONE_MCS,
                        "random", RefOntologyStrategy.RANDOM_MCS,
                        "random-of-some", RefOntologyStrategy.SOME_MCS))
                .create("ref-ontology", method -> refOntologyStrategy = method,
                        "method for reference ontology selection"));
        options.add(OptionType.options(
                Map.of("one-mus", BadAxiomStrategy.IN_ONE_MUS,
                        "some-mus", BadAxiomStrategy.IN_SOME_MUS,
                        "most-mus", BadAxiomStrategy.IN_MOST_MUS,
                        "least-mcs", BadAxiomStrategy.IN_LEAST_MCS,
                        "largest-mcs", BadAxiomStrategy.NOT_IN_LARGEST_MCS,
                        "one-mcs", BadAxiomStrategy.NOT_IN_ONE_MCS,
                        "some-mcs", BadAxiomStrategy.NOT_IN_SOME_MCS,
                        "random", BadAxiomStrategy.RANDOM))
                .create("bad-axiom", method -> badAxiomStrategy = method,
                        "method for bad axiom selection"));
        options.add(OptionType.FLAG.create("enhance-ref", b -> {
            enhanceRef = true;
        }, "keep the reference ontology as static axioms in the output"));
        options.add(OptionType.options(
                Map.of("exact", "exact",
                        "approximate", "approximate"))
                .create("power-index", index -> powerIndexType = index,
                        "power index to use for axiom selection (exact or approximate)"));
        return options;
    }

    @Override
    protected OntologyRepair getRepair() {
        PowerIndex powerIndex = "exact".equals(powerIndexType)
                ? new ShapleyInconsistencyValueExact()
                : new ShapleyInconsistencyValueApproximate();

        return new OntologyRepairWithPowerIndexes(
                coherence ? Ontology::isCoherent : Ontology::isConsistent,
                refOntologyStrategy,
                badAxiomStrategy,
                weakeningFlags,
                enhanceRef,
                powerIndex);
    }

    /**
     * One argument must be given, corresponding to an OWL ontology file path. E.g.,
     * run with the parameter src/test/resources/inconsistent/leftpolicies.owl
     *
     * @param args
     *            Must contain a file path of an ontology.
     */
    public static void main(String[] args) {
        (new RepairWithPowerIndexes()).launch(args);
    }
}

