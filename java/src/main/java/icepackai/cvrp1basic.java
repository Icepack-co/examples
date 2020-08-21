package icepackai;

import icepackai.CVRP.CvrpJkfdoctmp51N;
import icepackai.CVRP.CvrpJkfdoctmp51N.SolveRequest.SolveType;
import icepackai.CVRP.CvrpJkfdoctmp51N.CVRP.eDistanceType;

import java.util.*;
import java.util.stream.Collectors;
import dnl.utils.text.table.TextTable;

// A simple example of how to build and run a simple CVRP model
// A classic cvrp has a heterogeneous fleet. This means we need only specify the size of the
// vehicle and the number of vehicles available. The other aspect of this model is to include
// the location of the depot.

public class cvrp1basic {
  public cvrp1basic(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data = inputdata.stream().limit(10).collect(
        Collectors.toList()); // grabs just the first 10 items.;
  }

  public void Run() throws Exception {
    api = new apiHelper<CvrpJkfdoctmp51N.SolutionResponse>(
        CvrpJkfdoctmp51N.SolutionResponse.class, "cvrp-jkfdoctmp51n", configFile);
    CvrpJkfdoctmp51N.SolveRequest.Builder builder = CvrpJkfdoctmp51N.SolveRequest.newBuilder();
    // so here we're going to build the model
    CvrpJkfdoctmp51N.CVRP.Builder model =
        builder.getModel().toBuilder(); // this is the actual model container.

    // add locations to the matrix request
    for (int i = 0; i < data.size(); i++) {
      dataRow row = data.get(i);
      if (i == 0) { // treat the first point as the depot.
        model.setDepot(CvrpJkfdoctmp51N.Geocode.newBuilder()
                           .setId(row.id)
                           .setX(row.X)
                           .setY(row.Y)
                           .setQuantity(0.0f)
                           .build());
      } else {
        model.addPoints(CvrpJkfdoctmp51N.Geocode.newBuilder()
                            .setId(row.id)
                            .setX(row.X)
                            .setY(row.Y)
                            .setQuantity(20.0f)
                            .build());
        // add the points as demand points. Assume that each point has a demand quantity
        // of 20
      }
    }
    // configure the distance metric (although road network is the default)
    model.setDistancetype(eDistanceType.RoadNetwork);
    model.setVehicleCapacity(100); // set a vehicle capacity of 100
    model.setNumberOfVehicles(2); // allow the use of at-most, two vehicles.
    model.build();

    // create a solve request
    builder.setModel(model); // link up the model container
    builder.setSolveType(SolveType.Optimise); // just setting the default here (not needed but nice
                                              // to know what the field is)

    String requestId = api.Post(builder.build()); // send the model to the api

    CvrpJkfdoctmp51N.SolutionResponse solution =
        api.Get(requestId); // get the response (which is cast internally)

    // total stops
    Integer totalStops = 0;
    for (int i = 0; i < solution.getRoutesCount(); i++) {
      totalStops += solution.getRoutes(i).getSequenceCount();
    }

    Object[][] tabData = new Object[totalStops][];

    String[] columnNames = {
        "Vehicle", "Stop", "Distance Travelled", "Cumulative Distance", "Cumulative load"};

    totalStops = 0;
    for (int i = 0; i < solution.getRoutesCount(); i++) {
      CvrpJkfdoctmp51N.SolutionResponse.Route r = solution.getRoutes(i);
      if (r.getVisitCapacitiesCount() > 0) {
        float totalDistance = 0.0f;
        float cumulCap = r.getVisitCapacities(0);
        tabData[totalStops] = new Object[] {"Route_" + i, r.getSequence(0), 0.0f, 0.0f, cumulCap};
        totalStops++;
        for (int j = 1; j < r.getSequenceCount(); j++) {
          CvrpJkfdoctmp51N.Edge e = r.getEdges(j - 1);
          cumulCap += r.getVisitCapacities(j);
          totalDistance += e.getDistance();
          tabData[totalStops] = new Object[] {
              "Route_" + i, r.getSequence(j), e.getDistance(), totalDistance, cumulCap};
          totalStops++;
          for (int k = 0; k < e.getGeometryCount(); k++) {
            // so each one of these items forms part of the road-network used for a
            // particular route.
            // so the list of points (x,y) can be interpreted as the sequence through
            // the network and forms a line-string. For proper visualisations of this
            // response
            // see the R/Python examples which have leaflet and ipyleaflet plots
            // respectively.
          }
        }
      }
    }

    TextTable tt = new TextTable(columnNames, tabData);
    tt.printTable();
    // the cumulative quantity assigned to each route is <= 100.
  }

  private apiHelper<CvrpJkfdoctmp51N.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}