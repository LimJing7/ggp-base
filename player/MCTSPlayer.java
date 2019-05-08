import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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
    double explore_weight = 0.8;
    private final Random RAND = new Random();

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
            timeLeft = timeout - System.currentTimeMillis();
        }
        return this.bestMove(role, state, machine);

    }

    /**
     * computes the best move in the current search tree
     *
     * @return
     * @throws MoveDefinitionException
     * @throws TransitionDefinitionException
     */
    private Move bestMove(Role role, MachineState state, StateMachine machine) throws MoveDefinitionException, TransitionDefinitionException {
        List<Move> legalMoves = findLegals(role, state, machine);
        Move bestMove = null;
        MachineState bestState = null;
        double bestScore = Double.MIN_VALUE;
        for (Move move : legalMoves) {
            List<Move> action = new ArrayList<Move>(Arrays.asList(move));
            MachineState nextState = this.findNext(action, state, machine);
            double score = this.memo_table.get(nextState).getMean();
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
                bestState = nextState;
            }
        }
        System.out.println(bestScore);
        System.out.println(this.memo_table.get(bestState).getCount());
        return bestMove;

    }

    private void step(Role role, MachineState state, StateMachine machine)
            throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        MachineState selectedState = select(role, state, machine);
        // expand does not seem needed?
        simulate(role, selectedState);
    }

    /**
     * run simulation and does back-propagation automatically
     *
     * @param selectedState
     * @throws MoveDefinitionException
     * @throws TransitionDefinitionException
     * @throws GoalDefinitionException
     */
    private void simulate(Role role, MachineState selectedState)
            throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        // TODO Auto-generated method stub
        StateMachine machine = getStateMachine();
        MachineState currState = selectedState;
        int depth = 0;
        while (!this.findTerminalp(currState, machine)) {
            List<Role> roles = machine.getRoles();
            List<Move> move = Arrays.asList(new Move[roles.size()]);
            for (int i = 0; i < roles.size(); i++) {
                List<Move> options = findLegals(roles.get(i), currState, machine);
                move.set(i, options.get(RAND.nextInt(options.size())));
            }
            currState = this.findNext(move, currState, machine);
            depth ++;
//            System.out.println(depth);
//            System.out.println(currState);
        }
        double reward = this.findReward(role, currState, machine);
        this.memo_table.get(selectedState).addReward(reward);

    }

    private MachineState select(Role role, MachineState state, StateMachine machine)
            throws MoveDefinitionException, TransitionDefinitionException {
        if (!memo_table.containsKey(state)) {
            this.memo_table.put(state, new MCTSNode(this.memo_table, null));
            return state;
        } else if (this.findTerminalp(state, machine)) {
            return state;
        } else {
            double bestScore = Double.MIN_VALUE;
            MachineState selectedState = null;
            List<Move> legalMoves = findLegals(role, state, machine);
            for (Move move : legalMoves) {
                List<Move> action = new ArrayList<Move>(Arrays.asList(move));
                MachineState nextState = this.findNext(action, state, machine);
                if (!memo_table.containsKey(nextState)) {
                    this.memo_table.put(nextState, new MCTSNode(this.memo_table, state));
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
