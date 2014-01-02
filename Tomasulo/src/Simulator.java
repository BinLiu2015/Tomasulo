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
		regStatus = new int[8];
		memory = new Memory(10);

		programDone = false;
		commitDone = false;
		stopFetch = false;
	}

	static boolean isMemory(InstructionType type) {
		return type == InstructionType.LOAD || type == InstructionType.STORE;
	}

	static boolean writesToReg(InstructionType type) {
		return type == InstructionType.LOAD || type == InstructionType.ADD
				|| type == InstructionType.SUB || type == InstructionType.ADDI
				|| type == InstructionType.NAND || type == InstructionType.MULT;

	}

	static ReservationStation getEmptyRS(InstructionType type) {
		// TODO: Check how to handle similar instruction types
		for (int i = 0; i < resvStations.length; i++) {
			ReservationStation entry = resvStations[i];
			if (entry.busy == false && entry.type == getFunctionalUnit(type))
				return entry;
		}
		return null;
	}

	static InstructionType getFunctionalUnit(InstructionType type) {
		switch (type) {
		case ADD:
		case ADDI:
			return InstructionType.ADD;
		default:
			return type;
		}
	}

	static void commit() {
		if (reorderBuffer.isEmpty()) {
			if (programDone)
				commitDone = true;
			return; // Empty buffer
		}

		RobEntry entry = (RobEntry) reorderBuffer.getFirst();

		switch (entry.getType()) {
		case STORE:
			if (memory.writeData(entry.getDest(), entry.getVal()))
				reorderBuffer.moveHead();
			break;
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
			reorderBuffer.moveHead();
		}

	}

	static void write() {
		for (int i = 0; i < resvStations.length; i++) {
			ReservationStation rs = resvStations[i];
			if (rs.busy && rs.stage == Stage.EXECUTE && rs.remainingCycles <= 0) {
				// Updating ROB

				rs.stage = Stage.WRITE;
				Integer result = rs.run(); // Value from functional unit
				if (result == null)
					return;

				// Updating Reorder Buffer Entry
				RobEntry robEntry = (RobEntry) reorderBuffer.get(rs.getRob());
				robEntry.setReady();
				robEntry.setValue(result);

				if (writesToReg(rs.getType())
						&& regStatus[robEntry.getDest()] == rs.getRob()) {
					regStatus[robEntry.getDest()] = -1;
				}

				// Updating reservation stations
				for (int j = 0; j < resvStations.length; j++) {
					ReservationStation resvStation = resvStations[j];
					if (rs.getRob() == resvStation.qj) {
						resvStation.qj = 0;
						resvStation.vj = result;
					}
					if (rs.getRob() == resvStation.qk) {
						resvStation.qk = 0;
						resvStation.vk = result;
					}
				}
			}
			rs.busy = false;
			rs.clear();
		}
	}

	static void execute() {
		for (int i = 0; i < resvStations.length; i++) {
			ReservationStation entry = resvStations[i];
			if (entry.busy == false)
				continue;

			if (entry.stage == Stage.ISSUE && entry.qj == 0 && entry.qk == 0)
				entry.stage = Stage.EXECUTE;
			else if (entry.stage == Stage.EXECUTE && entry.remainingCycles > 0)
				entry.remainingCycles--;
		}
	}

	static void issue() {
		if (instructionBuffer.isEmpty() || reorderBuffer.isFull())
			return;

		InstructionEntry inst = (InstructionEntry) instructionBuffer.getFirst();
		ReservationStation rs = getEmptyRS(inst.getType());

		if (rs != null) {
			// Issue, fill in reservation station and rob and register status
			// table
			instructionBuffer.moveHead();

			if (regStatus[inst.getRS()] != -1) {
				rs.qj = regStatus[inst.getRS()];
				rs.vj = 0;
			} else {
				rs.qj = 0;
				rs.vj = regFile[inst.getRS()];
			}

			switch (inst.getType()) {
			case ADD:
			case SUB:
			case MULT:
			case NAND:
				if (regStatus[inst.getRT()] != -1) {
					rs.qk = regStatus[inst.getRT()];
					rs.vk = 0;
				} else {
					rs.qk = 0;
					rs.vk = regFile[inst.getRT()];
				}
				break;
			case BRANCH:
			case STORE:
				if (regStatus[inst.getRD()] != -1) {
					rs.qk = regStatus[inst.getRT()];
					rs.vk = 0;
				} else {
					rs.qk = 0;
					rs.vk = regFile[inst.getRT()];
				}
				break;
			default:
				rs.qk = 0;
				rs.vk = 0;
				rs.address = inst.getRT();
			}

			rs.setRob(reorderBuffer.tailIndex());
			if (instructionCycles.containsKey(inst.getType())) {
				rs.setCycles(instructionCycles.get(inst.getType()));
			} else {
				rs.setCycles(1);
			}
			rs.setBusy(true);
			rs.setStage(Stage.ISSUE);

			int destination = inst.getRD();

			if (writesToReg(rs.getType())) {
				regStatus[destination] = reorderBuffer.tailIndex();
			} else if (inst.getType() == InstructionType.BRANCH) {
				destination = inst.getInstAddress(); // put the original pc in
														// the dest field of the
														// rob
			} else {
				destination = -1;
			}

			RobEntry robEntry = new RobEntry(destination, rs.getType());
			reorderBuffer.add(robEntry);
			reorderBuffer.moveHead();

		}
	}

	static void fetch() {
		if (programDone)
			return;
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
			case END:
				programDone = true;
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

		while (!commitDone) {
			commit();
			write();
			execute();
			issue();
			fetch();
			
			//TODO: Conclude the simulation, calculate output
			//		Dont increment cycles if done
			cycle++;
		}
	}

}
