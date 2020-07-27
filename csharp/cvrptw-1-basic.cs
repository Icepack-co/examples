using System.Collections.Generic;
using System;
// A classic Cvrptwtw has a heterogeneous fleet. This means we need only specify the size of the
// vehicle and the number of vehicles available. The other aspect of this model is to include
// the location of the depot. The Cvrptwtw is costed differently to the Cvrptw. The objective is still
// to minimise the number of vehicles used, but also to minimise the total time. The classic cvrp
// aims to minimise the number of vehicles, then the total distance travelled.
// The cvrptw has time windows on each point. In this schema, we allow you to omit windows from 
// points if needed.

class cvrptw1basic : IRunner
{
  public cvrptw1basic(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data.GetRange(0, 10); // grab the first 10 items. We'll be assuming the first is the depot in this example
  }

  public void Run()
  {
    var api = new ApiHelper<Cvrptw.SolveRequest, Cvrptw.SolutionResponse>("cvrptw-acyas3nzweqb", configFile);
    // so here we're going to build the model 

    // create a solve request
    Cvrptw.SolveRequest sr = new Cvrptw.SolveRequest();

    sr.Model = new Cvrptw.Cvrptw(); // initialise the model container

    // add the depot (first point) in the model
    for (int i = 0; i < data.Count; i++)
    {
      if (i == 0)
      {
        sr.Model.Depot = new Cvrptw.Geocode
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
        // lets randomly split the windows into "morning" and "afternoon" windows
        // note that the windows are measured in minutes in this schema.

        sr.Model.Points.Add(new Cvrptw.Geocode
        {
          Id = data[i].id,
          X = (float)data[i].X,
          Y = (float)data[i].Y,
          windowStart = i % 2 == 0 ? (float)(8*60) : (float)(12*60),
          windowEnd = i % 2 == 0 ? (float)(12*60) : (float)(16*60),
          Quantity = (float)20
        });
      }

    }

    // configure the distance metric (although road network is the default)
    sr.Model.Distancetype = Cvrptw.Cvrptw.eDistanceType.RoadNetwork;
    sr.Model.VehicleCapacity = 100;  // set a vehicle capacity of 100
    sr.Model.NumberOfVehicles = 3;   // allow the use of at-most, three vehicles


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
          Console.WriteLine("  stop " + stopc + ": " + (int)(vCap) + ", " + r.Sequences[i] + ", " + r.arrivalTimes[i]);
          stopc++;
        }
      }
    }
    // the cumulative quantity assigned to each route is <= 100.

    return;
  }

  public Cvrptw.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}