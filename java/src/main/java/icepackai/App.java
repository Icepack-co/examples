package icepackai;

import java.util.*;

public class App {
  public static void main(String[] args) {
    try {
      String configFile = "../config.json";
      List<dataRow> data = dataRow.LoadData("../sample_data/publist.csv");

      // Matrix Examples
      matrix1basic basicMatrix = new matrix1basic(data, configFile);
      basicMatrix.Run();

      matrix1intermediate intermediateMatrix = new matrix1intermediate(data,
      configFile);
      intermediateMatrix.Run();

      // TSP Examples
      tsp1basic basicTsp = new tsp1basic(data, configFile);
      basicTsp.Run();

      // TSPTW Examples
      tsptw1basic basicTsptw = new tsptw1basic(data, configFile);
      basicTsptw.Run();

      data = dataRow.LoadData("../sample_data/publist_orders.csv");

      // CVRP Examples
      cvrp1basic basicCvrp = new cvrp1basic(data, configFile);
      basicCvrp.Run();

      // CVRPTW Examples
      cvrptw1basic basicCvrptw = new cvrptw1basic(data, configFile);
      basicCvrptw.Run();

      // IVR7 Examples
      ivr7_1_basic basicIvr7_1 = new ivr7_1_basic(data, configFile);
      basicIvr7_1.Run();

      ivr7_2_intermediate intermediateIvr7_2 = new ivr7_2_intermediate(data, configFile);
      intermediateIvr7_2.Run();

      ivr7_3_intermediate2 intermediateIvr7_3 = new ivr7_3_intermediate2(data, configFile);
      intermediateIvr7_3.Run();

      ivr7_4_advanced1 advancedIvr7_4 = new ivr7_4_advanced1(data, configFile);
      advancedIvr7_4.Run();

      ivr7_5_advanced2 advancedIvr7_5 = new ivr7_5_advanced2(data, configFile);
      advancedIvr7_5.Run();

      // IVR8 Examples
      ivr8_1_basic basicIvr8 = new ivr8_1_basic(data, configFile);
      basicIvr8.Run();

      ivr8_2_intermediate intermediateIvr8 = new ivr8_2_intermediate(data, configFile);
      intermediateIvr8.Run();

      ivr8_3_advanced advancedIvr8 = new ivr8_3_advanced(data, configFile);
      advancedIvr8.Run();

      // NS3 Examples
      data = dataRow.LoadData("../sample_data/publist_large.csv");
      ns3_1_basic basicNs3 = new ns3_1_basic(data, configFile);
      basicNs3.Run();

      ns3_2_intermediate intermidiateNs3 = new ns3_2_intermediate(data, configFile);
      intermidiateNs3.Run();
      
      ns3_3_advanced advancedNs3 = new ns3_3_advanced(data, configFile);
      advancedNs3.Run();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
