package icepackai;

import icepackai.IVR8.Ivr8Yni1C9K2Swof;
import icepackai.IVR8.Ivr8Yni1C9K2Swof.SolveRequest.SolveType;

import java.util.*;
import java.util.stream.Collectors;

// IVR8 Intermediate Example:
// Purpose: demonstrate how to use compartment constraints on a particular model.
// * Use a subset of the publist stops and configure a single vehicle
// * Use a simple two-rack compartment configuration to illustrate the basic assignment workings.
// * Add a group-limit constraint which only permits loads on the top-rack if there
//   is a task filling the space beneth it.
public class ivr8_2_intermediate {
  public ivr8_2_intermediate(List<dataRow> inputdata, String config) throws Exception {
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
                                                   .setCapacity(i < 4 ? 100f : 400f)
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
      // now we're going to add compartment relations which speak to the group limit.
      // we can create multiple group limits. if we want something like, "the mass on
      // the top may not exceed the mass on the bottom"
      Ivr8Yni1C9K2Swof.CompartmentSet.GroupLimit.Builder glim =
          Ivr8Yni1C9K2Swof.CompartmentSet.GroupLimit.newBuilder()
              .setDimensionId("capacity")
              .setLimit(0.0f);
      // so this says c1+c2+c3+c4-c5-c6-c7-c8 <= 0 is required for feasibility
      // writing this differently c1:c4 - c5:c8 <= 0 (grouping the c's together)
      // so c1:c4 <= c5:c8 (moving c5:c8 to the rhs)
      // which says the top rack (c1:c4) should sum to less than the bottom rack
      // (c5:c8)
      for (int i = 0; i < 8; i++) {
        glim.addCompartmentIds("c" + (i + 1)); // adds c1:c8 to the equation.
        glim.addCoefficients(i < 4 ? +1 : -1); // sets the value in the array
      }

      cset.addGroupLimits(glim.build());

      model.addCompartmentSets(cset.build());
      // then we assign the "double-decker" compartment set to the vehicle class.
      // we could have added it to each vehicle if we wanted, this is simply easier.
      model.setVehicleClasses(
          0, model.getVehicleClasses(0).toBuilder().setCompartmentSetId("double-decker").build());
    }

    System.out.println(model.getCompartmentSets(0).toString());
    // just to see what the pbf looks like. with the group limits

    builder.setModel(model.build());
    builder.setSolveType(SolveType.Optimise); // Optimise the solve request.

    String requestId = api.Post(builder.build()); // send the model to the api
    Ivr8Yni1C9K2Swof.SolutionResponse solution =
        api.Get(requestId); // get the response (which is cast internally)
    System.out.println(String.format("Solution cost: %02f", solution.getObjective()));

    ivr8helper.printSolution(solution, true, false, false, true);
    ivr8helper.printCompartmentSummary(model.build(), solution);
    // In this table you can see that the the sum for compartments 1:4 is always
    // less than 5:8 this way we're constrained by always having more weight on the
    // bottom rack than the top rack throughout the route (which is still pretty
    // well costed)

    // for visualisations see the R/python notebook for plots on the same example.
  }

  private apiHelper<Ivr8Yni1C9K2Swof.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}