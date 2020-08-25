package icepackai;

import java.util.*;

import icepackai.IVR7.Ivr7Kt461V8Eoaif;
import icepackai.IVR7.Ivr7Kt461V8Eoaif.InternalDimension.eMeasurementUnit;

import dnl.utils.text.table.TextTable;

// some helper functions for building ivr7 models.
public class ivr7helper {
  public static void makeDistanceTimeCapDims(Ivr7Kt461V8Eoaif.Model.Builder m) {
    Ivr7Kt461V8Eoaif.DimensionConfiguration.Builder dimBuilder =
        Ivr7Kt461V8Eoaif.DimensionConfiguration.newBuilder();

    dimBuilder.setTimeConfig(Ivr7Kt461V8Eoaif.InternalDimension.newBuilder()
                                 .setId("time")
                                 .setMeasurementUnit(eMeasurementUnit.MINUTES)
                                 .setSlackMax(1e6f)
                                 .setTardyMax(0)
                                 .build());

    dimBuilder.setDistanceConfig(Ivr7Kt461V8Eoaif.InternalDimension.newBuilder()
                                     .setId("distance")
                                     .setMeasurementUnit(eMeasurementUnit.KILOMETRES)
                                     .setSlackMax(0)
                                     .setTardyMax(0)
                                     .build());

    dimBuilder.addCapacityDimensions(Ivr7Kt461V8Eoaif.CapacityDimension.newBuilder()
                                         .setId("capacity")
                                         .setUnits("kg")
                                         .setSlackMax(0)
                                         .setTardyMax(0)
                                         .build());

    m.setDimensions(dimBuilder.build());
  }

  public static void makeLocations(
      Ivr7Kt461V8Eoaif.Model.Builder m, List<dataRow> d, float windowStart, float windowEnd) {
    for (int i = 0; i < d.size(); i++) {
      dataRow loc = d.get(i);
      if (windowEnd == 0.0f && windowStart == 0.0f) {
        m.addLocations(
            Ivr7Kt461V8Eoaif.Location.newBuilder()
                .setId(loc.id)
                .setGeocode(
                    Ivr7Kt461V8Eoaif.Geocode.newBuilder().setLatitude(loc.Y).setLongitude(loc.X))
                .build());
      } else {
        m.addLocations(
            Ivr7Kt461V8Eoaif.Location.newBuilder()
                .setId(loc.id)
                .setGeocode(
                    Ivr7Kt461V8Eoaif.Geocode.newBuilder().setLatitude(loc.Y).setLongitude(loc.X))
                .addAttributes(
                    Ivr7Kt461V8Eoaif.Location.Attribute.newBuilder()
                        .setDimensionId("time")
                        .addArrivalWindows(
                            Ivr7Kt461V8Eoaif.Window.newBuilder().setStart(8 * 60f).setEnd(
                                14 * 60f)))
                .build());
      }
    }
  }

  public static void makeLocations(Ivr7Kt461V8Eoaif.Model.Builder m, List<dataRow> d) {
    makeLocations(m, d, 0.0f, 0.0f);
  }

  public static void makeJobTimeCap(Ivr7Kt461V8Eoaif.Model.Builder m, List<dataRow> d,
      List<Integer> srcs, List<Integer> dests) throws Exception {
    if (srcs.size() != dests.size()) {
      throw new Exception("Expected srcs.Count == dests.Count");
    }
    for (int i = 0; i < srcs.size(); i++) {
      Integer si = srcs.get(i);
      Integer di = dests.get(i);
      Ivr7Kt461V8Eoaif.Job.Builder job = Ivr7Kt461V8Eoaif.Job.newBuilder();
      job.setId("job_" + d.get(di).id);
      job.setPickupTask(Ivr7Kt461V8Eoaif.Job.Task.newBuilder()
                            .setTaskId("pickup_" + d.get(di).id)
                            .setLocationId(d.get(si).id)
                            .addAttributes(Ivr7Kt461V8Eoaif.Job.Task.Attribute.newBuilder()
                                               .setDimensionId("time")
                                               .setQuantity(d.get(di).pickupTime)
                                               .build())
                            .addAttributes(Ivr7Kt461V8Eoaif.Job.Task.Attribute.newBuilder()
                                               .setDimensionId("capacity")
                                               .setQuantity(d.get(di).quantity)
                                               .build())
                            .build());

      job.setDropoffTask(Ivr7Kt461V8Eoaif.Job.Task.newBuilder()
                             .setTaskId("dropoff_" + d.get(di).id)
                             .setLocationId(d.get(di).id)
                             .addAttributes(Ivr7Kt461V8Eoaif.Job.Task.Attribute.newBuilder()
                                                .setDimensionId("time")
                                                .setQuantity(d.get(di).dropoffTime)
                                                .build())
                             .addAttributes(Ivr7Kt461V8Eoaif.Job.Task.Attribute.newBuilder()
                                                .setDimensionId("capacity")
                                                .setQuantity(-d.get(di).quantity)
                                                .build())
                             .build());
      job.setPenalty(10000f);
      m.addJobs(job.build());
    }
  }

