package www.ontologyutils.repair.powerindex;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * Represents a power index for computing the score (influence) of an axiom
 * within a set of axioms. Power indexes measure how much an axiom contributes
 * to the inconsistency of the ontology.
 */
public interface PowerIndex {
    /**
     * Computes the power index score of a target axiom within a set of axioms.
     * The score indicates the influence of the axiom on the inconsistency of the
     * set.
     *
     * @param axioms
     *            The set of axioms to evaluate the target axiom within.
     * @param targetAxiom
     *            The axiom for which to compute the power index score.
     * @return The power index score of the target axiom. Higher scores indicate
     *         greater influence on inconsistency.
     */
    double computeScore(Set<OWLAxiom> axioms, OWLAxiom targetAxiom);
}

