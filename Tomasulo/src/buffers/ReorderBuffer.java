package buffers;

import entries.RobEntry;;

public class ReorderBuffer extends CircularBuffer{
	
	public ReorderBuffer(int size){
		super();
		buffer = new RobEntry[size];
	}
}
