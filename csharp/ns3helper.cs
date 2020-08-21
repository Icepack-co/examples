using System.Collections.Generic;
using System;

class ns3helper{

  public static Ns3.DimensionConfiguration make_distance_time_user_dimensions(string userDimension){

    var dimconfig = new Ns3.DimensionConfiguration();
    dimconfig.distanceConfig = new Ns3.InternalDimension{
      Id = "distance",
      measurementUnit =  Ns3.InternalDimension.eMeasurementUnit.Kilometres,
    };

    dimconfig.timeConfig = new Ns3.InternalDimension{
      Id = "time",
      measurementUnit = Ns3.InternalDimension.eMeasurementUnit.Hours,
    };
    if(userDimension != ""){
      dimconfig.userDimensions.Add(new Ns3.UserDimension{
        Id = userDimension,
        Units = "unknown"
      });
    }

    return dimconfig;
  }

  public static Ns3.Node make_node(string id, float longitude, float latitude){
    var node = new Ns3.Node{
      Id = id,
      Geocode = new Ns3.Geocode{
        Longitude = longitude,
        Latitude = latitude,
      }
    };
    return node;
  }

  public static List<Ns3.Node> make_nodes(List<dataRow> rows ){
      List<Ns3.Node> nodes = new List<Ns3.Node>();
      foreach( var n in rows){
        nodes.Add(make_node(n.id, n.X, n.Y));
      }
      return nodes;
  }

  public static Ns3.DimensionRange make_dimension_range(string dimid, float min, float max){
    var dr = new Ns3.DimensionRange{
      dimensionId = dimid,
      maxRange = max,
      minRange = min,
      flowPenalty = 1e6f,
    };
    return dr;
  }

  public static Ns3.UnitDimensionCost make_udc(string dimid, float coef, float costperunit){
    var udc = new Ns3.UnitDimensionCost{
      dimensionIds = {dimid},
      dimensionCoefficients = new float[]{coef},
      costPerUnit = costperunit
    };
    return udc;
  } 

  public static Ns3.LaneRate make_lane_rate_distance(string src, string dest, float costperkm){
    var lr = new Ns3.LaneRate{
      Id = ("lr:" + src + "->" + dest),
      Source = src,
      Destination = dest,
      unitDimensionCosts = {make_udc("distance", 1.0f, costperkm)}
    };
    return lr;
  }

  public static Ns3.LaneRate make_lane_rate_distance_weight(string src, string dest, float costperkm, string weightdim, float costperunit){
    var lr = new Ns3.LaneRate{
      Id = ("lr:" + src + "->" + dest),
      Source = src,
      Destination = dest,
      unitDimensionCosts = {make_udc(weightdim, 1.0f, costperunit)}
    };
    if(costperkm != 0.0f){
      lr.unitDimensionCosts.Add(make_udc("distance", 1.0f, costperkm));
    }
    return lr;
  }

  public static Ns3.ProductGroup make_single_product_group(string product){
    var pg = new Ns3.ProductGroup{
      productGroupId = product,
      productId = product
    };
    return pg;
  }

  public static Ns3.CostModel make_cost_model_distance(string src, float costperkm){
    var cm = new Ns3.CostModel{
      Id = ("costmodel: " + src + ":Beer"),
      Source = src,
      productGroupIds = {"Beer"},
      unitDimensionCosts = {make_udc("distance", 1.0f, costperkm)}
    };
    return cm;
  }

