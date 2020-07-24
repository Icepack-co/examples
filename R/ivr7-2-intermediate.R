# IVR7 Intermediate example 1:
# Purpose: illustrate the use of modelling concepts
# Illustrates using time, distance and a single capacity dimension
# Location-windows (08:00 -> 14:00)
# Pickup-dropoff tasks (with task-times)
# One vehicle class (same travel profile)
# Two vehicle-cost classes
# Multiple vehicles (4)
# Heterogeneous fleet (2x2 ton and 2x3 ton capacity)
# Lunch breaks (1 hour break around 12:00)

rm(list = ls())

# test script
library(iceR)

# just up some pre-defined functions which wrap the data into pbf
source('ivr7-model-helper.R')

# create an api-helper object with the model type you'd like to solve.
api <- new("apiHelper", modelType = 'ivr7-kt461v8eoaif', configFile = '../config.json')

sr <- new (IVR7.SolveRequest)
sr$model <- new (IVR7.Model)

data <- read.csv('../sample_data/publist_orders.csv')
data$id %<>% as.character()
ggplot() + geom_point(data = data, aes(x = X, y = Y)) + theme_bw()

sr$model$dimensions <- make_distance_time_cap_dims() # this is a common configuration. See ivr7-model-helper.R for details
sr$model$dimensions$toString() %>% cat              # looks nice. 3 dimensions we're going to measure for each vehicle.

sr$model$locations <- make_locations(data)
sr$model$locations[[1]]$toString() %>% cat
# lets add arrival windows to all locations from 08:00 -> 14:00
# we're going to do this by simply modifying the location objects already constructed
la <- new (IVR7.Location.Attribute)
la$dimensionId <- 'time'
la$quantity <- 0
w <- new (IVR7.Window)
w$start <- 8*60 # 08:00 in our world, because we're measuring in minutes.
w$end <- 14*60  # 14:00
la$arrivalWindows <- w  # adding this as an arrival window. We could also add it as a departure window
                        # in which case it would be required to leave the location within the assigned
                        # window
la$toString() %>% cat # so this is what is being added to all locations.
for(i in 1:length(sr$model$locations)){
  sr$model$locations[[i]]$attributes <- la
}
rm(la, w)
sr$model$locations[[1]]$toString() %>% cat # tada


# lets build the jobs as well - same as the basic example
sr$model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
sr$model$jobs[[1]]$toString() %>% cat

# we're going to do the vehicle-configuration now.

# Two vehicle cost classes, one which is cheaper on time, one which is cheaper on distance, one
# which is more expensive if used (1000 vs 1200).
sr$model$vehicleCostClasses <- c(make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3),
                                 make_vcc_simple('vcc2', 1200, 0.1, 0.1, 0.1, 0.6, 2.5))


sr$model$vehicleCostClasses[[1]]$toString() %>% cat
sr$model$vehicleCostClasses[[2]]$toString() %>% cat

#using the same vehicle class as in the basic example.
sr$model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
sr$model$vehicleClasses[[1]]$toString() %>% cat

# now we can just specify the vehicles.
# lets provide 1 x 2 ton vehicles and 1x3 ton vehicles. 
# the reason for this is that we're modelling a full-blown pickup+dropoff model, so if there's
# time to reload, a vehicle can return to the depot and grab more goodies!

for(i in 1:4){
  vcc <- 'vcc1'
  vcap <- 2000
  if(i >= 3){ # make 1x2ton vehicles and 2x3ton vehicles.
    vcc <- 'vcc2'
    vcap <- 3000
  }
  sr$model$vehicles[[i]] <- make_vehicle_cap(paste0("vehicle_", i), 'vc1', vcc,
                                              vcap, # the vehicle capacity
                                              data$id[1], # start location
                                              data$id[1], # end location
                                              7*60,  # 7 AM
                                              18*60) # 6 PM
}

### Lunch breaks.
# so this is a touch more complex, we want to link our transit-rule to the time
# dimension, and when a certain amount has accumulated on the dimension, we trigger the rule.
sr$model$transitRules <- make_lunch_break_rule('lunch_break_rule', 'lunchy_munchy_',
                                               12*60, 60 ) # add the transit rule definition to the model
# now link the transit rule to the vehicle classes

sr$model$vehicleClasses[[1]]$transitRuleIds <- 'lunch_break_rule' # hence all vehicles (provided at the class level)

# now just configure the solve request to the solve type
sr$solveType <- 0 #0 optimise, 1 evaluate, 2 reoptimise

requestID <- api %>% postSolveRequest(sr)         # submit the model to the api
resp <- api %>% getResponse(requestID)            # retrieve the model response

tab <- tabulate(resp, sr)

# a tabulation of the transit rules that are being applied to the vehicles.
# where the fromStop == toStop this implies that the lunch break is performed at the stop
# where the fromStop != toStop this implies the lunch break is taken en-route.
# if it may only occur at stops, then feel free to disable the "inTransit" definition on the route,
# or vice-versa.
tab$transitRules
tab$infeasibilities # should be empty because all orders are completed.

tab$edges %>% head
tab$nodes %>% head

resp %>% plotResponse(sr)
resp %>% plotResponseLeaflet(sr)

# other cool things one can do because it's in tabular form:
transits <- tab$edges
transits$geometry <- NULL # just remove the geometries for this section so that the dataframe collapses nicely.
transits %>%
  mutate(TotalDistanceTravelled = distance_end - distance_start,
         TotalDriveTime = time_end - time_start) %>%
  group_by(vehicleId) %>%
  summarise(TotalDistance = sum(TotalDistanceTravelled),
            TravelTimeHrs = sum(TotalDriveTime)/60,
            DistanceCost = sum(distance_cost),
            TravelTimeCost = sum(time_cost))

stops <- tab$nodes
stops %>% filter(time_start < 8*60)  # only pickups here, none of the delivery nodes (which is correct)
                                     # since the vehicle-starts at the depot, so it's not changing location
                                     # if we wanted the window check to apply here, we would place the vehicles
                                     # at a different location (which may have the same geocode) to the depot

stops %>% filter(time_start > 14*60) # only vehicle-return nodes are allowed to be after 14:00

stops %>%
  mutate(StopTime = time_end - time_start,
         DropoffStop = capacity_end < capacity_start) %>%
  group_by(vehicleId) %>%
  summarise(TotalStopTime = sum(StopTime),
            MaxLoad = max(capacity_start),
            StopTimeCost = sum(time_cost),
            CustomerVisits = sum(DropoffStop))
# So we need 4 vehicles to perform all the visits feasibly.


