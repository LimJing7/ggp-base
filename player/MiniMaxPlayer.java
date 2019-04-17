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

public class MiniMaxPlayer extends ExampleLegalPlayer {

    HashMap<MachineState, Integer> memo_table;

    public static void main(String[] args) {
        Player.initialize(new MiniMaxPlayer().getName());
    }

    @Override
    public void start(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        this.memo_table = new HashMap<>();
    }

    @Override
    public Move play(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        StateMachine machine = getStateMachine();
        MachineState state = getCurrentState();

        Role role = getRole();

        // Gets all legal moves for our player in the current state
        List<Move> legalMoves = findLegals(role, state, machine);
        System.out.println(legalMoves);

        Move chosenMove = legalMoves.get(0);

        int score = 0;
        for (Move move : legalMoves) {
            int result = minScore(role, move, state);
            System.out.println(move + ": " + result);
            if (result == 100) {
                System.out.println("I am playing: " + move);
                return move;
            }
            if (result > score) {
                score = result;
                chosenMove = move;
            }

        }

        // Logging what decisions your player is making as well as other statistics
        // is a great way to debug your player and benchmark it against other players.
        System.out.println("I am playing: " + chosenMove);
        return chosenMove;

    }

    private int minScore(Role role, Move move, MachineState state)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        StateMachine machine = getStateMachine();
        List<Role> roles = machine.getRoles();
        Role opponent_role = null;
        for (Role r : roles) {
            if (!r.equals(role)) {
                opponent_role = r;
            }
        }

        List<Move> legalMoves = findLegals(role, state, machine);
//        Move move = legalMoves.get(0);
        List<Move> oppLegalMoves = findLegals(opponent_role, state, machine);
//        System.out.println("state: " + state);
        int score = 100;
        for (Move oppMove : oppLegalMoves) {

            List<Move> action = null;
            if (role == roles.get(0)) {
                action = Arrays.asList(move, oppMove);

            } else {
                action = Arrays.asList(oppMove, move);
            }

            MachineState newState = this.findNext(action, state, machine);
            int result;
            if (this.memo_table.containsKey(newState)) {
                result = this.memo_table.get(newState);
            } else {
                result = maxScore(role, newState);
                this.memo_table.put(newState, result);
            }
            if (result < score) {
                score = result;
            }
        }
        return score;
    }

    private int maxScore(Role role, MachineState state)
            throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        StateMachine machine = getStateMachine();
        List<Role> roles = machine.getRoles();
        Role opponent_role = null;
        for (Role r : roles) {
            if (!r.equals(role)) {
                opponent_role = r;
            }
        }
        if (this.findTerminalp(state, machine)) {
//            System.out.println("TERMINAL: " + this.findReward(role, state, machine));
            return this.findReward(role, state, machine);
        }
        List<Move> legalMoves = findLegals(role, state, machine);
        List<Move> oppLegalMoves = findLegals(opponent_role, state, machine);
        Move oppMove = oppLegalMoves.get(0);
        int score = 0;
        for (Move move : legalMoves) {
//            List<Move> action = null;
//            if (role == roles.get(0)) {
//                action = Arrays.asList(move, oppMove);
//
//            } else {
//                action = Arrays.asList(oppMove, move);
//            }
//            MachineState newState = this.findNext(action, state, machine);
            int result = minScore(role, move, state);
            if (result > score) {
                score = result;
            }
        }
        return score;
    }

    @Override
    public String getName() {
        return "MiniMaxPlayer";
    }

}
