import java.util.*;
import java.io.*;

import sun.font.CreatedFontTracker;
import memory.*;
import entries.*;
import buffers.*;

public class Simulator {

	// TODO: Implementing actual memory access
	// TODO: Change arrays of instruction buffers and rob to use buffer classes

	static int cycle; // Current cycle
	static int pc;

	static int[] regFile;
	static int[] regStatus;

	static InstructionBuffer instructionBuffer;
	static ReorderBuffer reorderBuffer;

	static ArrayList<ReservationStation> resvStations;

	static MemoryWrapper memory;

	static HashMap<InstructionType, Integer> instructionCycles;
	// Number of cycles to be taken by each instruction type

	static boolean programDone; // Indicates that the last psuedo end
								// instruction has been fetched
	static boolean commitDone; // Indicates that the last instruction has been
								// committed

	static void initializeDefault() {

		Assembler assembler = new Assembler();
		ArrayList<InstructionEntry> instructionList = assembler.read();

		cycle = 0;
		pc = 0;

		regFile = new int[8]; // repeat for all arrays
		regStatus = new int[8];
		for (int i = 0; i < 8; i++) {
			regFile[i] = i;
			regStatus[i] = -1;
		}

		instructionBuffer = new InstructionBuffer(100);
		reorderBuffer = new ReorderBuffer(100);

		resvStations = new ArrayList<ReservationStation>();
		resvStations.add(new ReservationStation(InstructionType.ADD));
		resvStations.add(new ReservationStation(InstructionType.ADD));
		resvStations.add(new ReservationStation(InstructionType.MUL));
		resvStations.add(new ReservationStation(InstructionType.MUL));
		resvStations.add(new ReservationStation(InstructionType.BEQ));
		resvStations.add(new ReservationStation(InstructionType.BEQ));

		// memory = new MemoryWrapper(instructionList, 0);
		memory = new MemoryWrapper();
		memory.loadInstructions(instructionList, 0);

		instructionCycles = new HashMap<InstructionType, Integer>();
		instructionCycles.put(InstructionType.ADD, 1);
		instructionCycles.put(InstructionType.BEQ, 1);
		instructionCycles.put(InstructionType.LW, 1);
		instructionCycles.put(InstructionType.MUL, 1);
		instructionCycles.put(InstructionType.SW, 1);

		programDone = false;
		commitDone = false;
	}

	static boolean isMemory(InstructionType type) {
		return type == InstructionType.LW || type == InstructionType.SW;
	}

	static boolean writesToReg(InstructionType type) {
		return type == InstructionType.LW || type == InstructionType.ADD
				|| type == InstructionType.SUB || type == InstructionType.ADDI
				|| type == InstructionType.NAND || type == InstructionType.MUL;

	}

	static ReservationStation getEmptyRS(InstructionType type) {
		// TODO: Check how to handle similar instruction types
		for (int i = 0; i < resvStations.size(); i++) {
			ReservationStation entry = resvStations.get(i);
			if (entry.busy == false && entry.type == getFunctionalUnit(type))
				return entry;
		}
		return null;
	}

	static InstructionType getFunctionalUnit(InstructionType type) {
		switch (type) {
		case ADD:
		case ADDI:
		case SUB:
		case NAND:
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
		if (!entry.isReady())
			return;
		switch (entry.getType()) {
		case SW:
			if (memory.writeData(entry.getDest(), entry.getVal(), cycle))
				;
			reorderBuffer.moveHead();
			break;
		// Assume immediate value is in VALUE field of rob entry
		// Assume original PC + 1 in the DESTINATION field
		case BEQ:
			if ((entry.getVal() > 0 && entry.isBranchTaken())
					|| (entry.getVal() < 0 && !entry.isBranchTaken())) {
				reorderBuffer.flush();
				instructionBuffer.flush();

				for (ReservationStation rs : resvStations)
					rs.clear();
				for (int i = 0; i < regStatus.length; i++)
					regStatus[i] = -1;

				pc = (entry.isBranchTaken()) ? entry.getDest() + entry.getVal()
						: entry.getDest();
				programDone = false;
			} else {
				reorderBuffer.moveHead();
			}
			break;
		default:
			regFile[entry.getDest()] = entry.getVal();
			reorderBuffer.moveHead();
		}

	}

	static void write() {
		for (int i = 0; i < resvStations.size(); i++) {
			ReservationStation rs = resvStations.get(i);
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
				for (int j = 0; j < resvStations.size(); j++) {
					ReservationStation resvStation = resvStations.get(j);
					if (rs.getRob() == resvStation.qj) {
						resvStation.qj = -1;
						resvStation.vj = result;
					}
					if (rs.getRob() == resvStation.qk) {
						resvStation.qk = -1;
						resvStation.vk = result;
					}
				}
				rs.busy = false;
				rs.clear();
			}

		}
	}

