import java.util.HashMap;

import org.ggp.base.util.statemachine.MachineState;

public class MCTSNode {

    private MachineState parent;
    private HashMap<MachineState, MCTSNode> containing_map;
    private double mean;
    private int count;

    public MCTSNode(HashMap<MachineState, MCTSNode> containing_map, MachineState parent) {
        this.containing_map = containing_map;
        if (parent != null) {
            this.parent = parent;
        }
        this.mean = 0;
        this.count = 0;
    }

    public void addReward(double value) {
        if (this.count == 0) {
            this.mean = value;
            this.count = 1;
        }
        this.mean = (this.mean * this.count + value) / (this.count + 1);
        this.count++;

        if (this.containing_map.containsKey(this.parent)) {
            this.containing_map.get(this.parent).addReward(value);
        }
    }

    public double getMean() {
        return mean;
    }

    public int getCount() {
        return count;
    }

    public MachineState getParent() {
        return parent;
    }
}
