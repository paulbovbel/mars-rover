package com.bovbel.marsrover;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * Centered interval tree, for fast lookup of chunks containing any byte index
 * 
 * Construction is O(n log(n)), with n log(n) for building tree + sum(k) log(n)
 * for sorting subsets of chunks at each node, where sum(k) = n
 * 
 * @author Pavel
 *
 */
public class IntervalChunkTree {
	
	//reference to root of tree
	IntervalNode root;
	
	/**
	 * Build tree from chunks, starting from root node, using all system threads
	 * @param chunks
	 */
	public IntervalChunkTree(Set<Chunk> chunks){	
		if(!chunks.isEmpty()){
			ExecutorService executor = new ForkJoinPool();
			try {
				root = executor.submit(new IntervalNodeGenerator(executor,chunks)).get();
			} catch (InterruptedException | ExecutionException e) {
				System.err.println("Error getting root node from future");
			}		
		}
	}
	
	/**
	 * Get tree size
	 * @return
	 */
	public int size() {
		return root.size();
	}
	
	/**
	 * Get list of chunks that contain a value
	 * @param value
	 * @return
	 */
	public List<Chunk> getChunksContainingValue(long value) {
		
		if (root == null){
			return null;			
		}	
		
		//start recursively building list from root node
		List<Chunk> output = buildChunkList(root, value);
		Collections.sort(output,Chunk.getSizeComparator());
		return output;
		
	}
	
	/**
	 * Recursive function for tree traversal while building chunk list
	 * @param start
	 * @param value
	 * @return
	 */
	private List<Chunk> buildChunkList(IntervalNode start, long value){
		List<Chunk> output = new LinkedList<>();
		
		//if value matches key, then it's safe to add all chunks at this node and stop recursing
		if (value == start.getKey()){			
			output.addAll(start.getLeftSortedChunks());
		}else{
			//Otherwise, filter through chunks at this node
			SortedSet<Chunk> querySet;
			if(value < start.getKey()){
				//select chunks presorted by left boundary
				querySet = start.getLeftSortedChunks();
				//check if node has a left child, if so, continue recursing
				if(start.getLeft() != null){
					output.addAll(buildChunkList(start.getLeft(), value));
				}
			}else{
				//select chunks presorted by right boundary
				querySet = start.getRightSortedChunks();
				//if node has right child, continue recursing
				if(start.getRight() != null){
					output.addAll(buildChunkList(start.getRight(), value));
				}
			}

			//filter through selected chunks and add to output
			for(Chunk eval : querySet){
				if(eval.contains(value)){
					output.add(eval);
				}else{
					//since chunks are sorted, safe to stop search on failed bounds check
					break;
				}
			}
		}
		return output;		
	}	

	/**
	 * Private class for a subtask to build a node in the tree
	 * @author Pavel
	 *
	 */
	private class IntervalNodeGenerator implements Callable<IntervalNode> {

		ExecutorService executor;
		Set<Chunk> inputChunks;
		
		/**
		 * Create task to build a node with a set of chunks
		 * @param executor
		 * @param inputChunks
		 */
		public IntervalNodeGenerator(ExecutorService executor, Set<Chunk> inputChunks){
			this.executor = executor;
			this.inputChunks = inputChunks;
		}
		
		@Override
		public IntervalNode call() throws Exception {
			//find median of input chunks, use as node key
			MedianCalculator calc = new MedianCalculator(inputChunks.size()*2);
			for (Chunk chunk : inputChunks){
				calc.enter(chunk.left);
				calc.enter(chunk.right);
			}
			long key = calc.getMedian();
			
			//filter input chunks into current node, pass remainder to child nodes
			Set<Chunk> forCurrentNode = new HashSet<>(), forLeftChild = new HashSet<>(), forRightChild = new HashSet<>();
			for (Chunk chunk : inputChunks){
				if(chunk.left <= key && chunk.right > key){
					forCurrentNode.add(chunk);
				}else if(chunk.left > key){
					forRightChild.add(chunk);
				}else if(chunk.right <= key){
					forLeftChild.add(chunk);
				}			
			}

			//construct node object using filtered chunks
			IntervalNode output = new IntervalNode(forCurrentNode, key);
			
			//create new subtasks for child nodes
			if(!forLeftChild.isEmpty()){
				output.futureLeft = executor.submit(new IntervalNodeGenerator(executor, forLeftChild));
			}
			if(!forRightChild.isEmpty()){
				output.futureRight = executor.submit(new IntervalNodeGenerator(executor, forRightChild));
			}
			return output;
		}
				
	}
	
	/**
	 * Private class representing tree node
	 * @author Pavel
	 *
	 */
	private class IntervalNode {
		
		private IntervalNode left, right;
		private Future<IntervalNode> futureLeft, futureRight;
		private SortedSet<Chunk> sortedLeft, sortedRight;
		private final long key;
		
		/**
		 * Construct node with provided set of chunks, creating sets sorted by left and 
		 * right boundary, for traversal later
		 * @param storedChunks
		 * @param key
		 */
		private IntervalNode(Set<Chunk> storedChunks, long key){
			
			this.key = key;
			
			this.sortedLeft = new TreeSet<>(Chunk.getLeftComparator());
			sortedLeft.addAll(storedChunks);
			this.sortedRight = new TreeSet<>(Chunk.getRightComparator()).descendingSet();
			sortedRight.addAll(storedChunks);

		}
		
		/**
		 * Recursively find size of this node's subtree
		 * @return
		 */			
		public int size(){
			int size = 1;
			if(getLeft() != null){
				size += getLeft().size();
			}
			if(getRight() != null){
				size += getRight().size();
			}
			return size;		
		}	
		
		public SortedSet<Chunk> getLeftSortedChunks(){
			return sortedLeft;
		}
		
		public SortedSet<Chunk> getRightSortedChunks(){
			return sortedRight;
		}
		
		public long getKey(){
			return key;
		}
		
		/**
		 * Return left child, retrieving from future is necessary. Returns null if no left child
		 * @return
		 */
		public IntervalNode getLeft(){
			if(left == null && futureLeft != null){
				try {
					left = futureLeft.get();
				} catch (InterruptedException | ExecutionException e) {
					System.err.println("Error processing left branch future");
				}			
			}
			return left;
		}
		
		/**
		 * Return right child, retrieving from future is necessary. Returns null if no right child
		 * @return
		 */
		public IntervalNode getRight(){
			if(right!=null){
				return right;
			}
			
			if(futureRight != null){			
				try {
					right = futureRight.get();
				} catch (InterruptedException | ExecutionException e) {
					System.err.println("Error processing right branch future");
				}
			}
			return right;		
		}
				
	}
	
}

