package icepackai;

import icepackai.IVR7.Ivr7Kt461V8Eoaif;
import icepackai.IVR7.Ivr7Kt461V8Eoaif.SolveRequest.SolveType;

import java.util.*;


// IVR7 basic example:
// Illustrates using time, distance and a single capacity dimension
// Locations
// Pickup-dropoff tasks (with task times)
// single vehicle class
// single vehicle-cost class
// multiple vehicles.
public class ivr7_1_basic {
  public ivr7_1_basic(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data = inputdata;
  }

  public void Run() throws Exception {
    api = new apiHelper<Ivr7Kt461V8Eoaif.SolutionResponse>(Ivr7Kt461V8Eoaif.SolutionResponse.class, "ivr7-kt461v8eoaif",
        configFile);
    Ivr7Kt461V8Eoaif.SolveRequest.Builder builder = Ivr7Kt461V8Eoaif.SolveRequest.newBuilder();
    // so here we're going to build the model
    Ivr7Kt461V8Eoaif.Model.Builder model = builder.getModel().toBuilder(); // this is the actual model container.

    // the first decision we have to make is which dimensional quantities to model
    // in this example. we're going to model the distance, time, and capacity of the
    // vehicle.
    ivr7helper.makeDistanceTimeCapDims(model);// adds distance, time & capacity

    // lets pretend the first point is where vehicles are going to begin and end
    // each day. unlike the tsp/cvrp/pdp models, the ivr7 requires that you specify
    // the unique locations that are going to be used in the model as a separate
    // entity. The reason for this is that you can then specify the locations once,
    // and reference those locations by id for other entities (such and
    // vehicles/jobs/tasks)
    ivr7helper.makeLocations(model, data); // adds all the locations to the model

    // so we've constructed some jobs with pickups and dropoffs, loading and offload
    // times, as well as the contribution to the capacity dimension. In this
    // example, we're pickup up all orders at the guiness storehouse and delivering
    // at the list of customers. 'make_job_time_cap' is just a simple function to
    // create this particular style of request, but you can make your own.
    ivr7helper.makeJobTimeCap(model, data, ivr7helper.Rep(0, data.size() - 1), ivr7helper.Seq(1, data.size()));

    // we're going to do the vehicle-configuration now.
    // we need to specify the cost classes available, the vehicle classes available,
    // and then the individual vehicles. we're going to create one of each to keep
    // things simple.
    model.addVehicleCostClasses(ivr7helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));

    // lets make the vehicle class. A vehicle class describes how the vehicle MOVES
    // through the network. so in other words, we can use the standard network
    // travel speeds, or we could make the vehicle move slower/faster relative to
    // the road network. We could also attach transit rules here which are great for
    // modelling lunch breaks, refueling stops etc. (i.e. conditional triggers on
    // the cumul values of the dimension). Covered in an advanced section.
    model.addVehicleClasses(ivr7helper.makeVcSimple("vc1", 1, 1, 1, 1));

    // now we can just specify the vehicles.
    // lets provide 5 x 2 ton vehicles. Although this is probably more than we need.
    // the reason for this is that we're modelling a full-blown pickup+dropoff
    // model, so if there's time to reload, a vehicle can return to the depot and
    // grab more goodies!
    for (int i = 0; i < 5; i++) {
      model.addVehicles(ivr7helper.makeVehicleCap("vehicle_" + i, // unique id for the vehicle.
          "vc1", // the vehicle class
          "vcc1", // the vehicle cost class
          2000, // the capacity of the vehicle
          data.get(0).id, // start location for the vehicle
          data.get(0).id, // end location for the vehicle
          7 * 60, // start time: 7 AM
          18 * 60 // end time: 6 PM
      ));
    }

    System.out.println(model.build().toString());
    builder.setModel(model.build());
    builder.setSolveType(SolveType.Optimise); // Optimise the solve request.

    String requestId = api.Post(builder.build()); // send the model to the api
    Ivr7Kt461V8Eoaif.SolutionResponse solution = api.Get(requestId); // get the response (which is cast internally)
    System.out.println(String.format("Solution cost: %02f", solution.getObjective()));

    ivr7helper.printSolution(solution, true, true, true, true);
    // the maximum quantity assigned to each vehicle is <= 2000 (the capacity
    // dimension). the majority of the cost is coming in the distance dimension
    // (because of the way we've configured the vehicle cost class)
    // for visualisations see the R/python notebook for plots on the same example.
  }

  private apiHelper<Ivr7Kt461V8Eoaif.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}