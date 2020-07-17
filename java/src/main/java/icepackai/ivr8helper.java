package icepackai;

import java.util.*;

import icepackai.IVR8.Ivr8Yni1C9K2Swof;
import icepackai.IVR8.Ivr8Yni1C9K2Swof.InternalDimension.eMeasurementUnit;

import dnl.utils.text.table.TextTable;

// some helper functions for building ivr8 models.
public class ivr8helper {
  public static void makeDistanceTimeCapDims(Ivr8Yni1C9K2Swof.Model.Builder m) {
    Ivr8Yni1C9K2Swof.DimensionConfiguration.Builder dimBuilder = Ivr8Yni1C9K2Swof.DimensionConfiguration.newBuilder();

    dimBuilder.setTimeConfig(Ivr8Yni1C9K2Swof.InternalDimension.newBuilder().setId("time")
        .setMeasurementUnit(eMeasurementUnit.MINUTES).setSlackMax(1e6f).setTardyMax(0).build());

    dimBuilder.setDistanceConfig(Ivr8Yni1C9K2Swof.InternalDimension.newBuilder().setId("distance")
        .setMeasurementUnit(eMeasurementUnit.KILOMETRES).setSlackMax(0).setTardyMax(0).build());

    dimBuilder.addCapacityDimensions(Ivr8Yni1C9K2Swof.CapacityDimension.newBuilder().setId("capacity").setUnits("kg")
        .setSlackMax(0).setTardyMax(0).build());

    m.setDimensions(dimBuilder.build());
  }

  public static void makeLocations(Ivr8Yni1C9K2Swof.Model.Builder m, List<dataRow> d, float windowStart,
      float windowEnd) {
    for (int i = 0; i < d.size(); i++) {
      dataRow loc = d.get(i);
      if (windowEnd == 0.0f && windowStart == 0.0f) {
        m.addLocations(Ivr8Yni1C9K2Swof.Location.newBuilder().setId(loc.id)
            .setGeocode(Ivr8Yni1C9K2Swof.Geocode.newBuilder().setLatitude(loc.Y).setLongitude(loc.X)).build());
      } else {
        m.addLocations(
            Ivr8Yni1C9K2Swof.Location.newBuilder().setId(loc.id)
                .setGeocode(Ivr8Yni1C9K2Swof.Geocode.newBuilder().setLatitude(loc.Y).setLongitude(loc.X))
                .addAttributes(Ivr8Yni1C9K2Swof.Location.Attribute.newBuilder().setDimensionId("time")
                    .addArrivalWindows(Ivr8Yni1C9K2Swof.Window.newBuilder().setStart(8 * 60f).setEnd(14 * 60f)))
                .build());
      }
    }
  }

  public static void makeLocations(Ivr8Yni1C9K2Swof.Model.Builder m, List<dataRow> d) {
    makeLocations(m, d, 0.0f, 0.0f);
  }

  public static void makeJobTimeCap(Ivr8Yni1C9K2Swof.Model.Builder m, List<dataRow> d, List<Integer> srcs,
      List<Integer> dests) throws Exception {
    if (srcs.size() != dests.size()) {
      throw new Exception("Expected srcs.Count == dests.Count");
    }
    for (int i = 0; i < srcs.size(); i++) {
      Integer si = srcs.get(i);
      Integer di = dests.get(i);
      Ivr8Yni1C9K2Swof.Job.Builder job = Ivr8Yni1C9K2Swof.Job.newBuilder();
      job.setId("job_" + d.get(di).id);
      job.setPickupTask(
          Ivr8Yni1C9K2Swof.Job.Task.newBuilder().setTaskId("pickup_" + d.get(di).id).setLocationId(d.get(si).id)
              .addAttributes(Ivr8Yni1C9K2Swof.Job.Task.Attribute.newBuilder().setDimensionId("time")
                  .setQuantity(d.get(di).pickupTime).build())
              .addAttributes(Ivr8Yni1C9K2Swof.Job.Task.Attribute.newBuilder().setDimensionId("capacity")
                  .setQuantity(d.get(di).quantity).build())
              .build());

      job.setDropoffTask(
          Ivr8Yni1C9K2Swof.Job.Task.newBuilder().setTaskId("dropoff_" + d.get(di).id).setLocationId(d.get(di).id)
              .addAttributes(Ivr8Yni1C9K2Swof.Job.Task.Attribute.newBuilder().setDimensionId("time")
                  .setQuantity(d.get(di).dropoffTime).build())
              .addAttributes(Ivr8Yni1C9K2Swof.Job.Task.Attribute.newBuilder().setDimensionId("capacity")
                  .setQuantity(-d.get(di).quantity).build())
              .build());
      job.setPenalty(10000f);
      m.addJobs(job.build());
    }
  }

