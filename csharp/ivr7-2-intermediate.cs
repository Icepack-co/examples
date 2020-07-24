// IVR7 Intermediate example 1:
// Purpose: illustrate the use of modelling concepts
// Illustrates using time, distance and a single capacity dimension
// Location-windows (08:00 -> 14:00)
// Pickup-dropoff tasks (with task-times)
// One vehicle class (same travel profile)
// Two vehicle-cost classes
// Multiple vehicles (2xc1, 2xc2)
// Heterogeneous fleet (2 ton and 3 ton capacity)
// Lunch breaks (1 hour break around 12:00)

using System.Collections.Generic;
using System;

class ivr7intermediate1 : IRunner
{
  public ivr7intermediate1(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data;
  }

  public void Run()
  {
    var api = new ApiHelper<Ivr7.SolveRequest, Ivr7.SolutionResponse>("ivr7-kt461v8eoaif", configFile);
    // so here we're going to build the model 

    // create a solve request
    Ivr7.SolveRequest sr = new Ivr7.SolveRequest();

    sr.Model = new Ivr7.Model(); // initialise the model container

    // we're going to reuse the helpers described in the ivr7basic example. Please see that for a reference.

    ivr7helper.makeDistanceTimeCapDims(sr.Model); // adds distance, time & capacity
    ivr7helper.makeLocations(sr.Model, data);     // adds all the locations to the model

    // we're going to add time windows to the locations. 08:00 - 14:00
    foreach (var l in sr.Model.Locations)
    {
      var a = new Ivr7.Location.Attribute()
      {
        dimensionId = "time",
        arrivalWindows = { new Ivr7.Window{
                  Start = 8*60,
                  End = 14*60
                }}
      };
      l.Attributes.Add(a);
    }

    ivr7helper.makeJobTimeCap(sr.Model, data, ivr7helper.Rep(0, data.Count - 1), ivr7helper.Seq(1, data.Count));
    // Two vehicle cost classes, one which is cheaper on time, one which is cheaper on distance, one
    // which is more expensive if used (1000 vs 1200).
    sr.Model.vehicleCostClasses.Add(ivr7helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    sr.Model.vehicleCostClasses.Add(ivr7helper.makeVccSimple("vcc2", 1200, 0.1f, 0.1f, 0.1f, 0.6f, 2.5f));

    sr.Model.vehicleClasses.Add(ivr7helper.makeVcSimple("vc1", 1, 1, 1, 1));

    // now we can just specify the vehicles.
    // lets provide 2 x 2 ton vehicles and 2 x 3 ton vehicles. Although this is probably more than we need.
    // the reason for this is that we're modelling a full-blown pickup+dropoff model, so if there's
    // time to reload, a vehicle can return to the depot and grab more goodies!

    for (int i = 0; i < 4; i++)
    {
      string vcc = "vcc1";
      float cap = 2000;
      if (i > 1)
      {
        vcc = "vcc2";
        cap = 3000;
      }
      sr.Model.Vehicles.Add(ivr7helper.makeVehicleCap("vehicle_" + i, // unique id for the vehicle.
                                                      "vc1",  // the vehicle class
                                                      vcc, // the vehicle cost class
                                                      cap,  // the capacity of the vehicle
                                                      data[0].id, // start location for the vehicle
                                                      data[0].id, // end location for the vehicle
                                                      7 * 60,  // start time: 7 AM
                                                      18 * 60  // end time: 6 PM
                                                      ));
    }

    // Lunch breaks.
    // so this is a touch more complex, we want to link our transit-rule to the time
    // dimension, and when a certain amount has accumulated on the dimension, we trigger the rule.
    sr.Model.transitRules.Add(ivr7helper.makeLunchBreakRule("lunch_break_rule", "lunchy_munchy_", 12 * 60, 60));

    // now link the transit rule to the vehicle classes
    sr.Model.vehicleClasses[0].transitRuleIds.Add("lunch_break_rule");

    sr.solveType = Ivr7.SolveRequest.SolveType.Optimise; // Optimise the solve request.

    // now it's just sending the model to the api

    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    // lets pull out the total distance
    lineString ls = new lineString();
    double totalDistance = 0.0;
    int stopCount = 0;
    foreach (var r in Solution.Routes)
    {
      foreach (var e in r.interStops)
      {
        foreach (var a in e.Attributes)
        {
          if (a.dimId == "distance")
          {
            totalDistance += (a.endValue - a.startValue); // the difference between the start and end value
          }
        }
        foreach (var g in e.routeSegments)
        {
          ls.Add(new double[] { g.Longitude, g.Latitude }); // these are the road-network edges if you want to visualise the route in 
                                                            //leaflet or on a map.
        }
      }
    }
    Console.WriteLine(string.Format("Total Cost: {0:0.00} \t ", Solution.Objective));
    Console.WriteLine(string.Format("Total distance: {0:0.00} km\t Stops: " + stopCount, totalDistance));

    ivr7helper.printSolution(Solution);
    // the maximum quantity assigned to each vehicle is <= 2000 (the capacity dimension).
    // the majority of the cost is coming in the distance dimension (because of the way we've configured the vehicle cost class)

    // for visualisations see the R/python notebook for plots on the same example.

    return;
  }

  public Ivr7.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}