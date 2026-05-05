package www.ontologyutils.repair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLAxiom;

import www.ontologyutils.refinement.AxiomWeakener;
import www.ontologyutils.repair.OntologyRepairRemoval.BadAxiomStrategy;
import www.ontologyutils.repair.OntologyRepairWeakening.RefOntologyStrategy;
import www.ontologyutils.repair.powerindex.PowerIndex;
import www.ontologyutils.toolbox.Ontology;
import www.ontologyutils.toolbox.Utils;

/**
 * An implementation of {@code OntologyRepair} using power indexes to select bad
 * axioms and weaker replacements. This extends the axiom weakening approach by
 * using a power index (e.g., Shapley value) to identify axioms with the highest
 * influence on inconsistency, and weakenings with the lowest influence.
 *
 * The algorithm:
 * 1. Takes a reference ontology using a strategy.
 * 2. While the ontology is not repaired:
 * 3. Select the bad axiom with the highest power index score.
 * 4. Get the set of weakened axioms of the bad axiom.
 * 5. Select the weaker axiom with the lowest power index score.
 * 6. Replace the bad axiom with the weaker axiom on the ontology.
 * 7. Return the repaired ontology.
 */
public class OntologyRepairWithPowerIndexes extends OntologyRepairWeakening {
    private final PowerIndex powerIndex;

    /**
     * @param isRepaired
     *            The monotone predicate testing whether an ontology is repaired.
     * @param refOntologySource
     *            The strategy for computing the reference ontology.
     * @param badAxiomSource
     *            The strategy for computing bad axioms.
     * @param weakeningFlags
     *            The flags to use for weakening.
     * @param enhanceRef
     *            Use the reference ontology as a base ontology that is always
     *            included in the repair.
     * @param powerIndex
     *            The power index to use for selecting axioms.
     */
    public OntologyRepairWithPowerIndexes(Predicate<Ontology> isRepaired, RefOntologyStrategy refOntologySource,
            BadAxiomStrategy badAxiomSource, int weakeningFlags, boolean enhanceRef, PowerIndex powerIndex) {
        super(isRepaired, refOntologySource, badAxiomSource, weakeningFlags, enhanceRef);
        this.powerIndex = powerIndex;
    }

    @Override
    public void repair(Ontology ontology) {
        infoMessage(Utils.formatOntologyState("Initial ontology state:", ontology));

        var refAxioms = Utils.randomChoice(getRefAxioms(ontology));
        infoMessage("Selected a reference ontology with " + refAxioms.size() + " axioms.");
        infoMessage(Utils.formatAxiomSet("Reference ontology:", refAxioms));
        if (enhanceRef) {
            ontology.addStaticAxioms(refAxioms);
        }

        try (var refOntology = ontology.cloneWithRefutable(refAxioms).withSeparateCache()) {
            var axiomWeakener = getWeakener(refOntology, ontology);
            while (!isRepaired(ontology)) {
                var currentAxioms = ontology.refutableAxioms().collect(Collectors.toSet());
                var badAxiomCandidates = findBadAxiomCandidates(ontology);
                var badAxiomScores = computeBadAxiomScores(badAxiomCandidates, currentAxioms);
                infoMessage("Found " + badAxiomScores.size() + " possible bad axioms.");
                infoMessage(Utils.formatShapleyTable("Power index values for possible bad axioms:",
                        badAxiomScores.entrySet().stream(), false));
                var badAxiom = selectBadAxiom(badAxiomScores);
                var weakerAxioms = collectWeakeningCandidates(badAxiom, axiomWeakener);
                while (weakerAxioms.isEmpty()) {
                    badAxiomScores.remove(badAxiom);
                    if (badAxiomScores.isEmpty()) {
                        throw new IllegalStateException("Could not find a weakenable bad axiom in ontology.");
                    }
                    badAxiom = selectBadAxiom(badAxiomScores);
                    weakerAxioms = collectWeakeningCandidates(badAxiom, axiomWeakener);
                }
                infoMessage("Selected the bad axiom " + Utils.prettyPrintAxiom(badAxiom) + ".");

                var weakerAxiomScores = scoreWeakeningCandidates(weakerAxioms, currentAxioms);
                infoMessage("Found " + weakerAxioms.size() + " weaker axioms.");
                infoMessage(Utils.formatShapleyTable(
                        "Candidate weakenings and power index values for " + Utils.prettyPrintAxiom(badAxiom)
                                + ":",
                        weakerAxiomScores.entrySet().stream(), true));
                var weakerAxiom = selectWeakening(weakerAxiomScores, badAxiom);
                infoMessage("Selected the weaker axiom " + Utils.prettyPrintAxiom(weakerAxiom) + ".");

                ontology.replaceAxiom(badAxiom, weakerAxiom);
                infoMessage(Utils.formatOntologyState("Ontology state after weakening:", ontology));
            }
        }

        infoMessage(Utils.formatOntologyState("Final ontology state after repair:", ontology));
    }

    private List<OWLAxiom> findBadAxiomCandidates(Ontology ontology) {
        var candidates = Utils.toList(findBadAxioms(ontology));
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Could not find a bad axiom in ontology.");
        }
        return candidates;
    }

    private Set<OWLAxiom> collectWeakeningCandidates(OWLAxiom axiom, AxiomWeakener weakener) {
        try {
            var weakenings = weakener.weakerAxioms(axiom).collect(Collectors.toSet());
            weakenings.remove(axiom);
            return weakenings;
        } catch (RuntimeException ex) {
            return Set.of();
        }
    }

    private Map<OWLAxiom, Double> scoreWeakeningCandidates(Set<OWLAxiom> weakenings, Set<OWLAxiom> currentAxioms) {
        if (weakenings == null || weakenings.isEmpty()) {
            throw new IllegalStateException("No weakening found for axiom.");
        }
        return weakenings.stream()
                .collect(Collectors.toMap(ax -> ax, ax -> powerIndex.computeScore(currentAxioms, ax)));
    }

    private OWLAxiom selectWeakening(Map<OWLAxiom, Double> weakerAxiomScores, OWLAxiom badAxiom) {
        return weakerAxiomScores.entrySet().stream()
                .min(Comparator.<Map.Entry<OWLAxiom, Double>>comparingDouble(Map.Entry::getValue)
                        .thenComparing(e -> Utils.prettyPrintAxiom(e.getKey())))
                .orElseThrow(() -> new IllegalStateException("Could not select a weakening for axiom: " + badAxiom))
                .getKey();
    }

    private Map<OWLAxiom, Double> computeBadAxiomScores(List<OWLAxiom> candidates, Set<OWLAxiom> axioms) {
        return candidates.stream()
                .collect(Collectors.toMap(ax -> ax, ax -> powerIndex.computeScore(axioms, ax)));
    }

    private OWLAxiom selectBadAxiom(Map<OWLAxiom, Double> badAxiomScores) {
        return badAxiomScores.entrySet().stream()
                .max(Comparator.<Map.Entry<OWLAxiom, Double>>comparingDouble(Map.Entry::getValue)
                        .thenComparing((e1, e2) -> Utils.prettyPrintAxiom(e2.getKey())
                                .compareTo(Utils.prettyPrintAxiom(e1.getKey()))))
                .orElseThrow(() -> new IllegalStateException("Could not find a bad axiom in ontology."))
                .getKey();
    }
}

