# IVR8 EXAMPLE SCRIPT (BASIC)

library(iceR)

source('ivr8-model-helper.R')

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'ivr8-yni1c9k2swof', configFile = '../config.json')

# Create the model
sr <- new(IVR8.SolveRequest)
sr$model <- new (IVR8.Model)

# Add some data to the model
data <- read.csv('../sample_data/publist_orders.csv')[1:9, ] 

# View the loaded data (if desired)
ggplot() + geom_point(data = data, aes(x = X, y = Y)) + theme_bw()

sr$model$dimensions <- make_distance_time_cap_dims()

sr$model$locations <- make_locations(data)

sr$model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))

sr$model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)

sr$model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)

sr$model$vehicles <- make_vehicle_cap("vehicle_1", 'vc1', 'vcc1',
                                    2000, 
                                    data$id[1], 
                                    data$id[1],
                                    7*60,
                                    18*60)

data$quantity %>% sum

sr$model$compartments <- list()
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
  sr$model$compartments  <- append(sr$model$compartments, comp)
}

cset <- new (IVR8.CompartmentSet)
cset$id <- 'double-decker'
cset$compartmentIds <- paste0('c', 1:8) 
sr$model$compartmentSets <- cset

sr$model$vehicleClasses[[1]]$compartmentSetId <- 'double-decker'

sr$solveType <- 0  # 0 for optimise, 1 for evaluate, 2 for reoptimise

# View the model in plain-text format (string or JSON)
sr$model$toString() %>% cat
sr$model$toJSON() %>% cat

# Submit the model to the API
requestID <- api %>% postSolveRequest(sr)

# Retrieve the model response
resp <- api %>% getResponse(requestID)
resp$objective
resp$routes[[1]]$stops

# Get the solve time
solveTime <- api %>% getSolveTime(requestID)

# Tabulate the data in native-R format
tab <- resp %>% tabulate(sr)
tab$nodes %>% select(taskId, compartmentId, jobId)
tab$edges %>% head # just inspect the head of the output

# View the produced summary
tab$compartmentSummary

# Plot the data 
resp %>% plotResponse(sr) # ggplot
resp %>% plotResponseLeaflet(sr) # leaflet (if it's mappable)

# Infeasible example
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
tab$nodes %>% select(taskId, compartmentId, jobId)
tab$edges %>% head
tab$compartmentSummary
tab$infeasibilities

resp %>% plotResponse(sr)
resp %>% plotResponseLeaflet(sr)