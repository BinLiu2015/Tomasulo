package entries;


public class RobEntry {
	
	int destination; //Register Index or Memory Address
	int value; //Value to be committed
	
	boolean ready;
	
	InstructionType type;
	
}
