package entries;

public class RobEntry implements Entry {

	int destination; // Register Index or Memory Address
	int value; // Value to be committed
	boolean branchTaken;

	boolean ready;

	InstructionType type;

	public RobEntry(int destination, InstructionType type) {
		this.destination = destination;
		this.type = type;
	}

	public void setReady() {
		ready = true;
	}

	public void setValue(int val) {
		value = val;
	}

	public void setDestination(int destination) {
		this.destination = destination;
	}

	public int getDest() {
		return destination;
	}

	public int getVal() {
		return value;
	}

	public InstructionType getType() {
		return type;
	}

	public boolean isBranchTaken() {
		return branchTaken;
	}

	public void setBranchTaken(boolean b) {
		branchTaken = b;
	}
	
	public boolean isReady(){
		return ready;
	}

}
