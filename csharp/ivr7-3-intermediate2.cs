// IVR7 Intermediate example 2:
// Purpose: illustrate the usage of the evaluate end-point and inline model manipulations.
// Builds a simple pickup/dropoff (similar to the basic model).
// Runs an eval on a sub-sequence to illustrate how to call the endpoint and
// interpret the responses in terms of infeasibility messages.

using System.Collections.Generic;
using System;

class ivr7intermediate2 : IRunner
{
  public ivr7intermediate2(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data;
  }

  public void Run()
  {
    var api = new ApiHelper<Ivr7.SolveRequest, Ivr7.SolutionResponse>("ivr7-kt461v8eoaif", configFile);
    // so here we're going to build the model 

    // create a solve request
    Ivr7.SolveRequest sr = new Ivr7.SolveRequest();

    sr.Model = new Ivr7.Model(); // initialise the model container

    // we're going to reuse the helpers described in the ivr7basic example. Please see that for a reference.
    ivr7helper.makeDistanceTimeCapDims(sr.Model); // adds distance, time & capacity
    ivr7helper.makeLocations(sr.Model, data);     // adds all the locations to the model
    ivr7helper.makeJobTimeCap(sr.Model, data, ivr7helper.Rep(0, data.Count - 1), ivr7helper.Seq(1, data.Count));
    sr.Model.vehicleCostClasses.Add(ivr7helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    sr.Model.vehicleClasses.Add(ivr7helper.makeVcSimple("vc1", 1, 1, 1, 1));
    for (int i = 0; i < 4; i++)
    {
      sr.Model.Vehicles.Add(ivr7helper.makeVehicleCap("vehicle_" + i, // unique id for the vehicle.
                                                      "vc1",  // the vehicle class
                                                      "vcc1", // the vehicle cost class
                                                      2000,  // the capacity of the vehicle
                                                      data[0].id, // start location for the vehicle
                                                      data[0].id, // end location for the vehicle
                                                      7 * 60,  // start time: 7 AM
                                                      18 * 60  // end time: 6 PM
                                                      ));
    }

    sr.solveType = Ivr7.SolveRequest.SolveType.Optimise; // Optimise the solve request.

    // now it's just sending the model to the api

    string requestId = api.Post(sr); // send the model to the api
    var initialSolution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    // so we can do a few things here, we can add constraints which weren't in the original
    // model, evaluate the same sequence and see if any constraints are broken?
    // sure, this sounds like fun. Lets add some time windows to all the locations and see
    // what that does.
    foreach (var l in sr.Model.Locations)
    {
      var a = new Ivr7.Location.Attribute()
      {
        dimensionId = "time",
        arrivalWindows = { new Ivr7.Window{
                  Start = 8*60,
                  End = 14*60
                }}
      };
      l.Attributes.Add(a);
    }


    // okay, so now we've added a 08:00 - 14:00 window on all the locations.
    // in order to evaluate our current solution against this new solution
    // we need to convert our current solution to a task sequence (which is done
    // by vehicle)
    // so the only catch here is that the shift-start and shift-end nodes are implicitly already there
    // so all we really need to do is pull out the nodes inbetween. So we filter on only the tasks we're
    // scheduling in the last solution we received.

    foreach (var r in initialSolution.Routes)
    {
      // so the definition is, for a vehicle: list the tasks that vehicle should perform
      // each item in the list is another vehicle. vehicles with no tasks can be omitted
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
        sr.Model.taskSequences.Add(ts);
      }
    }

    sr.solveType = Ivr7.SolveRequest.SolveType.Evaluate; // now evaluate this sequence with the new constraints

    requestId = api.Post(sr); // send the new model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    // just print the infeasibilities
    ivr7helper.printSolution(Solution, false, false, false, true);

    // so here we should have some constraints which have been broken.
    // We get told which dimension is related (if the constraint is related to a dimension)
    // we also get told which type of constraint (if known) and the degree to which the constraint is broken.

    //  if the constraints are tardy constraints being broken, this means the task starts AFTER the
    //  allowable window. the limit will be often be zero, the value will be the amount by which the
    //  vehicle is late. we can check the arrival time of the task to verify this.
    HashSet<string> infeasibleTasks = new HashSet<string>();
    foreach (var t in Solution.Infeasibilities)
    {
      infeasibleTasks.Add(t.taskId);
    }
    foreach (var r in initialSolution.Routes)
    { // now step through the initial solution and confirm this.
      foreach (var s in r.Stops)
      {
        if (infeasibleTasks.Contains(s.taskId))
        {
          foreach (var a in s.Attributes)
          {
            if (a.dimId == "time")
            {
              if (!(a.startValue > 14 * 60))
              {
                throw new Exception("Hmmm. a stop was marked as infeasible but it's arrival time looks okay?");
                // don't worry, this won't happen unless the solver is broken, or you're checking against
                // the incorrect solution reference.
              }
            }
          }
        }
      }
    }
    // we could try other things: how about we take out all tasks which are infeasible?
    // lets modify the solve request to take out these stops.
    foreach (var ts in sr.Model.taskSequences)
    {
      foreach (var tskId in ts.taskIds.ToArray())
      {
        if (infeasibleTasks.Contains(tskId))
        {
          ts.taskIds.Remove(tskId);         // lol. this is O(n) on the .net list (which is actually an array). For this example it's fine,
                                            // but in general, it's better a linkedlist for these kinds of operators. Although you need a prettty
                                            // massive model to really feel the performance impact of this.
        }
      }
    }
    // okay, but kinda obviously, the pickups were mostly feasible on their time windows, it was
    // just the dropoffs that were a problem. So now we have pickups without a dropoff?
    // what will happen?
    // lets re-run the model and check for infeasibilities
    requestId = api.Post(sr); // send the new model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    // just print the infeasibilities
    ivr7helper.printSolution(Solution, false, false, false, true);
    // so this is again, very intuitive. We find that there are a whole bunch of precendence
    // constraints which are then broken, cumul-pair constraints and task-pair constraints.
    // this is because there's a relation between the pickup and dropoff and either they're BOTH scheduled
    // or BOTH unscheduled. Having one task assigned to a vehicle in the schedule without the other breaks
    // a bunch of constraints.
    // so lets apply the same trick and remove these stops.
    foreach (var t in Solution.Infeasibilities)
    {
      infeasibleTasks.Add(t.taskId);
    }

    foreach (var ts in sr.Model.taskSequences)
    {
      foreach (var tskId in ts.taskIds.ToArray())
      {
        if (infeasibleTasks.Contains(tskId))
        {
          ts.taskIds.Remove(tskId);
        }
      }
    }

    requestId = api.Post(sr); // send the new model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)
    // just print the infeasibilities
    ivr7helper.printSolution(Solution, false, false, false, true);
    // great, this is actually an empty table now, which means there aren't any infeasibilities left in the schedule
    // but at what cost did that come? Quite a lot. the solution now is very expensive.
    // We could simply copy the solution, then switch the model back to optimise and re-run it

    var prevSolution = Solution;
    sr.solveType = Ivr7.SolveRequest.SolveType.Optimise;
    requestId = api.Post(sr);
    Solution = api.Get(requestId);
    if (prevSolution.Objective < Solution.Objective)
    {
      throw new Exception("Whoa, this doesn't make any sense :-)");
    }

    ivr7helper.printSolution(Solution);
    // with no infeasibilities.
    // for visualisations see the R/python notebook for plots on the same example.

    return;
  }

  public Ivr7.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}