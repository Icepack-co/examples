# IVR8 Intermediate Example:
# Purpose: demonstrate how to use compartment constraints on a particular model.
# * Use a subset of the publist stops and configure a single vehicle
# * Use a simple two-rack compartment configuration to illustrate the basic assignment workings.
# * Add a group-limit constraint which only permits loads on the top-rack if there
#   is a task filling the space beneth it.
rm(list= ls())
library(iceR)

source('ivr8-model-helper.R')
# create an api-helper object with the model type you'd like to solve.
api <- new("apiHelper", modelType = 'ivr8-yni1c9k2swof', configFile = '../config.json')

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

# let's pretend for a moment that we have a vehicle which is layed out as follows:
#  Top Rack    [ ] [ ] [ ] [ ]  100kg per "compartment"  c1, c2, c3, c4
#  Lower Rack  [ ] [ ] [ ] [ ]  400kg per "compartment"  c5, c6, c7, c8
100*4 + 400*4 # adds up to the 2 ton total limit on a vehicle (if every compartment could be filled to max)

# create the compartments
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

# now we're going to add compartment relations which speak to the group limit.
# we can create multiple group limits.
# if we want something like, "the mass on the top may not exceed the mass on the bottom"

g <-  new(IVR8.CompartmentSet.GroupLimit)
g$compartmentIds <- paste0('c', 1:8)
g$coefficients <- c(+1,+1,+1,+1,-1,-1,-1,-1)
g$dimensionId <- "capacity"
g$limit <- 0 # so this says c1+c2+c3+c4-c5-c6-c7-c8 <= 0 is required for feasibility
             # writing this differently c1:c4 - c5:c8 <= 0    (grouping the c's together)
             # so c1:c4 <= c5:c8                              (moving c5:c8 to the rhs)
             # which says the top rack (c1:c4) should sum to less than the bottom rack (c5:c8)
cset$groupLimits <-g
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
tab <- resp %>% tabulate(sr)
tab$nodes %>% select(taskId, compartmentId, jobId)
tab$compartmentSummary
# In this table you can see that the the sum for compartments 1:4 is always less than 5:8
# this way we're constrained by always having more weight on the bottom rack than the top
# rack throughout the route (which is still pretty well costed)

resp %>% plotResponseLeaflet(sr)

