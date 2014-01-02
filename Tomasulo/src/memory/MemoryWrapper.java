package memory;

//TODO: Store array of objects rather than integers
//TODO: Have just ONE place where memory size is set
//TODO: Instruction cache vs. data cache L1

//TODO: Have memory take array list of instructions
//TODO: Input from user for cache hierarchy

import java.util.*;
import entries.*;
import memory.*;

public class MemoryWrapper {

	Instruction i;
	InstructionEntry inst;
	Integer val;

	boolean busy;
	Cache c;

	boolean readingInstruction;

	public MemoryWrapper() {
		Memory mem = new Memory(1024, 5);
		Memory.store(0, 4);
		L1Cache nc = new L1Cache(L1Cache.WRITE_BACK, 10, 256, 32, 2);
		L2Cache nc2 = new L2Cache(L1Cache.WRITE_BACK, 10, 256, 64, 2);
		L3Cache nc3 = new L3Cache(L1Cache.WRITE_BACK, 10, 512, 128, 2);
		c = new Cache(3, nc, nc2, nc3);
	}
	
	public Integer readData(int address, int currentTime) {
		if (!busy) {
			i = new Instruction();
			try {
				val = (Integer) c.read(address, currentTime, i);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(e);
			}
			return null;
		} else {
			if (i.getCacheEndTime() != currentTime) {
				return null;
			} else {
				i = null;
				busy = false;
				return (Integer) val;
			}
		}
	}

	public InstructionEntry readInstruction(int address, int currentTime) {
		if (!readingInstruction) {
			i = new Instruction();
			try {
				inst = (InstructionEntry) c.read(address, currentTime, i);
			} catch (Exception e) {
				System.out.println(e);
			}
			return null;
		} else if (readingInstruction && i.getCacheEndTime() < currentTime) {
			return null;
		} else if (readingInstruction) {
			return inst;
		}
		return null;
	}

	public boolean writeData(int address, int val, int currentTime) {
		if (!busy) {
			i = new Instruction();
			try {
				c.write(address, val, currentTime, i);
			} catch (Exception e) {
				System.out.println(e);
			}
			return false;
		} else {
			if (i.getCacheEndTime() < currentTime)
				return false;
			else
				return true;
		}

	}
}
