# IVR 7 model function-helpers.
# basically just wrappers around common things which you may want to create.
make_distance_time_cap_dims <- function(){
  dimensions<-  new (IVR7.DimensionConfiguration)
  dimensions$timeConfig$id <- 'time'         # this is an internal dimension computed for you.
  dimensions$timeConfig$measurementUnit <- 1 # 'minutes' - see schema file for enum details
  dimensions$timeConfig$slackMax <- 1e6      # this means that by default we're allowed to be early for a window and then simply wait
  dimensions$timeConfig$tardyMax <- 0        # by default, you may not be late for a window.

  dimensions$distanceConfig$id <- 'distance'
  dimensions$distanceConfig$measurementUnit <- 4# 'km' - see schema file for enum details
  dimensions$distanceConfig$slackMax <- 0       # no slack in this dimension
  dimensions$distanceConfig$tardyMax <- 0       # no tardiness in this dimension

  capdim <- new (IVR7.CapacityDimension)        # you can add as many capacity dimensions as you like in this model
  capdim$id <- 'capacity'                       # but they should each have a unique dimension id
  capdim$units <- 'kg'
  capdim$slackMax <- 0
  capdim$tardyMax <- 0

  dimensions$capacityDimensions <- capdim    # could add a list of capacity dims here. i.e. mass, volume, etc.
  return (dimensions)
}

make_locations <- function(locdata){
  d <- list()
  for(i in 1:nrow(locdata)){
    l <- new (IVR7.Location)
    l$id <- locdata$id[i]
    l$geocode <- new (IVR7.Geocode)
    l$geocode$longitude = locdata$X[i]
    l$geocode$latitude = locdata$Y[i]
    # there are other attributes on the location level one may want to model.
    # this is covered in intermediate examples.
    d[[length(d) + 1]] <- l
  }
  return (d)
}

make_job_time_cap <- function(jobdata, src, dest){
  d<- list()
  if(length(src) != length(dest)){
    stop("length(src) != length(dest)")
  }
  for(i in 1:length(src)){
    di <- dest[i]
    si <- src[i]
    j <- new (IVR7.Job)
    j$id <- paste0("job_", jobdata$id[di])
    j$pickupTask <- new (IVR7.Job.Task)
    j$pickupTask$taskId <- paste0("pickup_", jobdata$id[di])
    j$pickupTask$locationId <- jobdata$id[si] # every job is picked up at the drop

    # we have a pickup quantity and dropoff quantity which are equal (a typical pickup/dropoff problem)
    # but we also have a pickup time and dropoff time.
    a_time <- new (IVR7.Job.Task.Attribute)
    a_time$dimensionId <- 'time'
    a_time$quantity <- jobdata$pickupTime[di]

    a_cap <- new (IVR7.Job.Task.Attribute)
    a_cap$dimensionId <- 'capacity'
    a_cap$quantity <- jobdata$quantity[di]
    # a$windows # we can provide a list of windows for the pickup here.
    j$pickupTask$attributes <- c(a_time, a_cap) #coppies the objects to the list, so we can reuse them

    j$dropoffTask <- new (IVR7.Job.Task)
    j$dropoffTask$taskId <- paste0('dropoff_', jobdata$id[di])
    j$dropoffTask$locationId <- jobdata$id[di] # the dropoff is at the location in the data table.

    #same for the dropoff task, we have a dropoff time and quantity (which happens to be the same)
    a_time$quantity <- jobdata$dropoffTime[di]
    a_cap$quantity <- -jobdata$quantity[di]  #note, the dropoff quantity is negative here (i.e. it's leaving the vehicle)
    j$dropoffTask$attributes <- c(a_time, a_cap)

    j$penalty <- 10000 # this piece is important. is specifies the penalty cost of NOT performing the job.
    # this is useful is you're trying to model the trade-off between opportunity cost and
    # actual cost if these are known. In general, it's acceptable to make this a large number
    # so that if there are errors in the data you can still get a feasible solution, with the
    # infeasibilities assessed by the api.
    d[[length(d) + 1]] <- j
  }
  return (d)
}

