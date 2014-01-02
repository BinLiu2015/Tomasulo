package memory;

public class Memory {
	public static int[] mem;
	private static int accessTime;

	public Memory(int size, int access) {
		mem = new int[size / 16];
		accessTime = access;
	}

	public static int load(int address) {
		return mem[address];
	}

	public static void store(int address, int value) {
		mem[address] = value;
	}

	public static int getAccessTime() {
		return accessTime;
	}
	
	public String toString(){
		String s = "";
		for(int i=0;i<mem.length; i++){
			s += "Mem[" + i + "] :" + mem[i] + "   "; 
			if(i%10 == 0)
				s+= "\n";
		}
		return s;
		
	}

}
