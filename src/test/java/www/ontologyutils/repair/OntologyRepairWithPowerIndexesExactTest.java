package www.ontologyutils.repair;

import org.junit.jupiter.api.parallel.*;

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
}


