// IVR8 Intermediate Example:
// Purpose: demonstrate how to use compartment constraints on a particular model.
// * Use a subset of the publist stops and configure a single vehicle
// * Use a simple two-rack compartment configuration to illustrate the basic assignment workings.
// * Add a group-limit constraint which only permits loads on the top-rack if there
//   is a task filling the space beneth it.
using System.Collections.Generic;
using System;

class ivr8intermediate : IRunner
{
  public ivr8intermediate(List<dataRow> data, string configFile = "../config.json")
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

    // now we're going to add compartment relations which speak to the group limit.
    // we can create multiple group limits.
    // if we want something like, "the mass on the top may not exceed the mass on the bottom"
    var glim = new Ivr8.CompartmentSet.GroupLimit
    {
      dimensionId = "capacity",
      Limit = 0
      // so this says c1+c2+c3+c4-c5-c6-c7-c8 <= 0 is required for feasibility
      // writing this differently c1:c4 - c5:c8 <= 0    (grouping the c's together)
      // so c1:c4 <= c5:c8                              (moving c5:c8 to the rhs)
      // which says the top rack (c1:c4) should sum to less than the bottom rack (c5:c8)
    };

    glim.Coefficients = new float[8];
    for (int i = 0; i < 8; i++)
    {
      glim.compartmentIds.Add("c" + (i + 1)); // adds c1:c8 to the equation.
      glim.Coefficients[i] = i < 4 ? +1 : -1; // sets the value in the array
    }

    cset.groupLimits.Add(glim);

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
    // In this table you can see that the the sum for compartments 1:4 is always less than 5:8
    // this way we're constrained by always having more weight on the bottom rack than the top
    // rack throughout the route (which is still pretty well costed)

    // for visualisations see the R/python notebook for plots on the same example.
    return;
  }

  public Ivr8.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}