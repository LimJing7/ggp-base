package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;

@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at your
     * discretion.
     */
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

    /**
     * Computes if the state is terminal. Should return the value of the terminal
     * proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
        // TODO: Compute whether the MachineState is terminal.
    	markbases(state, getPropNet());
    	return propmarkp(getPropNet().getTerminalProposition());
    }

    /**
     * Computes the goal for a role in the current state. Should return the value of
     * the goal proposition that is true for that role. If there is not exactly one
     * goal proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role) throws GoalDefinitionException {
        // TODO: Compute the goal for role in state.
    	markbases(state, getPropNet());
    	Set<Proposition> rewards= getPropNet().getGoalPropositions().get(role);
    	for (Proposition prop : rewards) {
			if(propmarkp(prop)) {
				return getGoalValue(prop);
			}
		}
      	throw new GoalDefinitionException(state, role);
        //return -1;
    }

    /**
     * Returns the initial state. The initial state can be computed by only setting
     * the truth value of the INIT proposition to true, and then computing the
     * resulting state.
     */
    @Override
    public MachineState getInitialState() {
        // TODO: Compute the initial state.
    	// NOTE:  Not sure how to initialize propnet initial proposition: getPropNet().-- verify!
    	PropNet pnet= getPropNet();
    	Proposition ips= pnet.getInitProposition();
    	if (ips !=null) {
    		ips.setValue(true);
    	}
    	return getStateFromBase();
        //return null;
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role) throws MoveDefinitionException {
        // TODO: Compute legal moves.
    	Map<GdlSentence, Proposition> amap= getPropNet().getInputPropositions();
    	List<Move> actions= new ArrayList<Move>();
    	for (Proposition p : amap.values()) {
			actions.add(getMoveFromProposition(p));
		}
    	return actions;
        //return null;

    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException {
        // TODO: Compute legal moves.

    	markbases(state, this.getPropNet());
    	Set<Proposition> legals = this.getPropNet().getLegalPropositions().get(role);
    	List<Move> actions= new ArrayList<Move>();
    	for (Proposition p : legals) {
			if(propmarkp(p)) {
				actions.add(getMoveFromProposition(p));
			}
		}
        return actions;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException {
        // TODO: Compute the next state.
    	markactions(moves, this.getPropNet());
    	markbases(state, this.getPropNet());
    	Map<GdlSentence, Proposition> bases= this.getPropNet().getBasePropositions();
    	Set<GdlSentence> nextState= new HashSet<GdlSentence>();
    	for (GdlSentence gs : bases.keySet()) {
			Component cp= bases.get(gs).getSingleInput();
			if (propmarkp(cp.getSingleInput())) {
				nextState.add(gs);
			}
		}
        return new MachineState(nextState);
    }

    /**
     * This should compute the topological ordering of propositions. Each component
     * is either a proposition, logical gate, or transition. Logical gates and
     * transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt from
     * this ordering.
     *
     * The base propositions values are set from the MachineState that operations
     * are performed on and the input propositions are set from the Moves that
     * operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */
    public List<Proposition> getOrdering() {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // TODO: Compute the topological ordering.

        Set<Component> added = new HashSet<>();
        Set<Component> boundary = new HashSet<Component>();
        Set<Component> newBoundary = new HashSet<Component>();

        for (Component component : components) {
            if (component instanceof Transition) {
                added.add(component);
            }
        }

        // init
        for (Component component: components) {
            Set<Component> inputs = component.getInputs();
            int count = 0;
            for (Component input : inputs) {
                if (!(input instanceof Transition)) {
                    count++;
                }
            }
            if (count == 0) {
                boundary.add(component);
                if (component instanceof Proposition) {
                    order.add((Proposition) component);
                }
            }
        }
        added.addAll(boundary);

//        propNet.renderToFile("/home/limjing7/Desktop/lala.dot");

        int steps = 1;
        while (order.size() != propositions.size() && boundary.size() != 0) {
            steps++;
            for (Component component : boundary) {
                Set<Component> outputs = component.getOutputs();
                for (Component output : outputs) {
                    Set<Component> inputs = output.getInputs();
                    if (added.containsAll(inputs)) {
                        if (!added.contains(output)) {
                            newBoundary.add(output);
                            if (output instanceof Proposition) {
                                order.add((Proposition) output);
                            }
                        }
                    }
                }
            }
            added.addAll(newBoundary);
            boundary.clear();
            boundary.addAll(newBoundary);
            newBoundary.clear();
//            System.out.println("steps: " + steps);
//            System.out.println("boundary size: " + boundary.size());
//            System.out.println("left: " + (propositions.size() - order.size()));
//            System.out.println("set left: " + (components.size() - added.size()));
        }

//        for (Component component : components) {
//            if (!added.contains(component)) {
//                System.out.println(component);
//            }
//        }

        return order;
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from
     * inputPropositions. and accordingly set their values etc. This is a naive
     * implementation when coupled with setting input values, feel free to change
     * this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves) {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++) {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     *
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p) {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     *
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition) {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState from the true
     * BasePropositions. This is correct but slower than more advanced
     * implementations You need not use this method!
     *
     * @return PropNetMachineState
     */
    public MachineState getStateFromBase() {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values()) {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue()) {
                contents.add(p.getName());
            }

        }
        return new MachineState(contents);
    }


    //********** Marking functions ****************
    private void markbases(MachineState state, PropNet propNet) {
    	clearpropnet(propNet);
    	Map<GdlSentence, Proposition> props= propNet.getBasePropositions();
    	Set<GdlSentence> stateContents= state.getContents();
    	for (GdlSentence gdlSentence : stateContents) {
			props.get(gdlSentence).setValue(true);
		}
    }

    private void clearpropnet(PropNet propNet) {
    	Proposition ips= propNet.getInitProposition();
    	if (ips !=null) {
    		ips.setValue(false);
    	}
    	Map<GdlSentence, Proposition> props= propNet.getBasePropositions();
    	for (GdlSentence gdlSentence : props.keySet()) {
			props.get(gdlSentence).setValue(false);
		}
    }

    private void markactions(List<Move> moves, PropNet propNet) {
    	Map<GdlSentence, Proposition> props= propNet.getInputPropositions();
    	for (GdlSentence gdlSentence : props.keySet()) {
			props.get(gdlSentence).setValue(false);
		}
    	//Not fully sure here..but I think should work
    	List<GdlSentence> todo= toDoes(moves);
    	for (GdlSentence move : todo) {
			props.get(move).setValue(true);
		}

    }

    //****** propagating view **********************
    private boolean propmarkp(Component cp) {

        if (cp instanceof Proposition) {
            Proposition proposition = (Proposition) cp;
            // "Base"
            if (cp.getInputs().size()==1 && cp.getSingleInput() instanceof Transition) {
                return cp.getValue();
            }
            //"Input"
            else if((proposition.getName().getName().getValue().equals("does"))) {
                return cp.getValue();
            }
            //"init prop"
            else if ((proposition.getName().getName().getValue().toUpperCase().equals("INIT"))) {
               return cp.getValue();
           }
            //"view"
            else if(cp.getInputs().size()==1) {
                return this.propmarkp(cp.getSingleInput());
            }
            else {
                return false;
            }
        }

    	//"Negation"
    	else if (cp instanceof Not) {
    		return this.propmarknegation(cp);
    	}
    	//"Conjunction"
    	else if (cp instanceof And) {
    		return this.propmarkconjunction(cp);
    	}
    	//"disjunction"
    	else if(cp instanceof Or) {
    		return this.propmarkdisjunction(cp);
    	}
    	//"constant"
    	else if(cp instanceof Constant) {
    		return cp.getValue();
    	}
    	else {
    		return false;
    	}
    }

    private boolean propmarknegation(Component cp) {
    	return !propmarkp(cp.getSingleInput());
    }
    private boolean propmarkconjunction(Component cp) {
    	Set<Component> sources= cp.getInputs();
    	for (Component s : sources) {
			if(!propmarkp(s)) {
				return false;
			}
		}
    	return true;
    }
    private boolean propmarkdisjunction(Component cp) {
    	Set<Component> sources= cp.getInputs();
    	for (Component s : sources) {
			if(propmarkp(s)) {
				return true;
			}
		}
    	return false;
    }
    //****** propagating view **********************

    public void setPropNet(PropNet propNet) {
    	this.propNet= propNet;
    }

	public MachineState performPropNetDepthCharge(MachineState state,
			int[] theDepth) throws TransitionDefinitionException,
			MoveDefinitionException {
		return performDepthCharge(state, theDepth);
	}
    //********** Marking functions ****************
	public PropNet getPropNet() {
		return propNet;
	}
}