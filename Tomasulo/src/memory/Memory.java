package memory;

public class Memory {
	
	// Example memory access assuming no caches
	
	Object[] memory;
	boolean reading;
	int cycles;
	int curAddress;
	
	final int accessTime;
	
	public Memory(int accessTime){
		memory = new Object[65536]; // Byte addressable, each word is 2 bytes, entry in first byte
		this.accessTime = accessTime;
	}
	
	public Object read(int address){
		if(!reading){
			reading = true;
			curAddress = address;
			cycles = accessTime-1;
			return null;
		} else if(cycles > 0 && curAddress == address){
			cycles--;
			return null;
		} else if(curAddress == address){
			reading = false;
			return memory[address];
		} else{
			return null;
		}
	}
	
}
