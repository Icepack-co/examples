package icepackai;

import icepackai.IVR8.Ivr8Yni1C9K2Swof;
import icepackai.IVR8.Ivr8Yni1C9K2Swof.SolveRequest.SolveType;

import java.util.*;
import java.util.stream.Collectors;

// IVR8 Basic Example:
// Purpose: demonstrate how to use compartment constraints on a particular model.
// * Use a subset of the publist stops and configure a single vehicle
// * Use a simple two-rack compartment configuration to illustrate the workings.
public class ivr8_1_basic {
  public ivr8_1_basic(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data =
        inputdata.stream().limit(9).collect(Collectors.toList()); // grabs just the first 9 items.;
  }

  public void Run() throws Exception {
    api = new apiHelper<Ivr8Yni1C9K2Swof.SolutionResponse>(
        Ivr8Yni1C9K2Swof.SolutionResponse.class, "ivr8-yni1c9k2swof", configFile);
    Ivr8Yni1C9K2Swof.SolveRequest.Builder builder = Ivr8Yni1C9K2Swof.SolveRequest.newBuilder();
    // so here we're going to build the model
    Ivr8Yni1C9K2Swof.Model.Builder model =
        builder.getModel().toBuilder(); // this is the actual model container.

    // see ivr7 basic examples for notes around each of these methods.
    // the ivr7/8 models are interchangeable, except that the IVR8 model supports
    // compartment modelling.
    ivr8helper.makeDistanceTimeCapDims(model); // adds distance, time & capacity
    ivr8helper.makeLocations(model, data); // adds all the locations to the model
    ivr8helper.makeJobTimeCap(
        model, data, ivr8helper.Rep(0, data.size() - 1), ivr8helper.Seq(1, data.size()));
    model.addVehicleCostClasses(ivr8helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    model.addVehicleClasses(ivr8helper.makeVcSimple("vc1", 1, 1, 1, 1));
    model.addVehicles(ivr8helper.makeVehicleCap("vehicle_0", // unique id for the vehicle.
        "vc1", // the vehicle class
        "vcc1", // the vehicle cost class
        2000, // the capacity of the vehicle
        data.get(0).id, // start location for the vehicle
        data.get(0).id, // end location for the vehicle
        7 * 60, // start time: 7 AM
        18 * 60 // end time: 6 PM
        ));

    // lets pretend for a moment that we have a vehicle which is layed out as
    // follows:
    // Top Rack...[ ] [ ] [ ] [ ] 100kg per "compartment" c1, c2, c3, c4
    // Lower Rack.[ ] [ ] [ ] [ ] 400kg per "compartment" c5, c6, c7, c8
    // 100*4 + 400*4 // adds up to the 2 ton total limit on a vehicle (if every
    // compartment could be filled to max)

    for (int i = 0; i < 8; i++) {
      model.addCompartments(Ivr8Yni1C9K2Swof.Compartment.newBuilder()
                                .setId("c" + (i + 1))
                                .addCapacities(Ivr8Yni1C9K2Swof.Compartment.Capacity.newBuilder()
                                                   .setDimensionId("capacity")
                                                   .setCapacity(i < 4 ? 100f : 400)
                                                   .build()) // switch between top and bottom rack
                                .build());
    }
    {
      // now we can define a compartment set (a container for the individual
      // compartments) which is attached to a vehicle (or a vehicle class if you
      // prefer).
      Ivr8Yni1C9K2Swof.CompartmentSet.Builder cset =
          Ivr8Yni1C9K2Swof.CompartmentSet.newBuilder().setId("double-decker");
      for (int i = 0; i < 8; i++) {
        // add all the defined compartments to the compartment set
        cset.addCompartmentIds("c" + (i + 1)).build();
      }
      model.addCompartmentSets(cset.build());
      // then we assign the "double-decker" compartment set to the vehicle class.
      // we could have added it to each vehicle if we wanted, this is simply easier.
      model.setVehicleClasses(
          0, model.getVehicleClasses(0).toBuilder().setCompartmentSetId("double-decker").build());
    }

    System.out.println(model.getCompartmentSets(0).toString());
    System.out.println(model.getCompartments(0).toString());
    System.out.println(model.getVehicleClasses(0).toString());
    // just to see what the pbf looks like.

    builder.setModel(model.build());
    builder.setSolveType(SolveType.Optimise); // Optimise the solve request.

    String requestId = api.Post(builder.build()); // send the model to the api
    Ivr8Yni1C9K2Swof.SolutionResponse solution =
        api.Get(requestId); // get the response (which is cast internally)
    System.out.println(String.format("Solution cost: %02f", solution.getObjective()));

    ivr8helper.printSolution(solution, true, true, true, true);
    ivr8helper.printCompartmentSummary(model.build(), solution);
    // okay, so what are we looking at here? So basically each "allocated" is when a
    // task is executed so either a pickup or a dropff. the capacity of each
    // compartment is listed at the top under "capacity" each stop shows where the
    // volume is added and we can see that only one change is made at each node.
    // this is because the task is assigned to a compartment. At no point is the
    // total volume allocated to a compartment more than the capacity of the
    // compartment. There are 16 allocations here because there are 8 jobs, i.e. 8
    // pickups, 8 dropoffs. So after each pickup we can see the state of the load on
    // the vehicle. It's maximum weight is at stop.8 => 1800 units.

    // now lets try somethign that's infeasible (by design) and see what happens.
    // we're going to clear the compartments, populate a new list and run the model.
    model.clearCompartments();
    for (int i = 0; i < 8; i++) {
      model.addCompartments(
          Ivr8Yni1C9K2Swof.Compartment.newBuilder()
              .setId("c" + (i + 1))
              .addCapacities(
                  Ivr8Yni1C9K2Swof.Compartment.Capacity.newBuilder()
                      .setDimensionId("capacity")
                      .setCapacity(i < 4 ? 150f : 350f)
                      .build()) // switch between top and bottom rack
                                // bottom rack is at 350 - which is less than the biggest order
              .build());
    }
    builder.setModel(model.build());
    builder.setSolveType(SolveType.Optimise); // Optimise the solve request.

    requestId = api.Post(builder.build()); // send the model to the api
    solution = api.Get(requestId); // get the response (which is cast internally)
    System.out.println(String.format("Solution cost: %02f", solution.getObjective()));

    ivr8helper.printSolution(solution, false, false, false, true);
    ivr8helper.printCompartmentSummary(model.build(), solution);
    // ah, but the api is nice enough to tell us that there is no feasible
    // compartment assignment exists for this particular set of tasks as well as the
    // constraining dimension (capacity). The limit and value's are negative here
    // indicating that the values aren't relevant. if you're looking for a more
    // informative error message, use the evaluate end-point which can identify for
    // a proposed sequence where things went wrong (or whether any feasible sub-set
    // exists)

    // for visualisations see the R/python notebook for plots on the same example.
  }

  private apiHelper<Ivr8Yni1C9K2Swof.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}