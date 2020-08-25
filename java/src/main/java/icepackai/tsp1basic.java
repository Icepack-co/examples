package icepackai;

import icepackai.TSP.TspMcvfz472Gty6;
import icepackai.TSP.TspMcvfz472Gty6.SolveRequest.SolveType;
import icepackai.TSP.TspMcvfz472Gty6.TSP.eDistanceType;

import java.util.*;
import dnl.utils.text.table.TextTable;

// A simple example of how to build and run a simple TSP model.
public class tsp1basic {
  public tsp1basic(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data = inputdata;
  }

  public void Run() throws Exception {
    api = new apiHelper<TspMcvfz472Gty6.SolutionResponse>(
        TspMcvfz472Gty6.SolutionResponse.class, "tsp-mcvfz472gty6", configFile);
    TspMcvfz472Gty6.SolveRequest.Builder builder = TspMcvfz472Gty6.SolveRequest.newBuilder();
    // so here we're going to build the model
    TspMcvfz472Gty6.TSP.Builder model =
        builder.getModel().toBuilder(); // this is the actual model container.

    // add locations to the matrix request
    for (int i = 0; i < data.size(); i++) {
      dataRow row = data.get(i);
      model.addPoints(
          TspMcvfz472Gty6.Geocode.newBuilder().setId(row.id).setX(row.X).setY(row.Y).build());
    }
    // configure the distance metric (although road network is the default)
    model.setDistancetype(eDistanceType.RoadNetwork);
    model.build();

    // create a solve request
    builder.setModel(model); // link up the model container
    builder.setSolveType(SolveType.Optimise); // just setting the default here (not needed but nice
                                              // to know what the field is)

    String requestId = api.Post(builder.build()); // send the model to the api

    TspMcvfz472Gty6.SolutionResponse solution =
        api.Get(requestId); // get the response (which is cast internally)
    System.out.println("Number of tour items: " + solution.getTourCount());
    float totalDistance = 0.0f;
    Object[][] tabData = new Object[solution.getTourCount()][];

    String[] columnNames = {"Stop", "Distance Travelled", "Cumulative Distance"};

    tabData[0] = new Object[] {solution.getTour(0), 0.0f, 0.0f};
    for (int i = 1; i < solution.getTourCount(); i++) {
      TspMcvfz472Gty6.Edge e =
          solution.getEdges(i - 1); // there is one less edge than the number of stops in a tour.
      totalDistance += e.getDistance();
      tabData[i] = new Object[] {solution.getTour(i), e.getDistance(), totalDistance};
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

  private apiHelper<TspMcvfz472Gty6.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}