package www.ontologyutils.repair;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.parallel.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import www.ontologyutils.refinement.AxiomWeakener;
import www.ontologyutils.toolbox.Ontology;

@Execution(ExecutionMode.CONCURRENT)
public class OntologyRepairWithPowerIndexesExactTest extends OntologyRepairTest {
    @Override
    protected OntologyRepair getRepairForConsistency() {
        return new OntologyRepairWithPowerIndexes(
                Ontology::isConsistent,
                OntologyRepairWithPowerIndexes.RefOntologyStrategy.ONE_MCS,
                OntologyRepairWithPowerIndexes.BadAxiomStrategy.SHAPLEY_EXACT,
                OntologyRepairWithPowerIndexes.WeakerAxiomStrategy.SHAPLEY_EXACT,
                AxiomWeakener.FLAG_DEFAULT,
                false
        );
    }

    @Override
    protected OntologyRepair getRepairForCoherence() {
        return new OntologyRepairWithPowerIndexes(
                Ontology::isCoherent,
                OntologyRepairWithPowerIndexes.RefOntologyStrategy.ONE_MCS,
                OntologyRepairWithPowerIndexes.BadAxiomStrategy.SHAPLEY_EXACT,
                OntologyRepairWithPowerIndexes.WeakerAxiomStrategy.SHAPLEY_EXACT,
                AxiomWeakener.FLAG_DEFAULT,
                false);
    }

    @Override
    @ParameterizedTest
    @ValueSource(strings = { "/inconsistent/leftpolicies-small.owl" })
    public void repairInconsistentOntologyFromFile(String resourceName) {
        super.repairInconsistentOntologyFromFile(resourceName);
    }

    @Override
    @ParameterizedTest
    @ValueSource(strings = { "/inconsistent/leftpolicies-small.owl" })
    public void repairWithNormalizationInconsistentOntologyFromFile(String resourceName) {
        super.repairWithNormalizationInconsistentOntologyFromFile(resourceName);
    }

    @Override
    @Disabled("Coherence repair not yet in focus")
    @ParameterizedTest
    @ValueSource(strings = { "/inconsistent/leftpolicies-small.owl" })
    public void repairIncoherentOntologyFromFile(String resourceName) {
        super.repairIncoherentOntologyFromFile(resourceName);
    }

    @Override
    @Disabled("Coherence repair not yet in focus")
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    public void repairIncoherentOntology(int seed) {
        super.repairIncoherentOntology(seed);
    }
}


