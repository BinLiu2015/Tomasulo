import entries.InstructionType;


public class ReservationStation {
	
	int vj, vk;
	int qj, qk;
	int address;
	
	int rob; //Reorder Buffer Destination
	int remainingCycles;
	
	boolean busy;
	
	Stage stage;
	InstructionType type;
	
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
	public void setType(InstructionType type) {
		this.type = type;
	}
	
	public int getCycles(){
		return remainingCycles;
	}
	
	public void setCycles(int cycles){
		remainingCycles = cycles;
	}
	
	public void setBusy(boolean busy){
		this.busy = busy;
	}
	
	public boolean getBusy(){
		return busy;
	}
}
