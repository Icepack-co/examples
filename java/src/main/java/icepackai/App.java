package icepackai;

import java.util.*;

public class App {
  public static void main(String[] args) {
    try {
      String configFile = "../config.json";
      List<dataRow> data = dataRow.LoadData("../sample_data/publist.csv");

      matrix1basic basicMatrix = new matrix1basic(data, configFile);
      basicMatrix.Run();

      matrix1intermediate intermediateMatrix = new matrix1intermediate(data,
      configFile);
      intermediateMatrix.Run();

      tsp1basic basicTsp = new tsp1basic(data, configFile);
      basicTsp.Run();

      tsptw1basic basicTsptw = new tsptw1basic(data, configFile);
      basicTsptw.Run();

      cvrp1basic basicCvrp = new cvrp1basic(data, configFile);
      basicCvrp.Run();

      data = dataRow.LoadData("../sample_data/publist_orders.csv");
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

      ivr8_1_basic basicIvr8 = new ivr8_1_basic(data, configFile);
      basicIvr8.Run();

      ivr8_2_intermediate intermediateIvr8 = new ivr8_2_intermediate(data, configFile);
      intermediateIvr8.Run();

      ivr8_3_advanced advancedIvr8 = new ivr8_3_advanced(data, configFile);
      advancedIvr8.Run();

    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
