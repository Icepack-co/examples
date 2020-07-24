// IVR8 Basic Example:
// Purpose: demonstrate how to use compartment constraints on a particular model.
// * Use a subset of the publist stops and configure a single vehicle
// * Use a simple two-rack compartment configuration to illustrate the workings.
using System.Collections.Generic;
using System;

class ivr8basic : IRunner
{
  public ivr8basic(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data.GetRange(0, 9);
  }

  public void Run()
  {
    var api = new ApiHelper<Ivr8.SolveRequest, Ivr8.SolutionResponse>("ivr8-yni1c9k2swof", configFile);
    // so here we're going to build the model 

    // create a solve request
    Ivr8.SolveRequest sr = new Ivr8.SolveRequest();

    sr.Model = new Ivr8.Model(); // initialise the model container

    // see ivr7 basic examples for notes around each of these methods.
    // the ivr7/8 models are interchangeable, except that the IVR8 model supports
    // compartment modelling.
    ivr8helper.makeDistanceTimeCapDims(sr.Model); // adds distance, time & capacity
    ivr8helper.makeLocations(sr.Model, data);     // adds all the locations to the model
    ivr8helper.makeJobTimeCap(sr.Model, data, ivr8helper.Rep(0, data.Count - 1), ivr8helper.Seq(1, data.Count));
    sr.Model.vehicleCostClasses.Add(ivr8helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    sr.Model.vehicleClasses.Add(ivr8helper.makeVcSimple("vc1", 1, 1, 1, 1));
    sr.Model.Vehicles.Add(ivr8helper.makeVehicleCap("vehicle_0", // unique id for the vehicle.
                                                      "vc1",  // the vehicle class
                                                      "vcc1", // the vehicle cost class
                                                      2000,  // the capacity of the vehicle
                                                      data[0].id, // start location for the vehicle
                                                      data[0].id, // end location for the vehicle
                                                      7 * 60,  // start time: 7 AM
                                                      18 * 60  // end time: 6 PM
                                                      ));

    // lets pretend for a moment that we have a vehicle which is layed out as follows:
    //  Top Rack    [ ] [ ] [ ] [ ]  100kg per "compartment"  c1, c2, c3, c4
    //  Lower Rack  [ ] [ ] [ ] [ ]  400kg per "compartment"  c5, c6, c7, c8
    // 100*4 + 400*4 // adds up to the 2 ton total limit on a vehicle (if every compartment could be filled to max)

    for (int i = 0; i < 8; i++)
    {
      sr.Model.Compartments.Add(
       new Ivr8.Compartment
       {
         Id = "c" + (i + 1),
         Capacities = {
          new Ivr8.Compartment.Capacity{
            dimensionId = "capacity",
            capacity = i < 4 ? 100 : 400  // switch between top and bottom rack
          }
        }
       });
    }
    // now we can define a compartment set (a container for the individual compartments)
    // which is attached to a vehicle.
    var cset = new Ivr8.CompartmentSet
    {
      Id = "double-decker"
    };
    foreach (var c in sr.Model.Compartments)
    {
      cset.compartmentIds.Add(c.Id); // add all the defined compartments to the model
    }
    sr.Model.compartmentSets.Add(cset);

    // then we assign the "double-decker" compartment set to the vehicle class.
    // we could have added it to each vehicle if we wanted, this is simply easier.
    sr.Model.vehicleClasses[0].compartmentSetId = "double-decker";

    sr.solveType = Ivr8.SolveRequest.SolveType.Optimise; // Optimise the solve request.

    // now it's just sending the model to the api

    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    ivr8helper.printSolution(Solution, true, false, false, false);
    ivr8helper.printCompartmentSummary(sr.Model, Solution);
    // okay, so what are we looking at here? So basically each "allocated" is when a task is executed
    // so either a pickup or a dropff. the capacity of each compartment is listed at the top under "capacity"
    // each stop shows where the volume is added and we can see that only one change is made at each node.
    // this is because the task is assigned to a compartment. At no point is the total volume allocated
    // to a compartment more than the capacity of the compartment. There are 16 allocations here because there
    // are 8 jobs, i.e. 8 pickups, 8 dropoffs. So after each pickup we can see the state of the load on
    // the vehicle. It's maximum weight is at stop.8 => 1800 units.


    // now lets try somethign that's infeasible (by design) and see what happens.
    // we're going to clear the compartments, populate a new list and run the model.
    sr.Model.Compartments.Clear();
    for (int i = 0; i < 8; i++)
    {
      sr.Model.Compartments.Add(
       new Ivr8.Compartment
       {
         Id = "c" + (i + 1),
         Capacities = {
          new Ivr8.Compartment.Capacity{
            dimensionId = "capacity",
            capacity = i < 4 ? 150 : 350  // bottom rack is at 350 - which is less than the biggest order
          }
        }
       });
    }
    requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)
    ivr8helper.printSolution(Solution, false, false, false, true);
    ivr8helper.printCompartmentSummary(sr.Model, Solution);
    // ah, but the api is nice enough to tell us that there is no feasible compartment assignment
    // exists for this particular set of tasks as well as the constraining dimension (capacity).
    // The limit and value's are negative here indicating that the values aren't relevant.
    // if you're looking for a more informative error message, use the evaluate end-point which
    // can identify for a proposed sequence where things went wrong (or whether any feasible sub-set exists)


    // for visualisations see the R/python notebook for plots on the same example.
    return;
  }

  public Ivr8.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}