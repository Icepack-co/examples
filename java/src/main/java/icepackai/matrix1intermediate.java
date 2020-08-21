package icepackai;

import icepackai.Matrix.MatrixVyv95N7Wchpl;
import icepackai.Matrix.MatrixVyv95N7Wchpl.MatrixRequest.eDistanceUnit;
import icepackai.Matrix.MatrixVyv95N7Wchpl.MatrixRequest.eDurationUnit;

import java.util.*;
import java.util.stream.Collectors;

import dnl.utils.text.table.TextTable;

// An intermediate example of how to use the matrix api to generate distance/time matricies
// creates a partial matrix with two sources and four destinations. (i.e. 8 elements = 2*4)
public class matrix1intermediate {
  public matrix1intermediate(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data =
        inputdata.stream().limit(6).collect(Collectors.toList()); // grabs just the first 6 items.
  }

  public void Run() throws Exception {
    api = new apiHelper<MatrixVyv95N7Wchpl.MatrixResponse>(
        MatrixVyv95N7Wchpl.MatrixResponse.class, "matrix-vyv95n7wchpl", configFile);
    // so here we're going to build the model

    // create a solve request
    MatrixVyv95N7Wchpl.MatrixRequest.Builder builder =
        MatrixVyv95N7Wchpl.MatrixRequest.newBuilder();

    // add locations to the matrix request
    for (int i = 0; i < data.size(); i++) {
      dataRow row = data.get(i);
      builder.addLocations(MatrixVyv95N7Wchpl.Location.newBuilder()
                               .setId(row.id)
                               .setGeocode(MatrixVyv95N7Wchpl.Geocode.newBuilder()
                                               .setLatitude(row.Y)
                                               .setLongitude(row.X)
                                               .build())
                               .build());
    }

    // in this example we add the first two locations as sources and the balance as destinations
    for (int i = 0; i < data.size(); i++) {
      if (i < 2) {
        builder.addSources(data.get(i).id);
      } else {
        builder.addDestinations(data.get(i).id);
      }
    }

    // configure the distance metric (although road network is the default)
    builder.setDurationUnit(eDurationUnit.MINUTES);
    builder.setDistanceUnit(eDistanceUnit.KILOMETRES);

    String requestId = api.Post(builder.build()); // send the model to the api

    MatrixVyv95N7Wchpl.MatrixResponse solution =
        api.Get(requestId); // get the response (which is cast internally)
    System.out.println("Matrix elements returned: " + solution.getElementsCount());
    // We'll write this one out in long form.

    String[] columnNames = {"FromId", "ToId", "Distance", "Duration"};
    Object[][] tabData = new Object[solution.getElementsCount()][];
    for (int i = 0; i < solution.getElementsCount(); i++) {
      MatrixVyv95N7Wchpl.MatrixResponse.Element e = solution.getElements(i);
      tabData[i] = new Object[] {e.getFromId(), e.getToId(), e.getDistance(), e.getDuration()};
    }

    TextTable tt = new TextTable(columnNames, tabData);
    tt.printTable();
  }

  private apiHelper<MatrixVyv95N7Wchpl.MatrixResponse> api;

  private String configFile;
  private List<dataRow> data;
}