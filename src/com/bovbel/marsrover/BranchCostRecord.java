package com.bovbel.marsrover;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Record of best (cost-wise) problem solutions at any level of image
 * completion (1-indexed position), for pruning search branches.
 * 
 * Checking if branch is viable is a O(log(n)) sorted tree traverse
 * 
 * Inserting a new viable branch is O(k log(n)), with log(n) for insertion +
 * k log(n) for removal of all no-longer-viable solutions
 * @author Pavel
 *
 */
public class BranchCostRecord {

	//sorted record of position and cost
	private TreeMap<Long, Double> costRecord = new TreeMap<>();
	
	//fair locking, slightly slower but prevents starvation for writing threads
	private ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
	
	/**
	 * Initialize problem solutions with infinite cost at 0 index, for consistency
	 */
	public BranchCostRecord(){
		costRecord.put(Long.valueOf(0), Double.POSITIVE_INFINITY);
	}
	
	/**
	 * Lookup best recorded cost at any position
	 * @param position
	 * @return
	 */
	public Double getCostAt(long position){
		return costRecord.get(position);		
	}
	
	/**
	 * Check if new cost at position would be a viable best-solution
	 * @param position
	 * @param cost
	 * @return
	 */
	private boolean isViable(long position, double cost){
		
		//obtain shared lock to read, prevent writing to records
		rwLock.readLock().lock();
		boolean viability;
		Entry<Long,Double> checkEntry = costRecord.ceilingEntry(position);
		if(checkEntry != null && checkEntry.getValue() < cost){
			//check if equal or higher position entries have lower cost
			viability = false;
		}else{
			viability = true;
		}		
		rwLock.readLock().unlock();
		return viability;
	}
	
	/**
	 * If cost at position is viable, add to records and remove all poorer solutions 
	 * @param position
	 * @param cost
	 * @return
	 */
	public boolean addIfViable(long position, double cost){
		//check if branch is viable
		if(!isViable(position, cost)){
			return false;
		}

		//ensure no threads are reading/writing to records
		rwLock.writeLock().lock();

		//get reverse iterator over all lower position entries
		Iterator<Entry<Long, Double>> lowerRecordsIterator = costRecord.headMap(position,false).descendingMap().entrySet().iterator();
		
		//remove all lower position entries with higher/equal cost, they are no longer viable
		while(lowerRecordsIterator.hasNext()){
			Entry<Long, Double> lowerEntry = lowerRecordsIterator.next();				
			if(lowerEntry.getValue() >= cost){
				lowerRecordsIterator.remove();
			}else{
				//if entry actually has lower cost, then stop removal since reverse iterating over sorted
				break;
			}				
		}			
		
		//add entry
		costRecord.put(position, cost);
		rwLock.writeLock().unlock();
		return true;
	}	
	
}
