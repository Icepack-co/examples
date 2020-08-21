package icepackai;

import icepackai.IVR7.Ivr7Kt461V8Eoaif;
import icepackai.IVR7.Ivr7Kt461V8Eoaif.SolveRequest.SolveType;
import icepackai.IVRData.IvrdataO43E0Dvs78Zq;

import java.util.*;

// IVR7 Advanced example:
// Purpose: illustrate the usage of the data-upload as well as model versioning.
// * Builds a simple pickup/dropoff (similar to the basic model).
// * Configures "open routing" - i.e. do not cost the return legs for the vehicles to the
// end-location
// * Illustrates how to reference matrix data in a (potentially) versioned model.
// * or load overriding elements on a particular matrix directly.
//     (one can provide a complete matrix in this manner too)

public class ivr7_5_advanced2 {
  public ivr7_5_advanced2(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data = inputdata;
  }

  public void Run() throws Exception {
    api = new apiHelper<Ivr7Kt461V8Eoaif.SolutionResponse>(
        Ivr7Kt461V8Eoaif.SolutionResponse.class, "ivr7-kt461v8eoaif", configFile);
    Ivr7Kt461V8Eoaif.SolveRequest.Builder builder = Ivr7Kt461V8Eoaif.SolveRequest.newBuilder();
    // so here we're going to build the model
    Ivr7Kt461V8Eoaif.Model.Builder model =
        builder.getModel().toBuilder(); // this is the actual model container.
    // we're going to reuse the helpers described in the ivr7basic example. Please
    // see that for an initial reference.
    // We want "Open Routing" which basically means that if a vehicle finishes it's
    // day at any node, it is then not supposed to cost the return trip home. So as
    // a start, we need to separate the depot (Guiness Storehouse) from the
    // vehicle-home location by name - it's okay if it's at the same point
    // (long/lat), but we're going to separate the identifier so we can control the
    // distance/time matrix nicely.

    // we're going to reuse the helpers described in the ivr7basic example. Please
    // see that for a reference.
    ivr7helper.makeDistanceTimeCapDims(model);
    ivr7helper.makeLocations(model, data);

    // we're going to add an exta location; the "vehicle-site"
    // and use the geocode of the Guiness storehouse.
    model.addLocations(Ivr7Kt461V8Eoaif.Location.newBuilder()
                           .setId("vehicle-site")
                           .setGeocode(Ivr7Kt461V8Eoaif.Geocode.newBuilder()
                                           .setLongitude(data.get(0).X)
                                           .setLatitude(data.get(0).Y)
                                           .build()));

    ivr7helper.makeJobTimeCap(
        model, data, ivr7helper.Rep(0, data.size() - 1), ivr7helper.Seq(1, data.size()));
    model.addVehicleCostClasses(ivr7helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    model.addVehicleClasses(ivr7helper.makeVcSimple("vc1", 1, 1, 1, 1));
    for (int i = 0; i < 4; i++) {
      model.addVehicles(ivr7helper.makeVehicleCap("vehicle_" + i, // unique id for the vehicle.
          "vc1", // the vehicle class
          "vcc1", // the vehicle cost class
          2000, // the capacity of the vehicle
          "vehicle-site", // start location for the vehicle //NOTE the change here.
          "vehicle-site", // end location for the vehicle
          7 * 60, // start time: 7 AM
          18 * 60 // end time: 6 PM
          ));
    }
    // okay, so that's a basic model. Lets now use the objects, but submit them to
    // the api through a different mechanism.

    // so we've created vehicles which need to start/end at "vehicle-site". Now we
    // can make the last change which is to override the distance between locations
    // and the "vehicle-site". we're only going to modify the distances FROM
    // locations TO "vehicle-site". If you wanted to do complete line-haul
    // outsourcing-style modelling, you could also do this for "vehicle-site"
    // TO all alocations. For now, we'll just demonstrate the open routing case.

    // you have two ways of doing this. Upload it via the data-api, or upload it as
    // part of the model.
    Boolean dataUpload = true;
    // it's nice to illustrate this if you've enabled the services on your key
    // but you can set this to false to get a feel for the other code path if
    // needed.
    if (dataUpload) {
      IvrdataO43E0Dvs78Zq.TransitSet.Builder ts = IvrdataO43E0Dvs78Zq.TransitSet.newBuilder();
      for (int i = 0; i < model.getLocationsCount(); i++) {
        ts.addTransits(IvrdataO43E0Dvs78Zq.TransitSet.TransitValue.newBuilder()
                           .setFromId(model.getLocations(i).getId())
                           .setToId("vehilce-site")
                           .setValue(0.0f)
                           .build());
      }
      IvrdataO43E0Dvs78Zq.CachedTransitSet datamodel =
          IvrdataO43E0Dvs78Zq.CachedTransitSet.newBuilder().setTransitSet(ts).build();
      System.out.println(datamodel.toString());
      apiHelper<IvrdataO43E0Dvs78Zq.CachedTransitSet> data_api =
          new apiHelper<IvrdataO43E0Dvs78Zq.CachedTransitSet>(
              IvrdataO43E0Dvs78Zq.CachedTransitSet.class, "ivrdata-o43e0dvs78zq", configFile);

      // epic: we just saved our model as a byte stream into this data payload.
      String transitModelID = data_api.Post(datamodel);

      // now create the additional transit generators and link them to the data that
      // has been uploaded
      model.addTransitGenerators(Ivr7Kt461V8Eoaif.TransitGenerator.newBuilder()
                                     .setId("custom_distance")
                                     .setRequestId(transitModelID)
                                     .build());
      model.addTransitGenerators(Ivr7Kt461V8Eoaif.TransitGenerator.newBuilder()
                                     .setId("custom_time")
                                     .setRequestId(transitModelID)
                                     .build());
      // Note, we're telling the API where to find the Transit-set data.
    } else {
      // embed the zero elements in the matrix in the payload directly (rather than
      // through a data-upload)
      Ivr7Kt461V8Eoaif.TransitSet.Builder ts = Ivr7Kt461V8Eoaif.TransitSet.newBuilder();
      for (int i = 0; i < model.getLocationsCount(); i++) {
        ts.addTransits(Ivr7Kt461V8Eoaif.TransitSet.TransitValue.newBuilder()
                           .setFromId(model.getLocations(i).getId())
                           .setToId("vehilce-site")
                           .setValue(0.0f)
                           .build());
      }
      model.addTransitGenerators(Ivr7Kt461V8Eoaif.TransitGenerator.newBuilder()
                                     .setId("custom_distance")
                                     .setTransitSet(ts.build()));
      // or we're explicily providing all the data.
      model.addTransitGenerators(Ivr7Kt461V8Eoaif.TransitGenerator.newBuilder()
                                     .setId("custom_time")
                                     .setTransitSet(ts.build()));
    }

    // now the last step, we need to tell the vehicles that they should use these
    // transit generators. Note. we're appending the transit generators here, so
    // keeping the roadnetwork distance/time in the list (the order of the list IS
    // important when layering matricies). There are 4 attributes in the
    // vehicle-class list now.
    model.setVehicleClasses(0,
        model.getVehicleClasses(0)
            .toBuilder()
            .addAttributes(Ivr7Kt461V8Eoaif.VehicleClass.Attribute.newBuilder()
                               .setDimensionId("time")
                               .setTransitGeneratorId("custom_time")
                               .setTransitCoef(1.0f)
                               .setTaskCoef(1.0f)
                               .setLocationCoef(1.0f)
                               .build())
            .addAttributes(Ivr7Kt461V8Eoaif.VehicleClass.Attribute.newBuilder()
                               .setDimensionId("distance")
                               .setTransitGeneratorId("custom_distance")
                               .setTransitCoef(1.0f)
                               .setTaskCoef(1.0f)
                               .setLocationCoef(1.0f)
                               .build()));

    System.out.println(model.getVehicleClasses(0).toString());
    // so now we have a roadnetwork distance + time generator followed by a custom
    // time and custom distance generator. the api will execute the transit
    // generators in the order they appear and will override previous values with
    // new values (if they exist). So in this case, it will build a transit matrix
    // using the road network, then overlay the custom matrix which was uploaded
    // with our data.

    builder.setModel(model.build());
    builder.setSolveType(SolveType.Optimise);

    String requestId = api.Post(builder.build()); // send the model to the api
    Ivr7Kt461V8Eoaif.SolutionResponse solution =
        api.Get(requestId); // get the response (which is cast internally)
    System.out.println(String.format("Solution cost: %02f", solution.getObjective()));

    ivr7helper.printSolution(solution, true, true, true, true);
    // lets confirm that the distances and times between the last stop and the
    // vehicle-site are indeed zero.

    Map<Integer, String> stopToLocation = new HashMap<Integer, String>();

    for (Ivr7Kt461V8Eoaif.SolutionResponse.Route r : solution.getRoutesList()) {
      for (Ivr7Kt461V8Eoaif.SolutionResponse.Stop s : r.getStopsList()) {
        stopToLocation.put(s.getId(), s.getLocationId());
      }
      for (Ivr7Kt461V8Eoaif.SolutionResponse.InterStop e : r.getInterStopsList()) {
        if (stopToLocation.get(e.getFromStopId()) != "vehicle-site"
            && stopToLocation.get(e.getToStopId()) == "vehicle-site") {
          for (Ivr7Kt461V8Eoaif.SolutionResponse.InterStopAttribute a : e.getAttributesList()) {
            if (a.getDimId() == "time" || a.getDimId() == "distance") {
              if (a.getEndValue() - a.getStartValue() != 0) {
                throw new Exception("distance and/or time is supposed to be zero!");
              }
            }
          }
        }
      }
    }
    // for visualisations see the R/python notebook for plots on the same example.
    // You'll see in the visuals that the stops that are farthest from the depot are
    // typically selected to be the route (because it results in the largest
    // saving).
  }

  private apiHelper<Ivr7Kt461V8Eoaif.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}