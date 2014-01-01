package entries;

public class InstructionEntry implements Entry{

	private int rs, rt; // Address is rt in case of load/store
	private int rd;

	private InstructionType type;

	public InstructionType getType() {
		return type;
	}

	public int getRS() {
		return rs;
	}

	public int getRT() {
		return rt;
	}

	public int getRD() {
		return rd;
	}

}
