package icepackai;

import icepackai.CVRPTW.CvrptwAcyas3Nzweqb;
import icepackai.CVRPTW.CvrptwAcyas3Nzweqb.SolveRequest.SolveType;
import icepackai.CVRPTW.CvrptwAcyas3Nzweqb.CVRPTW.eDistanceType;

import java.util.*;
import java.util.stream.Collectors;
import dnl.utils.text.table.TextTable;

// A simple example of how to build and run a simple CVRPTW model.
// A classic cvrptw has a heterogeneous fleet. This means we need only specify the size of the
// vehicle and the number of vehicles available. The other aspect of this model is to include
// the location of the depot. The cvrptw is costed differently to the cvrp. The objective is still
// to minimise the number of vehicles used, but also to minimise the total time. The classic cvrp
// aims to minimise the number of vehicles, then the total distance travelled.
// The cvrptw has time windows on each point. In this schema, we allow you to omit windows from
// points if needed.

public class cvrptw1basic {
  public cvrptw1basic(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data = inputdata.stream().limit(10).collect(
        Collectors.toList()); // grabs just the first 10 items.;
  }

  public void Run() throws Exception {
    api = new apiHelper<CvrptwAcyas3Nzweqb.SolutionResponse>(
        CvrptwAcyas3Nzweqb.SolutionResponse.class, "cvrptw-acyas3nzweqb", configFile);
    CvrptwAcyas3Nzweqb.SolveRequest.Builder builder = CvrptwAcyas3Nzweqb.SolveRequest.newBuilder();
    // so here we're going to build the model
    CvrptwAcyas3Nzweqb.CVRPTW.Builder model =
        builder.getModel().toBuilder(); // this is the actual model container.

    // add locations to the matrix request
    for (int i = 0; i < data.size(); i++) {
      dataRow row = data.get(i);
      if (i == 0) { // treat the first point as the depot.
        model.setDepot(CvrptwAcyas3Nzweqb.Geocode.newBuilder()
                           .setId(row.id)
                           .setX(row.X)
                           .setY(row.Y)
                           .setQuantity(0.0f)
                           .build());
      } else {
        // we randomly allocate morning and afternoon time windows here.
        if (i % 2 == 0) {
          model.addPoints(CvrptwAcyas3Nzweqb.Geocode.newBuilder()
                              .setId(row.id)
                              .setX(row.X)
                              .setY(row.Y)
                              .setWindowStart(8 * 60) // Morning window: 08:00 -> 12:00
                              .setWindowEnd(12 * 60)
                              .setQuantity(20.0f)
                              .build());
        } else {
          model.addPoints(CvrptwAcyas3Nzweqb.Geocode.newBuilder()
                              .setId(row.id)
                              .setX(row.X)
                              .setY(row.Y)
                              .setWindowStart(12 * 60) // Afternoon window: 12:00 -> 16:00
                              .setWindowEnd(16 * 60)
                              .setQuantity(20.0f)
                              .build());
          // add the points as demand points. Assume that each point has a demand quantity
          // of 20
        }
      }
    }
    // configure the distance metric (although road network is the default)
    model.setDistancetype(eDistanceType.RoadNetwork);
    model.setVehicleCapacity(100); // set a vehicle capacity of 100
    model.setNumberOfVehicles(3); // allow the use of at-most, three vehicles.
    model.build();

    // create a solve request
    builder.setModel(model); // link up the model container
    builder.setSolveType(SolveType.Optimise); // just setting the default here (not needed but nice
                                              // to know what the field is)

    String requestId = api.Post(builder.build()); // send the model to the api

    CvrptwAcyas3Nzweqb.SolutionResponse solution =
        api.Get(requestId); // get the response (which is cast internally)

    // total stops
    Integer totalStops = 0;
    for (int i = 0; i < solution.getRoutesCount(); i++) {
      totalStops += solution.getRoutes(i).getSequenceCount();
    }

    Object[][] tabData = new Object[totalStops][];

    String[] columnNames = {"Vehicle", "Stop", "Distance Travelled", "Cumulative Distance",
        "Cumulative load", "Arrival Time"};

    totalStops = 0;
    for (int i = 0; i < solution.getRoutesCount(); i++) {
      CvrptwAcyas3Nzweqb.SolutionResponse.Route r = solution.getRoutes(i);

      float totalDistance = 0.0f;
      if (r.getVisitCapacitiesCount() > 0) {
        float cumulCap = r.getVisitCapacities(0);
        tabData[totalStops] = new Object[] {
            "Route_" + i, r.getSequence(0), 0.0f, 0.0f, cumulCap, r.getArrivalTimes(0)};
        totalStops++;
        for (int j = 1; j < r.getSequenceCount(); j++) {
          CvrptwAcyas3Nzweqb.Edge e = r.getEdges(j - 1);
          cumulCap += r.getVisitCapacities(j);
          totalDistance += e.getDistance();
          tabData[totalStops] = new Object[] {"Route_" + i, r.getSequence(j), e.getDistance(),
              totalDistance, cumulCap, r.getArrivalTimes(j)};
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

  private apiHelper<CvrptwAcyas3Nzweqb.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}