package icepackai;

import icepackai.IVR7.Ivr7Kt461V8Eoaif;
import icepackai.IVR7.Ivr7Kt461V8Eoaif.SolveRequest.SolveType;
import icepackai.IVRData.IvrdataO43E0Dvs78Zq;

import java.util.*;

// IVR7 Advanced example:
// Purpose: illustrate the usage of the data-upload as well as model versioning.
// * Builds a simple pickup/dropoff (similar to the basic model).
// * Illustrates how to use the parent solve-request container for evaluate requests against a versioned model
// ** This is a quicker manner in which to run evaluate requests against the api where a large chunk
//    of model content is required to define the model. Send it once, then just run requests against that model,
//    and we'll handle moving the data around on our side.
public class ivr7_4_advanced1 {
  public ivr7_4_advanced1(List<dataRow> inputdata, String config) throws Exception {
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
    // okay, so that's a basic model. Lets now use the objects, but submit them to
    // the api
    // through a different mechanism.

    apiHelper<IvrdataO43E0Dvs78Zq.CachedModel> data_api = new apiHelper<IvrdataO43E0Dvs78Zq.CachedModel>(
        IvrdataO43E0Dvs78Zq.CachedModel.class, "ivrdata-o43e0dvs78zq", configFile);
    IvrdataO43E0Dvs78Zq.CachedModel dataModel = IvrdataO43E0Dvs78Zq.CachedModel.newBuilder()
        .setModel(model.build().toByteString()).build();
    // epic: we just saved our model as a byte stream into this data payload.
    String modelID = data_api.Post(dataModel);
    // so now on the main builder, we don't have to set the model, just the model
    // id!

    builder.setModelID(modelID);
    builder.setSolveType(SolveType.Optimise);
    System.out.println(builder.build().toString()); // so a very simple follow up request.

    String requestId = api.Post(builder.build()); // send the model to the api
    Ivr7Kt461V8Eoaif.SolutionResponse Solution = api.Get(requestId); // get the response (which is cast internally)

    System.out.println(String.format("Solution cost: %02f", Solution.getObjective()));

    ivr7helper.printSolution(Solution, true, true, true, true);
    // this also means that because we have a model which is versioned separately
    // from the solve request, we can use the solve request with the task-sequence
    // and have that apply to a model. So lets extract the task sequence from the
    // solved model.

    for (Ivr7Kt461V8Eoaif.SolutionResponse.Route r : Solution.getRoutesList()) {
      Ivr7Kt461V8Eoaif.TaskSequence.Builder nts = Ivr7Kt461V8Eoaif.TaskSequence.newBuilder()
          .setVehicleId(r.getVehicleId());
      for (int i = 1; i < r.getStopsCount() - 1; i++) {
        nts.addTaskId(r.getStops(i).getTaskId());
      }
      builder.addRoutes(nts);// NOTE: We're adding the tasks to the solve request, not the model - the task
                             // sequence will be applied to the model we've referenced in this example
    }
    builder.setSolveType(SolveType.Evaluate);
    System.out.println(builder.build().toString()); // so a very simple follow up request - but with the evaluate
                                                    // sequence
    requestId = api.Post(builder.build()); // send the model to the api
    Ivr7Kt461V8Eoaif.SolutionResponse evalSolution = api.Get(requestId); // get the response (which is cast internally)
    ivr7helper.printSolution(evalSolution, true, true, true, true);

    if (Math.abs(evalSolution.getObjective() - Solution.getObjective()) > 0.01f) {
      throw new Exception("Evaluation not identical to original solution value?");
    }
    // so this is pretty nice when we think about it. It means that if you want to
    // evaluate several permutations (i.e. modifications on a UI) then you don't
    // have to resend the model each time, you can send it only when the master data
    // is modified (i.e. times, locations, tasks etc) and then just use an evaluate
    // solve request against a particular task-sequence.

    // for visualisations see the R/python notebook for plots on the same example.
  }

  private apiHelper<Ivr7Kt461V8Eoaif.SolutionResponse> api;

  private String configFile;
  private List<dataRow> data;
}