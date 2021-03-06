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

public class MCTSPlayer extends LegalPropPlayer {

    HashMap<MCTSNodeKey, MCTSNodeValue> state_tree;
    double explore_weight = 14;
    private final Random RAND = new Random();

    public static void main(String[] args) {
        Player.initialize(new MCTSPlayer().getName());

    }

    @Override
    public void start(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        this.state_tree = new HashMap<MCTSNodeKey, MCTSNodeValue>();

        StateMachine machine = getStateMachine();
        MachineState state = getCurrentState();
        Role role = getRole();

//        int nDepthCharges = 0;
//        MCTSNodeKey key = new MCTSNodeKey(state, null);
//        if (this.state_tree.containsKey(key)) {
//            System.out.println("Original count: " + this.state_tree.get(key).getCount());
//        } else {
//            System.out.println("Original count: 0");
//        }
//
//        long timeLeft = timeout - System.currentTimeMillis();
//        while (timeLeft > 2000) {
//            nDepthCharges++;
//            this.step(role, state, machine);
//            timeLeft = timeout - System.currentTimeMillis();
//        }
//
//        System.out.println("Number of Depth Charges: " + nDepthCharges);
    }

    @Override
    public Move play(long timeout)
            throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

        StateMachine machine = getStateMachine();
        MachineState state = getCurrentState();
        Role role = getRole();

        int nDepthCharges = 0;
        MCTSNodeKey key = new MCTSNodeKey(state, null);
        if (this.state_tree.containsKey(key)) {
            System.out.println("Original count: " + this.state_tree.get(key).getCount());
        } else {
            System.out.println("Original count: 0");
        }

        long timeLeft = timeout - System.currentTimeMillis();
        while (timeLeft > 2000) {
            nDepthCharges++;
            this.step(role, state, machine);
            timeLeft = timeout - System.currentTimeMillis();
        }

        System.out.println("Number of Depth Charges: " + nDepthCharges);
        System.out.println(state);
        System.out.println("Final count: " + this.state_tree.get(key).getCount());

