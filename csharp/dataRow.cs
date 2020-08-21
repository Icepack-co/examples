
using System.Collections.Generic;


public class dataRow
{
  public string id { get; set; }

  public float X { get; set; }

  public float Y { get; set; }

  public float pickupTime { get; set; }

  public float dropoffTime { get; set; }

  public float quanity { get; set; }

  public static List<dataRow> LoadData(string filename, int maxItems = int.MaxValue)
  {
    List<dataRow> res = new List<dataRow>();
    var lines = System.IO.File.ReadAllLines(filename);
    bool ns3format = (lines[0].Split(",")[0] == "id" && lines[0].Split(",")[1] == "name");
    for (int i = 1; i < lines.Length; i++)
    {
      if (res.Count >= maxItems)
      {
        break;
      }
      var line = lines[i];
      var items = line.Split(",");
      if (items.Length < 4)
      {
        // just the locations and geocodes
        res.Add(new dataRow
        {
          id = items[0],
          X = float.Parse(items[1], System.Globalization.CultureInfo.InvariantCulture),
          Y = float.Parse(items[2], System.Globalization.CultureInfo.InvariantCulture)
        });
      }
      else
      {
        if(ns3format){
          res.Add(new dataRow
          {
            id = items[0],
            X = float.Parse(items[2], System.Globalization.CultureInfo.InvariantCulture),
            Y = float.Parse(items[3], System.Globalization.CultureInfo.InvariantCulture),
            quanity = float.Parse(items[4], System.Globalization.CultureInfo.InvariantCulture)
          });
        }else{
          // load the pickup/dropoff times and quantites
          res.Add(new dataRow
          {
            id = items[0],
            X = float.Parse(items[1], System.Globalization.CultureInfo.InvariantCulture),
            Y = float.Parse(items[2], System.Globalization.CultureInfo.InvariantCulture),
            pickupTime = float.Parse(items[3], System.Globalization.CultureInfo.InvariantCulture),
            dropoffTime = float.Parse(items[4], System.Globalization.CultureInfo.InvariantCulture),
            quanity = float.Parse(items[5], System.Globalization.CultureInfo.InvariantCulture)
          });
        }        
      }
    }
    return res;
  }
}