  public static Ivr7Kt461V8Eoaif.VehicleCostClass makeVccSimple(String name, float fixedcost,
      float time_transit_costcoef, float time_loc_costcoef, float time_task_costcoef,
      float time_slack_costcoef, float distance_transit_costcoef) {
    return Ivr7Kt461V8Eoaif.VehicleCostClass.newBuilder()
        .setId(name)
        .setFixedCost(fixedcost)
        .addAttributes(Ivr7Kt461V8Eoaif.VehicleCostClass.Attribute.newBuilder()
                           .setDimensionId("time")
                           .setTransitCostCoef(time_transit_costcoef)
                           .setTaskCostCoef(time_task_costcoef)
                           .setLocationCostCoef(time_loc_costcoef)
                           .setSlackCostCoef(time_slack_costcoef)
                           .build())
        .addAttributes(Ivr7Kt461V8Eoaif.VehicleCostClass.Attribute.newBuilder()
                           .setDimensionId("distance")
                           .setTransitCostCoef(distance_transit_costcoef)
                           .build())
        .build();
  }

  public static Ivr7Kt461V8Eoaif.VehicleClass makeVcSimple(String name, float time_transit_coef,
      float time_task_coef, float time_loc_coef, float distance_transit_coef) {
    return Ivr7Kt461V8Eoaif.VehicleClass.newBuilder()
        .setId(name)
        .addAttributes(Ivr7Kt461V8Eoaif.VehicleClass.Attribute.newBuilder()
                           .setDimensionId("time")
                           .setTransitGeneratorId("roadnetwork_time")
                           .setTransitCoef(time_transit_coef)
                           .setTaskCoef(time_task_coef)
                           .setLocationCoef(time_loc_coef)
                           .build())
        .addAttributes(Ivr7Kt461V8Eoaif.VehicleClass.Attribute.newBuilder()
                           .setDimensionId("distance")
                           .setTransitGeneratorId("roadnetwork_distance")
                           .setTransitCoef(distance_transit_coef)
                           .build())
        .build();
  }

  public static Ivr7Kt461V8Eoaif.Vehicle makeVehicleCap(String name, String vehicleClass,
      String vehicleCostClass, float capacity, String startLocation, String endLocation,
      float startTime, float endTime) {
    return Ivr7Kt461V8Eoaif.Vehicle.newBuilder()
        .setId(name)
        .setClassId(vehicleClass)
        .setCostClassId(vehicleCostClass)
        .setShift(
            Ivr7Kt461V8Eoaif.Vehicle.Shift.newBuilder()
                .setShiftStart(
                    Ivr7Kt461V8Eoaif.Vehicle.Task.newBuilder()
                        .setLocationId(startLocation)
                        .addAttributes(
                            Ivr7Kt461V8Eoaif.Vehicle.Task.Attribute.newBuilder()
                                .setDimensionId("time")
                                .setQuantity(0.0f)
                                .addWindows(
                                    Ivr7Kt461V8Eoaif.Window.newBuilder().setStart(startTime).setEnd(
                                        endTime))
                                .build())
                        .build())
                .setShiftEnd(Ivr7Kt461V8Eoaif.Vehicle.Task.newBuilder()
                                 .setLocationId(endLocation)
                                 .addAttributes(Ivr7Kt461V8Eoaif.Vehicle.Task.Attribute.newBuilder()
                                                    .setDimensionId("time")
                                                    // then just add the time windows to the shift
                                                    .setQuantity(0.0f)
                                                    .addWindows(Ivr7Kt461V8Eoaif.Window.newBuilder()
                                                                    .setStart(startTime)
                                                                    .setEnd(endTime))
                                                    .build())
                                 .build())
                .build())
        .addCapacities(Ivr7Kt461V8Eoaif.Vehicle.Capacity.newBuilder()
                           .setDimensionId("capacity")
                           .setCapacity(capacity)
                           .build())
        .build();
  }

  public static Ivr7Kt461V8Eoaif.TransitRule makeLunchBreakRule(
      String name, String prefix, float lunchtime, float breaklength) {
    return Ivr7Kt461V8Eoaif.TransitRule.newBuilder()
        .setId(name)
        .setDimensionId("time")
        .setRuleIdPrefix(prefix)
        .setUseStandingState(true)
        .setUseTransitState(true)
        .addTriggers(
            Ivr7Kt461V8Eoaif.TransitRule.Trigger.newBuilder().setValue(lunchtime).setQuantity(
                breaklength))
        .build();
  }

