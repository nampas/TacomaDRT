package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.LinkedList;

import edu.pugetsound.npastor.utils.Constants;

public class Vehicle {

	//TODO: what is capacity?
	public static final int VEHICLE_CAPACITY = 20;
	
	private int mVehicleId;
	private VehicleScheduleNode mScheduleRoot;
	private int mScheduleSize = 0;
	
	
	public Vehicle(int id) {
		mVehicleId = id;
		initSchedule();
		
	}
	
	private void initSchedule() {
		// Add start and end jobs
		VehicleScheduleJob startJob = new VehicleScheduleJob(null, null, Constants.BEGIN_OPERATION_HOUR*60, VehicleScheduleJob.JOB_TYPE_START);
		VehicleScheduleJob endJob = new VehicleScheduleJob(null, null, Constants.END_OPERATION_HOUR*60, VehicleScheduleJob.JOB_TYPE_END);
		mScheduleRoot = new VehicleScheduleNode(startJob, null, null);
		VehicleScheduleNode endNode = new VehicleScheduleNode(endJob, mScheduleRoot, null);
		VehicleScheduleNode.setNext(mScheduleRoot, endNode);
		mScheduleSize = 2;
	}
	
	public VehicleScheduleNode getScheduleRoot() {
		return mScheduleRoot;
	}
	
	public int getIdentifier() {
		return mVehicleId;
	}
	
	public String scheduleToString() {
		String str = "Vehicle " + mVehicleId + " schedule:";
		
		VehicleScheduleNode curNode = mScheduleRoot;
		while(curNode != null) {
			str += "\n " + curNode.getJob();
			curNode = curNode.getNext();
		}
		return str;
	}
}