make_vcc_simple <- function(name, fixedcost, time_transit_costcoef,
                            time_loc_costcoef,time_task_costcoef,
                            time_slack_costcoef,
                            distance_transit_costcoef){
  vcc <- new (IVR7.VehicleCostClass)
  vcc$id <- name
  vcc$fixedCost <- fixedcost # a fixed cost associated with using this particular vehicle
  vcc_time_cost <- new (IVR7.VehicleCostClass.Attribute)
  vcc_time_cost$dimensionId <- 'time'
  vcc_time_cost$transitCostCoef <-time_transit_costcoef
  vcc_time_cost$taskCostCoef <- time_task_costcoef
  vcc_time_cost$locationCostCoef <- time_loc_costcoef
  vcc_time_cost$slackCostCoef <- time_slack_costcoef #discourage being early slightly.

  vcc_dist_cost <- new (IVR7.VehicleCostClass.Attribute)
  vcc_dist_cost$dimensionId <- 'distance'
  vcc_dist_cost$transitCostCoef <- distance_transit_costcoef # say 3 units per km. so favour shorter routes where possible.
  # a note: the default cost coefficients are zero. So you can simply specify where they are not zero.
  vcc$attributes <- c(vcc_time_cost, vcc_dist_cost)
  return (vcc)
}

make_vc_simple <- function(name, time_transit_coef, time_task_coef, time_loc_coef,
                           distance_transit_coef){
  vc <- new (IVR7.VehicleClass)
  vc$id <- name
  vca_t <- new (IVR7.VehicleClass.Attribute)
  vca_t$dimensionId <- 'time'
  vca_t$transitCoef <- time_transit_coef   # this would make the vehicle move 20% slower between locations if it was 1.2
  vca_t$taskCoef <- time_task_coef
  vca_t$locationCoef <- time_loc_coef
  vca_t$transitGeneratorId <- 'roadnetwork_time' # this tells the vehicle to use the roadnetwork_time transits.
  # the reason this is separate is that one may want to provide your own
  # transit matrix which you could then link to the time-dimension.

  vca_d <- new (IVR7.VehicleClass.Attribute)
  vca_d$dimensionId <- 'distance'
  vca_d$transitGeneratorId <- 'roadnetwork_distance' #again, just linking up to the road-network distance
  #coefficients are 1.0 by default in the attrivues.
  vca_d$transitCoef <- distance_transit_coef
  vc$attributes <- c(vca_t, vca_d)
  return (vc);
}


make_vehicle_cap <- function(name,
                             vehicleClass, vehicleCostClass, capacity,
                             startloc, endloc,
                             start_time, end_time){
  v <- new (IVR7.Vehicle)
  v$id <- name
  v$classId <- vehicleClass
  v$costClassId <- vehicleCostClass
  v$shift <- new (IVR7.Vehicle.Shift)
  v$shift$shiftStart <- new (IVR7.Vehicle.Task)
  v$shift$shiftStart$locationId <- startloc # we're setting the start of the vehicle to the depot
  v$shift$shiftEnd <- new (IVR7.Vehicle.Task)
  v$shift$shiftEnd$locationId <- endloc   # we're setting the end of the vehicle to the depot

  vatt <- new (IVR7.Vehicle.Task.Attribute)
  vatt$dimensionId <- 'time'
  vatt$quantity <- 0
  vatt$windows <- new (IVR7.Window)
  vatt$windows[[1]]$start <- start_time # 7 AM
  vatt$windows[[1]]$end <- end_time # 6 PM
  v$shift$shiftStart$attributes <- vatt
  v$shift$shiftEnd$attributes <- vatt

  # configure the individual vehicle capacity
  vcap <- new (IVR7.Vehicle.Capacity)
  vcap$dimensionId <- 'capacity'
  vcap$capacity <- capacity
  v$capacities <- vcap

  return (v);
}

make_lunch_break_rule <- function(name, prefix, lunchtime, breaklength){
  tr <- new(IVR7.TransitRule)
  tr$id <- name
  tr$dimensionId <- 'time'
  tr$useStandingState <- TRUE # sure, we would allow the execution of the rule while a vehicle is stationary
  tr$useTransitState <- TRUE # why not, if the vehicle is permitted to stop while travelling between stops then this is okay
  tr$ruleIdPrefix <- prefix
  trigger <-  new (IVR7.TransitRule.Trigger)
  trigger$value <- lunchtime      # so when cross the indicated barrier,
  trigger$quantity <- breaklength # we then incur a quantity of x minutes (the break)
  tr$triggers<-trigger
  return (tr);
}
