using System;
using Newtonsoft.Json;
using ProtoBuf;

namespace csharp
{
  class Program
  {
    static void Main(string[] args)
    {
      // Matrix examples:
      var data = dataRow.LoadData("../sample_data/publist.csv");

      matrix1basic mat1 = new matrix1basic(data);
      mat1.Run();

      matrix1intermediate mat2 = new matrix1intermediate(data);
      mat2.Run();

      // TSP Examples
      tsp1basic basicTsp = new tsp1basic(data);
      basicTsp.Run();

      // TSPTW Examples
      tsptw1basic basicTsptw = new tsptw1basic(data);
      basicTsptw.Run();

      data = dataRow.LoadData("../sample_data/publist_orders.csv");

      // CVRP Examples
      cvrp1basic basicCvrp = new cvrp1basic(data);
      basicCvrp.Run();

      cvrptw1basic basicCvrptw = new cvrptw1basic(data);
      basicCvrptw.Run();

      // IVR7 Examples
      ivr7basic basicIvr7 = new ivr7basic(data);
      basicIvr7.Run();

      ivr7intermediate1 intermediateIvr71 = new ivr7intermediate1(data);
      intermediateIvr71.Run();

      ivr7intermediate2 intermediateIvr72 = new ivr7intermediate2(data);
      intermediateIvr72.Run();

      ivr7advanced1 advancedIvr71 = new ivr7advanced1(data);
      advancedIvr71.Run();

      ivr7advanced2 advancedIvr72 = new ivr7advanced2(data);
      advancedIvr72.Run();

      // IVR8 Examples
      ivr8basic basicIvr8 = new ivr8basic(data);
      basicIvr8.Run();

      ivr8intermediate intermediateIvr8 = new ivr8intermediate(data);
      intermediateIvr8.Run();

      ivr8advanced advancedIvr8 = new ivr8advanced(data);
      advancedIvr8.Run();

      // NS3 Examples
      data = dataRow.LoadData("../sample_data/publist_large.csv", 100);
      ns3basic basicns3 = new ns3basic(data);
      basicns3.Run();

      ns3intermediate intermediatens3 = new ns3intermediate(data);
      intermediatens3.Run();

      ns3advanced advancedns3 = new ns3advanced(data);
      advancedns3.Run();

    }
  }
}