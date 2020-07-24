using System.Collections.Generic;
using System.Text;

// A simple example of how to build a complete matrix [n by n] using the api.
class matrix1basic : IRunner
{
  public matrix1basic(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data.GetRange(0, 5); // grabs just the first 5 items.
  }

  public void Run()
  {
    // so here we're going to build the model 
    var api = new ApiHelper<Matrix.MatrixRequest, Matrix.MatrixResponse>("matrix-vyv95n7wchpl", configFile);
    // create a solve request
    Matrix.MatrixRequest sr = new Matrix.MatrixRequest();

    // add locations to the matrix request
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
      sr.Sources.Add(d.id); // this will ensure a complete matrix. because destinations are empty, we assume you're asking for sources:sources
    }

    // configure the distance metric (although road network is the default)
    sr.distanceUnit = Matrix.MatrixRequest.eDistanceUnit.Kilometres;
    sr.durationUnit = Matrix.MatrixRequest.eDurationUnit.Minutes;

    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    System.Console.WriteLine(Solution.Elements.Count + " elements returned by the api");
    // lets make a matrix!
    Dictionary<string, int> idToIndex = new Dictionary<string, int>();
    for (int i = 0; i < data.Count; i++)
    {
      idToIndex.Add(data[i].id, i);
    }
    double[][] DM = new double[data.Count][];
    double[][] TM = new double[data.Count][];
    for (int i = 0; i < data.Count; i++)
    {
      DM[i] = new double[data.Count]; // make a square matrix
      TM[i] = new double[data.Count]; // make a square matrix
    }
    foreach (var item in Solution.Elements)
    {
      DM[idToIndex[item.fromId]][idToIndex[item.toId]] = item.Distance;
      TM[idToIndex[item.fromId]][idToIndex[item.toId]] = item.Distance;
    }
    printMatrix(DM, "Distance matrix");
    printMatrix(TM, "Time Matrix");
    return;
  }

  private void printMatrix(double[][] matrix, string name)
  {
    System.Console.WriteLine(name + ":");
    int rows = matrix.GetLength(0);
    for (int i = 0; i < rows; i++)
    {
      StringBuilder sb = new StringBuilder();
      int columns = matrix[i].Length;
      for (int j = 0; j < columns; j++)
      {
        sb.Append(string.Format("|{0:0.00}", matrix[i][j]));
      }
      sb.Append("|");
      System.Console.WriteLine(sb.ToString());
    }
    System.Console.WriteLine("");
  }

  public Matrix.MatrixResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}