package icepackai;

import java.util.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;

public class dataRow {
  public dataRow(String _id, float x, float y) {
    id = _id;
    X = x;
    Y = y;
  };

  public dataRow(String _id, float x, float y, float ptime, float dtime, float qty) {
    id = _id;
    X = x;
    Y = y;
    pickupTime = ptime;
    dropoffTime = dtime;
    quantity = qty;
  };

  public String id;
  public float X;
  public float Y;
  public float pickupTime;
  public float dropoffTime;
  public float quantity;

  public static List<dataRow> LoadData(String filename) throws Exception {
    List<dataRow> res = new ArrayList<dataRow>();
    List<String> lines = Files.readAllLines(Paths.get(filename), Charset.defaultCharset());

    Boolean ns3format =
        (lines.get(0).split(",")[0].equals("id") && lines.get(0).split(",")[1].equals("name"));

    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);
      String[] items = line.split(",");
      if (items.length < 4) {
        res.add(new dataRow(items[0], Float.parseFloat(items[1]), Float.parseFloat(items[2])));
      } else {
        if (ns3format) {
          res.add(new dataRow(items[0], Float.parseFloat(items[2]), Float.parseFloat(items[3]), 0,
              0, Float.parseFloat(items[4])));
        } else {
          res.add(new dataRow(items[0], Float.parseFloat(items[1]), Float.parseFloat(items[2]),
              Float.parseFloat(items[3]), Float.parseFloat(items[4]), Float.parseFloat(items[5])));
        }
      }
    }
    return res;
  }
};
