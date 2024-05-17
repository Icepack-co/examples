# IVR7 EXAMPLE SCRIPT (BASIC)

library(iceR)

source('ivr7-model-helper.R')

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'ivr7-kt461v8eoaif', configFile = '../config.json')

# Create the model
sr <- new (IVR7.SolveRequest)
sr$model <- new (IVR7.Model)

# Add some data to the model
data <- read.csv('../sample_data/publist_orders.csv')

# View the loaded data (if desired)
ggplot() + geom_point(data = data, aes(x = X, y = Y)) + theme_bw()

sr$model$dimensions <- make_distance_time_cap_dims() 
sr$model$dimensions$toString() %>% cat            

sr$model$locations <- make_locations(data)
sr$model$locations[[1]]$toString() %>% cat

sr$model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
sr$model$jobs[[1]]$toString() %>% cat

sr$model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)
sr$model$vehicleCostClasses[[1]]$toString() %>% cat 

sr$model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
sr$model$vehicleClasses[[1]]$toString() %>% cat

for(i in 1:2){
  sr$model$vehicles[[i]] <- make_vehicle_cap( paste0("vehicle_", i), 'vc1', 'vcc1',
                                              2000, # the vehicle capacity
                                              data$id[1], # start location
                                              data$id[1], # end location
                                              7*60,  # 7 AM
                                              18*60) # 6 PM
}

sr$solveType <- 0 # 0 for optimise, 1 for evaluate, 2 for reoptimise

# View the model in plain-text format (string or JSON)
sr$model$toString() %>% cat
sr$model$toJSON() %>% cat

# Submit the model to the API
requestID <- api %>% postSolveRequest(sr)

# Retrieve the model response
resp <- api %>% getResponse(requestID)

# Get the solve time
solveTime <- api %>% getSolveTime(requestID)

# Tabulate the data in native-R format
tab <- resp %>% tabulate(sr)       
tab$nodes %>% head # just inspect the head of the output
tab$edges %>% head # just inspect the head of the output

# Plot the data 
resp %>% plotResponse(sr) # ggplot
resp %>% plotResponseLeaflet(sr) # leaflet (if it's mappable)

# Additional options
transits <- tab$edges
transits$geometry <- NULL
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