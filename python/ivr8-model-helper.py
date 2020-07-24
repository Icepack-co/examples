# a simple class which defines some common functions for working with the ivr8 series of models

def make_distance_time_cap_dims():
  dimensions = ivr8_yni1c9k2swof_pb2.DimensionConfiguration()
  dimensions.timeConfig.id = 'time'
  dimensions.timeConfig.measurementUnit = 1
  dimensions.timeConfig.slackMax = 1e6
  dimensions.timeConfig.tardyMax = 0
  dimensions.distanceConfig.id = 'distance'
  dimensions.distanceConfig.measurementUnit = 4
  dimensions.distanceConfig.slackMax = 0
  dimensions.distanceConfig.tardyMax = 0
  capdim = ivr8_yni1c9k2swof_pb2.CapacityDimension()
  capdim.id = 'capacity'
  capdim.units = 'kg'
  capdim.slackMax = 0
  capdim.tardyMax = 0
  dimensions.capacityDimensions.extend([capdim])
  return dimensions

def make_locations(data):
  locs = list()
  for index, row in data.iterrows():
    l = ivr8_yni1c9k2swof_pb2.Location()
    l.id = row.id
    l.geocode.longitude = row.X
    l.geocode.latitude = row.Y
    locs.append(l)
  return locs

# main data frame, row indexes for source and destination job combinations
def make_job_time_cap(data, src, dest):
  d = list()
  if( len(src) != len(dest)):
    sys.exit('len(src) != len(dest)')

  for index, si  in enumerate(src):
    di = dest[index]
    j = ivr8_yni1c9k2swof_pb2.Job()
    j.id = 'job_' + data['id'][di]
    j.pickupTask.taskId = "pickup_" + str(data['id'][di])
    j.pickupTask.locationId = data['id'][si]

    a_time =  ivr8_yni1c9k2swof_pb2.Job.Task.Attribute()
    a_time.dimensionId = 'time'
    a_time.quantity = data['pickupTime'][di]

    a_cap =  ivr8_yni1c9k2swof_pb2.Job.Task.Attribute()
    a_cap.dimensionId = 'capacity'
    a_cap.quantity = data['quantity'][di]

    j.pickupTask.attributes.append(a_time)
    j.pickupTask.attributes.append(a_cap)

    j.dropoffTask.taskId = "dropoff_" + str(data['id'][di])
    j.dropoffTask.locationId = data['id'][di]
    a_time.quantity = data['dropoffTime'][di]
    a_cap.quantity = -a_cap.quantity

    j.dropoffTask.attributes.append(a_time)
    j.dropoffTask.attributes.append(a_cap)

    j.penalty = 10000

    d.append(j)
  return d

def make_vcc_simple(name, fixedcost, time_transit_costcoef,
                            time_loc_costcoef,time_task_costcoef,
                            time_slack_costcoef, distance_transit_costcoef):
  vcc = ivr8_yni1c9k2swof_pb2.VehicleCostClass()
  vcc.id = name
  vcc.fixedCost = fixedcost # a fixed cost associated with using this particular vehicle
  vcc_time_cost = ivr8_yni1c9k2swof_pb2.VehicleCostClass.Attribute()
  vcc_time_cost.dimensionId = 'time'
  vcc_time_cost.transitCostCoef = time_transit_costcoef
  vcc_time_cost.taskCostCoef = time_task_costcoef
  vcc_time_cost.locationCostCoef = time_loc_costcoef
  vcc_time_cost.slackCostCoef = time_slack_costcoef #discourage being early slightly.

  vcc_dist_cost = ivr8_yni1c9k2swof_pb2.VehicleCostClass.Attribute()
  vcc_dist_cost.dimensionId = 'distance'
  vcc_dist_cost.transitCostCoef = distance_transit_costcoef # say 3 units per km. so favour shorter routes where possible.
  # a note: the default cost coefficients are zero. So you can simply specify where they are not zero.
  vcc.attributes.append(vcc_time_cost)
  vcc.attributes.append(vcc_dist_cost)
  return (vcc)

def make_vc_simple(name, time_transit_coef, time_task_coef, time_loc_coef,
                           distance_transit_coef):
  vc = ivr8_yni1c9k2swof_pb2.VehicleClass()
  vc.id = name
  vca_t = ivr8_yni1c9k2swof_pb2.VehicleClass.Attribute()
  vca_t.dimensionId = 'time'
  vca_t.transitCoef = time_transit_coef   # this would make the vehicle move 20% slower between locations if it was 1.2
  vca_t.taskCoef = time_task_coef
  vca_t.locationCoef = time_loc_coef
  vca_t.transitGeneratorId = 'roadnetwork_time' # this tells the vehicle to use the roadnetwork_time transits.
  # the reason this is separate is that one may want to provide your own
  # transit matrix which you could then link to the time-dimension.

  vca_d = ivr8_yni1c9k2swof_pb2.VehicleClass.Attribute()
  vca_d.dimensionId = 'distance'
  vca_d.transitGeneratorId = 'roadnetwork_distance' #again, just linking up to the road-network distance
  #coefficients are 1.0 by default in the attrivues.
  vca_d.transitCoef = distance_transit_coef
  vc.attributes.append(vca_t)
  vc.attributes.append(vca_d)
  return (vc)

def make_vehicle_cap(name,vehicleClass, vehicleCostClass, capacity,
                             startloc, endloc,
                             start_time, end_time):
  v = ivr8_yni1c9k2swof_pb2.Vehicle()
  v.id = name
  v.classId = vehicleClass
  v.costClassId = vehicleCostClass
  v.shift.shiftStart.locationId = startloc # we're setting the start of the vehicle to the depot
  v.shift.shiftEnd.locationId = endloc   # we're setting the end of the vehicle to the depot

  vatt = ivr8_yni1c9k2swof_pb2.Vehicle.Task.Attribute()
  vatt.dimensionId = 'time'
  vatt.quantity = 0
  w = ivr8_yni1c9k2swof_pb2.Window()
  w.start = start_time # 7 AM
  w.end = end_time # 6 PM
  vatt.windows.append(w)
  v.shift.shiftStart.attributes.append(vatt)
  v.shift.shiftEnd.attributes.append(vatt)

  # configure the individual vehicle capacity
  vcap = ivr8_yni1c9k2swof_pb2.Vehicle.Capacity()
  vcap.dimensionId = 'capacity'
  vcap.capacity = capacity
  v.capacities.append(vcap)
  return (v)

def make_lunch_break_rule(name, prefix, lunchtime, breaklength):
  tr = ivr8_yni1c9k2swof_pb2.TransitRule()
  tr.id = name
  tr.dimensionId = 'time'
  tr.useStandingState = True # sure, we would allow the execution of the rule while a vehicle is stationary
  tr.useTransitState = True # why not, if the vehicle is permitted to stop while travelling between stops then this is okay
  tr.ruleIdPrefix = prefix
  trigger =  ivr8_yni1c9k2swof_pb2.TransitRule.Trigger()
  trigger.value = lunchtime      # so when cross the indicated barrier,
  trigger.quantity = breaklength # we then incur a quantity of x minutes (the break)
  tr.triggers.append(trigger)
  return (tr)

