// IVR7 basic example:
// Illustrates using time, distance and a single capacity dimension
// Locations
// Pickup-dropoff tasks (with task times)
// single vehicle class
// single vehicle-cost class
// multiple vehicles.
using System.Collections.Generic;
using System;

class ivr7basic : IRunner
{
  public ivr7basic(List<dataRow> data, string configFile = "../config.json")
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

    // the first decision we have to make is which dimensional quantities to model in this example.
    // we're going to model the distance, time, and capacity of the vehicle.
    ivr7helper.makeDistanceTimeCapDims(sr.Model); // adds distance, time & capacity

    // lets pretend the first point is where vehicles are going to begin and end each day.
    // unlike the tsp/cvrp/pdp models, the ivr7 requires that you specify the unique locations
    // that are going to be used in the model as a separate entity. The reason for this is that you
    // can then specify the locations once, and reference those locations by id for other entities (such and vehicles/jobs/tasks)
    ivr7helper.makeLocations(sr.Model, data);     // adds all the locations to the model

    // so we've constructed some jobs with pickups and dropoffs, loading and offload times, as well as the
    // contribution to the capacity dimension. In this example, we're pickup up all orders at the guiness storehouse
    // and delivering at the list of customers. 'make_job_time_cap' is just a simple function to create this
    // particular style of request, but you can make your own.
    ivr7helper.makeJobTimeCap(sr.Model, data, ivr7helper.Rep(0, data.Count - 1), ivr7helper.Seq(1, data.Count));


    // we're going to do the vehicle-configuration now.
    // we need to specify the cost classes available, the vehicle classes available, and then the individual vehicles.
    // we're going to create one of each to keep things simple.
    sr.Model.vehicleCostClasses.Add(ivr7helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));


    // lets make the vehicle class. A vehicle class describes how the vehicle MOVES through the network.
    // so in other words, we can use the standard network travel speeds, or we could make the vehicle
    // move slower/faster relative to the road network. We could also attach transit rules here which are
    // great for modelling lunch breaks, refueling stops etc. (i.e. conditional triggers on the cumul values
    // of the dimension). Covered in an advanced section.
    sr.Model.vehicleClasses.Add(ivr7helper.makeVcSimple("vc1", 1, 1, 1, 1));

    // now we can just specify the vehicles.
    // lets provide 5 x 2 ton vehicles. Although this is probably more than we need.
    // the reason for this is that we're modelling a full-blown pickup+dropoff model, so if there's
    // time to reload, a vehicle can return to the depot and grab more goodies!
    for (int i = 0; i < 5; i++)
    {
      sr.Model.Vehicles.Add(ivr7helper.makeVehicleCap("vehicle_" + i, // unique id for the vehicle.
                                                      "vc1",  // the vehicle class
                                                      "vcc1", // the vehicle cost class
                                                      2000,  // the capacity of the vehicle
                                                      data[0].id, // start location for the vehicle
                                                      data[0].id, // end location for the vehicle
                                                      7 * 60,  // start time: 7 AM
                                                      18 * 60  // end time: 6 PM
                                                      ));
    }

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