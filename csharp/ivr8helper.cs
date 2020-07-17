using System.Collections.Generic;
using System;
using System.Linq;

class ivr8helper
{
  public static void makeDistanceTimeCapDims(Ivr8.Model m)
  {
    m.Dimensions = new Ivr8.DimensionConfiguration();

    m.Dimensions.timeConfig = new Ivr8.InternalDimension
    {
      Id = "time",
      measurementUnit = Ivr8.InternalDimension.eMeasurementUnit.Minutes,
      slackMax = (float)1e6,
      tardyMax = 0
    };
    m.Dimensions.distanceConfig = new Ivr8.InternalDimension
    {
      Id = "distance",
      measurementUnit = Ivr8.InternalDimension.eMeasurementUnit.Kilometres,
      slackMax = 0,
      tardyMax = 0
    };

    m.Dimensions.capacityDimensions.Add(
      new Ivr8.CapacityDimension
      {
        Id = "capacity",
        Units = "kg",
        slackMax = 0,
        tardyMax = 0
      }
    );
  }

  public static void makeLocations(Ivr8.Model m, List<dataRow> d)
  {
    m.Locations.Clear();
    foreach (var l in d)
    {
      m.Locations.Add(new Ivr8.Location
      {
        Id = l.id,
        Geocode = new Ivr8.Geocode
        {
          Longitude = l.X,
          Latitude = l.Y
        }
      });

    }
  }

  public static void makeJobTimeCap(Ivr8.Model m, List<dataRow> d, List<int> srcs, List<int> dests)
  {
    m.Jobs.Clear();
    if (srcs.Count != dests.Count)
    {
      throw new System.Exception("Expected srcs.Count == dests.Count");
    }
    for (int i = 0; i < srcs.Count; i++)
    {
      int si = srcs[i];
      int di = dests[i];
      Ivr8.Job j = new Ivr8.Job();
      j.Id = "job_" + d[di].id;
      j.pickupTask = new Ivr8.Job.Task();
      j.pickupTask.taskId = "pickup_" + d[di].id;
      j.pickupTask.locationId = d[si].id;

      j.pickupTask.Attributes.Add(
        new Ivr8.Job.Task.Attribute
        {
          dimensionId = "time",
          Quantity = d[di].pickupTime
        });

      j.pickupTask.Attributes.Add(
        new Ivr8.Job.Task.Attribute
        {
          dimensionId = "capacity",
          Quantity = d[di].quanity
        }
      );

      j.dropoffTask = new Ivr8.Job.Task();
      j.dropoffTask.taskId = "dropoff_" + d[di].id;
      j.dropoffTask.locationId = d[di].id;
      // careful here, in C# the object is passed by ref.
      // so instantiate a new set of attributes, don't reuse the object's created
      // prior to this.
      j.dropoffTask.Attributes.Add(
        new Ivr8.Job.Task.Attribute
        {
          dimensionId = "time",
          Quantity = d[di].dropoffTime
        });

      j.dropoffTask.Attributes.Add(
        new Ivr8.Job.Task.Attribute
        {
          dimensionId = "capacity",
          Quantity = -d[di].quanity
        }
      );
      j.Penalty = 10000;
      m.Jobs.Add(j);
    }
  }

  public static Ivr8.VehicleClass makeVcSimple(string name, float time_transit_coef,
                                               float time_task_coef, float time_loc_coef, float distance_transit_coef)
  {
    Ivr8.VehicleClass vc = new Ivr8.VehicleClass
    {
      Id = name
    };
    vc.Attributes.Add(
      new Ivr8.VehicleClass.Attribute
      {
        dimensionId = "time",
        transitCoef = time_transit_coef,
        taskCoef = time_task_coef,
        locationCoef = time_loc_coef,
        transitGeneratorId = "roadnetwork_time"
      });
    vc.Attributes.Add(
      new Ivr8.VehicleClass.Attribute
      {
        dimensionId = "distance",
        transitCoef = time_transit_coef,
        taskCoef = time_task_coef,
        locationCoef = time_loc_coef,
        transitGeneratorId = "roadnetwork_distance"
      });
    return vc;
  }

