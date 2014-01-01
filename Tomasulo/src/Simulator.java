import java.util.*;
import java.io.*;

import memory.Memory;
import entries.*;
import buffers.*;

public class Simulator {

	// TODO: Implementing actual memory access
	// TODO: Change arrays of instruction buffers and rob to use buffer classes

	static int cycle; // Current cycle
	static int pc;
	static boolean stopFetch;

	static int[] regFile;
	static int[] regStatus;
	static InstructionEntry[] instructionMemory;

	static InstructionBuffer instructionBuffer;
	static ReorderBuffer reorderBuffer;

	static ReservationStation[] resvStations;

	static Memory memory;

	static HashMap<InstructionType, Integer> instructionCycles;
	// Number of cycles to be taken by each instruction type

	static boolean programDone; // Indicates that the last psuedo end
								// instruction has been fetched
	static boolean commitDone; // Indicates that the last instruction has been
								// committed

	static void initializeDefault() {
		instructionCycles = new HashMap<InstructionType, Integer>();
		instructionCycles.put(InstructionType.ADD, 1);
		instructionCycles.put(InstructionType.BRANCH, 1);
		instructionCycles.put(InstructionType.LOAD, 1);
		instructionCycles.put(InstructionType.MULT, 1);
		instructionCycles.put(InstructionType.STORE, 1);

		pc = 0;
		cycle = 0;

		regFile = new int[8]; // repeat for all arrays
		memory = new Memory(10);

		programDone = false;
		commitDone = false;
		stopFetch = false;
	}

	static boolean isMemory(InstructionType type) {
		return type == InstructionType.LOAD || type == InstructionType.STORE;
	}

	static boolean writesToReg(InstructionType type) {
		return type == InstructionType.LOAD || type == InstructionType.JUMPL
				|| type == InstructionType.ADD || type == InstructionType.SUB
				|| type == InstructionType.ADDI || type == InstructionType.NAND
				|| type == InstructionType.MULT;

	}

	static ReservationStation getEmptyRS(InstructionType type) {
		// TODO: Check how to handle similar instruction types
		for (int i = 0; i < resvStations.length; i++) {
			ReservationStation entry = resvStations[i];
			if (entry.busy == false && entry.type == type)
				return entry;
		}
		return null;
	}

	static void commit() {
		// TODO: Flushing when wrong branch prediction

		if (reorderBuffer.isEmpty()) {
			if (programDone)
				commitDone = true;
			return; // Empty buffer
		}

		RobEntry entry = (RobEntry) reorderBuffer.getFirst();

		switch (entry.getType()) {
		case STORE:
			memory.writeData(entry.getDest(), entry.getVal());

			// Assume immediate value is in VALUE field of rob entry
			// Assume original PC + 1 in the DESTINATION field
		case BRANCH:
			if ((entry.getVal() > 0 && entry.isBranchTaken())
					|| (entry.getVal() < 0 && !entry.isBranchTaken())) {
				reorderBuffer.flush();
				instructionBuffer.flush();

				for (ReservationStation rs : resvStations)
					rs.clear();
				pc = (entry.isBranchTaken()) ? entry.getDest() : entry
						.getDest() + entry.getVal();
			}
			break;
		default:
			regFile[entry.getDest()] = entry.getVal();
		}

		reorderBuffer.moveHead();
	}

	static void write() {
		for (int i = 0; i < resvStations.length; i++) {
			ReservationStation entry = resvStations[i];
			if (entry.busy && entry.stage == Stage.EXECUTE
					&& entry.remainingCycles <= 0) {
				// Updating ROB

				entry.stage = Stage.WRITE;
				Integer result = entry.run(); // Value from functional unit
				if (result == null)
					return;

				// Updating Reorder Buffer Entry
				RobEntry robEntry = (RobEntry) reorderBuffer
						.get(entry.getRob());
				robEntry.setReady();
				robEntry.setValue(result);

				if (writesToReg(entry.getType())
						&& regStatus[robEntry.getDest()] == entry.getRob()) {
					regStatus[robEntry.getDest()] = -1;
				}

				// Updating reservation stations
				for (int j = 0; j < resvStations.length; j++) {
					ReservationStation rs = resvStations[j];
					if (entry.getRob() == rs.qj) {
						rs.qj = 0;
						rs.vj = result;
					}
					if (entry.getRob() == rs.qk) {
						rs.qk = 0;
						rs.vk = result;
					}
				}
			}
			entry.busy = false;
			// TODO: Clear Reservation Station
		}
	}

