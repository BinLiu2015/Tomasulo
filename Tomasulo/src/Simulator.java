import java.util.*;
import java.io.*;

import memory.Memory;

import entries.*;
import buffers.*;

public class Simulator {
	
	//TODO: Implementing actual memory access
	//TODO: Change arrays of instruction buffers and rob to use buffer classes
	
	static int cycle; // Current cycle
	static int pc;
	
	static int[] regFile;
	static int[] regStatus;
	static InstructionEntry[] instructionMemory;
	
	static InstructionBuffer instructionBuffer;	
	static ReorderBuffer reorderBuffer;

	
	static ReservationStation[] resvStations;
	
	static Memory memory;
	
	static HashMap<InstructionType, Integer> instructionCycles;
	//Number of cycles to be taken by each instruction type
	
	static boolean programDone; //Indicates that the last psuedo end instruction has been fetched
	static boolean commitDone; //Indicates that the last instruction has been committed
	
	static void initializeDefault(){
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
	}
	
	static boolean isMemory(InstructionType type){
		return type == InstructionType.LOAD || type == InstructionType.STORE;
	}
	
	static Integer run(ReservationStation entry){
		//TODO: return correct results for each instruction type
		
		if(entry.type == InstructionType.LOAD){
			DataEntry data = (DataEntry)memory.readData(entry.address);
			if(data==null) return null;
		}
		
		return 10;
	}
	
	static ReservationStation getEmptyRS(InstructionType type){
		for(int i=0; i<resvStations.length; i++){
			ReservationStation entry = resvStations[i];
			if(entry.busy == false && entry.type == type) return entry;
		}
		return null;
	}
	
	static void commit(){
		//TODO: Flushing when wrong branch prediction
		
		if(reorderBuffer.isEmpty()){
			if(programDone) commitDone = true;
			return; //Empty buffer
		}
		
		RobEntry entry = (RobEntry) reorderBuffer.getFirst();
		
		if(isMemory(entry.getType())){
			memory.writeData(entry.getDest(), entry.getVal());
		} else{
			regFile[entry.getDest()] = entry.getVal();
		}
		
		reorderBuffer.moveHead();
	}
	
	static void write(){
		for(int i=0; i<resvStations.length; i++){
			ReservationStation entry = resvStations[i];
			
			if(entry.busy && entry.stage == Stage.EXECUTE && entry.remainingCycles <= 0){
				//Updating ROB
				
				entry.stage = Stage.WRITE;	
				Integer result = run(entry); //Value from functional unit
				if(result == null) return;
				
				//Updating Reorder Buffer Entry
				RobEntry robEntry = (RobEntry)reorderBuffer.getFirst();
				robEntry.setReady();
				robEntry.setValue(result);
				
				//Updating reservation stations
				for(int j=0; j<resvStations.length; j++){
					ReservationStation rs = resvStations[j];
					if(entry.dest == rs.qj){
						rs.qj = 0;
						rs.vj = result;
					}
					if(entry.dest == rs.qk){
						rs.qk = 0;
						rs.vk = result;
					}
				}
			}
			entry.busy = false;
			//TODO: Clear Reservation Station
		}
	}
	
	static void execute(){
		for(int i=0; i<resvStations.length; i++){
			ReservationStation entry = resvStations[i];
			if(entry.busy == false) continue;
			
			if(entry.stage == Stage.ISSUE && entry.qj == 0 && entry.qk == 0) entry.stage = Stage.EXECUTE;
			else if(entry.stage == Stage.EXECUTE && entry.remainingCycles>0) entry.remainingCycles--; 
		}
	}
	
	static void issue(){
		if(instructionBuffer.isEmpty() || reorderBuffer.isFull()) return;
		
		InstructionEntry entry = (InstructionEntry) instructionBuffer.getFirst();
		ReservationStation rs = getEmptyRS(entry.getType());
		if(rs!=null){	
			if(!instructionBuffer.isEmpty() && !reorderBuffer.isFull()){
				//TODO: Issue, fill in reservation station and rob and register status table,
				instructionBuffer.moveHead();
				
				//Don't forget to compute entry address for load and store
			}
		}
	}
	
	static void fetch(){
		//TODO: Check that the instruction fetched is the pseudo end
		InstructionEntry entry = (InstructionEntry)memory.readInstruction(pc*2);
		
		if(entry != null && !instructionBuffer.isFull()){
			instructionBuffer.add(entry);
			pc+=1; // For now, word consists of 2 bytes, and we're accessing the first byte
			
			//TODO: Update pc if jump/branch
		}
	}
	
	public static void main (String[]args) throws IOException{
		
		initializeDefault();
		
		while(pc<instructionMemory.length){
			commit();
			write();
			execute();
			issue();
			fetch();
			
			cycle++;
		}
	}
	
	
}
