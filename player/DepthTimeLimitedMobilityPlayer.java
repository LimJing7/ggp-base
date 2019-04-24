import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ggp.base.apps.player.Player;
import org.ggp.base.util.Pair;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class DepthTimeLimitedMobilityPlayer extends OneTwoPlayer {

    int searchDepth = 17;

    public static void main(String[] args) {
        Player.initialize(new DepthTimeLimitedMobilityPlayer().getName());
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

        int nodesExplored = 0;
        double bestScore = 0;

        if (this.onePlayer) {
            for (int currentSearchDepth = 0; currentSearchDepth < this.searchDepth; currentSearchDepth++) {
                long timeLeft = timeout - System.currentTimeMillis();
                if (timeLeft < 2000) {
                    nodesExplored--;
                    break;
                }
                // Returns the move with best score.
                for (Move move : legalMoves) {
                    List<Move> action = new ArrayList<Move>(Arrays.asList(move));
                    Pair<Integer, Double> result = oneMaxScorePair(role, this.findNext(action, state, machine), timeout,
                            currentSearchDepth);
                    nodesExplored += result.left;
                    double score = result.right;
                    if (score == 100) {
                        chosenMove = move;
                        break;
                    } else {
                        if (score > bestScore) {
                            bestScore = score;
                            chosenMove = move;
                        }
                    }
                    timeLeft = timeout - System.currentTimeMillis();
                    if (timeLeft < 2000) {
                        nodesExplored--;
                        break;
                    }
                }
            }
        } else {
            // Score
            double alpha = Double.NEGATIVE_INFINITY;
            double beta = Double.POSITIVE_INFINITY;
            for (int currentSearchDepth = 0; currentSearchDepth < this.searchDepth; currentSearchDepth++) {
                long timeLeft = timeout - System.currentTimeMillis();
                if (timeLeft < 2000) {
                    nodesExplored--;
                    break;
                }
                for (Move move : legalMoves) {
                    Pair<Integer, Double> res = twoMinScorePair(role, move, state, alpha, beta, timeout,
                            currentSearchDepth);
                    nodesExplored += res.left;
                    double result = res.right;
                    System.out.println(result);
                    if (result == 100) {
                        System.out.println("I am playing: " + move);
                        return move;
                    }
                    if (result > bestScore) {
                        bestScore = result;
                        chosenMove = move;
                    }
                    timeLeft = timeout - System.currentTimeMillis();
                    if (timeLeft < 2000) {
                        nodesExplored--;
                        break;
                    }
                }
            }

        }
        // Logging what decisions your player is making as well as other statistics
        // is a great way to debug your player and benchmark it against other players.
        nodesExplored += 1;
        System.out.println("Score: " + bestScore);
        System.out.println("Nodes explored: " + nodesExplored);
        System.out.println("I am playing: " + chosenMove);
        return chosenMove;
    }

    protected double mobility(Role role, MachineState state)
            throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine machine = getStateMachine();

        List<Move> legalMoves = findLegals(role, state, machine);
        List<Move> feasibleMoves = findActions(role, machine);
//        System.out.println((legalMoves.size() / (double) feasibleMoves.size() * 100));
        return (legalMoves.size() / (double) feasibleMoves.size() * 100);

    }

    protected double focus(Role role, MachineState state)
            throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine machine = getStateMachine();

        List<Move> legalMoves = findLegals(role, state, machine);
        List<Move> feasibleMoves = findActions(role, machine);
        return (100 - legalMoves.size() / (double) feasibleMoves.size() * 100);

    }

    protected Pair<Integer, Double> twoMinScorePair(Role role, Move move, MachineState state, double alpha, double beta,
            long timeout, int searchDepth)
            throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {

        StateMachine machine = getStateMachine();
        List<Role> roles = machine.getRoles();
        Role opponent_role = null;
        for (Role r : roles) {
            if (!r.equals(role)) {
                opponent_role = r;
            }
        }
        if (searchDepth <= 0) {
            return Pair.of(0, focus(opponent_role, state));
        }

        int nodesExplored = 0;

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
            Pair<Integer, Double> res = twoMaxScorePair(role, newState, alpha, beta, timeout, searchDepth - 1);
            nodesExplored += res.left;
            double result = res.right;
            if (beta > result) {
                beta = result;
            }
            if (beta <= alpha) {
                return Pair.of(nodesExplored, alpha);
            }
            long timeLeft = timeout - System.currentTimeMillis();
            if (timeLeft < 2000) {
                nodesExplored--;
                break;
            }

        }
        nodesExplored += 1;
        return Pair.of(nodesExplored, beta);
    }

    protected Pair<Integer, Double> twoMaxScorePair(Role role, MachineState state, double alpha, double beta,
            long timeout, int searchDepth)
            throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine machine = getStateMachine();
        if (this.findTerminalp(state, machine)) {
            return Pair.of(1, (double) this.findReward(role, state, machine));
        }
        if (searchDepth <= 0) {
            // return Pair.of(0, alpha);
            return Pair.of(0, mobility(role, state));
        }
        int nodesExplored = 0;
        List<Move> legalMoves = findLegals(role, state, machine);
        for (Move move : legalMoves) {
            Pair<Integer, Double> res = twoMinScorePair(role, move, state, alpha, beta, timeout, searchDepth - 1);
            nodesExplored += res.left;
            double result = res.right;
            if (alpha < result) {
                alpha = result;
            }
            if (alpha >= beta) {
                return Pair.of(nodesExplored, beta);
            }
            long timeLeft = timeout - System.currentTimeMillis();
            if (timeLeft < 2000) {
                nodesExplored--;
                break;
            }

        }
        nodesExplored += 1;
        return Pair.of(nodesExplored, alpha);

    }

    protected Pair<Integer, Double> oneMaxScorePair(Role role, MachineState state, long timeout, int searchDepth)
            throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine machine = getStateMachine();
        if (this.findTerminalp(state, machine)) {
            return Pair.of(1, (double) this.findReward(role, state, machine));
        }
        if (searchDepth <= 0) {
            return Pair.of(0, this.mobility(role, state));
        }
        int nodesExplored = 0;
        List<Move> legalMoves = findLegals(role, state, machine);
        double best_score = 0;
        for (Move move : legalMoves) {
            List<Move> action = new ArrayList<Move>(Arrays.asList(move));
            MachineState newState = this.findNext(action, state, machine);
            Pair<Integer, Double> result = oneMaxScorePair(role, newState, timeout, searchDepth - 1);
            nodesExplored += result.left;
            double score = result.right;
            if (score > best_score) {
                best_score = score;
            }
            long timeLeft = timeout - System.currentTimeMillis();
            if (timeLeft < 2000) {
                nodesExplored--;
                break;
            }
        }
        nodesExplored += 1;
        return Pair.of(nodesExplored, best_score);
    }

    @Override
    public String getName() {
        return "Depth Time Limited Mobility Player";
    }

}