  public static Ivr8.VehicleCostClass makeVccSimple(string name, float fixedcost, float time_transit_costcoef,
                          float time_loc_costcoef, float time_task_costcoef, float time_slack_costcoef,
                          float distance_transit_costcoef)
  {
    Ivr8.VehicleCostClass vcc = new Ivr8.VehicleCostClass
    {
      Id = name,
      fixedCost = fixedcost
    };
    vcc.Attributes.Add(new Ivr8.VehicleCostClass.Attribute
    {
      dimensionId = "time",
      transitCostCoef = time_transit_costcoef,
      taskCostCoef = time_task_costcoef,
      locationCostCoef = time_loc_costcoef,
      slackCostCoef = time_slack_costcoef
    });

    vcc.Attributes.Add(new Ivr8.VehicleCostClass.Attribute
    {
      dimensionId = "distance",
      transitCostCoef = distance_transit_costcoef
    });
    return vcc;
  }

  public static Ivr8.Vehicle makeVehicleCap(string name, string vehicleClass, string vehicleCostClass, float capacity,
  string startLocation, string endLocation, float startTime, float endTime)
  {
    Ivr8.Vehicle v = new Ivr8.Vehicle
    {
      Id = name,
      classId = vehicleClass,
      costClassId = vehicleCostClass,
      shift = new Ivr8.Vehicle.Shift
      {
        shiftStart = new Ivr8.Vehicle.Task
        {
          locationId = startLocation,
        },
        shiftEnd = new Ivr8.Vehicle.Task
        {
          locationId = endLocation
        }
      }
    };
    // then just add the time windows to the shift
    v.shift.shiftStart.Attributes.Add(new Ivr8.Vehicle.Task.Attribute
    {
      dimensionId = "time",
      Quantity = 0,
      Windows = { new Ivr8.Window { Start = startTime, End = endTime } }
    });

    v.shift.shiftEnd.Attributes.Add(new Ivr8.Vehicle.Task.Attribute
    {
      dimensionId = "time",
      Quantity = 0,
      Windows = { new Ivr8.Window { Start = startTime, End = endTime } }
    });
    // lastly, set the capacity of the vehicle.
    v.Capacities.Add(new Ivr8.Vehicle.Capacity
    {
      dimensionId = "capacity",
      capacity = capacity
    });
    return v;
  }

  public static Ivr8.TransitRule makeLunchBreakRule(string name, string prefix, float lunchtime, float breaklength)
  {
    var tr = new Ivr8.TransitRule
    {
      Id = name,
      ruleIdPrefix = prefix,
      dimensionId = "time",
      useStandingState = true,
      useTransitState = true,
      Triggers = {
            new Ivr8.TransitRule.Trigger{
              Value = lunchtime,
              Quantity = breaklength
            }
          }
    };
    return tr;
  }


  class agg
  {
    public void Add(float delta, float cost)
    {
      cumul += delta;
      min = Math.Min(cumul, min);
      max = Math.Max(cumul, max);
      totalCost += cost;
    }
    public float min = (float)1e30;
    public float max = 0;
    public float cumul = 0;
    public float totalCost = 0;
  };

