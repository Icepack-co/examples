package icepackai;

import icepackai.Matrix.MatrixVyv95N7Wchpl;
import icepackai.Matrix.MatrixVyv95N7Wchpl.MatrixRequest.eDistanceUnit;
import icepackai.Matrix.MatrixVyv95N7Wchpl.MatrixRequest.eDurationUnit;

import java.util.*;
import java.util.stream.Collectors;

// A simple example of how to build a complete matrix [n by n] using the api.
public class matrix1basic {
  public matrix1basic(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data =
        inputdata.stream().limit(5).collect(Collectors.toList()); // grabs just the first 5 items.
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
      builder.addSources(row.id); // this will ensure a complete matrix. because destinations are
                                  // empty, we assume you're asking for sources:sources
    }

    // configure the distance metric (although road network is the default)
    builder.setDurationUnit(eDurationUnit.MINUTES);
    builder.setDistanceUnit(eDistanceUnit.KILOMETRES);

    String requestId = api.Post(builder.build()); // send the model to the api

    MatrixVyv95N7Wchpl.MatrixResponse solution =
        api.Get(requestId); // get the response (which is cast internally)
    System.out.println("Matrix elements returned: " + solution.getElementsCount());
    HashMap<String, Integer> idToIndex = new HashMap<String, Integer>();
    for (int i = 0; i < data.size(); i++) {
      idToIndex.put(data.get(i).id, i);
    }
    Float[][] DM = new Float[data.size()][];
    Float[][] TM = new Float[data.size()][];
    for (int i = 0; i < data.size(); i++) {
      DM[i] = new Float[data.size()]; // make a square matrix
      TM[i] = new Float[data.size()]; // make a square matrix
      DM[i][i] = 0f; // set the diagonal to zero
      TM[i][i] = 0f;
    }
    for (int i = 0; i < solution.getElementsCount(); i++) {
      icepackai.Matrix.MatrixVyv95N7Wchpl.MatrixResponse.Element e = solution.getElements(i);
      DM[idToIndex.get(e.getFromId())][idToIndex.get(e.getToId())] = e.getDistance();
      TM[idToIndex.get(e.getFromId())][idToIndex.get(e.getToId())] = e.getDuration();
    }
    printMatrix(DM, "Distance matrix");
    printMatrix(TM, "Distance matrix");
  }

  private void printMatrix(Float[][] matrix, String name) {
    System.out.println(name + ":");
    int rows = matrix.length;
    for (int i = 0; i < rows; i++) {
      StringBuilder sb = new StringBuilder();
      int columns = matrix[i].length;
      for (int j = 0; j < columns; j++) {
        sb.append(String.format("|%02f", matrix[i][j]));
      }
      sb.append("|");
      System.out.println(sb.toString());
    }
    System.out.println("");
  }

  private apiHelper<MatrixVyv95N7Wchpl.MatrixResponse> api;

  private String configFile;
  private List<dataRow> data;
}