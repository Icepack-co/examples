# IVR7 basic example:
# Illustrates using time, distance and a single capacity dimension
# Locations
# Pickup-dropoff tasks (with task times)
# single vehicle class
# single vehicle-cost class
# multiple vehicles.

rm(list = ls())

# test script
library(iceR)

source('../examples/R/ivr7-model-helper.R')

# create an api-helper object with the model type you'd like to solve.
api <- new("apiHelper", modelType = 'ivr7-kt461v8eoaif', configFile = '../config.json')

sr <- new (IVR7.SolveRequest)
sr$model <- new (IVR7.Model)

data <- read.csv('../sample_data/publist_orders.csv')
data$id %<>% as.character()
ggplot() + geom_point(data = data, aes(x = X, y = Y)) + theme_bw()

# the first decision we have to make is which dimensional quantities to model in this example.
# we're going to model the distance, time, and capacity of the vehicle.
sr$model$dimensions <- make_distance_time_cap_dims() # this is a common configuration. See ivr7-model-helper.R for details
sr$model$dimensions$toString() %>% cat            # looks nice. 3 dimensions we're going to measure for each vehicle.

# lets pretend the first point is where vehicles are going to begin and end each day.

# unlike the tsp/cvrp/pdp models, the ivr7 requires that you specify the unique locations
# that are going to be used in the model as a separate entity. The reason for this is that you
# can then specify the locations once, and reference those locations by id for other entities (such and vehicles/jobs/tasks)
sr$model$locations <- make_locations(data)
sr$model$locations[[1]]$toString() %>% cat

# that then takes care of creating a unique location for each item in our dataset.
# type "make_locations" for the details of how the pbf was constructed

# lets build the jobs as well
sr$model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
sr$model$jobs[[1]]$toString() %>% cat
# so we've constructed some jobs with pickups and dropoffs, loading and offload times, as well as the
# contribution to the capacity dimension. In this example, we're pickup up all orders at the guiness storehouse
# and delivering at the list of customers. 'make_job_time_cap' is just a simple function to create this
# particular style of request, but you can make your own.

# we're going to do the vehicle-configuration now.
# we need to specify the cost classes available, the vehicle classes available, and then the individual vehicles.
# we're going to create one of each to keep things simple.
sr$model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)

sr$model$vehicleCostClasses[[1]]$toString() %>% cat # so we're costing the vehicle, time at 0.01 units per minute, and distance at 3 units per km.

# lets make the vehicle class. A vehicle class describes how the vehicle MOVES through the network.
# so in other words, we can use the standard network travel speeds, or we could make the vehicle
# move slower/faster relative to the road network. We could also attach transit rules here which are
# great for modelling lunch breaks, refueling stops etc. (i.e. conditional triggers on the cumul values
# of the dimension). Covered in an advanced section.

sr$model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
sr$model$vehicleClasses[[1]]$toString() %>% cat

# now we can just specify the vehicles.
# lets provide 5 x 2 ton vehicles. Although this is probably more than we need.
# the reason for this is that we're modelling a full-blown pickup+dropoff model, so if there's
# time to reload, a vehicle can return to the depot and grab more goodies!

for(i in 1:5){
  sr$model$vehicles[[i]] <- make_vehicle_cap( paste0("vehicle_", i), 'vc1', 'vcc1',
                                              2000, # the vehicle capacity
                                              data$id[1], # start location
                                              data$id[1], # end location
                                              7*60,  # 7 AM
                                              18*60) # 6 PM
}

# now just configure the solve request to the solve type
sr$solveType <- 0 #0 optimise, 1 evaluate, 2 reoptimise

requestID <- api %>% postSolveRequest(sr)         # submit the model to the api
resp <- api %>% getResponse(requestID)            # retrieve the model response

tab <- tabulate(resp, sr)

tab$edges %>% head
tab$nodes %>% head

resp %>% plotResponse(sr)
resp %>% plotResponseLeaflet(sr)

#other cool things one can do because it's in tabular form:
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

stops %>%
  mutate(StopTime = time_end - time_start,
       DropoffStop = capacity_end < capacity_start) %>%
  group_by(vehicleId) %>%
  summarise(TotalStopTime = sum(StopTime),
            MaxLoad = max(capacity_start),
            StopTimeCost = sum(time_cost),
            CustomerVisits = sum(DropoffStop))

# nice to verify the capacity constraint on the vehicle hasn't been broken. I.e. the
# maximum load amount on the vehicle at any point on its route was <= 2000