  public static void printSolution(Ivr8.SolutionResponse solution, bool printnodes = true,
                                                                  bool printaggstats = true,
                                                                  bool printTrrules = true,
                                                                  bool printInfeas = true)
  {

    int[] maxchar = new int[5] {"locationId".Length,
                                "taskId".Length,
                                "jobId".Length,
                                "vehicleId".Length,
                                "compartmentId".Length }; // column widths for Stopid locationId, taskId, jobId, vehicleId, compartmentId
    foreach (var r in solution.Routes)
    {
      maxchar[3] = Math.Max(maxchar[3], r.vehicleId.Length);
      foreach (var s in r.Stops)
      {
        maxchar[0] = Math.Max(maxchar[0], s.locationId.Length);
        maxchar[1] = Math.Max(maxchar[1], s.taskId.Length);
        maxchar[2] = Math.Max(maxchar[2], s.jobId.Length);
        maxchar[4] = Math.Max(maxchar[4], s.compartmentId.Length);
      }
    }
    string formatLine = "";
    if (printnodes)
    {
      formatLine = "|{0,-7}|{1,-" + maxchar[0] + "}|{2,-" + maxchar[1] + "}|{3,-" + maxchar[2] + "}|{4,-" + maxchar[3] + "}|{5," + maxchar[4] + "}|";
      Console.WriteLine(String.Format(formatLine, "stopId", "locationId", "taskId", "jobId", "vehicleId", "compartmentId"));
      foreach (var r in solution.Routes)
      {
        if (r.Stops.Count > 2)
        {
          foreach (var s in r.Stops)
          {
            Console.WriteLine(String.Format(formatLine, s.Id, s.locationId, s.taskId, s.jobId, r.vehicleId, s.compartmentId));
          }
          Console.WriteLine(""); // pop a gap in for the next vehicle.
        }
      }
    }
    // find all the dimensions in the model
    HashSet<string> dims = new HashSet<string>();
    foreach (var r in solution.Routes)
    {
      foreach (var s in r.Stops)
      {
        foreach (var a in s.Attributes)
        {
          dims.Add(a.dimId);
        }
      }
    }

    // because tables can't be too wide in c# we're just going to summarise the solution here by vehicle
    // into the cumulative, min, and max quantities per vehicle/dimension.
    Dictionary<string, Dictionary<string, agg>> vehicleStats = new Dictionary<string, Dictionary<string, agg>>();

    int dimLenChar = "dimension".Length;
    int ruleIdCharLen = "ruleId".Length;
    bool hasTrRules = false;

    foreach (var d in dims)
    {
      dimLenChar = Math.Max(dimLenChar, d.Length);
    }

    foreach (var r in solution.Routes)
    {
      vehicleStats.Add(r.vehicleId, new Dictionary<string, agg>());
      foreach (var d in dims)
      {
        vehicleStats[r.vehicleId].Add(d, new agg());
      }
      foreach (var s in r.Stops)
      {
        foreach (var a in s.Attributes)
        {
          vehicleStats[r.vehicleId][a.dimId].Add(a.endValue - a.startValue, a.Cost);
        }
      }
      foreach (var s in r.interStops)
      {
        foreach (var a in s.Attributes)
        {
          vehicleStats[r.vehicleId][a.dimId].Add(a.endValue - a.startValue, a.Cost);
        }
      }
      foreach (var tr in r.transitRuleAttributes)
      {
        ruleIdCharLen = Math.Max(ruleIdCharLen, tr.ruleId.Length);
        hasTrRules = true;
      }
    }
    // lets print the summary data out. |vehicleId|dimension|Min|Max|Cumul|Cost
    if (printaggstats)
    {
      Console.WriteLine("Vehilce dimension summary");
      formatLine = "|{0,-" + maxchar[3] + "}|{1,-" + dimLenChar + "}|{2,10}|{3,10}|{4,10}|{5,10}|";
      Console.WriteLine(String.Format(formatLine, "vehicleId", "dimension", "Min", "Max", "Cumul", "Cost"));
      foreach (var kvp in vehicleStats)
      {
        foreach (var l in kvp.Value)
        {
          if (l.Value.min != (float)(1e30))
          { // otherwise we know it had no values
            Console.WriteLine(String.Format(formatLine, kvp.Key, l.Key,
                              string.Format("{0:0.00}", l.Value.min),
                              string.Format("{0:0.00}", l.Value.max),
                              string.Format("{0:0.00}", l.Value.cumul),
                              string.Format("{0:0.00}", l.Value.totalCost)));
          }
        }
      }
    }
    // tabulate the tranist rules.
    // vehicleId|ruleId|dimID|fromstopid|tostopid|startvalue|endvalue|cost
    if (hasTrRules && printTrrules)
    {
      Console.WriteLine("\nVehilce transit rule summary:");
      formatLine = "|{0,-" + maxchar[3] + "}|{1," + ruleIdCharLen + "}|{2,-" + dimLenChar + "}|{3,10}|{4,10}|{5,10}|{6,10}|{7,10}|";
      Console.WriteLine(String.Format(formatLine, "vehicleId", "ruleId", "dimension", "fromStopId", "toStopId", "startValue", "endValue", "cost"));
      foreach (var r in solution.Routes)
      {
        foreach (var tr in r.transitRuleAttributes)
        {
          Console.WriteLine(String.Format(formatLine,
                            r.vehicleId, tr.ruleId, tr.dimId, tr.fromStopId, tr.toStopId,
                            string.Format("{0:0.00}", tr.startValue),
                            string.Format("{0:0.00}", tr.endValue),
                            string.Format("{0:0.00}", tr.Cost)));
        }
      }
    }
    // lastly, lets tabulate the infeasibilities
    if (printInfeas)
    {
      Console.WriteLine("\nInfeasibilities:");
      //"dimension|message|limit|value|count|taskId"
      int messageCharLen = "message".Length;
      foreach (var i in solution.Infeasibilities)
      {
        foreach (var m in i.infeasibilityInfoes)
        {
          messageCharLen = Math.Max(messageCharLen, m.Message.Length);
          dimLenChar = Math.Max(m.dimId.Length, dimLenChar);
        }
      }
      formatLine = "|{0,-" + dimLenChar + "}|{1,-" + messageCharLen + "}|{2,10}|{3,10}|{4,10}|{5," + maxchar[1] + "}|";
      Console.WriteLine(String.Format(formatLine, "dimenion", "message", "limit", "value", "count", "taskId"));
      foreach (var i in solution.Infeasibilities)
      {
        foreach (var m in i.infeasibilityInfoes)
        {
          Console.WriteLine(String.Format(formatLine,
                        m.dimId, m.Message,
                        string.Format("{0:0.00}", m.Limit),
                        string.Format("{0:0.00}", m.Value),
                        string.Format("{0:0.00}", m.Count),
                        i.taskId));
        }
      }
    }
  }
  public static void printCompartmentSummary(Ivr8.Model model, Ivr8.SolutionResponse solution)
  {
    Console.WriteLine("\nCompartment Summary:");
    // for each vehicle, for each capacitated dimension create a template to populate for each column.
    // technically... we should be pulling the master list of compartments from the model, not the solution
    // response, but this method is quite nice looking the way it doesn't have a model as input. So we're going
    // to approximate the table here.
    int[] maxchar = new int[5] {"locationId".Length,
                                "taskId".Length,
                                "jobId".Length,
                                "vehicleId".Length,
                                "compartmentId".Length}; // column widths for Stopid locationId, taskId, jobId, vehicleId, compartmentId
    foreach (var r in solution.Routes)
    {
      maxchar[3] = Math.Max(maxchar[3], r.vehicleId.Length);
      foreach (var s in r.Stops)
      {
        maxchar[0] = Math.Max(maxchar[0], s.locationId.Length);
        maxchar[1] = Math.Max(maxchar[1], s.taskId.Length);
        maxchar[2] = Math.Max(maxchar[2], s.jobId.Length);
        maxchar[4] = Math.Max(maxchar[4], s.compartmentId.Length);
      }
    }


    Dictionary<string, Ivr8.CompartmentSet> vehicleCset = new Dictionary<string, Ivr8.CompartmentSet>();
    foreach (var vc in model.vehicleClasses)
    {
      if (vc.compartmentSetId != "")
      {
        foreach (var v in model.Vehicles)
        {
          if (v.classId == v.classId)
          {
            vehicleCset[v.Id] = model.compartmentSets.Where(t => t.Id == vc.compartmentSetId).First();
          }
        }
      }
    }
    foreach (var v in model.Vehicles)
    {
      if (v.compartmentSetId != "")
      {
        // then this overrides anything at the class level
        vehicleCset[v.Id] = model.compartmentSets.Where(t => t.Id == v.compartmentSetId).First();
      }
    }
    Dictionary<string, Ivr8.Compartment> compartmentLookup = new Dictionary<string, Ivr8.Compartment>();
    foreach (var c in model.Compartments)
    {
      compartmentLookup.Add(c.Id, c);
    }

    foreach (var r in solution.Routes)
    {
      if (r.Stops.Count > 2)
      {
        string vid = r.vehicleId;
        Dictionary<string, float> compartmentValues = new Dictionary<string, float>();
        var cset = vehicleCset[vid];
        foreach (var comp in cset.compartmentIds)
        {
          compartmentValues.Add(comp, 0);
        }
        HashSet<string> activeCaps = new HashSet<string>();
        foreach (var c in cset.compartmentIds)
        {
          foreach (var capdim in compartmentLookup[c].Capacities)
          {
            activeCaps.Add(capdim.dimensionId);
          }
        }

        foreach (var dim in activeCaps)
        {
          Console.WriteLine("Vehicle: " + vid + " dimension:" + dim);
          string formatLine = "|{0,-8}|";
          int nc = cset.compartmentIds.Count;
          object[] fargs = new object[nc + 4];
          fargs[0] = "";
          for (int i = 1; i <= nc; i++)
          {
            formatLine += ("{" + i + ",6}|");
            fargs[i] = cset.compartmentIds[i - 1];
          }
          fargs[nc + 1] = fargs[nc + 2] = fargs[nc + 3] = "";
          formatLine += ("{" + (nc + 1) + "," + maxchar[1] + "}|");  // task
          formatLine += ("{" + (nc + 2) + "," + maxchar[3] + "}|");  // vehicle
          formatLine += ("{" + (nc + 3) + ",10}|");  // dimension
          Console.WriteLine(String.Format(formatLine, fargs));
          // then we can just add a line for the compartment capacities
          fargs[0] = "capacity";
          for (int i = 0; i < nc; i++)
          {
            fargs[i + 1] = (int)compartmentLookup[cset.compartmentIds[i]].Capacities.Where(q => q.dimensionId == dim).First().capacity;
          }
          fargs[nc + 1] = "taskId";
          fargs[nc + 2] = "vehicleId";
          fargs[nc + 3] = "dimension";
          Console.WriteLine(String.Format(formatLine, fargs));

          for (int si = 1; si < r.Stops.Count - 1; si++)
          { // skip the first and last stop because we know there is no compartment interaction there.
            var s = r.Stops[si];
            if (s.compartmentId != "")
            {
              foreach (var a in s.Attributes)
              {
                if (a.dimId == dim)
                {
                  compartmentValues[s.compartmentId] += (a.endValue - a.startValue);
                }
              }
            }
            // print the row after each update over the attributes.
            fargs[0] = "stop." + si;
            for (int i = 1; i <= nc; i++)
            {
              fargs[i] = (int)(compartmentValues[cset.compartmentIds[i - 1]]);
            }
            fargs[nc + 1] = s.taskId;
            fargs[nc + 2] = vid;
            fargs[nc + 3] = dim;
            Console.WriteLine(String.Format(formatLine, fargs));
          }
        }
      }
    }
  }

  // Repeat a value to a certain count
  public static List<int> Rep(int value, int count)
  {
    List<int> res = new List<int>(count);
    for (int i = 0; i < count; i++)
    {
      res.Add(value);
    }
    return res;
  }

  // Create a sequence [from:to) (exclusive of the to)
  // assumes you're not going to call this with [-int.MinValue, int.MaxValue] :-p
  public static List<int> Seq(int from, int to)
  {
    List<int> res = new List<int>();
    while (from != to)
    {
      res.Add(from);
      from++;
    }
    return res;
  }

}