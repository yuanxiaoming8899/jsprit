/*******************************************************************************
 * Copyright (C) 2013  Stefan Schroeder
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package algorithms;

import org.apache.log4j.Logger;

import util.Neighborhood;
import algorithms.ActivityInsertionCostsCalculator.ActivityInsertionCosts;
import algorithms.HardConstraints.HardRouteLevelConstraint;
import basics.Job;
import basics.Shipment;
import basics.costs.VehicleRoutingTransportCosts;
import basics.route.DefaultShipmentActivityFactory;
import basics.route.Driver;
import basics.route.End;
import basics.route.PickupShipment;
import basics.route.Start;
import basics.route.TourActivity;
import basics.route.TourShipmentActivityFactory;
import basics.route.Vehicle;
import basics.route.VehicleImpl.NoVehicle;
import basics.route.VehicleRoute;



final class ShipmentInsertionCalculator implements JobInsertionCalculator{

	private static final Logger logger = Logger.getLogger(ShipmentInsertionCalculator.class);

	private HardRouteLevelConstraint hardRouteLevelConstraint;
	
	private Neighborhood neighborhood = new Neighborhood() {
		
		@Override
		public boolean areNeighbors(String location1, String location2) {
			return true;
		}
	};
	
	private ActivityInsertionCostsCalculator activityInsertionCostsCalculator;
	
	private VehicleRoutingTransportCosts transportCosts;
	
	private TourShipmentActivityFactory activityFactory;
	
	public void setNeighborhood(Neighborhood neighborhood) {
		this.neighborhood = neighborhood;
		logger.info("initialise neighborhood " + neighborhood);
	}
	
	public ShipmentInsertionCalculator(VehicleRoutingTransportCosts routingCosts, ActivityInsertionCostsCalculator activityInsertionCostsCalculator, HardRouteLevelConstraint hardRouteLevelConstraint) {
		super();
		this.activityInsertionCostsCalculator = activityInsertionCostsCalculator;
		this.hardRouteLevelConstraint = hardRouteLevelConstraint;
		this.transportCosts = routingCosts;
		activityFactory = new DefaultShipmentActivityFactory();
		logger.info("initialise " + this);
	}
	
	@Override
	public String toString() {
		return "[name=calculatesServiceInsertion]";
	}
	
	/**
	 * Calculates the marginal cost of inserting job i locally. This is based on the
	 * assumption that cost changes can entirely covered by only looking at the predecessor i-1 and its successor i+1.
	 *  
	 */
	@Override
	public InsertionData calculate(final VehicleRoute currentRoute, final Job jobToInsert, final Vehicle newVehicle, double newVehicleDepartureTime, final Driver newDriver, final double bestKnownCosts) {
		if(jobToInsert == null) throw new IllegalStateException("jobToInsert is missing.");
		if(newVehicle == null || newVehicle instanceof NoVehicle) throw new IllegalStateException("newVehicle is missing.");
		if(!(jobToInsert instanceof Shipment)) throw new IllegalStateException("jobToInsert should be of type Shipment!");
		InsertionContext insertionContext = new InsertionContext(currentRoute, jobToInsert, newVehicle, newDriver, newVehicleDepartureTime);
		if(!hardRouteLevelConstraint.fulfilled(insertionContext)){
			return InsertionData.noInsertionFound();
		}
		
		double bestCost = bestKnownCosts;
		ActivityInsertionCosts bestMarginals = null;
		Shipment shipment = (Shipment)jobToInsert;
		TourActivity pickupShipment = activityFactory.createPickup(shipment);
		TourActivity deliveryShipment = activityFactory.createDelivery(shipment);
		
		int pickupInsertionIndex = InsertionData.NO_INDEX;
		int deliveryInsertionIndex = InsertionData.NO_INDEX;
		int insertionIndex = 0;
		Start start = Start.newInstance(newVehicle.getLocationId(), newVehicle.getEarliestDeparture(), newVehicle.getLatestArrival());
		start.setEndTime(newVehicleDepartureTime);
		
		End end = End.newInstance(newVehicle.getLocationId(), 0.0, newVehicle.getLatestArrival());
		
//		TourActivity prevAct = start;
//		double prevActStartTime = newVehicleDepartureTime;
//		int actIndex = 0;
////		currentRoute.getTourActivities().getActivities().
//		for(int i=0)
//		for(TourActivity nextAct : currentRoute.getTourActivities().getActivities()){
//			if(neighborhood.areNeighbors(deliveryAct2Insert.getLocationId(), prevAct.getLocationId()) && neighborhood.areNeighbors(deliveryAct2Insert.getLocationId(), nextAct.getLocationId())){
//				ActivityInsertionCosts mc = calculate(insertionContext, prevAct, nextAct, deliveryAct2Insert, prevActStartTime);
//				if(mc != null){ 
//					if(mc.getAdditionalCosts() < bestCost){
//						bestCost = mc.getAdditionalCosts();
//						bestMarginals = mc;
//						insertionIndex = actIndex;
//					}
//				}
//			}
//			double nextActArrTime = prevActStartTime + transportCosts.getTransportTime(prevAct.getLocationId(), nextAct.getLocationId(), prevActStartTime, newDriver, newVehicle);
//			double nextActEndTime = CalcUtils.getActivityEndTime(nextActArrTime, nextAct);
//			
//			prevActStartTime = nextActEndTime;
//
//			prevAct = nextAct;
//			actIndex++;
//		}
//		End nextAct = end;
//		if(neighborhood.areNeighbors(deliveryAct2Insert.getLocationId(), prevAct.getLocationId()) && neighborhood.areNeighbors(deliveryAct2Insert.getLocationId(), nextAct.getLocationId())){
//			ActivityInsertionCosts mc = calculate(insertionContext, prevAct, nextAct, deliveryAct2Insert, prevActStartTime);
//			if(mc != null) {
//				if(mc.getAdditionalCosts() < bestCost){
//					bestCost = mc.getAdditionalCosts();
//					bestMarginals = mc;
//					insertionIndex = actIndex;
//				}
//			}
//		}			
//
//		if(insertionIndex == InsertionData.NO_INDEX) {
//			return InsertionData.noInsertionFound();
//		}
		InsertionData insertionData = new InsertionData(bestCost, InsertionData.NO_INDEX, insertionIndex, newVehicle, newDriver);
		insertionData.setVehicleDepartureTime(newVehicleDepartureTime);
		insertionData.setAdditionalTime(bestMarginals.getAdditionalTime());
		return insertionData;
	}

	public ActivityInsertionCosts calculate(InsertionContext iFacts, TourActivity prevAct, TourActivity nextAct, TourActivity newAct, double departureTimeAtPrevAct) {	
		return activityInsertionCostsCalculator.calculate(iFacts, prevAct, nextAct, newAct, departureTimeAtPrevAct);
		
	}
}
