import buffers.ReorderBuffer;
import entries.InstructionType;
import entries.RobEntry;

public class ReservationStation {

	int vj, vk;
	int qj, qk;
	int address;

	int rob; // Reorder Buffer Destination
	int remainingCycles;

	boolean busy;

	Stage stage;
	final InstructionType type;
	InstructionType operation;

	public ReservationStation (InstructionType type){
		this.type = type;
	}
	
	public int getVj() {
		return vj;
	}

	public void setVj(int vj) {
		this.vj = vj;
	}

	public int getVk() {
		return vk;
	}

	public void setVk(int vk) {
		this.vk = vk;
	}

	public int getQj() {
		return qj;
	}

	public void setQj(int qj) {
		this.qj = qj;
	}

	public int getQk() {
		return qk;
	}

	public void setQk(int qk) {
		this.qk = qk;
	}

	public int getRob() {
		return rob;
	}

	public void setRob(int rob) {
		this.rob = rob;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	public InstructionType getType() {
		return type;
	}

	public int getCycles() {
		return remainingCycles;
	}

	public void setCycles(int cycles) {
		remainingCycles = cycles;
	}

	public void setBusy(boolean busy) {
		this.busy = busy;
	}

	public boolean getBusy() {
		return busy;
	}
	
	public void setOperation(InstructionType operation){
		this.operation = operation;
	}

	public Integer run() {
		switch (operation) {
		case ADD:
			return vj + vk;
		case ADDI:
			return vj + address;
		case SUB:
			return vj - vk;
		case MUL:
			return vj * vk;
		case NAND:
			return ~(vj & vk);
		case LW:
			address = vj + address;
			return (Integer) Simulator.memory.readData(address);
		case SW:
			address = vj + address;
			((RobEntry)Simulator.reorderBuffer.get(rob)).setDestination(address);
			return Simulator.memory.writeData(address, vk) ? null : 1;
		case BEQ:
			RobEntry robEntry = (RobEntry) Simulator.reorderBuffer.get(rob);
			robEntry.setBranchTaken(vj == vk);
			return address;

		default:
			return null;
		}
	}

	public void clear() {
		vj = vk = rob = address = remainingCycles = 0;
		qj = qk = -1;
		busy = false;
		operation = null;
		stage = null;
	}

}
