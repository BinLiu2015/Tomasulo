
public class ReorderBuffer {
	
	int destination; //Register Index or Memory Address
	int value; //Value to be committed
	
	boolean ready;
	
	InstructionType type;
	
}
