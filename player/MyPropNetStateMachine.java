import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;

public class MyPropNetStateMachine extends SamplePropNetStateMachine {
    /** The underlying proposition network */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    @Override
    public void initialize(List<Gdl> description) {
        try {
            propNet = OptimizingPropNetFactory.create(description);
            roles = propNet.getRoles();
            ordering = getOrdering();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isTerminal(MachineState state) {
        // TODO Auto-generated method stub
        return super.isTerminal(state);
    }

    private void markBaseProps(MachineState state) {
        Set<GdlSentence> state_set = state.getContents();
        for (Entry<GdlSentence, Proposition> entry : this.propNet.getBasePropositions().entrySet()) {
            if (state_set.contains(entry.getKey())) {
                entry.getValue().setValue(true);
            } else {
                entry.getValue().setValue(false);
            }
        }
    }

    private void markInputProps(MachineState state) {
        Set<GdlSentence> state_set = state.getContents();
        for (Entry<GdlSentence, Proposition> entry : this.propNet.getInputPropositions().entrySet()) {
            if (state_set.contains(entry.getKey())) {
                entry.getValue().setValue(true);
            } else {
                entry.getValue().setValue(false);
            }
        }
    }
}
