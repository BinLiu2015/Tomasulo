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
		Memory mem = new Memory(1024, 10);
		Memory.store(0, 4);
		L1Cache nc = new L1Cache(L1Cache.WRITE_BACK, 10, 256, 32, 2);
		L2Cache nc2 = new L2Cache(L1Cache.WRITE_BACK, 10, 256, 64, 2);
		L3Cache nc3 = new L3Cache(L1Cache.WRITE_BACK, 10, 512, 128, 2);

		nc3.setL1(nc);
		nc3.setL2(nc2);
		nc2.setL3(nc3);
		nc2.setL1(nc);
		nc.setL2(nc2);
		nc.setL3(nc3);
		c = new Cache(3, nc, nc2, nc3);
	}

	public MemoryWrapper(int memoryAccessTime, L1Cache nc, L2Cache nc2,
			L3Cache nc3) {
		Memory mem = new Memory(1024, memoryAccessTime);
		if (nc3 != null) {
			nc3.setL1(nc);
			nc3.setL2(nc2);
		}
		if (nc2 != null) {
			nc2.setL3(nc3);
			nc2.setL1(nc);
		}
		nc.setL2(nc2);
		nc.setL3(nc3);
		c = new Cache(3, nc, nc2, nc3);

	}

	public void loadInstructions(ArrayList<InstructionEntry> instructionList,
			int startIndex) {
		for (int i = 0; i < instructionList.size(); i++) {
			Memory.store(i * 2 + startIndex, instructionList.get(i));
		}
	}

	public Integer readData(int address, int currentTime) {
		if (!busy) {
			i = new Instruction();
			try {
				val = (Integer) c.read(address, currentTime, i, false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
			}
			return null;
		} else {
			if (currentTime < i.getCacheEndTime()) {
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
			readingInstruction = true;
			try {
				inst = (InstructionEntry) c.read(address, currentTime, i, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		} else if (readingInstruction && currentTime < i.getCacheEndTime()) {
			return null;
		} else if (readingInstruction) {
			readingInstruction = false;
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