  public static Ivr8Yni1C9K2Swof.VehicleCostClass makeVccSimple(String name, float fixedcost,
      float time_transit_costcoef, float time_loc_costcoef, float time_task_costcoef, float time_slack_costcoef,
      float distance_transit_costcoef) {
    return Ivr8Yni1C9K2Swof.VehicleCostClass.newBuilder().setId(name).setFixedCost(fixedcost)
        .addAttributes(Ivr8Yni1C9K2Swof.VehicleCostClass.Attribute.newBuilder().setDimensionId("time")
            .setTransitCostCoef(time_transit_costcoef).setTaskCostCoef(time_task_costcoef)
            .setLocationCostCoef(time_loc_costcoef).setSlackCostCoef(time_slack_costcoef).build())
        .addAttributes(Ivr8Yni1C9K2Swof.VehicleCostClass.Attribute.newBuilder().setDimensionId("distance")
            .setTransitCostCoef(distance_transit_costcoef).build())
        .build();
  }

  public static Ivr8Yni1C9K2Swof.VehicleClass makeVcSimple(String name, float time_transit_coef, float time_task_coef,
      float time_loc_coef, float distance_transit_coef) {
    return Ivr8Yni1C9K2Swof.VehicleClass.newBuilder().setId(name)
        .addAttributes(Ivr8Yni1C9K2Swof.VehicleClass.Attribute.newBuilder().setDimensionId("time")
            .setTransitGeneratorId("roadnetwork_time").setTransitCoef(time_transit_coef).setTaskCoef(time_task_coef)
            .setLocationCoef(time_loc_coef).build())
        .addAttributes(Ivr8Yni1C9K2Swof.VehicleClass.Attribute.newBuilder().setDimensionId("distance")
            .setTransitGeneratorId("roadnetwork_distance").setTransitCoef(distance_transit_coef).build())
        .build();
  }

  public static Ivr8Yni1C9K2Swof.Vehicle makeVehicleCap(String name, String vehicleClass, String vehicleCostClass,
      float capacity, String startLocation, String endLocation, float startTime, float endTime) {
    return Ivr8Yni1C9K2Swof.Vehicle.newBuilder().setId(name).setClassId(vehicleClass).setCostClassId(vehicleCostClass)
        .setShift(Ivr8Yni1C9K2Swof.Vehicle.Shift.newBuilder().setShiftStart(Ivr8Yni1C9K2Swof.Vehicle.Task.newBuilder()
            .setLocationId(startLocation)
            .addAttributes(Ivr8Yni1C9K2Swof.Vehicle.Task.Attribute.newBuilder().setDimensionId("time").setQuantity(0.0f)
                .addWindows(Ivr8Yni1C9K2Swof.Window.newBuilder().setStart(startTime).setEnd(endTime)).build())
            .build())
            .setShiftEnd(Ivr8Yni1C9K2Swof.Vehicle.Task.newBuilder().setLocationId(endLocation)
                .addAttributes(Ivr8Yni1C9K2Swof.Vehicle.Task.Attribute.newBuilder().setDimensionId("time")
                    // then just add the time windows to the shift
                    .setQuantity(0.0f)
                    .addWindows(Ivr8Yni1C9K2Swof.Window.newBuilder().setStart(startTime).setEnd(endTime)).build())
                .build())
            .build())
        .addCapacities(
            Ivr8Yni1C9K2Swof.Vehicle.Capacity.newBuilder().setDimensionId("capacity").setCapacity(capacity).build())
        .build();
  }

