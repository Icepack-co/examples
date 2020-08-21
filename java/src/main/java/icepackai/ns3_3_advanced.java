package icepackai;

import icepackai.NS3.Ns3Tbfvuwtge2Iq;
import icepackai.NS3.Ns3Tbfvuwtge2Iq.SolveRequest.SolveType;
import icepackai.NS3.Ns3Tbfvuwtge2Iq.SolveRequest.GeometryOutput;

import java.util.*;
import java.util.stream.Collectors;

// An advanced network sourcing model
// uses one production, two intermediate and multiple consumption locations.
// We're moving one product "Beer" measured in the dimension "weight"
// How are movements in the network costed?
// - Same configuration as the intermediate-2 example:
//   * We have two lane rates between our main production center and the two warehouses
//   * and a distribution "Cost Model" between the sources (proudction + 2 x warehouses) and
//   consumption nodes
//   * Lane rates between Production and Intermediate nodes are costed on a cost per km basis.
//   * Cost models to distribute the quantities further is also based on a (more expensive) cost per
//   km.
//   * It's typical that high utilisation vehicles move between warehouses (and typically larger
//   vehicles, achieving a lower cost per km / cost per ton)
//   * And that smaller vehicles handle the secondary distribution (at a higher cost per ton)
//   * adding a fixed cost trigger for using a warehouse
//   * the allows modelling selecting between the two warehouses, or potentially using both.
// - We're going to add a constraint where deliveries from the guiness storehouse may only be
//   made withing a radius of 50 km's. This is typical when you need to restrict a fleet to
//   a vehicle type (like a secondary delivery vehicle) which you don't want leaving a major city
// - we're also going to add a constraint where only 90% of the volume can be serviced by the
//   production node, which means we'll incur consumption penalties, but that's okay.

public class ns3_3_advanced {
  public ns3_3_advanced(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data = inputdata.stream().limit(100).collect(
        Collectors.toList()); // grabs just the first 100 items.
  }

  private double DegreesToRadians(double degrees) {
    return degrees * Math.PI / 180.0;
  }

  private double GreatCircleDistance(double lat1, double lon1, double lat2, double lon2) {
    double radius = 6371; // Radius of the Earth in km.
    lat1 = DegreesToRadians(lat1);
    lon1 = DegreesToRadians(lon1);
    lat2 = DegreesToRadians(lat2);
    lon2 = DegreesToRadians(lon2);
    double d_lat = lat2 - lat1;
    double d_lon = lon2 - lon1;
    double h = Math.sin(d_lat / 2) * Math.sin(d_lat / 2)
        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(d_lon / 2) * Math.sin(d_lon / 2);
    return 2 * radius * Math.asin(Math.sqrt(h));
  }

  private double GreatCircleDistance(Ns3Tbfvuwtge2Iq.Node n1, Ns3Tbfvuwtge2Iq.Node n2) {
    return GreatCircleDistance(n1.getGeocode().getLatitude(), n1.getGeocode().getLongitude(),
        n2.getGeocode().getLatitude(), n2.getGeocode().getLongitude());
  }

