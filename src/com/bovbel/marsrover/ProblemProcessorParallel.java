package com.bovbel.marsrover;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Problem processor to store problem information and do high level processing
 * @author Pavel
 *
 */
public class ProblemProcessorParallel {

	private final long numBytes;
	
	private ForkJoinPool pool = new ForkJoinPool();
	private IntervalChunkTree intervalTree;
	private BranchCostRecord branchCostRecord = new BranchCostRecord();
	private List<Chunk> bestSequence;
	
	/**
	 * Initialize problem by creating interval tree, and beginning search at 0 position
	 * @param numBytes
	 * @param chunks
	 */
	public ProblemProcessorParallel(long numBytes, Set<Chunk> chunks){

		this.numBytes = numBytes;		
		intervalTree = new IntervalChunkTree(chunks);
		
		try{
			pool.invoke(new ProblemBranch(0, 0 , new LinkedList<Chunk>()));
		}catch(CancellationException ex){
			System.err.println("Cancelled");
		}
		
	}
	
	/**
	 * Retrieve chunk sequence of best solution, null if no solution
	 * @return
	 */
	public List<Chunk> getBestSequence(){
		return bestSequence;		
	}
	
	/**
	 * Retrieve cost of best solution from records, null if no solution
	 * @return
	 */
	public Double getLowestCost(){
		return branchCostRecord.getCostAt(numBytes);
	}
	
	/**
	 * Inner class representing one processing step in solution
	 * @author Pavel
	 *
	 */
	@SuppressWarnings("serial")
	private class ProblemBranch extends RecursiveAction{

		final long currentByte;
		final double currentCost;
		final List<Chunk> currentChunks;
		
		/**
		 * Load in current problem branch progress
		 * @param currentByte byte index of this solution branch so far
		 * @param currentCost transmission cost of this solution branch so far
		 * @param currentChunks list of chunks downloaded in this solution so far
		 */
		public ProblemBranch(long currentByte, double currentCost, List<Chunk> currentChunks){
			this.currentByte = currentByte;
			this.currentCost = currentCost;
			this.currentChunks = currentChunks;
		}
		
		@Override
		protected void compute() {
			
			//Query interval tree for all chunks that contain the current byte index			
			List<Chunk> potentialChunks = intervalTree.getChunksContainingValue(currentByte);
			
			//Check if query returns non empty list
			if(potentialChunks != null && !potentialChunks.isEmpty()){
				
				//Start building list of potential new search branches
				List<ProblemBranch> newBranches = new LinkedList<>();
				
				//Iterate over all chunks returned from interval tree query
				for (Chunk next : potentialChunks){					
					double newBranchCost = currentCost + next.cost();					
					
					//check if this chunk solves problem, and if it's currently the best solution
					if(next.right >= numBytes && branchCostRecord.addIfViable(numBytes, newBranchCost)){
						//record chunk as solution
						bestSequence = new LinkedList<>(currentChunks);
						bestSequence.add(next);
						
					//check records if this adding this chunk to the search sequence creates a viable  
					//path for continued searching (smallest cost recorded for chunk's right boundary)
					}else if(branchCostRecord.addIfViable(next.right, newBranchCost)){
						List<Chunk> newBranchChunks = new LinkedList<>(currentChunks);
						newBranchChunks.add(next);
						newBranches.add(new ProblemBranch(next.right, newBranchCost, newBranchChunks));
					}															
				}
				//Pass all viable new branches off for processing
				invokeAll(newBranches);
			}else{
				//no chunks found to fill gap, so stop searching
				System.err.println("No chunks available to fill gap at " + currentByte);
				pool.shutdownNow();
			}
							
		}
		
	}
	
}
