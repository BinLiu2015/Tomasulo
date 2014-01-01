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
			return vj + vk;
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