  public void Run() throws Exception {
    api = new apiHelper<Ns3Tbfvuwtge2Iq.SolutionResponse>(
        Ns3Tbfvuwtge2Iq.SolutionResponse.class, "ns3-tbfvuwtge2iq", configFile);
    // so here we're going to build the model

    // create a solve request
    Ns3Tbfvuwtge2Iq.SolveRequest.Builder sr = Ns3Tbfvuwtge2Iq.SolveRequest.newBuilder();

    Ns3Tbfvuwtge2Iq.Model.Builder model = Ns3Tbfvuwtge2Iq.Model.newBuilder();

    // we're going to employ the model helper here
    // lets use a single dimension for this model of weight.
    model.setDimensions(ns3helper.makeDistanceTimeUserDims("weight"));

    List<dataRow> productionNodes = new ArrayList<dataRow>();
    List<dataRow> warehouseNodes = new ArrayList<dataRow>();
    List<dataRow> demandNodes = new ArrayList<dataRow>();

    // lets assume we can go factory-direct or through a warehouse!
    List<String> sources = new ArrayList<String>();

    for (int i = 0; i < data.size(); i++) {
      dataRow row = data.get(i);
      if (row.quantity == -1) {
        productionNodes.add(row);
        sources.add(row.id);
      } else if (row.quantity == -2) {
        warehouseNodes.add(row);
        sources.add(row.id);
      } else {
        demandNodes.add(row);
      }
    }

    List<Ns3Tbfvuwtge2Iq.Node> p_nodes = ns3helper.makeNodes(productionNodes);
    List<Ns3Tbfvuwtge2Iq.Node> w_nodes = ns3helper.makeNodes(warehouseNodes);
    List<Ns3Tbfvuwtge2Iq.Node> d_nodes = ns3helper.makeNodes(demandNodes);

    for (int i = 0; i < d_nodes.size(); i++) {
      // each demand node must have the quantity demand[i] delivered, so the range here
      // is actually [demand[i], demand[i]].
      // Not meeting this range incurs a large penalty cost.
      Ns3Tbfvuwtge2Iq.Node.ProductFlow.Builder pf = Ns3Tbfvuwtge2Iq.Node.ProductFlow.newBuilder();
      pf.setProductId("Beer").addDimensionRanges(ns3helper.make_dimension_range(
          "weight", demandNodes.get(i).quantity, demandNodes.get(i).quantity));

      // we're only going to allow the source Guiness Storehouse if it's within 50km's (as the crow
      // flies) from the location of the node.
      List<String> allowableSources = new ArrayList<String>();
      for (int w = 0; w < warehouseNodes.size(); w++) {
        allowableSources.add(warehouseNodes.get(w).id);
      }
      if (GreatCircleDistance(p_nodes.get(0), d_nodes.get(i)) < 50) {
        allowableSources.add(p_nodes.get(0).getId());
      }

      d_nodes.set(i,
          d_nodes.get(i)
              .toBuilder()
              .addConsumption(pf.build())
              .addAllAllowableSources(allowableSources)
              .build());
      // the nodes are built as compelete items here, so we can convert them, add to the node, and
      // save it back in the list.
    }

    for (int i = 0; i < p_nodes.size(); i++) {
      // the production node has no limit on the amount that can be produced.
      // so we can simply set the upper bound to the sum of all demand, i.e. [0, sum(demands)]
      // this way we know that the facility can produce enough to satisfy all the demand
      Ns3Tbfvuwtge2Iq.Node.ProductFlow.Builder pf = Ns3Tbfvuwtge2Iq.Node.ProductFlow.newBuilder();
      float totalQty = 0.0f;
      for (int j = 0; j < demandNodes.size(); j++) {
        totalQty += demandNodes.get(j).quantity;
      }
      totalQty *= 0.9f; // note: we're decreasing the total amount the production node can
                        // manufacture here, so there will be demand that goes unserviced
      pf.setProductId("Beer").addDimensionRanges(
          ns3helper.make_dimension_range("weight", 0, totalQty)
              .toBuilder()
              .setFlowPenalty(1e8f)
              .build());
      // note the penalty on over-servicing on the production node is now greater than the demand
      // nodes (i.e. it's a more strict soft-constraint)
      p_nodes.set(i, p_nodes.get(i).toBuilder().addProduction(pf.build()).build());
    }

    // this is where we add the fixed costs to the warehouse nodes.
    for (int i = 0; i < w_nodes.size(); i++) {
      // grab the node, convert it to a builder, add the extra fields,
      // then save if back to the list.
      w_nodes.set(i,
          w_nodes.get(i)
              .toBuilder()
              .setFlow(Ns3Tbfvuwtge2Iq.Node.Flow.newBuilder()
                           .addFixedDimensionCosts(Ns3Tbfvuwtge2Iq.FixedDimensionCost.newBuilder()
                                                       .addDimensionIds("weight")
                                                       .setFixedCost(10000f)
                                                       .build())
                           .build())
              .build());
    }

    for (int i = 0; i < p_nodes.size(); i++) {
      model.addNodes(p_nodes.get(i));
    }
    for (int i = 0; i < w_nodes.size(); i++) {
      model.addNodes(w_nodes.get(i));
    }
    for (int i = 0; i < d_nodes.size(); i++) {
      model.addNodes(d_nodes.get(i));
    }

    model.addLaneRates(ns3helper.make_lane_rate_distance(sources.get(0), sources.get(1), 0.1f));
    model.addLaneRates(ns3helper.make_lane_rate_distance(sources.get(0), sources.get(2), 0.1f));

    model.addProductGroups(ns3helper.make_simple_product_group("Beer"));

    for (int i = 0; i < sources.size(); i++) {
      model.addCostModels(ns3helper.make_cost_model_distance(sources.get(i), 0.2f));
    }

    sr.setModel(model);
    sr.setGeometryOutput(GeometryOutput.Aggregate);
    sr.setSolveType(SolveType.Optimise);

    String requestId = api.Post(sr.build()); // send the model to the api
    Ns3Tbfvuwtge2Iq.SolutionResponse solution =
        api.Get(requestId); // get the response (which is cast internally)

    ns3helper.printSolution(solution);
    // you'll notice now that both warehouses are being used with the changes made.
    // There are some consumption penalties incurred and the production amount is exactly at 90% of
    // the total demand (as requested)

    // for nice visualisations see the R/python notebook for the same example.
  }

  private apiHelper<Ns3Tbfvuwtge2Iq.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}