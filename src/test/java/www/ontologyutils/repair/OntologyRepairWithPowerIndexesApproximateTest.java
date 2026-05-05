package www.ontologyutils.repair;

import org.junit.jupiter.api.parallel.*;

import www.ontologyutils.refinement.AxiomWeakener;
import www.ontologyutils.repair.powerindex.ShapleyInconsistencyValueApproximate;
import www.ontologyutils.toolbox.Ontology;

@Execution(ExecutionMode.CONCURRENT)
public class OntologyRepairWithPowerIndexesApproximateTest extends OntologyRepairTest {
    @Override
    protected OntologyRepair getRepairForConsistency() {
        return new OntologyRepairWithPowerIndexes(
                Ontology::isConsistent,
                OntologyRepairWeakening.RefOntologyStrategy.ONE_MCS,
                OntologyRepairRemoval.BadAxiomStrategy.IN_SOME_MUS,
                AxiomWeakener.FLAG_DEFAULT,
                false,
                new ShapleyInconsistencyValueApproximate());
    }

    @Override
    protected OntologyRepair getRepairForCoherence() {
        return new OntologyRepairWithPowerIndexes(
                Ontology::isCoherent,
                OntologyRepairWeakening.RefOntologyStrategy.ONE_MCS,
                OntologyRepairRemoval.BadAxiomStrategy.IN_SOME_MUS,
                AxiomWeakener.FLAG_DEFAULT,
                false,
                new ShapleyInconsistencyValueApproximate());
    }
}