  public static void printSolution(Ns3.SolutionResponse resp, Ns3.SolveRequest sr){
    // we're just going to show the first 5 items from each table that one can construct here.
    // we also give an illustration of how to construct the geometries in this context.

    //assignment table.
    int[] maxchar = new int[]{ "Lane Rate".Length, "Cost Model".Length, "Source".Length,
    "Destination".Length, "Product".Length, "Amount".Length, "Cost".Length, "Distance".Length, "Duration".Length };
    foreach(var a in resp.Assignments){
      maxchar[0] = Math.Max(maxchar[0], a.laneRateId.Length);
      maxchar[1] = Math.Max(maxchar[1], a.costModelId.Length);
      maxchar[2] = Math.Max(maxchar[2], a.Source.Length);
      maxchar[3] = Math.Max(maxchar[3], a.Destination.Length);
      maxchar[4] = Math.Max(maxchar[4], a.productId.Length);
      //a.Amount
      //a.Cost
      //a.Distance
      //a.Duration
    }
    Console.WriteLine("Assignments:");
    string formatLine = "|{0,-" + maxchar[0] + "}|{1,-" + maxchar[1] + "}|{2,-" + maxchar[2] + "}|{3,-" +
     maxchar[3] + "}|{4,-" + maxchar[4] + "}|{5,10}|{6,10}|{7,10}|{8,10}|";
    Console.WriteLine(String.Format(formatLine, "Lane Rate", "Cost Model", "Source", "Destination", "Product", "Amount", "Cost", "Distance", "Duration"));
    int maxprint = 0;
    foreach(var a in resp.Assignments){
      Console.WriteLine(String.Format(formatLine, a.laneRateId, a.costModelId, a.Source, a.Destination, a.productId,
                        string.Format("{0:0.00}", a.Amount),
                        string.Format("{0:0.00}", a.Cost),
                        string.Format("{0:0.00}", a.Distance),
                        string.Format("{0:0.00}", a.Duration)));
                        maxprint++;
      if(maxprint > 10){
        Console.WriteLine("table truncated...");
        maxprint = 0;
        break;
      }
    }
    Console.WriteLine("");

    Console.WriteLine("Node Flow:");
    maxchar[0] = "NodeId".Length;

    foreach(var nf in resp.nodeFlows){
        maxchar[0] = Math.Max(maxchar[0], nf.nodeId.Length);
    }

    formatLine = "|{0,-" + maxchar[0] + "}|{1,10}|{2,10}|{3,10}|{4,10}|{5," + "ProductFixedCost".Length+ "}|{6," + 
    "ProductFlowCost".Length +"}|{7,10}|{8,10}|{9,10}|{10,10}|{11,10}|{12,10}|";
    Console.WriteLine(String.Format(formatLine, "NodeId", "InFlow", "OutFlow", "FixedCost", "FlowCost",  
            "ProductFixedCost", "ProductFlowCost", "P-Amount", "P-Cost", "P-Penalty", "C-Amount", "C-Cost", "C-Penalty"));
    
    foreach(var nf in resp.nodeFlows){
      Console.WriteLine(String.Format(formatLine, nf.nodeId, 
                        string.Format("{0:0.00}", nf.inFlow),
                        string.Format("{0:0.00}", nf.outFlow),
                        string.Format("{0:0.00}", nf.fixedCost),
                        string.Format("{0:0.00}", nf.flowCost),
                        string.Format("{0:0.00}", nf.productFixedCost),
                        string.Format("{0:0.00}", nf.productFlowCost),
                        string.Format("{0:0.00}", nf.productionAmount),
                        string.Format("{0:0.00}", nf.productionCost),
                        string.Format("{0:0.00}", nf.productionPenalty),
                        string.Format("{0:0.00}", nf.consumptionAmount),
                        string.Format("{0:0.00}", nf.consumptionCost),
                        string.Format("{0:0.00}", nf.consumptionPenalty)));
      maxprint++;
      if(maxprint > 10){
        Console.WriteLine("table truncated...");
        maxprint = 0;
        break;
      }
    }
    Console.WriteLine("");

    Console.WriteLine("Node Product Flow:");
    maxchar[0] = "NodeId".Length;
    maxchar[1] = "ProductId".Length;

    foreach(var nf in resp.nodeFlows){
        maxchar[0] = Math.Max(maxchar[0], nf.nodeId.Length);
    }

    formatLine = "|{0,-" + maxchar[0] + "}|{1,-" + maxchar[1] +"}|{2,10}|{3,10}|{4,10}|{5,10}|{6,10}|{7,10}|{8,10}|{9,10}|{10,10}|{11,10}|";
    Console.WriteLine(String.Format(formatLine, "NodeId", "ProductId", "InFlow", "OutFlow", "FixedCost", "FlowCost",  
            "P-Amount", "P-Cost", "P-Penalty", "C-Amount", "C-Cost", "C-Penalty"));
    
    foreach(var nf in resp.nodeProductFlows){
      Console.WriteLine(String.Format(formatLine, nf.nodeId, nf.productId, 
                        string.Format("{0:0.00}", nf.inFlow),
                        string.Format("{0:0.00}", nf.outFlow),
                        string.Format("{0:0.00}", nf.fixedCost),
                        string.Format("{0:0.00}", nf.flowCost),
                        string.Format("{0:0.00}", nf.productionAmount),
                        string.Format("{0:0.00}", nf.productionCost),
                        string.Format("{0:0.00}", nf.productionPenalty),
                        string.Format("{0:0.00}", nf.consumptionAmount),
                        string.Format("{0:0.00}", nf.consumptionCost),
                        string.Format("{0:0.00}", nf.consumptionPenalty)));
      maxprint++;
      if(maxprint > 10){
        Console.WriteLine("table truncated...");
        maxprint = 0;
        break;
      }
    }


    // lets unpack the geometries for each of the "routes"
    List<lineString> routeData = new List<lineString>();
    foreach(var r in resp.Routes){
      lineString  ls = new lineString();
      foreach(var i in r.geometrySequences){
         for(int j = 0; j < resp.geometrySequences[i].X.Length; j++){
           ls.Add(new double[2]{resp.geometrySequences[i].X[j], resp.geometrySequences[i].Y[j]});
         }
      }
      routeData.Add(ls);
    }
    var cplot = new consolePlot(routeData);

  }

}