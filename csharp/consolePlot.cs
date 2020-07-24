using System.Collections.Generic;
using System;
using System.Text;
class lineString : List<double[]>{ }

// this is just for run. You would never do this in a proper application
class consolePlot
{
  public consolePlot(List<lineString> routes)
  {

    int width = (int)(Console.WindowWidth * 0.6);
    int height = (int)(Console.WindowHeight);

    //width = Math.Min(width - 1, height - 1);
    //height = Math.Min(width - 1, height - 1);


    // lets get the bounds of the plot data. We're not going to do the mecator re-projection
    // we'll rather pretend it's just cartesian.
    double minX = 1e30;
    double minY = 1e30;
    double maxX = -1e30;
    double maxY = -1e30;
    double boundaryVal = 0.001;
    foreach (var gl in routes)
    {
      foreach (var g in gl)
      {
        minX = Math.Min(minX, g[0] - boundaryVal);
        maxX = Math.Max(maxX, g[0] + boundaryVal);
        minY = Math.Min(minY, g[1] - boundaryVal);
        maxY = Math.Max(maxY, g[1] + boundaryVal);
      }
    }
    // then we need to make a simple matrix which indicates where the points lie on the map.
    double rangeX = maxX - minX;
    double rangeY = maxY - minY;

    int[][] M = new int[width][];
    for (int i = 0; i < M.Length; i++)
    {
      M[i] = new int[width];
    }
    // then we can work out where the points lie on the grid.
    foreach (var gl in routes)
    {
      foreach (var g in gl)
      {
        // transform the coordinate to the grid.
        double x = ((g[0] - minX) / rangeX) * width;
        double y = ((g[1] - minY) / rangeY) * width;
        M[(int)y][(int)x] += 1;
      }
    }
    for (int i = width - 1; i > -1; i--)
    { // have to write from the bottom up
      // now write each route to the console.
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < width; j++)
      {
        if (M[i][j] > 0)
        {
          sb.Append("*");
        }
        else
        {
          sb.Append(" ");
        }
      }
      Console.WriteLine(sb.ToString());
    }
  }
}