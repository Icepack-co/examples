package icepackai;

import java.util.*;

import icepackai.NS3.Ns3Tbfvuwtge2Iq.InternalDimension.eMeasurementUnit;
import icepackai.CVRP.CvrpJkfdoctmp51N.SolutionResponse;
import icepackai.NS3.*;

import dnl.utils.text.table.TextTable;

// some helper functions for building ns3 models.
public class ns3helper {
  public static Ns3Tbfvuwtge2Iq.DimensionConfiguration makeDistanceTimeUserDims(String userDim) {
    Ns3Tbfvuwtge2Iq.DimensionConfiguration.Builder dimBuilder =
        Ns3Tbfvuwtge2Iq.DimensionConfiguration.newBuilder();
    dimBuilder.setDistanceConfig(Ns3Tbfvuwtge2Iq.InternalDimension.newBuilder()
                                     .setId("distance")
                                     .setMeasurementUnit(eMeasurementUnit.KILOMETRES)
                                     .build());
    dimBuilder.setTimeConfig(Ns3Tbfvuwtge2Iq.InternalDimension.newBuilder()
                                 .setId("time")
                                 .setMeasurementUnit(eMeasurementUnit.HOURS)
                                 .build());
    if (userDim != "") {
      dimBuilder.addUserDimensions(
          Ns3Tbfvuwtge2Iq.UserDimension.newBuilder().setId(userDim).setUnits("unknown").build());
    }
    return dimBuilder.build();
  }

  public static Ns3Tbfvuwtge2Iq.Node makeNode(String id, float longitude, float latitude) {
    Ns3Tbfvuwtge2Iq.Node.Builder n = Ns3Tbfvuwtge2Iq.Node.newBuilder();
    n.setId(id).setGeocode(
        Ns3Tbfvuwtge2Iq.Geocode.newBuilder().setLongitude(longitude).setLatitude(latitude).build());
    return n.build();
  }

  public static List<Ns3Tbfvuwtge2Iq.Node> makeNodes(List<dataRow> rows) {
    List<Ns3Tbfvuwtge2Iq.Node> nodes = new ArrayList<Ns3Tbfvuwtge2Iq.Node>();
    for (int i = 0; i < rows.size(); i++) {
      dataRow loc = rows.get(i);
      nodes.add(makeNode(loc.id, loc.X, loc.Y));
    }
    return nodes;
  }

  public static Ns3Tbfvuwtge2Iq.DimensionRange make_dimension_range(
      String dimid, float min, float max) {
    Ns3Tbfvuwtge2Iq.DimensionRange.Builder dr = Ns3Tbfvuwtge2Iq.DimensionRange.newBuilder();
    return dr.setDimensionId(dimid).setMaxRange(max).setMinRange(min).setFlowPenalty(1e6f).build();
  }

  public static Ns3Tbfvuwtge2Iq.UnitDimensionCost make_udc(
      String dimid, float coef, float costperunit) {
    Ns3Tbfvuwtge2Iq.UnitDimensionCost.Builder udc = Ns3Tbfvuwtge2Iq.UnitDimensionCost.newBuilder();
    return udc.addDimensionIds(dimid)
        .addDimensionCoefficients(coef)
        .setCostPerUnit(costperunit)
        .build();
  }

  public static Ns3Tbfvuwtge2Iq.LaneRate make_lane_rate_distance(
      String src, String dest, float costperkm) {
    Ns3Tbfvuwtge2Iq.LaneRate.Builder lr = Ns3Tbfvuwtge2Iq.LaneRate.newBuilder();
    return lr.setId("lr:" + src + "->" + dest)
        .setSource(src)
        .setDestination(dest)
        .addUnitDimensionCosts(make_udc("distance", 1.0f, costperkm))
        .build();
  }

  public static Ns3Tbfvuwtge2Iq.LaneRate make_lane_rate_distance_weight(
      String src, String dest, float costperkm, String weightdim, float costperunit) {
    Ns3Tbfvuwtge2Iq.LaneRate.Builder lr = Ns3Tbfvuwtge2Iq.LaneRate.newBuilder();
    lr = lr.setId("lr:" + src + "->" + dest)
             .setSource(src)
             .setDestination(dest)
             .addUnitDimensionCosts(make_udc(weightdim, 1.0f, costperunit));
    if (costperkm != 0.0f) {
      return lr.addUnitDimensionCosts(make_udc("distance", 1.0f, costperkm)).build();
    }
    return lr.build();
  }

  public static Ns3Tbfvuwtge2Iq.ProductGroup make_simple_product_group(String product) {
    Ns3Tbfvuwtge2Iq.ProductGroup.Builder pg = Ns3Tbfvuwtge2Iq.ProductGroup.newBuilder();
    return pg.setProductId(product).setProductGroupId(product).build();
  }

