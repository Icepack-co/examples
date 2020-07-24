// IVR8 Advanced Example:
// Purpose: demonstrate how to use compartment constraints on a particular model.
// * Use a subset of the publist stops and configure a single vehicle
// * Use a simple one-rack comparment configuration
// * Add allowable-compartment assignments (i.e. which jobs may be assigned to which compartments)
using System.Collections.Generic;
using System;

class ivr8advanced : IRunner
{
  public ivr8advanced(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data.GetRange(0, 9);
  }

  public void Run()
  {
    var api = new ApiHelper<Ivr8.SolveRequest, Ivr8.SolutionResponse>("ivr8-yni1c9k2swof", configFile);
    // so here we're going to build the model 

    // create a solve request
    Ivr8.SolveRequest sr = new Ivr8.SolveRequest();

    sr.Model = new Ivr8.Model(); // initialise the model container

    // see ivr7 basic examples for notes around each of these methods.
    // the ivr7/8 models are interchangeable, except that the IVR8 model supports
    // compartment modelling.
    ivr8helper.makeDistanceTimeCapDims(sr.Model); // adds distance, time & capacity
    ivr8helper.makeLocations(sr.Model, data);     // adds all the locations to the model
    ivr8helper.makeJobTimeCap(sr.Model, data, ivr8helper.Rep(0, data.Count - 1), ivr8helper.Seq(1, data.Count));
    sr.Model.vehicleCostClasses.Add(ivr8helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    sr.Model.vehicleClasses.Add(ivr8helper.makeVcSimple("vc1", 1, 1, 1, 1));
    sr.Model.Vehicles.Add(ivr8helper.makeVehicleCap("vehicle_0", // unique id for the vehicle.
                                                      "vc1",  // the vehicle class
                                                      "vcc1", // the vehicle cost class
                                                      2000,  // the capacity of the vehicle
                                                      data[0].id, // start location for the vehicle
                                                      data[0].id, // end location for the vehicle
                                                      7 * 60,  // start time: 7 AM
                                                      18 * 60  // end time: 6 PM
                                                      ));

    //  We're going to simplify the config in this example slightly.
    //  Lower Rack  [ ] [ ] [ ] [ ]  500kg per "compartment"  c1, c2, c3, c4

    for (int i = 0; i < 4; i++)
    {
      sr.Model.Compartments.Add(
       new Ivr8.Compartment
       {
         Id = "c" + (i + 1),
         Capacities = {
          new Ivr8.Compartment.Capacity{
            dimensionId = "capacity",
            capacity = 500
          }
        }
       });
    }
    // now we can define a compartment set (a container for the individual compartments)
    // which is attached to a vehicle.
    var cset = new Ivr8.CompartmentSet
    {
      Id = "tanker"
    };
    foreach (var c in sr.Model.Compartments)
    {
      cset.compartmentIds.Add(c.Id); // add all the defined compartments to the model
    }

    sr.Model.compartmentSets.Add(cset);

    // now we can go back through the tasks and allocate them to allowable compartments
    // this is normal in fuel delivery systems where you have diesel/petrol constraints.
    // we're just going to decide on which jobs may go in which compartments based on the
    // index, and lets see if that's feasible. Obviously, you'll create it using proper logic
    // based on the business rules.

    Dictionary<string, HashSet<string>> allowableCompartments = new Dictionary<string, HashSet<string>>(); // just storing this for later
    for (int j = 0; j < sr.Model.Jobs.Count; j++)
    {
      var job = sr.Model.Jobs[j];
      job.compartmentRelations = new Ivr8.Job.CompartmentRelation();
      job.compartmentRelations.type = Ivr8.Job.CompartmentRelation.Type.Inclusive;
      if (j % 2 == 0)
      {
        job.compartmentRelations.compartmentIds.Add("c2");
        job.compartmentRelations.compartmentIds.Add("c4");
        allowableCompartments.Add(job.Id, new HashSet<string> { "c2", "c4" });
      }
      else
      {
        job.compartmentRelations.compartmentIds.Add("c1");
        job.compartmentRelations.compartmentIds.Add("c3");
        allowableCompartments.Add(job.Id, new HashSet<string> { "c1", "c3" });
      }
    }

    sr.Model.vehicleClasses[0].compartmentSetId = "tanker";

    sr.solveType = Ivr8.SolveRequest.SolveType.Optimise; // Optimise the solve request.

    // now it's just sending the model to the api

    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    ivr8helper.printSolution(Solution, true, false, false, false);
    ivr8helper.printCompartmentSummary(sr.Model, Solution);
    // so the compartment summary is nice - but it doesn't tell us whether we stuck to the constraints
    // around the relations for each of the jobs.

    // we can check against the "allowableCompartments" dictionary we created earlier here.
    foreach (var r in Solution.Routes)
    {
      foreach (var s in r.Stops)
      {
        if (s.compartmentId != "")
        {
          // check if the job id is allowed to go on this compartment!
          if (allowableCompartments.ContainsKey(s.jobId))
          {
            if (!allowableCompartments[s.jobId].Contains(s.compartmentId))
            {
              throw new Exception("Compartment assigned which wasn't in the inclusion list!");
            }
          }
        }
      }
    }
    // so that's nice, we can see that at each task assignment we only used compartments
    // which were in the allowable set we provided the api.
    // it's probably worth noting that the default is all compartments in a compartment-set are allowed
    // so you can either specify an inclusive sub-set, or excluded sub-set.
    // if all compartments are excluded then it will let you know that there's no feasible allocation



    // for visualisations see the R/python notebook for plots on the same example.
    return;
  }

  public Ivr8.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}