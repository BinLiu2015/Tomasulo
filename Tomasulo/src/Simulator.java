import java.util.*;
import java.io.*;

import memory.Memory;

import entries.*;


public class Simulator {
	
	//TODO: Implementing actual memory access
	//TODO: Change arrays of instruction buffers and rob to use buffer classes
	
	static int cycle; // Current cycle
	static int pc;
	
	static int[] regFile;
	static int[] regStatus;
	static InstEntry[] instructionMemory;
	
	static InstEntry[] instructionBuffer;
	static int ibHead;
	static int ibTail;
	
	static RobEntry[] rob;
	static int robHead;
	static int robTail;
	
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
		
		if(robHead == robTail){
			if(programDone) commitDone = true;
			return; //Empty buffer
		}
		
		RobEntry entry = rob[robHead];
		
		if(isMemory(entry.type)){
			memory[entry.destination] = entry.value;
			//TODO: actual memory access
		} else{
			regFile[entry.destination] = entry.value;
		}
		
		robHead++;
	}
	
	static void write(){
		for(int i=0; i<resvStations.length; i++){
			ReservationStation entry = resvStations[i];
			
			if(entry.busy && entry.stage == Stage.EXECUTE && entry.remainingCycles <= 0){
				//Updating ROB
				
				entry.stage = Stage.WRITE;	
				Integer result = run(entry); //Value from functional unit
				if(result == null) return;
				
				rob[entry.dest].ready = true;
				rob[entry.dest].value = result;
				
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
		InstEntry entry = instructionBuffer[ibHead];
		ReservationStation rs = getEmptyRS(entry.type);
		if(rs!=null){
			//if( ((robTail+1)%rob.length) == robHead) return;
			
			if(!instructionBuffer.isEmpty() && !rob.isFull()){
				//TODO: Issue, fill in reservation station and rob and register status table,
				// move instructionBuffer head
				
				//Don't forget to compute entry address for load and store
			}
		}
	}
	
	static void fetch(){
		//TODO: Check that the instruction fetched is the pseudo end
		InstEntry inst = (InstEntry)memory.readInstruction(pc*2);
		
		if(inst != null && !instructionBuffer.isFull()){
			instructionBuffer.add(inst);
			
			switch(inst.getType()) {
			case JUMP:
				pc += inst.getRS();
			case BRANCH:
				pc += inst.getRT();
			case JUMPL:
				pc += inst.getRS();
				default:
					break;
			}
			pc+=1; // For now, word consists of 2 bytes, and we're accessing the first byte
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