	static void execute() {
		for (int i = 0; i < resvStations.size(); i++) {
			ReservationStation entry = resvStations.get(i);
			if (entry.busy == false)
				continue;

			if (entry.stage == Stage.ISSUE && entry.qj == -1 && entry.qk == -1)
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
				// Ready in ROB but not in Reg file (Written but not committed
				int testRob = reorderBuffer.findDest(inst.getRS());
				if (testRob != -1) {
					rs.vj = testRob;
				} else {
					rs.vj = regFile[inst.getRS()];
				}
				rs.qj = -1;
			}

			switch (inst.getType()) {
			case ADD:
			case SUB:
			case MUL:
			case NAND:
				if (regStatus[inst.getRT()] != -1) {
					rs.qk = regStatus[inst.getRT()];
					rs.vk = 0;
				} else {
					int testRob = reorderBuffer.findDest(inst.getRT());
					if (testRob != -1) {
						rs.vk = testRob;
					} else {
						rs.vk = regFile[inst.getRT()];
					}
					rs.qk = -1;
				}
				break;
			case BEQ:
			case SW:
				if (regStatus[inst.getRD()] != -1) {
					rs.qk = regStatus[inst.getRD()];
					rs.vk = 0;
				} else {
					int testRob = reorderBuffer.findDest(inst.getRD());
					if (testRob != -1) {
						rs.vk = testRob;
					} else {
						rs.vk = regFile[inst.getRD()];
					}
					rs.qk = -1;
				}
				rs.address = inst.getRT();
				break;
			default:
				rs.qk = -1;
				rs.vk = -1;
				rs.address = inst.getRT();
			}

			rs.setRob(reorderBuffer.tailIndex());
			if (instructionCycles.containsKey(rs.getType())) {
				rs.setCycles(instructionCycles.get(rs.getType()));
			} else {
				rs.setCycles(1);
			}
			rs.setBusy(true);
			rs.setStage(Stage.ISSUE);
			rs.setOperation(inst.getType());

			int destination = inst.getRD();

			if (writesToReg(rs.getType())) {
				regStatus[destination] = reorderBuffer.tailIndex();
			} else if (inst.getType() == InstructionType.BEQ) {
				destination = inst.getInstAddress();
				// put the original pc in the dest field of the rob
			} else {
				destination = -1;
			}

			RobEntry robEntry = new RobEntry(destination, rs.getType());
			reorderBuffer.add(robEntry);

		}
	}

	static void fetch() {
		if (programDone)
			return;
		InstructionEntry inst = (InstructionEntry) memory.readInstruction(
				pc * 2, cycle);

		if (inst != null && !instructionBuffer.isFull()) {
			// System.out.println("Fetched instruction " + inst.getType());
			switch (inst.getType()) {
			case JMP: {
				pc += 1 + regFile[inst.getRD()] + inst.getRS();
				break;
			}
			case BEQ: {
				inst.setPcAddress(pc + 1);
				pc += (inst.getRT() >= 0) ? 1 : inst.getRT() + 1;
				instructionBuffer.add(inst);
				break;
			}
			case JALR: {
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
				pc += 1;
				// For now, word consists of 2 bytes, and we're accessing the
				// first byte
				instructionBuffer.add(inst);
				break;
			}
		}
	}

	public static void createRS(int count, int time, InstructionType type) {
		for (int i = count; i > 0; i--)
			resvStations.add(new ReservationStation(type));
		instructionCycles.put(type, time);
	}

	public static void run() {

		// initializeDefault();

		while (!commitDone) {

			commit();
			write();
			execute();
			issue();
			fetch();

			// TODO: Conclude the simulation, calculate output
			// Dont increment cycles if done
			cycle++;
		}

		System.out.println("Number of cycles taken is " + cycle);
		System.out.println("IPC: "  + (pc-1) / (double)cycle );
		System.out.println("For Lv1 Cache: " + memory.getL1CacheR());
		System.out.println("For Lv2 Cache: " + memory.getL2CacheR());
		System.out.println("For Lv3 Cache: " + memory.getL3CacheR());
		for (int i = 0; i < regFile.length; i++)
			System.out.print(regFile[i] + " ");
	}

	public static void main(String... args) {
		initializeDefault();
		run();
	}
}
