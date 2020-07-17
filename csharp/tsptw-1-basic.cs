using System.Collections.Generic;
using System;
// this example illustrates how to use the api to solve a simple tsp with time windows.
// time windows are randomly generated so there's a change not all stops can be serviced
class tsptw1basic : IRunner
{
  public tsptw1basic(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data;
  }

  public void Run()
  {
    var api = new ApiHelper<Tsptw.SolveRequest, Tsptw.SolutionResponse>("tsptw-kcxbievqo879", configFile);
    // so here we're going to build the model 

    // create a solve request
    Tsptw.SolveRequest sr = new Tsptw.SolveRequest();

    sr.Model = new Tsptw.Tsp(); // initialise the model container

    Random rand = new Random();
    // add points to the tsp model
    foreach (var d in data)
    {
      // lets randomly create a window here.
      double rupper = 2500;
      double ws = rand.NextDouble() * rupper;
      double we = ws + rupper; //we don't accept backwards windows, so we'll just set these to some positive width upper amount.
      sr.Model.Points.Add(new Tsptw.Geocode
      {
        Id = d.id,
        X = d.X,
        Y = d.Y,
        windowStart = (float)ws,
        windowEnd = (float)we,
      });
    }

    // configure the distance metric (although road network is the default)
    sr.Model.Distancetype = Tsptw.Tsp.eDistanceType.RoadNetwork;

    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    // lets pull out the total distance
    lineString ls = new lineString();
    double totalDistance = 0.0;
    foreach (var e in Solution.Edges)
    {
      totalDistance += e.Distance;
      foreach (var g in e.Geometries)
      {
        ls.Add(new double[2] { g.X, g.Y });
      }
    }
    Console.WriteLine(string.Format("Total distance: {0:0.00} km\t Stops: " + Solution.Tours.Count, totalDistance));
    int maxchar = 0;
    foreach (var d in data)
    {
      maxchar = Math.Max(d.id.Length, maxchar); // so that the table displays nicely in the console :-) 
    }
    string formatLine = "|{0,-" + maxchar + "}|{1,11}|";
    Console.WriteLine(String.Format(formatLine, "Location", "ArrivalTime"));
    for (int i = 0; i < Solution.arrivalTimes.Length; i++)
    {
      Console.WriteLine(String.Format(formatLine, Solution.Tours[i], Solution.arrivalTimes[i]));
    }
    // just for fun - you wouldn't ever plot things this way - but it is nice to "see" the line segments
    Console.WriteLine("Approximate Tour Map:");
    new consolePlot(new List<lineString> { ls });

    return;
  }

  public Tsptw.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}