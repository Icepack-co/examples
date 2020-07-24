using System.Collections.Generic;
using System;
// this example illustrates how to use the api to solve a simple cvrp
// which is classic capacitated vehicle routing problem with a heterogeneous fleet.
// This means we need only specify the size of the vehicle and the number of vehicles available.
// The other aspect of this model is to include the location of the depot.

class cvrp1basic : IRunner
{
  public cvrp1basic(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data.GetRange(0, 10); // grab the first 10 items. We'll be assuming the first is the depot in this example
  }

  public void Run()
  {

    var api = new ApiHelper<Cvrp.SolveRequest, Cvrp.SolutionResponse>("cvrp-jkfdoctmp51n", configFile);
    // so here we're going to build the model 

    // create a solve request
    Cvrp.SolveRequest sr = new Cvrp.SolveRequest();

    sr.Model = new Cvrp.Cvrp(); // initialise the model container

    // add the depot (first point) in the model
    for (int i = 0; i < data.Count; i++)
    {
      if (i == 0)
      {
        sr.Model.Depot = new Cvrp.Geocode
        {
          Id = data[i].id,
          X = (float)data[i].X,
          Y = (float)data[i].Y,
          Quantity = (float)0.0
        }; // this adds the depot to the model
      }
      else
      {
        // add the points as demand points. Assume that each point has a demand quantity of 20
        sr.Model.Points.Add(new Cvrp.Geocode
        {
          Id = data[i].id,
          X = (float)data[i].X,
          Y = (float)data[i].Y,
          Quantity = (float)20
        });
      }

    }

    // configure the distance metric (although road network is the default)
    sr.Model.Distancetype = Cvrp.Cvrp.eDistanceType.RoadNetwork;
    sr.Model.VehicleCapacity = 100;  // set a vehicle capacity of 100
    sr.Model.NumberOfVehicles = 2;   // allow the use of at-most, two vehicles.


    // now it's just sending the 

    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    // lets pull out the total distance
    lineString ls = new lineString();
    double totalDistance = 0.0;
    int stopCount = 0;
    foreach (var r in Solution.Routes)
    {
      stopCount += r.Sequences.Count;
      foreach (var e in r.Edges)
      {
        totalDistance += e.Distance;
      }
    }
    Console.WriteLine(string.Format("Total Cost: {0:0.00} \t ", Solution.Objective));
    Console.WriteLine(string.Format("Total distance: {0:0.00} km\t Stops: " + stopCount, totalDistance));

    foreach (var r in Solution.Routes)
    {
      if (r.Sequences.Count > 0)
      {
        Console.WriteLine("Route: ");
        int stopc = 1;
        double vCap = 0.0;
        for (int i = 0; i < r.Sequences.Count; i++)
        {
          vCap += r.visitCapacities[i];
          Console.WriteLine("  stop " + stopc + ": " + (int)(vCap) + ", " + r.Sequences[i]);
          stopc++;
        }
      }
    }
    // the cumulative quantity assigned to each route is <= 100.

    return;
  }

  public Cvrp.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}