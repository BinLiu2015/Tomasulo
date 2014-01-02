package entries;

public enum InstructionType {
	LW, SW, 
	JMP,   // Unconditional Branch
	BEQ, // Conitional Branch
	JALR,  // Jump and link  
	RET,
	ADD, SUB, NAND, MUL, ADDI,
	END // Pseudo End
}