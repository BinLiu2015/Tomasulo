import java.util.*;
import java.io.*;

public class Simulator {
	
	//TODO: Implementing actual memory access
	
	static int cycle; // Current cycle
	static int pc;
	
	static int[] regFile;
	static int[] regStatus;
	static Instruction[] instructionMemory;
	
	static Instruction[] instructionBuffer;
	static int ibHead;
	static int ibTail;
	
	static ReservationStation[] resvStations;
	
	static int[] memory;
	
	static ReorderBuffer[] rob;
	static int robHead;
	static int robTail;
	
	static HashMap<InstructionType, Integer> instructionCycles;
	
	static void initializeDefault(){
		instructionCycles = new HashMap<InstructionType, Integer>();
		instructionCycles.put(InstructionType.ADD, 1);
		instructionCycles.put(InstructionType.BRANCH, 1);
		instructionCycles.put(InstructionType.LOAD, 1);
		instructionCycles.put(InstructionType.MULT, 1);
		instructionCycles.put(InstructionType.STORE, 1);
		
		pc = 0;
		cycle = 0;
		
		regFile = new int[8]; // repeat for all
	}
	
	static boolean isMemory(InstructionType type){
		return type == InstructionType.LOAD || type == InstructionType.STORE;
	}
	
	static int run(ReservationStation entry){
		//TODO: return correct results for each instruction type
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
		
		if(robHead == robTail) return; //Empty buffer
		
		ReorderBuffer entry = rob[robHead];
		
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
			
			if(entry.stage == Stage.EXECUTE && entry.remainingCycles <= 0){
				//Updating ROB
				entry.stage = Stage.WRITE;	
				int result = run(entry); //Value from functional unit
				
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
		Instruction entry = instructionBuffer[ibHead];
		ReservationStation rs = getEmptyRS(entry.type);
		if(rs!=null){
			if( ((robTail+1)%instructionBuffer.length) == robHead) return;
			
			//TODO: Issue!
		}
	}
	
	static void fetch(){
		
	}
	
	public static void main (String[]args) throws IOException{
		
		initializeDefault();
		
		while(pc<instructionMemory.length){
			commit();
			write();
			execute();
			issue();
			fetch();
		}
	}
	
	
}
