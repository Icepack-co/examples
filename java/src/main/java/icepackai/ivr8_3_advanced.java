package icepackai;

import icepackai.IVR8.Ivr8Yni1C9K2Swof;
import icepackai.IVR8.Ivr8Yni1C9K2Swof.Job.CompartmentRelation.Type;
import icepackai.IVR8.Ivr8Yni1C9K2Swof.SolveRequest.SolveType;

import java.util.*;
import java.util.stream.Collectors;

// IVR8 Advanced Example:
// Purpose: demonstrate how to use compartment constraints on a particular model.
// * Use a subset of the publist stops and configure a single vehicle
// * Use a simple one-rack comparment configuration
// * Add allowable-compartment assignments (i.e. which jobs may be assigned to which compartments)
public class ivr8_3_advanced {
  public ivr8_3_advanced(List<dataRow> inputdata, String config) throws Exception {
    this.configFile = config;
    this.data = inputdata.stream().limit(9).collect(Collectors.toList()); // grabs just the first 9 items.;
  }

  public void Run() throws Exception {
    api = new apiHelper<Ivr8Yni1C9K2Swof.SolutionResponse>(Ivr8Yni1C9K2Swof.SolutionResponse.class, "ivr8-yni1c9k2swof",
        configFile);
    Ivr8Yni1C9K2Swof.SolveRequest.Builder builder = Ivr8Yni1C9K2Swof.SolveRequest.newBuilder();
    // so here we're going to build the model
    Ivr8Yni1C9K2Swof.Model.Builder model = builder.getModel().toBuilder(); // this is the actual model container.

    // see ivr7 basic examples for notes around each of these methods.
    // the ivr7/8 models are interchangeable, except that the IVR8 model supports
    // compartment modelling.
    ivr8helper.makeDistanceTimeCapDims(model);// adds distance, time & capacity
    ivr8helper.makeLocations(model, data); // adds all the locations to the model
    ivr8helper.makeJobTimeCap(model, data, ivr8helper.Rep(0, data.size() - 1), ivr8helper.Seq(1, data.size()));
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

    // We're going to simplify the config in this example slightly.
    // Lower Rack [ ] [ ] [ ] [ ] 500kg per "compartment" c1, c2, c3, c4

    for (int i = 0; i < 4; i++) {
      model.addCompartments(Ivr8Yni1C9K2Swof.Compartment.newBuilder().setId("c" + (i + 1))
          .addCapacities(
              Ivr8Yni1C9K2Swof.Compartment.Capacity.newBuilder().setDimensionId("capacity").setCapacity(500f).build())
          .build());
    }
    {
      // now we can define a compartment set (a container for the individual
      // compartments) which is attached to a vehicle (or a vehicle class if you
      // prefer).
      Ivr8Yni1C9K2Swof.CompartmentSet.Builder cset = Ivr8Yni1C9K2Swof.CompartmentSet.newBuilder().setId("tanker");
      for (int i = 0; i < 4; i++) {
        // add all the defined compartments to the compartment set
        cset.addCompartmentIds("c" + (i + 1)).build();
      }
      model.addCompartmentSets(cset.build());
      // then we assign the "tanker" compartment set to the vehicle class.
      // we could have added it to each vehicle if we wanted, this is simply easier.
      model.setVehicleClasses(0, model.getVehicleClasses(0).toBuilder().setCompartmentSetId("tanker").build());
    }

    System.out.println(model.getCompartmentSets(0).toString());
    // just to see what the pbf looks like. with the group limits

    // now we can go back through the tasks and allocate them to allowable
    // compartments this is normal in fuel delivery systems where you have
    // diesel/petrol constraints. we're just going to decide on which jobs may go in
    // which compartments based on the index, and lets see if that's feasible.
    // Obviously, you'll create it using proper logic based on the business rules.

    Map<String, HashSet<String>> allowableCompartments = new HashMap<String, HashSet<String>>();
    // just storing this for later
    for (int j = 0; j < model.getJobsCount(); j++) {
      Ivr8Yni1C9K2Swof.Job.Builder job = model.getJobs(j).toBuilder();
      job.setCompartmentRelations(Ivr8Yni1C9K2Swof.Job.CompartmentRelation.newBuilder().setType(Type.INCLUSIVE)
          .addCompartmentIds(j % 2 == 0 ? "c2" : "c1").addCompartmentIds(j % 2 == 0 ? "c4" : "c3").build());
      model.setJobs(j, job.build()); // copy back to the main container.
      if (j % 2 == 0) {
        allowableCompartments.put(job.getId(), new HashSet<String>() {
          {
            add("c2");
            add("c4");
          }
        });
      } else {
        allowableCompartments.put(job.getId(), new HashSet<String>() {
          {
            add("c1");
            add("c3");
          }
        });
      }
    }
    System.out.println(model.getJobs(0).toString()); // so that you can see how it's specified

    builder.setModel(model.build());
    builder.setSolveType(SolveType.Optimise); // Optimise the solve request.

    String requestId = api.Post(builder.build()); // send the model to the api
    Ivr8Yni1C9K2Swof.SolutionResponse solution = api.Get(requestId); // get the response (which is cast internally)
    System.out.println(String.format("Solution cost: %02f", solution.getObjective()));

    ivr8helper.printSolution(solution, true, false, false, false);
    ivr8helper.printCompartmentSummary(model.build(), solution);
    // so the compartment summary is nice - but it doesn't tell us whether we stuck
    // to the constraints around the relations for each of the jobs.

    // we can check against the "allowableCompartments" lookup we created earlier
    // here.
    for (Ivr8Yni1C9K2Swof.SolutionResponse.Route r : solution.getRoutesList()) {
      for (Ivr8Yni1C9K2Swof.SolutionResponse.Stop s : r.getStopsList()) {
        if (s.getCompartmentId() != "") {
          // check if the job id is allowed to go on this compartment!
          if (allowableCompartments.containsKey(s.getJobId())) {
            if (!allowableCompartments.get(s.getJobId()).contains(s.getCompartmentId())) {
              throw new Exception("Compartment assigned which wasn't in the inclusion list!");
            }
          }
        }
      }
    }
    // so that's nice, we can see that at each task assignment we only used
    // compartments which were in the allowable set we provided the api. it's
    // probably worth noting that the default is all compartments in a
    // compartment-set are allowed so you can either specify an inclusive sub-set,
    // or excluded sub-set. if all compartments are excluded then it will let you
    // know that there's no feasible allocation

    // for visualisations see the R/python notebook for plots on the same example.
  }

  private apiHelper<Ivr8Yni1C9K2Swof.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}