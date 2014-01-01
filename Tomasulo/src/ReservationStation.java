import entries.InstructionType;


public class ReservationStation {
	
	int vj, vk;
	int qj, qk;
	int dest; //Reorder Buffer Destination
	int address;
	int remainingCycles;
	
	boolean busy;
	
	Stage stage;
	InstructionType type;
	
	public Integer run(){
		switch(type) {
		case ADD:
		case ADDI:
			return vj + vk;
			break;
		case SUB:
			return vj - vk;
			break;
		case MULT:
			return vj * vk;
			break;
		case NAND:
			return ~(vj & vk);
			break;
		case LOAD:
			return (Integer) Simulator.memory.read(address);
			break;
		case STORE:
			return Simulator.memory.writeData(address, vk) ? null : 1;
			break;
		case BRANCH:
			Simulator.reorderBuffer.get(dest);
			
		default:
			return null;
		}
	}

	public void clear() {
		vj = vk = qj = qk = dest = address = remainingCycles = 0;
		busy = false;
		type = null;
		stage = null;
	}
	
}
