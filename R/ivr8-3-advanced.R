# IVR8 EXAMPLE SCRIPT (ADVANCED)

library(iceR)

source('ivr8-model-helper.R')

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'ivr8-yni1c9k2swof', configFile = '../config.json')

# Create the model
sr <- new(IVR8.SolveRequest)
sr$model <- new (IVR8.Model)

# Add some data to the model
data <- read.csv('../sample_data/publist_orders.csv')[1:9, ] 

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

sr$model$compartments <- list()
for(i in 1:4){
  comp <- new (IVR8.Compartment)
  comp$id <- paste0("c", i)
  comp$capacities <- new (IVR8.Compartment.Capacity)
  comp$capacities[[1]]$dimensionId <- "capacity"
  comp$capacities[[1]]$capacity <- 500 # top rack
  sr$model$compartments  <- append(sr$model$compartments, comp)
}

for(i in 1:length(sr$model$jobs)){
  relations <- new (IVR8.Job.CompartmentRelation)
  relations$type <- 0  # 0 for inclusions, 1 for exclusions
  if(i %% 2 == 0){ # just an alternating pattern
    relations$compartmentIds <- paste0("c", c(1,3))
  }else{
    relations$compartmentIds <- paste0("c", c(2,4))
  }
  sr$model$jobs[[i]]$compartmentRelations <- relations
}

sr$model$jobs[[1]]$toString() %>% cat

cset <- new (IVR8.CompartmentSet)
cset$id <- 'tanker'
cset$compartmentIds <- paste0('c', 1:4) 
sr$model$compartmentSets <- cset 

sr$model$vehicleClasses[[1]]$compartmentSetId <- 'tanker'

sr$solveType <- 0  # 0 for optimise, 1 for evaluate, 2 for reoptimise

# View the model in plain-text format (string or JSON)
sr$model$toString() %>% cat
sr$model$toJSON() %>% cat

# Submit the model to the API
requestID <- api %>% postSolveRequest(sr)

# Retrieve the model response
resp <- api %>% getResponse(requestID)
resp$objective

# Tabulate the data in native-R format
tab <- resp %>% tabulate(sr)

# View the produced summary
tab$compartmentSummary

jdata<- do.call(rbind, lapply(sr$model$jobs,
              function(i){data.frame(id = i$id,
                          allowableCompartments =
                            paste0(i$compartmentRelations$compartmentIds, collapse = ','),
                          stringsAsFactors = F)}))

tab$nodes %>% select(taskId, compartmentId, jobId) %>% left_join(jdata, by = c('jobId' = 'id'))

# Plot the data 
resp %>% plotResponse(sr) # ggplot
resp %>% plotResponseLeaflet(sr) # leaflet (if it's mappable)
