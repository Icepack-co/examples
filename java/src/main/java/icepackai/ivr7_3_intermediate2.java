package icepackai;

import icepackai.IVR7.Ivr7Kt461V8Eoaif;
import icepackai.IVR7.Ivr7Kt461V8Eoaif.SolveRequest.SolveType;

import java.util.*;

// IVR7 Intermediate example 2:
// Purpose: illustrate the usage of the evaluate end-point and inline model manipulations.
// Builds a simple pickup/dropoff (similar to the basic model).
// Runs an eval on a sub-sequence to illustrate how to call the endpoint and
// interpret the responses in terms of infeasibility messages.
public class ivr7_3_intermediate2 {
  public ivr7_3_intermediate2(List<dataRow> inputdata, String config) throws Exception {
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
    ivr7helper.makeLocations(model, data);
    ivr7helper.makeJobTimeCap(model, data, ivr7helper.Rep(0, data.size() - 1), ivr7helper.Seq(1, data.size()));
    model.addVehicleCostClasses(ivr7helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    model.addVehicleClasses(ivr7helper.makeVcSimple("vc1", 1, 1, 1, 1));

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
    builder.setModel(model.build());
    builder.setSolveType(SolveType.Optimise);

    String requestId = api.Post(builder.build()); // send the model to the api
    Ivr7Kt461V8Eoaif.SolutionResponse initialSolution = api.Get(requestId); // get the response (which is cast
                                                                            // internally)

    // so we can do a few things here, we can add constraints which weren't in the
    // original model, evaluate the same sequence and see if any constraints are
    // broken?
    // sure, this sounds like fun. Lets add some time windows to all the locations
    // and see what that does.
    for (int i = 0; i < model.getLocationsCount(); i++) {
      model.setLocations(i,
          model.getLocations(i).toBuilder()
              .addAttributes(Ivr7Kt461V8Eoaif.Location.Attribute.newBuilder().setDimensionId("time")
                  .addArrivalWindows(Ivr7Kt461V8Eoaif.Window.newBuilder().setStart(8 * 60f).setEnd(14 * 60f))));
      // this effectively adds a window onto each location.
    }

    // okay, so now we've added a 08:00 - 14:00 window on all the locations.
    // in order to evaluate our current solution against this new solution we need
    // to convert our current solution to a task sequence (which is done by vehicle)
    // so the only catch here is that the shift-start and shift-end nodes are
    // implicitly already there so all we really need to do is pull out the nodes
    // inbetween. So we filter on only the tasks we're scheduling in the last
    // solution we received.
    for (Ivr7Kt461V8Eoaif.SolutionResponse.Route r : initialSolution.getRoutesList()) {
      // so the definition is, for a vehicle: list the tasks that vehicle should
      // perform each item in the list is another vehicle. vehicles with no tasks can
      // be omitted
      List<String> tasklist = new ArrayList<String>();
      for (int i = 1; i < r.getStopsCount() - 1; i++) {
        tasklist.add(r.getStops(i).getTaskId());
      }
      Ivr7Kt461V8Eoaif.TaskSequence.Builder ts = Ivr7Kt461V8Eoaif.TaskSequence.newBuilder();
      if (tasklist.size() > 0) {
        ts.setVehicleId(r.getVehicleId());
        for (String t : tasklist) {
          ts.addTaskId(t);
        }
        model.addTaskSequence(ts);
      }
    }

    builder.setModel(model.build());
    builder.setSolveType(SolveType.Evaluate); // tell the api to evaluate this sequence with the new constraints.

    requestId = api.Post(builder.build()); // send the model to the api
    Ivr7Kt461V8Eoaif.SolutionResponse evalSolution = api.Get(requestId); // get the response (which is cast internally)

    System.out.println(String.format("Solution cost: %02f", initialSolution.getObjective()));

    if (Math.abs(evalSolution.getObjective() - initialSolution.getObjective()) > 0.01f) {
      throw new Exception(
          "hmmm.. feels like something wasn't configured correctly in the eval payload since we're expecting the same objective cost.");
    }

    ivr7helper.printSolution(evalSolution, false, false, false, true);
    // so here we should have some constraints which have been broken.
    // We get told which dimension is related (if the constraint is related to a
    // dimension) we also get told which type of constraint (if known) and the
    // degree to which the constraint is broken.

    // if the constraints are tardy constraints being broken, this means the task
    // starts AFTER the allowable window. the limit will be often be zero, the value
    // will be the amount by which the vehicle is late. we can check the arrival
    // time of the task to verify this.
    HashSet<String> infeasibleTasks = new HashSet();
    for (Ivr7Kt461V8Eoaif.SolutionResponse.Infeasibility t : evalSolution.getInfeasibilitiesList()) {
      infeasibleTasks.add(t.getTaskId());
    }
    for (Ivr7Kt461V8Eoaif.SolutionResponse.Route r : initialSolution.getRoutesList()) {
      for (Ivr7Kt461V8Eoaif.SolutionResponse.Stop s : r.getStopsList()) {
        if (infeasibleTasks.contains(s.getTaskId())) {
          for (Ivr7Kt461V8Eoaif.SolutionResponse.StopAttribute a : s.getAttributesList()) {
            if (a.getDimId() == "time") {
              if (!(a.getStartValue() > 14 * 60.0f)) {
                throw new Exception("Hmmm. a stop was marked as infeasible but it's arrival time looks okay?");
                // don't worry, this won't happen unless the solver is broken, or you're
                // checking against
                // the incorrect solution reference.
              }
            }
          }
        }
      }
    }
    // we could try other things: how about we take out all tasks which are
    // infeasible?
    // lets modify the solve request to take out these stops.

    for (int i = 0; i < model.getTaskSequenceCount(); i++) {
      Ivr7Kt461V8Eoaif.TaskSequence ts = model.getTaskSequence(i);
      Ivr7Kt461V8Eoaif.TaskSequence.Builder nts = Ivr7Kt461V8Eoaif.TaskSequence.newBuilder()
          .setVehicleId(ts.getVehicleId());
      for (String tskId : ts.getTaskIdList()) {
        if (!infeasibleTasks.contains(tskId)) {
          nts.addTaskId(tskId);
        }
      }
      model.setTaskSequence(i, nts); // so this rebuilds the task sequence without the infeasible tasks.
    }

    builder.setModel(model.build()); // update the solve request with the modified model
    requestId = api.Post(builder.build()); // send the model to the api
    evalSolution = api.Get(requestId); // get the response (which is cast internally)
    ivr7helper.printSolution(evalSolution, false, false, false, true);
    // so this is again, very intuitive. We find that there are a whole bunch of
    // precendence constraints which are then broken, cumul-pair constraints and
    // task-pair constraints. this is because there's a relation between the pickup
    // and dropoff and either they're BOTH scheduled or BOTH unscheduled. Having one
    // task assigned to a vehicle in the schedule without the other breaks
    // a bunch of constraints.

    // so lets apply the same trick and remove these stops.
    for (Ivr7Kt461V8Eoaif.SolutionResponse.Infeasibility t : evalSolution.getInfeasibilitiesList()) {
      infeasibleTasks.add(t.getTaskId());
    }
    for (int i = 0; i < model.getTaskSequenceCount(); i++) {
      Ivr7Kt461V8Eoaif.TaskSequence ts = model.getTaskSequence(i);
      Ivr7Kt461V8Eoaif.TaskSequence.Builder nts = Ivr7Kt461V8Eoaif.TaskSequence.newBuilder()
          .setVehicleId(ts.getVehicleId());
      for (String tskId : ts.getTaskIdList()) {
        if (!infeasibleTasks.contains(tskId)) {
          nts.addTaskId(tskId);
        }
      }
      model.setTaskSequence(i, nts); // so this rebuilds the task sequence without the infeasible tasks.
    }
    builder.setModel(model.build()); // update the solve request with the modified model
    requestId = api.Post(builder.build()); // send the model to the api
    evalSolution = api.Get(requestId); // get the response (which is cast internally)
    ivr7helper.printSolution(evalSolution, false, false, false, true);
    // great, this is actually an empty table now, which means there aren't any
    // infeasibilities left in the schedule but at what cost did that come? Quite a
    // lot. the solution now is very expensive. We could simply copy the solution,
    // then switch the model back to optimise and re-run it

    Ivr7Kt461V8Eoaif.SolutionResponse prevSolution = evalSolution;
    builder.setSolveType(SolveType.Optimise); // switch back to optimising this model.

    requestId = api.Post(builder.build()); // send the model to the api
    Ivr7Kt461V8Eoaif.SolutionResponse Solution = api.Get(requestId); // get the response (which is cast internally)

    if (prevSolution.getObjective() < Solution.getObjective()) {
      throw new Exception("Whoa, this doesn't make any sense :-)");
    }
    ivr7helper.printSolution(Solution, true, true, true, true);
    // with no infeasibilities.
    // for visualisations see the R/python notebook for plots on the same example.
  }

  private apiHelper<Ivr7Kt461V8Eoaif.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}