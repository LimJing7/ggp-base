import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MCTSNodeValue {

    private MCTSNodeKey parent;
    private List<MCTSNodeKey> parents;
    private HashMap<MCTSNodeKey, MCTSNodeValue> containing_map;
    private double mean;
    private int count;

    public MCTSNodeValue(HashMap<MCTSNodeKey, MCTSNodeValue> containing_map, MCTSNodeKey parent) {
        this.containing_map = containing_map;
        this.parents = new ArrayList<MCTSNodeKey>();
        if (parent != null) {
            this.parents.add(parent);
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

//        for (MCTSNodeKey parent : parents) {
//            if (this.containing_map.containsKey(parent)) {
//                this.containing_map.get(parent).addReward(value);
//            }
//        }
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

    public List<MCTSNodeKey> getParents() {
        return parents;
    }

    public void setParent(MCTSNodeKey newParent) {
        this.containing_map.get(this.parent).removeChild(this);
        this.parent = newParent;
        this.containing_map.get(parent).addChild(this);
    }

    public void updateParent(MCTSNodeKey newParent) {
        this.parent = newParent;
        this.parents.add(newParent);
    }

    private void removeChild(MCTSNodeValue oldChildValue) {
//        double a = this.mean;
//        double b = oldChildValue.getMean();
//        int c = this.count;
//        int d = oldChildValue.count;
        this.mean = this.mean * this.count - oldChildValue.getMean() * oldChildValue.getCount();
        this.count -= oldChildValue.getCount();
        if (this.count == 0) {
            this.mean = 0;
        } else {
            this.mean /= this.count;
        }
//        if (Double.isNaN(this.mean) || Double.isInfinite(this.mean)) {
//            System.out.println("*******");
//            System.out.println("a: " + a);
//            System.out.println("c: " + c);
//            System.out.println("b: " + b);
//            System.out.println("d: " + d);
//            System.out.println("*******");
//        }
    }

    private void addChild(MCTSNodeValue newChildValue) {
//        double a = this.mean;
//        double b = newChildValue.getMean();
//        int c = this.count;
//        int d = newChildValue.count;
        this.mean = this.mean * this.count + newChildValue.getMean() * newChildValue.getCount();
        this.count += newChildValue.getCount();
        this.mean /= this.count;
//        if (Double.isNaN(this.mean) || Double.isInfinite(this.mean)) {
//            System.out.println("*******");
//            System.out.println("a: " + a);
//            System.out.println("c: " + c);
//            System.out.println("b: " + b);
//            System.out.println("d: " + d);
//            System.out.println("*******");
//        }
    }
}
