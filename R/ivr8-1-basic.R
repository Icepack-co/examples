# IVR8 Basic Example:
# Purpose: demonstrate how to use compartment constraints on a particular model.
# * Use a subset of the publist stops and configure a single vehicle
# * Use a simple two-rack compartment configuration to illustrate the workings.

rm(list= ls())
library(iceR)

source('../examples/R/ivr8-model-helper.R')
# create an api-helper object with the model type you'd like to solve.
api <- new("apiHelper", modelType = 'ivr8-yni1c9k2swof')

model <- new (IVR8.Model)
data <- read.csv('../sample_data/publist_orders.csv')
data$id %<>% as.character()
data <- data[1:9, ] # we're going to use location 1 as the depot, 2:9 as the stops.

# see ivr7 basic examples for notes around each of these methods.
# the ivr7/8 models are interchangeable, except that the IVR8 model supports
# compartment modelling.
model$dimensions <- make_distance_time_cap_dims()
model$locations <- make_locations(data)
model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)
model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
model$vehicles <- make_vehicle_cap("vehicle_1", 'vc1', 'vcc1',
                                    2000, # the vehicle capacity
                                    data$id[1], # start location
                                    data$id[1], # end location
                                    7*60,  # 7 AM
                                    18*60) # 6 PM
# so everything up to this point is highly similar to what we had before.
# except now we're going to add some constraints around the compartments themselves
# on the vehicle.
data$quantity %>% sum # the sum of quantity here is actually less than the size of the vehicle (in aggregate)
                      # but! does it meet our compartment constraints?

# lets pretend for a moment that we have a vehicle which is layed out as follows:
#  Top Rack    [ ] [ ] [ ] [ ]  100kg per "compartment"  c1, c2, c3, c4
#  Lower Rack  [ ] [ ] [ ] [ ]  400kg per "compartment"  c5, c6, c7, c8
100*4 + 400*4 # adds up to the 2 ton total limit on a vehicle (if every compartment could be filled to max)


# so compartments can be defined separately from compartments sets.
# you can use any combination of compartment defintions in a compartment set (to
# define a collection against which constraint checks are performed)
model$compartments <- list()
for(i in 1:8){
  comp <- new (IVR8.Compartment)
  comp$id <- paste0("c", i)
  comp$capacities <- new (IVR8.Compartment.Capacity)
  comp$capacities[[1]]$dimensionId <- "capacity"
  if(i <= 4){
    comp$capacities[[1]]$capacity <- 100 # top rack
  }else{
    comp$capacities[[1]]$capacity <- 400 # bottom rack
  }
  model$compartments  <- append(model$compartments, comp)
}


# now we can define a compartment set (a container for the individual compartments)
# which is attached to a vehicle.
cset <- new (IVR8.CompartmentSet)
cset$id <- 'double-decker'
cset$compartmentIds <- paste0('c', 1:8) #indicates that this compartment set has c1:c8 available.
model$compartmentSets <- cset # add it to the model

# then we assign the "double-decker" compartment set to the vehicle class.
# we could have added it to each vehicle if we wanted, this is simply easier.
model$vehicleClasses[[1]]$compartmentSetId <- 'double-decker'

sr <- new(IVR8.SolveRequest)
sr$model <- model
sr$solveType <- 0

requestID <- api %>% postSolveRequest(sr)
resp <- api %>% getResponse(requestID)
resp$objective
resp$routes[[1]]$stops
tab <- resp %>% tabulate(sr)

tab$nodes %>% select(taskId, compartmentId, jobId)

tab$compartmentSummary # ivr8 produces a nice summary
# okay, so what are we looking at here? So basically each "allocated" is when a task is executed
# so either a pickup or a dropff. the capacity of each compartment is listed at the top under "capacity"
# each stop shows where the volume is added and we can see that only one change is made at each node.
# this is because the task is assigned to a compartment. At no point is the total volume allocated
# to a compartment more than the capacity of the compartment. There are 16 allocations here because there
# are 8 jobs, i.e. 8 pickups, 8 dropoffs. So after each pickup we can see the state of the load on
# the vehicle. It's maximum weight is at stop.8 => 1800 units.

# now lets try somethign that's infeasible (by design) and see what happens.
# we're going to clear the compartments, populate a new list and run the model.
sr$model$compartments <- list()
for(i in 1:8){
  comp <- new (IVR8.Compartment)
  comp$id <- paste0("c", i)
  comp$capacities <- new (IVR8.Compartment.Capacity)
  comp$capacities[[1]]$dimensionId <- "capacity"
  if(i <= 4){
    comp$capacities[[1]]$capacity <- 150 # top rack
  }else{
    comp$capacities[[1]]$capacity <- 350 # bottom rack is at 350 - which is less than the biggest order
  }
  sr$model$compartments  <- append(sr$model$compartments, comp)
}


requestID <- api %>% postSolveRequest(sr)
resp <- api %>% getResponse(requestID)
resp$objective
tab <- resp %>% tabulate(sr)
tab$nodes %>% select(taskId, compartmentId, jobId) # so now we only have 10 stops scheduled :-/
tab$compartmentSummary
tab$infeasibilities
# ah, but the api is nice enough to tell us that there is no feasible compartment assignment
# exists for this particular set of tasks as well as the constraining dimension (capacity).
# The limit and value's are negative here indicating that the values aren't relevant.
# if you're looking for a more informative error message, use the evaluate end-point which
# can identify for a proposed sequence where things went wrong (or whether any feasible sub-set exists)
