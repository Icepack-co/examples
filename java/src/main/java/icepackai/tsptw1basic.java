package icepackai;

import java.util.*;

import icepackai.TSPTW.TsptwKcxbievqo879;
import icepackai.TSPTW.TsptwKcxbievqo879.SolveRequest.SolveType;
import icepackai.TSPTW.TsptwKcxbievqo879.TSP.eDistanceType;

import java.util.Random;

import dnl.utils.text.table.TextTable;

// A simple example of how to build and run a simple TSP model.
public class tsptw1basic {
  public tsptw1basic(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data = inputdata;
  }

  public void Run() throws Exception {
    api = new apiHelper<TsptwKcxbievqo879.SolutionResponse>(
        TsptwKcxbievqo879.SolutionResponse.class, "tsptw-kcxbievqo879", configFile);
    TsptwKcxbievqo879.SolveRequest.Builder builder = TsptwKcxbievqo879.SolveRequest.newBuilder();
    // so here we're going to build the model
    TsptwKcxbievqo879.TSP.Builder model =
        builder.getModel().toBuilder(); // this is the actual model container.

    Random rand = new Random();
    // add locations to the matrix request
    for (int i = 0; i < data.size(); i++) {
      dataRow row = data.get(i);
      // lets randomly create a window here.
      double rupper = 2500.0;
      double ws = rand.nextDouble() * rupper;
      double we = ws + rupper; // we don't accept backwards windows, so we'll just set these to some
                               // positive width upper amount.
      model.addPoints(TsptwKcxbievqo879.Geocode.newBuilder()
                          .setId(row.id)
                          .setX(row.X)
                          .setY(row.Y)
                          .setWindowStart((float) ws)
                          .setWindowEnd((float) we)
                          .build());
    }
    // configure the distance metric (although road network is the default)
    model.setDistancetype(eDistanceType.RoadNetwork);
    model.build();

    // create a solve request
    builder.setModel(model); // link up the model container
    builder.setSolveType(SolveType.Optimise); // just setting the default here (not needed but nice
                                              // to know what the field is)

    String requestId = api.Post(builder.build()); // send the model to the api

    TsptwKcxbievqo879.SolutionResponse solution =
        api.Get(requestId); // get the response (which is cast internally)
    System.out.println("Number of tour items: " + solution.getTourCount());
    float totalDistance = 0.0f;
    Object[][] tabData = new Object[solution.getTourCount()][];

    String[] columnNames = {"Stop", "Distance Travelled", "Cumulative Distance", "Arrival Time"};

    tabData[0] = new Object[] {solution.getTour(0), 0.0f, 0.0f, 0.0f};
    for (int i = 1; i < solution.getTourCount(); i++) {
      TsptwKcxbievqo879.Edge e =
          solution.getEdges(i - 1); // there is one less edge than the number of stops in a tour.
      totalDistance += e.getDistance();
      tabData[i] = new Object[] {
          solution.getTour(i), e.getDistance(), totalDistance, solution.getArrivalTimes(i)};
      for (int j = 0; j < e.getGeometryCount(); j++) {
        // so each one of these items forms part of the road-network used.
        // so the list of points (x,y) can be interpreted as the sequence through
        // the network and forms a line-string. For proper visualisations of this
        // response
        // see the R/Python examples which have leaflet and ipyleaflet plots
        // respectively.
      }
    }

    TextTable tt = new TextTable(columnNames, tabData);
    tt.printTable();
    System.out.printf("Total distance: %02f\n", totalDistance);
  }

  private apiHelper<TsptwKcxbievqo879.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}