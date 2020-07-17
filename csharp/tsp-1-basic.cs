using System.Collections.Generic;

class tsp1basic : IRunner
{
  public tsp1basic(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data;
  }

  public void Run()
  {
    var api = new ApiHelper<Tsp.SolveRequest, Tsp.SolutionResponse>("tsp-mcvfz472gty6", configFile);
    // so here we're going to build the model 

    // create a solve request
    Tsp.SolveRequest sr = new Tsp.SolveRequest();

    sr.Model = new Tsp.Tsp(); // initialise the model container

    // add points to the tsp model
    foreach (var d in data)
    {
      sr.Model.Points.Add(new Tsp.Geocode
      {
        Id = d.id,
        X = d.X,
        Y = d.Y
      });
    }

    // configure the distance metric (although road network is the default)
    sr.Model.Distancetype = Tsp.Tsp.eDistanceType.RoadNetwork;

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
    System.Console.WriteLine(string.Format("Total distance: {0:0.00} km\t Stops: " + Solution.Tours.Count, totalDistance));

    // just for fun - you wouldn't ever plot things this way - but it is nice to see the line segments
    new consolePlot(new List<lineString> { ls });

    return;
  }

  public Tsp.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}