  public static Ivr8Yni1C9K2Swof.TransitRule makeLunchBreakRule(String name, String prefix, float lunchtime,
      float breaklength) {
    return Ivr8Yni1C9K2Swof.TransitRule.newBuilder().setId(name).setDimensionId("time").setRuleIdPrefix(prefix)
        .setUseStandingState(true).setUseTransitState(true)
        .addTriggers(Ivr8Yni1C9K2Swof.TransitRule.Trigger.newBuilder().setValue(lunchtime).setQuantity(breaklength))
        .build();
  }

  public static void printSolution(Ivr8Yni1C9K2Swof.SolutionResponse solution, Boolean printnodes,
      Boolean printaggstats, Boolean printTrrules, Boolean printInfeas) {

    class agg {
      public void add(float delta, float cost) {
        cumul += delta;
        min = Math.min(cumul, min);
        max = Math.max(cumul, max);
        totalCost += cost;
      }

      public float min = (float) 1e30;
      public float max = 0;
      public float cumul = 0;
      public float totalCost = 0;
    }
    ;
    if (printnodes) {
      String[] columnNames = { "stopId", "locationId", "taskId", "jobId", "vehicleId", "compartmentId" };
      List<Object[]> tabData = new ArrayList<Object[]>();

      for (int i = 0; i < solution.getRoutesCount(); i++) {
        Ivr8Yni1C9K2Swof.SolutionResponse.Route r = solution.getRoutes(i);
        if (r.getStopsCount() > 2) {
          for (int j = 1; j < r.getStopsCount() - 1; j++) {
            Ivr8Yni1C9K2Swof.SolutionResponse.Stop s = r.getStops(j);
            tabData.add(new Object[] { s.getId(), s.getLocationId(), s.getTaskId(), s.getJobId(), r.getVehicleId(),
                s.getCompartmentId() });
          }
          if (i != solution.getRoutesCount() - 1) {
            tabData.add(new Object[] { "", "", "", "", "", "" });
          }
        }
      }
      printTable(tabData, columnNames);
    }
    HashSet<String> dims = new HashSet<String>();
    for (Ivr8Yni1C9K2Swof.SolutionResponse.Route r : solution.getRoutesList()) {
      for (Ivr8Yni1C9K2Swof.SolutionResponse.Stop s : r.getStopsList()) {
        for (Ivr8Yni1C9K2Swof.SolutionResponse.StopAttribute a : s.getAttributesList()) {
          dims.add(a.getDimId());
        }
      }
    }

    Boolean hasTrRules = false;
    Map<String, HashMap<String, agg>> vehicleStats = new HashMap<String, HashMap<String, agg>>();

    for (Ivr8Yni1C9K2Swof.SolutionResponse.Route r : solution.getRoutesList()) {
      vehicleStats.put(r.getVehicleId(), new HashMap<String, agg>());
      for (String dim : dims) {
        vehicleStats.get(r.getVehicleId()).put(dim, new agg());
      }
      hasTrRules = hasTrRules || r.getTransitRuleAttributesCount() > 0;

      for (Ivr8Yni1C9K2Swof.SolutionResponse.Stop s : r.getStopsList()) {
        for (Ivr8Yni1C9K2Swof.SolutionResponse.StopAttribute a : s.getAttributesList()) {
          vehicleStats.get(r.getVehicleId()).get(a.getDimId()).add(a.getEndValue() - a.getStartValue(), a.getCost());
        }
      }

      for (Ivr8Yni1C9K2Swof.SolutionResponse.InterStop s : r.getInterStopsList()) {
        for (Ivr8Yni1C9K2Swof.SolutionResponse.InterStopAttribute a : s.getAttributesList()) {
          vehicleStats.get(r.getVehicleId()).get(a.getDimId()).add(a.getEndValue() - a.getStartValue(), a.getCost());
        }
      }
    }

    if (printaggstats) {
      System.out.println("\nVehilce dimension summary");
      String[] columnNames = { "vehicleId", "dimension", "Min", "Max", "Cumul", "Cost" };
      List<Object[]> tabData = new ArrayList<Object[]>();
      for (Map.Entry<String, HashMap<String, agg>> vSet : vehicleStats.entrySet()) {
        for (Map.Entry<String, agg> stat : vSet.getValue().entrySet()) {
          agg a = stat.getValue();
          if (a.min != 1e30f) {
            tabData.add(new Object[] { vSet.getKey(), stat.getKey(), a.min, a.max, a.cumul, a.totalCost });
          }
        }
      }
      printTable(tabData, columnNames);
    }

    if (hasTrRules && printTrrules) {
      System.out.println("\nVehilce transit rule summary:");
      String[] columnNames = new String[] { "vehicleId", "ruleId", "dimension", "fromStopId", "toStopId", "startValue",
          "endValue", "cost" };
      List<Object[]> tabData = new ArrayList<Object[]>();
      for (Ivr8Yni1C9K2Swof.SolutionResponse.Route r : solution.getRoutesList()) {
        for (Ivr8Yni1C9K2Swof.SolutionResponse.TransitRuleAttribute tr : r.getTransitRuleAttributesList()) {
          tabData.add(new Object[] { r.getVehicleId(), tr.getRuleId(), tr.getDimId(), tr.getFromStopId(),
              tr.getToStopId(), tr.getStartValue(), tr.getEndValue(), tr.getCost() });
        }
      }
      printTable(tabData, columnNames);
    }

    if (printInfeas) {
      System.out.println("\nInfeasibilities:");
      String[] columnNames = new String[] { "dimenion", "message", "limit", "value", "count", "taskId" };
      List<Object[]> tabData = new ArrayList<Object[]>();
      for (Ivr8Yni1C9K2Swof.SolutionResponse.Infeasibility i : solution.getInfeasibilitiesList()) {
        for (Ivr8Yni1C9K2Swof.SolutionResponse.Infeasibility.Info m : i.getInfeasibilityInfoList()) {
          tabData.add(
              new Object[] { m.getDimId(), m.getMessage(), m.getLimit(), m.getValue(), m.getCount(), i.getTaskId() });
        }
      }
      printTable(tabData, columnNames);
    }

  }

