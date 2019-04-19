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

public class OneTwoPlayer extends ExampleLegalPlayer {

    HashMap<MachineState, Double> memo_table;
    Boolean onePlayer;

    public static void main(String[] args) {
        Player.initialize(new OneTwoPlayer().getName());

    }

    @Override
    public void start(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        this.memo_table = new HashMap<>();
        StateMachine machine = getStateMachine();
        if (machine.getRoles().size() == 1) {
            onePlayer = true;
        } else if (machine.getRoles().size() == 2) {
            onePlayer = false;
        }
    }

    @Override
    public Move play(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

        StateMachine machine = getStateMachine();
        MachineState state = getCurrentState();
        Role role = getRole();

        // Gets all legal moves for our player in the current state
        List<Move> legalMoves = findLegals(role, state, machine);
        Move chosenMove = legalMoves.get(0);

        if (this.onePlayer) {

            double bestScore = 0;

            // Returns the move with best score.
            for (Move move : legalMoves) {
                List<Move> action = new ArrayList<Move>(Arrays.asList(move));
                double score = oneMaxScore(role, this.findNext(action, state, machine), timeout);
                if (score == 100) {
                    chosenMove = move;
                    break;
                } else {
                    if (score > bestScore) {
                        bestScore = score;
                        chosenMove = move;
                    }
                }
            }
        } else {
            // Score
            double score = 0;
            double alpha = Double.NEGATIVE_INFINITY;
            double beta = Double.POSITIVE_INFINITY;
            for (Move move : legalMoves) {
                double result = twoMinScore(role, move, state, alpha, beta, timeout);
                if (result == 100) {
                    System.out.println("I am playing: " + move);
                    return move;
                }
                if (result > score) {
                    score = result;
                    chosenMove = move;
                }

            }
        }
        // Logging what decisions your player is making as well as other statistics
        // is a great way to debug your player and benchmark it against other players.
        System.out.println("I am playing: " + chosenMove);
        return chosenMove;
    }

    protected double twoMinScore(Role role, Move move, MachineState state, double alpha, double beta, long timeout)
            throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

        StateMachine machine = getStateMachine();
        List<Role> roles = machine.getRoles();
        Role opponent_role = null;
        for (Role r : roles) {
            if (!r.equals(role)) {
                opponent_role = r;
            }
        }

        List<Move> oppLegalMoves = findLegals(opponent_role, state, machine);
        for (Move oppMove : oppLegalMoves) {

            // List<Move> action;
            List<Move> action = null;
            if (role.equals(roles.get(0))) {
                action = Arrays.asList(move, oppMove);
            } else {
                action = Arrays.asList(oppMove, move);
            }

            // List<Move> action = new ArrayList<Move>(Arrays.asList(action,move));
            MachineState newState = this.findNext(action, state, machine);
            double result;
            if (this.memo_table.containsKey(newState)) {
                result = this.memo_table.get(newState);
            } else {
                result = twoMaxScore(role, newState, alpha, beta, timeout);
                this.memo_table.put(newState, result);
            }
            if (beta > result) {
                beta = result;
            }
            if (beta <= alpha) {
                return alpha;
            }

        }
        return beta;
    }

    protected double twoMaxScore(Role role, MachineState state, double alpha, double beta, long timeout)
            throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine machine = getStateMachine();
        if (this.findTerminalp(state, machine)) {
            return this.findReward(role, state, machine);
        }
        List<Move> legalMoves = findLegals(role, state, machine);
        for (Move move : legalMoves) {
            double result = twoMinScore(role, move, state, alpha, beta, timeout);
            if (alpha < result) {
                alpha = result;
            }
            if (alpha >= beta) {
                return beta;
            }

        }
        return alpha;

    }

    protected double oneMaxScore(Role role, MachineState state, long timeout) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine machine = getStateMachine();
        if (this.findTerminalp(state, machine)) {
            return this.findReward(role, state, machine);
        }
        List<Move> legalMoves = findLegals(role, state, machine);
        double best_score = 0;
        for (Move move : legalMoves) {
            List<Move> action = new ArrayList<Move>(Arrays.asList(move));
            MachineState newState = this.findNext(action, state, machine);
            double score = 0;
            if (this.memo_table.containsKey(newState)) {
                score = this.memo_table.get(newState);
            } else {
                score = oneMaxScore(role, newState, timeout);
                this.memo_table.put(newState, score);
            }
            if (score > best_score) {
                best_score = score;
            }
        }
        return best_score;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "One or Two Player";
    }
}