  public static void printSolution(Ivr7Kt461V8Eoaif.SolutionResponse solution, Boolean printnodes,
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
    };
    if (printnodes) {
      String[] columnNames = {"stopId", "locationId", "taskId", "jobId", "vehicleId"};
      List<Object[]> tabData = new ArrayList<Object[]>();

      for (int i = 0; i < solution.getRoutesCount(); i++) {
        Ivr7Kt461V8Eoaif.SolutionResponse.Route r = solution.getRoutes(i);
        if (r.getStopsCount() > 2) {
          for (int j = 1; j < r.getStopsCount() - 1; j++) {
            Ivr7Kt461V8Eoaif.SolutionResponse.Stop s = r.getStops(j);
            tabData.add(new Object[] {
                s.getId(), s.getLocationId(), s.getTaskId(), s.getJobId(), r.getVehicleId()});
          }
          if (i != solution.getRoutesCount() - 1) {
            tabData.add(new Object[] {"", "", "", "", ""});
          }
        }
      }
      printTable(tabData, columnNames);
    }
    HashSet<String> dims = new HashSet<String>();
    for (Ivr7Kt461V8Eoaif.SolutionResponse.Route r : solution.getRoutesList()) {
      for (Ivr7Kt461V8Eoaif.SolutionResponse.Stop s : r.getStopsList()) {
        for (Ivr7Kt461V8Eoaif.SolutionResponse.StopAttribute a : s.getAttributesList()) {
          dims.add(a.getDimId());
        }
      }
    }

    Boolean hasTrRules = false;
    Map<String, HashMap<String, agg>> vehicleStats = new HashMap<String, HashMap<String, agg>>();

    for (Ivr7Kt461V8Eoaif.SolutionResponse.Route r : solution.getRoutesList()) {
      vehicleStats.put(r.getVehicleId(), new HashMap<String, agg>());
      for (String dim : dims) {
        vehicleStats.get(r.getVehicleId()).put(dim, new agg());
      }
      hasTrRules = hasTrRules || r.getTransitRuleAttributesCount() > 0;

      for (Ivr7Kt461V8Eoaif.SolutionResponse.Stop s : r.getStopsList()) {
        for (Ivr7Kt461V8Eoaif.SolutionResponse.StopAttribute a : s.getAttributesList()) {
          vehicleStats.get(r.getVehicleId())
              .get(a.getDimId())
              .add(a.getEndValue() - a.getStartValue(), a.getCost());
        }
      }

      for (Ivr7Kt461V8Eoaif.SolutionResponse.InterStop s : r.getInterStopsList()) {
        for (Ivr7Kt461V8Eoaif.SolutionResponse.InterStopAttribute a : s.getAttributesList()) {
          vehicleStats.get(r.getVehicleId())
              .get(a.getDimId())
              .add(a.getEndValue() - a.getStartValue(), a.getCost());
        }
      }
    }

    if (printaggstats) {
      System.out.println("\nVehilce dimension summary");
      String[] columnNames = {"vehicleId", "dimension", "Min", "Max", "Cumul", "Cost"};
      List<Object[]> tabData = new ArrayList<Object[]>();
      for (Map.Entry<String, HashMap<String, agg>> vSet : vehicleStats.entrySet()) {
        for (Map.Entry<String, agg> stat : vSet.getValue().entrySet()) {
          agg a = stat.getValue();
          if (a.min != 1e30f) {
            tabData.add(
                new Object[] {vSet.getKey(), stat.getKey(), a.min, a.max, a.cumul, a.totalCost});
          }
        }
      }
      printTable(tabData, columnNames);
    }

    if (hasTrRules && printTrrules) {
      System.out.println("\nVehilce transit rule summary:");
      String[] columnNames = new String[] {"vehicleId", "ruleId", "dimension", "fromStopId",
          "toStopId", "startValue", "endValue", "cost"};
      List<Object[]> tabData = new ArrayList<Object[]>();
      for (Ivr7Kt461V8Eoaif.SolutionResponse.Route r : solution.getRoutesList()) {
        for (Ivr7Kt461V8Eoaif.SolutionResponse.TransitRuleAttribute tr :
            r.getTransitRuleAttributesList()) {
          tabData.add(
              new Object[] {r.getVehicleId(), tr.getRuleId(), tr.getDimId(), tr.getFromStopId(),
                  tr.getToStopId(), tr.getStartValue(), tr.getEndValue(), tr.getCost()});
        }
      }
      printTable(tabData, columnNames);
    }

    if (printInfeas) {
      System.out.println("\nInfeasibilities:");
      String[] columnNames =
          new String[] {"dimenion", "message", "limit", "value", "count", "taskId"};
      List<Object[]> tabData = new ArrayList<Object[]>();
      for (Ivr7Kt461V8Eoaif.SolutionResponse.Infeasibility i : solution.getInfeasibilitiesList()) {
        for (Ivr7Kt461V8Eoaif.SolutionResponse.Infeasibility.Info m :
            i.getInfeasibilityInfoList()) {
          tabData.add(new Object[] {m.getDimId(), m.getMessage(), m.getLimit(), m.getValue(),
              m.getCount(), i.getTaskId()});
        }
      }
      printTable(tabData, columnNames);
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