  public static Ns3Tbfvuwtge2Iq.CostModel make_cost_model_distance(String src, float costperkm) {
    Ns3Tbfvuwtge2Iq.CostModel.Builder cm = Ns3Tbfvuwtge2Iq.CostModel.newBuilder();
    return cm.setId("costmodel: " + src + ":Beer")
        .setSource(src)
        .addProductGroupIds("Beer")
        .addUnitDimensionCost(make_udc("distance", 1.0f, costperkm))
        .build();
  }

  public static void printSolution(Ns3Tbfvuwtge2Iq.SolutionResponse solution) {
    System.out.println("Assignment table:");
    String[] columnNames = new String[] {"Lane Rate", "Cost Model", "Source", "Destination",
        "Product", "Amount", "Cost", "Distance", "Duration"};
    List<Object[]> tabData = new ArrayList<Object[]>();

    Integer maxPrint = 0;
    for (Ns3Tbfvuwtge2Iq.SolutionResponse.Assignment r : solution.getAssignmentsList()) {
      tabData.add(
          new Object[] {r.getLaneRateId(), r.getCostModelId(), r.getSource(), r.getDestination(),
              r.getProductId(), r.getAmount(), r.getCost(), r.getDistance(), r.getDuration()});
      maxPrint++;
      if (maxPrint > 10) {
        break;
      }
    }
    printTable(tabData, columnNames);
    if (maxPrint > 10) {
      System.out.println("Ouputput truncated..." + (solution.getAssignmentsCount() - maxPrint)
          + " items skipped.");
    }

    columnNames = new String[] {"NodeId", "InFlow", "OutFlow", "FixedCost", "FlowCost",
        "ProductFixedCost", "ProductFlowCost", "P-Amount", "P-Cost", "P-Penalty", "C-Amount",
        "C-Cost", "C-Penalty"};
    tabData.clear();
    maxPrint = 0;
    for (Ns3Tbfvuwtge2Iq.SolutionResponse.NodeFlow nf : solution.getNodeFlowsList()) {
      tabData.add(new Object[] {nf.getNodeId(), nf.getInFlow(), nf.getOutFlow(), nf.getFixedCost(),
          nf.getFlowCost(), nf.getProductFixedCost(), nf.getProductFlowCost(),
          nf.getProductionAmount(), nf.getProductionCost(), nf.getProductionPenalty(),
          nf.getConsumptionAmount(), nf.getConsumptionCost(), nf.getConsumptionPenalty()});
      maxPrint++;
      if (maxPrint > 10) {
        break;
      }
    }
    System.out.println("\nNode Flow table:");
    printTable(tabData, columnNames);
    if (maxPrint > 10) {
      System.out.println("Ouputput truncated..." + (solution.getAssignmentsCount() - maxPrint)
          + " items skipped.");
    }

    columnNames = new String[] {"NodeId", "ProductId", "InFlow", "OutFlow", "FixedCost", "FlowCost",
        "P-Amount", "P-Cost", "P-Penalty", "C-Amount", "C-Cost", "C-Penalty"};
    tabData.clear();
    maxPrint = 0;
    for (Ns3Tbfvuwtge2Iq.SolutionResponse.NodeProductFlow nf : solution.getNodeProductFlowsList()) {
      tabData.add(new Object[] {nf.getNodeId(), nf.getInFlow(), nf.getProductId(), nf.getOutFlow(),
          nf.getFixedCost(), nf.getFlowCost(), nf.getProductionAmount(), nf.getProductionCost(),
          nf.getProductionPenalty(), nf.getConsumptionAmount(), nf.getConsumptionCost(),
          nf.getConsumptionPenalty()});
      maxPrint++;
      if (maxPrint > 10) {
        break;
      }
    }
    System.out.println("\nNode Product Flow table:");
    printTable(tabData, columnNames);
    if (maxPrint > 10) {
      System.out.println("Ouputput truncated..." + (solution.getAssignmentsCount() - maxPrint)
          + " items skipped.");
    }

    // then just for the curious reader. Each of the geometries is available in the payload. The
    // route defines the sequence of geometry segments that should be joined to form the linestring
    // for a particular route.
    List<List<double[]>> routes = new ArrayList<List<double[]>>();
    for (Ns3Tbfvuwtge2Iq.SolutionResponse.Route r : solution.getRoutesList()) {
      List<double[]> linestring = new ArrayList<double[]>();
      for (int index : r.getGeometrySequenceList()) {
        Ns3Tbfvuwtge2Iq.SolutionResponse.GeometrySequence gs = solution.getGeometrySequence(index);
        for (int i = 0; i < gs.getXCount(); i++) {
          linestring.add(new double[] {gs.getX(i), gs.getY(i)});
        }
      }
      routes.add(linestring);
    }
  }

  private static void printTable(List<Object[]> tabData, String[] columnNames) {
    Object[][] tab = new Object[tabData.size()][];
    for (int i = 0; i < tabData.size(); i++) {
      tab[i] = tabData.get(i);
    }
    TextTable tt = new TextTable(columnNames, tab);
    tt.printTable();
  }
}