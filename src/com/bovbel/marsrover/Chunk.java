package com.bovbel.marsrover;

import java.util.Comparator;

/**
 * Representation of image chunk
 * @author Pavel
 *
 */
public class Chunk {

	public static long bandwidth = 0, latency = 0;	
	public final long left, right;
	
	/**
	 * Make new chunk representation
	 * @param left left bound (inclusive)
	 * @param right right bound (exclusive)
	 */
	public Chunk(long left, long right){
		this.left = left;
		this.right = right;
	}
	
	/**
	 * Check if chunk contains byte index
	 * @param index
	 * @return true if 
	 */
	public boolean contains(long index){
		return (index >= left && index < right);
	}
	
	/**
	 * Get size of chunk in bytes
	 * @return
	 */
	public long size(){
		return right - left;
	}
	
	/**
	 * Get cost to request and transmit chunk
	 * @return
	 */
	public double cost(){
		 return 2*latency + size() / (double)bandwidth;
	}
	
	/**
	 * Compare chunks by size
	 * @return
	 */
	static public Comparator<Chunk> getSizeComparator(){
		return new Comparator<Chunk>(){
			@Override
			public int compare(Chunk a, Chunk b) {
				return Long.compare(a.size(), b.size());
			}		
		};
	}
	
	/**
	 * compare chunks by left bound
	 * @return
	 */
	static public Comparator<Chunk> getLeftComparator(){
		return new Comparator<Chunk>(){
			@Override
			public int compare(Chunk a, Chunk b) {
				return Long.compare(a.left, b.left);
			}		
		};
	}
	
	/**
	 * compare chunks by right bound
	 * @return
	 */
	static public Comparator<Chunk> getRightComparator(){
		return new Comparator<Chunk>(){
			@Override
			public int compare(Chunk a, Chunk b) {
				return Long.compare(a.right, b.right);
			}			
		};
	}
	
	/**
	 * Check if chunks are logically equivalent
	 */
	@Override
	public boolean equals(Object obj){
		if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Chunk))
            return false;

        Chunk rhs = (Chunk) obj;
        return rhs.left == left && rhs.right == right;
	}
	
	/**
	 * String representation of chunk
	 */
	@Override
	public String toString(){
		return "["+left+","+right+"]";	
	}
	
	/**
	 * Hashcode of chunk, XOR bound hashcodes
	 */
	@Override
	public int hashCode(){
		return Long.valueOf(left).hashCode()^Long.valueOf(right).hashCode();
		
	}
	
}
