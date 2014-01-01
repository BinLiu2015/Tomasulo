package entries;


public class InstructionEntry implements Entry{
	
	int rs, rt; // Address is rt in case of load/store
	int rd;
	
	InstructionType type;
	
}
