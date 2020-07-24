package icepackai;

import icepackai.IVR7.Ivr7Kt461V8Eoaif;
import icepackai.IVR7.Ivr7Kt461V8Eoaif.SolveRequest.SolveType;

import java.util.*;

// IVR7 Intermediate example 1:
// Purpose: illustrate the use of modelling concepts
// Illustrates using time, distance and a single capacity dimension
// Location-windows (08:00 -> 14:00)
// Pickup-dropoff tasks (with task-times)
// One vehicle class (same travel profile)
// Two vehicle-cost classes
// Multiple vehicles (2xc1, 2xc2)
// Heterogeneous fleet (2 ton and 3 ton capacity)
// Lunch breaks (1 hour break around 12:00)
public class ivr7_2_intermediate {
  public ivr7_2_intermediate(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data = inputdata;
  }

  public void Run() throws Exception {
    api = new apiHelper<Ivr7Kt461V8Eoaif.SolutionResponse>(Ivr7Kt461V8Eoaif.SolutionResponse.class, "ivr7-kt461v8eoaif",
        configFile);
    Ivr7Kt461V8Eoaif.SolveRequest.Builder builder = Ivr7Kt461V8Eoaif.SolveRequest.newBuilder();
    // so here we're going to build the model
    Ivr7Kt461V8Eoaif.Model.Builder model = builder.getModel().toBuilder(); // this is the actual model container.

    // we're going to reuse the helpers described in the ivr7basic example. Please
    // see that for a reference.
    ivr7helper.makeDistanceTimeCapDims(model);

    // we're going to add time windows to the locations. 08:00 - 14:00. In java it's
    // easiest to do this as you compile the object
    ivr7helper.makeLocations(model, data, 8 * 60f, 14 * 60f);
    System.out.println(model.getLocations(0).toString()); // not that we now have an arrival attribute which has been
                                                          // populated

    ivr7helper.makeJobTimeCap(model, data, ivr7helper.Rep(0, data.size() - 1), ivr7helper.Seq(1, data.size()));

    model.addVehicleCostClasses(ivr7helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    model.addVehicleCostClasses(ivr7helper.makeVccSimple("vcc2", 1200, 0.1f, 0.1f, 0.1f, 0.6f, 2.5f));
    model.addVehicleClasses(ivr7helper.makeVcSimple("vc1", 1, 1, 1, 1));

    // now we can just specify the vehicles.
    // lets provide 2 x 2 ton vehicles and 2 x 3 ton vehicles. Although this is
    // probably more than we need. the reason for this is that we're modelling a
    // full-blown pickup+dropoff model, so if there's time to reload, a vehicle can
    // return to the depot and grab more goodies!
    for (int i = 0; i < 4; i++) {
      String vcc = "vcc1";
      float cap = 2000f;
      if (i > 1) {
        vcc = "vcc2";
        cap = 3000f;
      }
      model.addVehicles(ivr7helper.makeVehicleCap("vehicle_" + i, // unique id for the vehicle.
          "vc1", // the vehicle class
          vcc, // the vehicle cost class
          cap, // the capacity of the vehicle
          data.get(0).id, // start location for the vehicle
          data.get(0).id, // end location for the vehicle
          7 * 60, // start time: 7 AM
          18 * 60 // end time: 6 PM
      ));
    }

    // Lunch breaks.
    // so this is a touch more complex, we want to link our transit-rule to the time
    // dimension, and when a certain amount has accumulated on the dimension, we
    // trigger the rule.
    model.addTransitRules(ivr7helper.makeLunchBreakRule("lunch_break_rule", "lunchy_munchy_", 12 * 60.0f, 60.0f));

    // now link the transit rule to the vehicle classes
    model.setVehicleClasses(0, model.getVehicleClasses(0).toBuilder().addTransitRuleIds("lunch_break_rule"));
    System.out.println(model.getVehicleClasses(0).toString());

    builder.setModel(model.build());
    builder.setSolveType(SolveType.Optimise);

    String requestId = api.Post(builder.build()); // send the model to the api
    Ivr7Kt461V8Eoaif.SolutionResponse solution = api.Get(requestId); // get the response (which is cast internally)
    System.out.println(String.format("Solution cost: %02f", solution.getObjective()));

    ivr7helper.printSolution(solution, true, true, true, true);
    // the maximum quantity assigned to each vehicle is <= 2000 | <= 3000 (the
    // capacity dimension). the majority of the cost is coming in the distance
    // dimension (because of the way we've configured the vehicle cost class)
    // for visualisations see the R/python notebook for plots on the same example.
  }

  private apiHelper<Ivr7Kt461V8Eoaif.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}