using System.Collections.Generic;
using System.Text;
using System;

// An intermediate example of how to use the matrix api to generate distance/time matricies
// creates a partial matrix with two sources and four destinations. (i.e. 8 elements = 2*4)

class matrix1intermediate : IRunner
{
  public matrix1intermediate(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data.GetRange(0, 6); //
  }

  public void Run()
  {
    // so here we're going to build the model 
    var api = new ApiHelper<Matrix.MatrixRequest, Matrix.MatrixResponse>("matrix-vyv95n7wchpl", configFile);
    // create a solve request
    Matrix.MatrixRequest sr = new Matrix.MatrixRequest();


    // add points to the matrix request
    foreach (var d in data)
    {
      sr.Locations.Add(new Matrix.Location
      {
        Id = d.id,
        Geocode = new Matrix.Geocode
        {
          Longitude = d.X,
          Latitude = d.Y
        }
      });
    }

    // in this example we add the first two locations as sources and the balance as destinations
    for (int i = 0; i < data.Count; i++)
    {
      if (i < 2)
      {
        sr.Sources.Add(data[i].id);
      }
      else
      {
        sr.Destinations.Add(data[i].id);
      }
    }

    // configure the distance metric (although road network is the default)
    sr.distanceUnit = Matrix.MatrixRequest.eDistanceUnit.Kilometres;
    sr.durationUnit = Matrix.MatrixRequest.eDurationUnit.Minutes;

    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    Console.WriteLine(Solution.Elements.Count + " elements returned by the api");

    // We'll write this one out in long form. 
    int maxchar = 0;
    foreach (var d in data)
    {
      maxchar = Math.Max(d.id.Length, maxchar); // so that the table displays nicely in the console :-) 
    }
    string formatLine = "|{0,-" + maxchar + "}|{1,-" + maxchar + "}|{2,15}|{3,15}|";
    Console.WriteLine(String.Format(formatLine, "From", "To", "Distance", "Time"));
    foreach (var item in Solution.Elements)
    {
      Console.WriteLine(String.Format(formatLine, item.fromId, item.toId, item.Distance, item.Duration));
    }

    return;
  }


  public Matrix.MatrixResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}