  public static void printCompartmentSummary(Ivr8Yni1C9K2Swof.Model model, Ivr8Yni1C9K2Swof.SolutionResponse solution) {
    System.out.println("\nCompartment Summary:");
    // for each vehicle, for each capacitated dimension create a template to
    // populate for each column.
    // technically... we should be pulling the master list of compartments from the
    // model, not the solution
    // response, but this method is quite nice looking the way it doesn't have a
    // model as input. So we're going
    // to approximate the table here.
    String[] columnNames = new String[] { "locationId", "taskId", "jobId", "vehicleId", "compartmentId" };

    Map<String, Ivr8Yni1C9K2Swof.CompartmentSet> vehicleCset = new HashMap<String, Ivr8Yni1C9K2Swof.CompartmentSet>();
    for (Ivr8Yni1C9K2Swof.VehicleClass vc : model.getVehicleClassesList()) {
      if (vc.getCompartmentSetId() != "") {
        for (Ivr8Yni1C9K2Swof.Vehicle v : model.getVehiclesList()) {
          if (v.getClassId() == vc.getId()) {
            for (Ivr8Yni1C9K2Swof.CompartmentSet cset : model.getCompartmentSetsList()) {
              if (cset.getId() == vc.getCompartmentSetId()) {
                vehicleCset.put(v.getId(), cset);
              }
            }
          }
        }
      }
    }
    for (Ivr8Yni1C9K2Swof.Vehicle v : model.getVehiclesList()) {
      if (v.getCompartmentSetId() != "") {
        // then this overrides anything at the class level
        for (Ivr8Yni1C9K2Swof.CompartmentSet cset : model.getCompartmentSetsList()) {
          if (cset.getId() == v.getCompartmentSetId()) {
            vehicleCset.put(v.getId(), cset);
          }
        }
      }
    }
    Map<String, Ivr8Yni1C9K2Swof.Compartment> compartmentLookup = new HashMap<String, Ivr8Yni1C9K2Swof.Compartment>();
    for (Ivr8Yni1C9K2Swof.Compartment c : model.getCompartmentsList()) {
      compartmentLookup.put(c.getId(), c);
    }

    for (Ivr8Yni1C9K2Swof.SolutionResponse.Route r : solution.getRoutesList()) {
      if (r.getStopsCount() > 2) {
        String vid = r.getVehicleId();
        Map<String, Float> compartmentValues = new HashMap<String, Float>();
        Ivr8Yni1C9K2Swof.CompartmentSet cset = vehicleCset.get(vid);
        for (String comp : cset.getCompartmentIdsList()) {
          compartmentValues.put(comp, 0.0f);
        }
        HashSet<String> activeCaps = new HashSet<String>();
        for (String c : cset.getCompartmentIdsList()) {
          for (Ivr8Yni1C9K2Swof.Compartment.Capacity capdim : compartmentLookup.get(c).getCapacitiesList()) {
            activeCaps.add(capdim.getDimensionId());
          }
        }

        for (String dim : activeCaps) {
          System.out.println("Vehicle: " + vid + " dimension:" + dim);
          int nc = cset.getCompartmentIdsCount();
          List<Object[]> tabData = new ArrayList<Object[]>();
          columnNames = new String[nc + 4];
          columnNames[0] = "";
          for (int i = 1; i <= nc; i++) {
            columnNames[i] = cset.getCompartmentIds(i - 1);
          }
          columnNames[nc + 1] = columnNames[nc + 2] = columnNames[nc + 3] = "";

          Object[] toprow = new Object[nc + 4];
          // Console.WriteLine(String.Format(formatLine, toprow));
          // then we can just add a line for the compartment capacities
          toprow[0] = "capacity";
          for (int i = 0; i < nc; i++) {
            for (Ivr8Yni1C9K2Swof.Compartment.Capacity cap : compartmentLookup.get(cset.getCompartmentIds(i))
                .getCapacitiesList()) {
              if (cap.getDimensionId() == dim) {
                toprow[i + 1] = cap.getCapacity();
              }
            }
          }
          toprow[nc + 1] = "taskId";
          toprow[nc + 2] = "vehicleId";
          toprow[nc + 3] = "dimension";
          tabData.add(toprow);

          for (int si = 1; si < r.getStopsCount() - 1; si++) { // skip the first and last stop because we know there is
                                                               // no compartment interaction there.
            Ivr8Yni1C9K2Swof.SolutionResponse.Stop s = r.getStops(si);
            if (s.getCompartmentId() != "") {
              for (Ivr8Yni1C9K2Swof.SolutionResponse.StopAttribute a : s.getAttributesList()) {
                if (dim.compareTo(a.getDimId()) == 0) { // so java is a bit sticky about string comparisons
                                                        // this is the bullet proof way to compare apparently
                  float nv = compartmentValues.get(s.getCompartmentId()) + (a.getEndValue() - a.getStartValue());
                  compartmentValues.put(s.getCompartmentId(), nv);
                }
              }
            }
            // print the row after each update over the attributes.
            Object[] row = new Object[nc + 4];
            row[0] = "stop." + si;
            for (int i = 1; i <= nc; i++) {
              row[i] = (compartmentValues.get(cset.getCompartmentIds(i - 1)));
            }
            row[nc + 1] = s.getTaskId();
            row[nc + 2] = vid;
            row[nc + 3] = dim;
            tabData.add(row);
          }
          printTable(tabData, columnNames);
        }
      }
    }
  }

  private static void printTable(List<Object[]> tabData, String[] columnNames) {
    Object[][] tab = new Object[tabData.size()][];
    for (int i = 0; i < tabData.size(); i++) {
      tab[i] = tabData.get(i);
    }
    TextTable tt = new TextTable(columnNames, tab);
    tt.printTable();
  }

  // Repeat a value to a certain count
  public static List<Integer> Rep(Integer value, Integer count) {
    List<Integer> res = new ArrayList<Integer>();
    for (int i = 0; i < count; i++) {
      res.add(value);
    }
    return res;
  }

  // Create a sequence [from:to) (exclusive of the to)
  // assumes you're not going to call this with [-int.MinValue, int.MaxValue] :-p
  public static List<Integer> Seq(Integer from, Integer to) {
    List<Integer> res = new ArrayList<Integer>();
    while (from != to) {
      res.add(from);
      from++;
    }
    return res;
  }

}