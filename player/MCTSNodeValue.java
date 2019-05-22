import java.util.HashMap;

public class MCTSNodeValue {

    private MCTSNodeKey parent;
    private HashMap<MCTSNodeKey, MCTSNodeValue> containing_map;
    private double mean;
    private int count;

    public MCTSNodeValue(HashMap<MCTSNodeKey, MCTSNodeValue> containing_map, MCTSNodeKey parent) {
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

    public MCTSNodeKey getParent() {
        return parent;
    }
}
