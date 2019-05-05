import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.ggp.base.apps.player.Player;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSPlayer extends ExampleLegalPlayer {

    HashMap<MachineState, MCTSNode> memo_table;
    double explore_weight = 0.2;

    public static void main(String[] args) {
        Player.initialize(new MCTSPlayer().getName());

    }

    @Override
    public void start(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        this.memo_table = new HashMap<MachineState, MCTSNode>();
    }

    @Override
    public Move play(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

        StateMachine machine = getStateMachine();
        MachineState state = getCurrentState();
        Role role = getRole();

        long timeLeft = timeout - System.currentTimeMillis();
        while (timeLeft > 2000) {
            this.step(role, state, machine);
        }
        return this.bestMove();

    }

    /**
     * computes the best move in the current search tree
     * @return
     */
    private Move bestMove() {
        // TODO Auto-generated method stub
        return null;
    }

    private void step(Role role, MachineState state, StateMachine machine)
            throws MoveDefinitionException, TransitionDefinitionException {
        MachineState selectedState = select(role, state, machine);
        // expand does not seem needed?
        simulate(selectedState);

    }

    /**
     * run one simulation and does backpropagation automatically
     * @param selectedState
     */
    private void simulate(MachineState selectedState) {
        // TODO Auto-generated method stub

    }

    private MachineState select(Role role, MachineState state, StateMachine machine)
            throws MoveDefinitionException, TransitionDefinitionException {
        // TODO Auto-generated method stub
        if (!memo_table.containsKey(state)) {
            return state;
        } else {
            double bestScore = Double.MIN_VALUE;
            MachineState selectedState = null;
            List<Move> legalMoves = findLegals(role, state, machine);
            for (Move move : legalMoves) {
                List<Move> action = new ArrayList<Move>(Arrays.asList(move));
                MachineState nextState = this.findNext(action, state, machine);
                if (!memo_table.containsKey(nextState)) {
                    return nextState;
                } else {
                    double score = selectfn(role, nextState, machine);
                    if (score > bestScore) {
                        selectedState = nextState;
                    }
                }
            }
            return select(role, selectedState, machine);
        }
    }

    private double selectfn(Role role, MachineState state, StateMachine machine) {
        MCTSNode node = this.memo_table.get(state);
        MCTSNode parentNode = this.memo_table.get(node.getParent());
        return node.getMean() + this.explore_weight * Math.sqrt(Math.log(parentNode.getCount()) / node.getCount());
    }

    @Override
    public String getName() {
        return "MCTS Player";
    }

}
