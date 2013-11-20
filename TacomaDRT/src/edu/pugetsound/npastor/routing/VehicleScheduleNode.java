package edu.pugetsound.npastor.routing;

import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

public class VehicleScheduleNode {
	
	private static final String TAG = "VehicleScheduleNode";

	private VehicleScheduleJob mJob;
	private VehicleScheduleNode mNext;
	private VehicleScheduleNode mPrevious;
	
	public VehicleScheduleNode(VehicleScheduleJob job, VehicleScheduleNode previous, VehicleScheduleNode next) {
		mJob = job;
		mPrevious = previous;
		mNext = next;
	}
	
	private void setNext(VehicleScheduleNode next) {
		mNext = next;
	}
	
	private void setPrevious(VehicleScheduleNode previous) {
		mPrevious = previous;
	}
	
	public VehicleScheduleNode getNext() {
		return mNext;
	}
	
	public VehicleScheduleNode getPrevious() {
		return mPrevious;
	}
	
	public VehicleScheduleJob getJob() {
		return mJob;
	}
	
	public boolean hasNext() {
		if(mNext != null)
			return true;
		else 
			return false;
	}
	
	public boolean hasPrevious() {
		if(mPrevious != null)
			return true;
		else 
			return false;
	}
	
	@Override
	public boolean equals(Object compare) {
		VehicleScheduleNode compareNode = (VehicleScheduleNode) compare;
		Trip thisTrip = mJob.getTrip();
		Trip compareTrip = compareNode.getJob().getTrip();
		if(compareTrip == null || thisTrip == null)
			return false;
		// Equal if the jobs the nodes contain have the same id's and 
		else if(compareTrip.getIdentifier() == thisTrip.getIdentifier() &&
				compareNode.getJob().getType() == mJob.getType())
			return true;
		else
			return false;
	}
	
	@Override
	public String toString() {
		return "Node: " + mJob;
	}
	
	// ************************************************
	//         STATIC FUNCTIONS FOR PERFORMING
	//              LIST-LIKE OPERATIONS
	// ************************************************
	
	/**
	 * Set next as the next node in the list, after cur
	 * @param cur The node to be inserted after cur
	 * @param next The node whose next will be next
	 */
	public static void setNext(VehicleScheduleNode cur, VehicleScheduleNode next) {
		VehicleScheduleNode existingNext = cur.getNext();
		cur.setNext(next);
		next.setPrevious(cur);
		if(existingNext != null) {
			next.setNext(existingNext);
			existingNext.setPrevious(next);
		}
	}
	
	/**
	 * Sets previous as the node before cur in the list
	 * @param cur The node which will be just after previous
	 * @param previous The node which will be just before cur
	 */
	public static void setPrevious(VehicleScheduleNode cur, VehicleScheduleNode previous) {
		VehicleScheduleNode existingPrev = cur.getPrevious();
		cur.setPrevious(previous);
		previous.setNext(cur);
		if(existingPrev != null) {
			previous.setPrevious(existingPrev);
			existingPrev.setNext(previous);
		}
	}
	
	/**
	 * Removes the specified node, updating both of its neighboring nodes
	 * @param node Node to remove
	 */
	public static void remove(VehicleScheduleNode node) {
		// Update neighbor pointers
		VehicleScheduleNode next = node.getNext();
		VehicleScheduleNode prev = node.getPrevious();
		if(next != null)
			next.setPrevious(prev);
		if(prev != null)
			prev.setNext(next);
		
		// Remove this node's pointers
		node.setNext(null);
		node.setPrevious(null);
	}
	
	/**
	 * Given a root node, puts the specified node at the specified index
	 * @param root 
	 * @param index
	 */
	public static void put(VehicleScheduleNode root, VehicleScheduleNode put, int index) {
		
		VehicleScheduleNode curNode = root;
		for(int i = 0; i < index; i++) {
			if(curNode.hasNext())
				curNode = curNode.getNext();
			else 
				throw new IndexOutOfBoundsException("List beginning at this root has a length less than " + (index + 1));
		}
		setPrevious(curNode, put);
	}
	
	/**
	 * Returns the index of the specified node, starting from the specified root
	 * @param root List root
	 * @param node Node to find index of
	 * @return Index of specified node, or -1 if the node is not in the list
	 */
	public static int getIndex(VehicleScheduleNode root, VehicleScheduleNode node) {
		int index = 0;
		VehicleScheduleNode curNode = root;
		while(curNode != null) {
			if(curNode.equals(node))
				return index;
			else
				index++;
			
			curNode = curNode.getNext();
		}
		return -1;
	}
	
	public static String getListString(VehicleScheduleNode root) {		
		String str = "";
		VehicleScheduleNode curNode = root;
		while(curNode != null) {
			str += curNode + "\n";
			
			curNode = curNode.getNext();
		}
		
		return str;
	}

}