        return this.bestMove(role, state, machine);

    }

    /**
     * computes the best move in the current search tree
     *
     * @return
     * @throws MoveDefinitionException
     * @throws TransitionDefinitionException
     */
    private Move bestMove(Role role, MachineState state, StateMachine machine)
            throws MoveDefinitionException, TransitionDefinitionException {
        List<Move> legalMoves = findLegals(role, state, machine);
        Move bestMove = null;
        MCTSNodeKey bestKey = null;
        double bestScore = -Double.MAX_VALUE;
        if (machine.getRoles().size() > 1) {
            for (Move move : legalMoves) {
                MCTSNodeKey newKey = new MCTSNodeKey(state, move);
                double score;
                int count;
                try {
                    score = this.state_tree.get(newKey).getMean();
                    count = this.state_tree.get(newKey).getCount();
                } catch (Exception nullPointerException) {
                    score = -Double.MAX_VALUE;
                    count = 0;
                }

                System.out.println("" + move + ": " + score + "\t" + "(" + count + ")");

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                    bestKey = newKey;
                }
            }
        } else {
            for (Move move : legalMoves) {
                List<Move> actions = new ArrayList<Move>();
                actions.add(move);
                MachineState nextState = this.findNext(actions, state, machine);
                MCTSNodeKey newKey = new MCTSNodeKey(nextState, null);
                double score;
                int count;
                try {
                    score = this.state_tree.get(newKey).getMean();
                    count = this.state_tree.get(newKey).getCount();
                } catch (Exception nullPointerException) {
                    score = -Double.MAX_VALUE;
                    count = 0;
                }

                System.out.println("" + move + ": " + score + "\t" + "(" + count + ")");

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                    bestKey = newKey;
                }
            }
        }
        System.out.println("bestScore: " + bestScore);
        System.out.println("best count: " + this.state_tree.get(bestKey).getCount());
        return bestMove;

    }

    private void step(Role role, MachineState state, StateMachine machine)
            throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        MCTSNodeKey key = new MCTSNodeKey(state, null);
        MCTSNodeKey selectedKey = null;
        if (machine.getRoles().size() > 1) {
            selectedKey = multiPlayerSelect(role, key, machine);
        } else {
            selectedKey = singlePlayerSelect(role, key, machine);
        }
        MCTSNodeKey currKey = selectedKey;
        List<MachineState> history = new ArrayList<MachineState>();
        boolean done = false;
        while (this.state_tree.containsKey(currKey)) {
            history.add(currKey.getState());
            currKey = this.state_tree.get(currKey).getParent();
            if (key.equals(currKey)) {
                done = true;
            }
        }
        // expand does not seem needed?
        for (int i = 0; i < 1; i++) {
            simulate(role, selectedKey);
        }
    }

    /**
     * run simulation and does back-propagation automatically
     *
     * @param selectedState
     * @throws MoveDefinitionException
     * @throws TransitionDefinitionException
     * @throws GoalDefinitionException
     */
    private void simulate(Role myRole, MCTSNodeKey key)
            throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        StateMachine machine = getStateMachine();
        MachineState state = key.getState();
        Move move = key.getMove();

        MachineState currState = state;
        List<Role> roles = machine.getRoles();

        if (move != null) {
            List<Move> actions = Arrays.asList(new Move[roles.size()]);
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i) != myRole) {
                    List<Move> options = findLegals(roles.get(i), currState, machine);
                    actions.set(i, options.get(RAND.nextInt(options.size())));
                } else {
                    actions.set(i, move);
                }
            }
            currState = this.findNext(actions, currState, machine);
        }

        int depth = 0;
        while (!this.findTerminalp(currState, machine)) {
            List<Move> actions = Arrays.asList(new Move[roles.size()]);
            for (int i = 0; i < roles.size(); i++) {
                List<Move> options = findLegals(roles.get(i), currState, machine);
                actions.set(i, options.get(RAND.nextInt(options.size())));
            }
            currState = this.findNext(actions, currState, machine);
            depth++;
//            System.out.println(depth);
//            System.out.println(currState);
        }
        double reward = this.findReward(myRole, currState, machine);
        this.state_tree.get(key).addReward(reward);

    }

    private MCTSNodeKey multiPlayerSelect(Role role, MCTSNodeKey key, StateMachine machine)
            throws MoveDefinitionException, TransitionDefinitionException {
        if (!state_tree.containsKey(key)) {
            // should only occur at the very start unless it did not manage to run depth
            // charges for all the children
            this.state_tree.put(key, new MCTSNodeValue(this.state_tree, null));
            return key;
        } else if (this.findTerminalp(key.getState(), machine)) {
            // if this is terminal, move should be null
            return key;
        } else {
            MachineState state = key.getState();
            Move move = key.getMove();
            if (move == null) {
                double bestScore = -Double.MAX_VALUE;
                List<MCTSNodeKey> selectedKeys = new ArrayList<MCTSNodeKey>();
                MCTSNodeKey selectedKey = null;
                List<Move> legalMoves = findLegals(role, state, machine);
                for (Move myMove : legalMoves) {
                    MCTSNodeKey newKey = new MCTSNodeKey(state, myMove);
                    if (!state_tree.containsKey(newKey)) {
                        this.state_tree.put(newKey, new MCTSNodeValue(this.state_tree, key));
                        return newKey;
                    } else {
                        double score = selectfn_max(role, newKey, machine);
                        if (score > bestScore) {
                            selectedKeys.clear();
                            selectedKeys.add(newKey);
                            bestScore = score;
                        } else if (score == bestScore) {
                            selectedKeys.add(newKey);
                        }
                    }
                }
                if (selectedKeys.size() == 0) {
                    System.out.println(legalMoves.size());
                }
                selectedKey = selectedKeys.get(RAND.nextInt(selectedKeys.size()));
                this.state_tree.get(selectedKey).setParent(key);
                return multiPlayerSelect(role, selectedKey, machine);
            } else {
                // choose opponents' moves
                List<Role> roles = machine.getRoles();
                List<List<Move>> allMovesList = new ArrayList<List<Move>>();
                for (Role role2 : roles) {
                    if (!role2.equals(role)) {
                        List<Move> oppMoves = findLegals(role2, state, machine);
                        allMovesList.add(oppMoves);
                    } else {
                        List<Move> myMoveList = new ArrayList<Move>();
                        myMoveList.add(move);
                        allMovesList.add(myMoveList);
                    }
                }
                List<List<Move>> allPossibilities = cartesianProduct(allMovesList);
                double bestScore = Double.MAX_VALUE;
                List<MCTSNodeKey> selectedKeys = new ArrayList<MCTSNodeKey>();
                MCTSNodeKey selectedKey = null;
                for (List<Move> actions : allPossibilities) {
                    MachineState nextState = this.findNext(actions, state, machine);
                    MCTSNodeKey newKey = new MCTSNodeKey(nextState, null);
                    if (!state_tree.containsKey(newKey)) {
                        this.state_tree.put(newKey, new MCTSNodeValue(this.state_tree, key));
                        return newKey;
                    } else {
                        double score = selectfn_min(role, newKey, machine);
                        if (score < bestScore) {
                            selectedKeys.clear();
                            selectedKeys.add(newKey);
                            bestScore = score;
                        } else if (score == bestScore) {
                            selectedKeys.add(newKey);
                        }
                    }
                }
                if (selectedKeys.size() == 0) {
                    System.out.println(allPossibilities.size());
                    List<Move> actions = allPossibilities.get(0);
                    MachineState nextState = this.findNext(actions, state, machine);
                    MCTSNodeKey newKey = new MCTSNodeKey(nextState, null);
                    double score = selectfn_min(role, newKey, machine);
                    System.out.println(score);
                    MCTSNodeValue val = this.state_tree.get(newKey);
                    System.out.println(val);
                    System.out.println("node count: " + val.getCount());
                    System.out.println("node mean: " + val.getMean());
                    MCTSNodeValue par_val = this.state_tree.get(val.getParent());
                    System.out.println("parent count: " + par_val.getCount());
                }
//                System.out.println(selectedKeys.size());
                selectedKey = selectedKeys.get(RAND.nextInt(selectedKeys.size()));
                this.state_tree.get(selectedKey).setParent(key);
                return multiPlayerSelect(role, selectedKey, machine);
            }

        }
    }

    private MCTSNodeKey singlePlayerSelect(Role role, MCTSNodeKey key, StateMachine machine)
            throws MoveDefinitionException, TransitionDefinitionException {
        if (!state_tree.containsKey(key)) {
            // should only occur at the very start unless it did not manage to run depth
            // charges for all the children
            this.state_tree.put(key, new MCTSNodeValue(this.state_tree, null));
            return key;
        } else if (this.findTerminalp(key.getState(), machine)) {
            // if this is terminal, move should be null
            return key;
        } else {
            MachineState state = key.getState();
            Move move = key.getMove();
            if (move == null) {
                double bestScore = -Double.MAX_VALUE;
                List<MCTSNodeKey> selectedKeys = new ArrayList<MCTSNodeKey>();
                MCTSNodeKey selectedKey = null;
                List<Move> legalMoves = findLegals(role, state, machine);
                for (Move myMove : legalMoves) {
                    List<Move> actions = new ArrayList<Move>();
                    actions.add(myMove);
                    MachineState nextState = this.findNext(actions, state, machine);
                    MCTSNodeKey newKey = new MCTSNodeKey(nextState, null);
                    if (!state_tree.containsKey(newKey)) {
                        this.state_tree.put(newKey, new MCTSNodeValue(this.state_tree, key));
                        return newKey;
                    } else {
                        double score = selectfn_max(role, newKey, machine);
                        if (score > bestScore) {
                            selectedKeys.clear();
                            selectedKeys.add(newKey);
                            bestScore = score;
                        } else if (score == bestScore) {
                            selectedKeys.add(newKey);
                        }
                    }
                }
                selectedKey = selectedKeys.get(RAND.nextInt(selectedKeys.size()));
                this.state_tree.get(selectedKey).setParent(key);
                return singlePlayerSelect(role, selectedKey, machine);
            } else {
                System.out.println("ERROR!!!! Don't come here!");
                return null;
            }

        }
    }

    protected <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> resultLists = new ArrayList<List<T>>();
        if (lists.size() == 0) {
            resultLists.add(new ArrayList<T>());
            return resultLists;
        } else {
            List<T> firstList = lists.get(0);
            List<List<T>> remainingLists = cartesianProduct(lists.subList(1, lists.size()));
            for (T condition : firstList) {
                for (List<T> remainingList : remainingLists) {
                    ArrayList<T> resultList = new ArrayList<T>();
                    resultList.add(condition);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }

    private double selectfn_max(Role role, MCTSNodeKey key, StateMachine machine) {
        MCTSNodeValue node = this.state_tree.get(key);
        MCTSNodeValue parentNode = this.state_tree.get(node.getParent());
        return node.getMean() + this.explore_weight * Math.sqrt(Math.log(parentNode.getCount()) / node.getCount());
    }

    private double selectfn_min(Role role, MCTSNodeKey key, StateMachine machine) {
        MCTSNodeValue node = this.state_tree.get(key);
        MCTSNodeValue parentNode = this.state_tree.get(node.getParent());
        return node.getMean() - this.explore_weight * Math.sqrt(Math.log(parentNode.getCount()) / node.getCount());
    }

    @Override
    public String getName() {
        return "MCTS Player";
    }

}