	static void execute() {
		for (int i = 0; i < resvStations.length; i++) {
			ReservationStation entry = resvStations[i];
			if (entry.busy == false)
				continue;

			if (entry.stage == Stage.ISSUE && entry.qj == 0 && entry.qk == 0) {
				entry.stage = Stage.EXECUTE;
				if (isMemory(entry.type))
					entry.address = entry.vj + entry.address;
			} else if (entry.stage == Stage.EXECUTE
					&& entry.remainingCycles > 0)
				entry.remainingCycles--;
		}
	}

	static void issue() {
		if (instructionBuffer.isEmpty() || reorderBuffer.isFull())
			return;

		InstructionEntry entry = (InstructionEntry) instructionBuffer
				.getFirst();
		ReservationStation rs = getEmptyRS(entry.getType());

		if (rs != null) {
			// Issue, fill in reservation station and rob and register status
			// table
			instructionBuffer.moveHead();

			if (regStatus[entry.getRS()] != -1) {
				rs.qj = regStatus[entry.getRS()];
				rs.vj = 0;
			} else {
				rs.qj = 0;
				rs.vj = regFile[entry.getRS()];
			}

			if (entry.getType() == InstructionType.ADD
					|| entry.getType() == InstructionType.SUB
					|| entry.getType() == InstructionType.MULT
					|| entry.getType() == InstructionType.NAND) {
				// Instructions that have two source operands
				if (regStatus[entry.getRT()] != -1) {
					rs.qk = regStatus[entry.getRT()];
					rs.vk = 0;
				} else {
					rs.qk = 0;
					rs.vk = regFile[entry.getRT()];
				}
			} else {
				// Immediate value
				rs.qk = 0;
				rs.vk = 0;
				rs.address = entry.getRT();
			}

			// TODO: Store handling

			rs.setRob(reorderBuffer.tailIndex());
			if (instructionCycles.containsKey(entry.getType())) {
				rs.setCycles(instructionCycles.get(entry.getType()));
			} else {
				rs.setCycles(1);
			}
			rs.setBusy(true);
			rs.setStage(Stage.ISSUE);
			rs.setType(entry.getType());

			int destination = entry.getRD();

			if (writesToReg(rs.getType())) {
				regStatus[destination] = reorderBuffer.tailIndex();
			} else {
				destination = -1;
			}

			RobEntry robEntry = new RobEntry(destination, rs.getType());
			reorderBuffer.add(robEntry);
			reorderBuffer.moveHead();

		}
	}

	static void fetch() {
		// TODO: Check that the instruction fetched is the pseudo end
		InstructionEntry inst = (InstructionEntry) memory
				.readInstruction(pc * 2);

		if (inst != null && !instructionBuffer.isFull()) {

			switch (inst.getType()) {
			case JUMP: {
				pc += 1 + regFile[inst.getRD()] + inst.getRS();
				break;
			}
			case BRANCH: {
				pc += inst.getRT() + 1;
				instructionBuffer.add(inst);
				break;
			}
			case JUMPL: {
				pc += regFile[inst.getRS()];
				regFile[inst.getRD()] = pc + 1;
				break;
			}
			case RET: {
				pc = regFile[inst.getRD()];
				break;
			}
			default:
				pc += 1; // For now, word consists of 2 bytes, and we're
							// accessing the first byte
				instructionBuffer.add(inst);
				break;
			}
		}
	}

	public static void main(String[] args) throws IOException {

		initializeDefault();

		while (pc < instructionMemory.length) {
			commit();
			write();
			execute();
			issue();
			fetch();

			cycle++;
		}
	}

}
