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

public class CompulsiveDeliberationPlayer extends ExampleLegalPlayer {

    HashMap<MachineState, Integer> memo_table;

	public static void main(String[] args) {
		Player.initialize(new CompulsiveDeliberationPlayer().getName());
	}

    @Override
    public void start(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        this.memo_table = new HashMap<>();
    }

	@Override
	public Move play(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//Gets our state machine (the same one as returned in getInitialStateMachine)
		//This State Machine simulates the game we are currently playing.
		StateMachine machine = getStateMachine();

		//Gets the current state we're in (e.g. move 2 of a game of tic tac toe where X just played in the center)
		MachineState state = getCurrentState();

		//Gets our role (e.g. X or O in a game of tic tac toe)
		Role role = getRole();

		//Gets all legal moves for our player in the current state
		List<Move> legalMoves = findLegals(role, state, machine);

		Move chosenMove = legalMoves.get(0);
		int bestScore = 0;

		//Returns the move with best score.
		for (Move move : legalMoves) {
			List<Move> action = new ArrayList<Move>(Arrays.asList(move));
			int score = maxScore(role, this.findNext(action, state, machine));
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

		//Logging what decisions your player is making as well as other statistics
		//is a great way to debug your player and benchmark it against other players.
		System.out.println("I am playing: " + chosenMove);
		return chosenMove;
	}

	/**
	 * Returns the name of the player.
	 */
	@Override
	public String getName() {
		return "Compulsive Deliberation Player";
	}

	protected int maxScore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine machine = getStateMachine();
		if (this.findTerminalp(state, machine)) {
			return this.findReward(role, state, machine);
		}
		List<Move> legalMoves = findLegals(role, state, machine);
		int best_score = 0;
		for (Move move : legalMoves) {
			List<Move> action = new ArrayList<Move>(Arrays.asList(move));
			MachineState newState = this.findNext(action, state, machine);
			int score = 0;
			if (this.memo_table.containsKey(newState)) {
			    score = this.memo_table.get(newState);
			} else {
			    score = maxScore(role, newState);
			    this.memo_table.put(newState, score);
			}
			if (score > best_score) {
				best_score = score;
			}
		}
		return best_score;
	}


}
