package com.bovbel.marsrover;

import java.util.Collections;
import java.util.PriorityQueue;

/**
 * Streaming median calculator, not thread safe, used to determine median of a series of entries using
 * the streaming median algorithm, via one minheap and one maxheap. O(1) query, O(n) add
 * if capacity is not underestimated and heaps dont have to be resized.
 * 
 * @author Pavel
 *
 */
public class MedianCalculator {
	
	private PriorityQueue<Long> minHeap, maxHeap;
	
	/**
	 * Initializes median calculator with a capacity of 10
	 */
	public MedianCalculator(){
		this(10);
	}
	
	/**
	 * Initializes median calculator with an estimated initial capacity
	 * @param capacity
	 */
	public MedianCalculator(int capacity){
		
		minHeap = new PriorityQueue<>(capacity/2);
		maxHeap = new PriorityQueue<>(capacity/2, Collections.reverseOrder());
		
	}
	
	/**
	 * Enter new value to streaming median calculation
	 * @param value value to enter
	 */
	public void enter(long value){
						
		if(maxHeap.peek() == null || value < maxHeap.peek()){
			maxHeap.add(value);
		}else{
			minHeap.add(value);
		}
		rebalance();
	}
	
	/**
	 * Get median of currently entered values
	 * @return
	 */
	public long getMedian(){
		
		if(getDiff() > 0){
			return maxHeap.peek();
		}else if(getDiff() < 0){
			return minHeap.peek();
		}else{
			//rounding
			return (maxHeap.peek() + minHeap.peek()) / 2;
		}		
	}

	/**
	 * Check if heaps need to be rebalanced
	 */
	private void rebalance(){
		
		if(getDiff() > 1){
			minHeap.add(maxHeap.poll());
		}else if(getDiff() < -1){
			maxHeap.add(minHeap.poll());
		}
		
	}
	
	private int getDiff(){
		return maxHeap.size() - minHeap.size();
	}
}