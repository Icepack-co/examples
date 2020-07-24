// IVR7 Advanced example:
// Purpose: illustrate the usage of the data-upload as well as model versioning.
// * Builds a simple pickup/dropoff (similar to the basic model).
// * Illustrates how to use the parent solve-request container for evaluate requests against a versioned model
// ** This is a quicker manner in which to run evaluate requests against the api where a large chunk
//    of model content is required to define the model. Send it once, then just run requests against that model,
//    and we'll handle moving the data around on our side.

using System.Collections.Generic;
using System;

class ivr7advanced1 : IRunner
{
  public ivr7advanced1(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data;
  }

  public void Run()
  {
    var api = new ApiHelper<Ivr7.SolveRequest, Ivr7.SolutionResponse>("ivr7-kt461v8eoaif", configFile);
    // so here we're going to build the model 

    // create a solve request

    var m = new Ivr7.Model(); // initialise the model container

    // we're going to reuse the helpers described in the ivr7basic example. Please see that for a reference.
    ivr7helper.makeDistanceTimeCapDims(m); // adds distance, time & capacity
    ivr7helper.makeLocations(m, data);     // adds all the locations to the model
    ivr7helper.makeJobTimeCap(m, data, ivr7helper.Rep(0, data.Count - 1), ivr7helper.Seq(1, data.Count));
    m.vehicleCostClasses.Add(ivr7helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    m.vehicleClasses.Add(ivr7helper.makeVcSimple("vc1", 1, 1, 1, 1));
    for (int i = 0; i < 4; i++)
    {
      m.Vehicles.Add(ivr7helper.makeVehicleCap("vehicle_" + i, // unique id for the vehicle.
                                                      "vc1",  // the vehicle class
                                                      "vcc1", // the vehicle cost class
                                                      2000,  // the capacity of the vehicle
                                                      data[0].id, // start location for the vehicle
                                                      data[0].id, // end location for the vehicle
                                                      7 * 60,  // start time: 7 AM
                                                      18 * 60  // end time: 6 PM
                                                      ));
    }

    // okay, so that's a basic model. Lets now use the objects, but submit them to the api
    // through a different mechanism.
    var data_api = new ApiHelper<IVRData.CachedModel, object>("ivrdata-o43e0dvs78zq", configFile);
    // so the data api allows us to push just the data, without anything else.
    var dataModel = new IVRData.CachedModel();
    dataModel.Model = ApiHelper<object, object>.SerialiseObject<Ivr7.Model>(m);

    // epic: we just saved our model as a byte stream into this data payload.
    string modelID = data_api.Post(dataModel);

    var sr = new Ivr7.SolveRequest(); // we can now make a solve request which references the model we've uploaded
    sr.modelID = modelID; // tell the solve request to use the model we uploaded.
    sr.solveType = Ivr7.SolveRequest.SolveType.Optimise; // Optimise the solve request.

    // now it's just sending the model to the api
    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)
    ivr7helper.printSolution(Solution);
    // this also means that because we have a model which is versioned separately from the
    // solve request, we can use the solve request with the task-sequence and have that apply
    // to a model. So lets extract the task sequence from the solved model.


    foreach (var r in Solution.Routes)
    {
      List<string> tasklist = new List<string>();
      for (int i = 1; i < r.Stops.Count - 1; i++)
      {
        tasklist.Add(r.Stops[i].taskId);
      }
      if (tasklist.Count > 0)
      {
        var ts = new Ivr7.TaskSequence { vehicleId = r.vehicleId };
        foreach (var t in tasklist)
        {
          ts.taskIds.Add(t);
        }
        sr.Routes.Add(ts); //NOTE: We're adding the tasks to the solve request, not the model - the task sequence will be 
                           // applied to the model we've referenced in this example
      }
    }

    sr.solveType = Ivr7.SolveRequest.SolveType.Evaluate; // now evaluate this sequence with the model reference (i.e. a minimal data-send)

    requestId = api.Post(sr);
    var EvalSolution = api.Get(requestId);

    // just print the infeasibilities
    ivr7helper.printSolution(EvalSolution);

    if (Math.Abs(EvalSolution.Objective - Solution.Objective) > 0.01f)
    {
      throw new Exception("Evaluation not identical to original solution value?");
    }
    // so this is pretty nice when we think about it. It means that if you want to evaluate
    // several permutations (i.e. modifications on a UI) then you don't have to resend the model each
    // time, you can send it only when the master data is modified (i.e. times, locations, tasks etc)
    // and then just use an evaluate solve request against a particular task-sequence.

    // for visualisations see the R/python notebook for plots on the same example.

    return;
  }

  public Ivr7.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}