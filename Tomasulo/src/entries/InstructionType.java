package entries;

public enum InstructionType {
	LOAD, STORE, 
	ADD, ADDI, MULT, SUB,
	BRANCH, // Conitional Branch
	JUMP,   // Unconditional Branch
	END,
	JUMPL,  // Jump and link  
	RET,
	NAND
}