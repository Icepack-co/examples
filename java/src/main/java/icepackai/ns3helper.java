package icepackai;


import java.util.*;

import icepackai.NS3.Ns3Tbfvuwtge2Iq.InternalDimension.eMeasurementUnit;
import icepackai.NS3.*;

import dnl.utils.text.table.TextTable;


// some helper functions for building ivr7 models.
public class ns3helper {
  public static Ns3Tbfvuwtge2Iq.DimensionConfiguration makeDistanceTimeUserDims(String userDim) {
    
    Ns3Tbfvuwtge2Iq.DimensionConfiguration.Builder dimBuilder = Ns3Tbfvuwtge2Iq.DimensionConfiguration.newBuilder();
    dimBuilder.setDistanceConfig(
      Ns3Tbfvuwtge2Iq.InternalDimension.newBuilder().setId("distance").setMeasurementUnit(eMeasurementUnit.KILOMETRES).build()
    );
    dimBuilder.setTimeConfig(
      Ns3Tbfvuwtge2Iq.InternalDimension.newBuilder().setId("time").setMeasurementUnit(eMeasurementUnit.HOURS).build()
    );
    if(userDim != ""){
      dimBuilder.addUserDimensions(
        Ns3Tbfvuwtge2Iq.UserDimension.newBuilder().setId(userDim).setUnits("unknown").build()
      );
    }
    return dimBuilder.build();
  }

  public static Ns3Tbfvuwtge2Iq.Node makeNode(String id, float longitude, float latitude){
    Ns3Tbfvuwtge2Iq.Node.Builder n = Ns3Tbfvuwtge2Iq.Node.newBuilder();
    n.setId(id).setGeocode(Ns3Tbfvuwtge2Iq.Geocode.newBuilder().setLongitude(longitude).setLatitude(latitude).build());
    return n.build();
  }

  public static List<Ns3Tbfvuwtge2Iq.Node> makeNodes(List<dataRow> rows){
    List<Ns3Tbfvuwtge2Iq.Node> nodes = new ArrayList<Ns3Tbfvuwtge2Iq.Node>();
    for (int i = 0; i < rows.size(); i++) {
      dataRow loc = rows.get(i);
      nodes.add(makeNode(loc.id, loc.X, loc.Y));
    }
    return nodes;
  }

}