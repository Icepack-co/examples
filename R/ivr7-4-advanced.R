# IVR7 EXAMPLE SCRIPT (ADVANCED 1)

library(iceR)

source('ivr7-model-helper.R')

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'ivr7-kt461v8eoaif', configFile = '../config.json')

# Create the model
sr <- new (IVR7.SolveRequest)
model <- new (IVR7.Model)

# Add some data to the model
data <- read.csv('../sample_data/publist_orders.csv')

model$dimensions <- make_distance_time_cap_dims()
model$locations <- make_locations(data)
model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)
model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
model$vehicles <- lapply(1:4, function(i){
  make_vehicle_cap( paste0("vehicle_", i), 'vc1', 'vcc1',
                    2000,
                    data$id[1],
                    data$id[1],
                    7*60,
                    18*60)
})

# Upload/submit the model via the data upload API
data_api <- new("apiHelper", modelType = 'ivrdata-o43e0dvs78zq', configFile = '../config.json')
data_model <- new (IVRData.CachedModel)
data_model$model <- model$serialize(NULL) 
modelID <-  data_api %>% postSolveRequest(data_model)
rm(model)

sr$modelID <- modelID # tell the solve request to use the model we uploaded.

sr$solveType <- 0 # 0 for optimise, 1 for evaluate, 2 for reoptimise

sr$toString() %>% cat

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

# Manipulate the model and evaluate
sr$routes <- list() 

vs <- tab$nodes$vehicleId %>% unique()
for(v in vs){
  ts <- new (IVR7.TaskSequence)
  ts$vehicleId <- v
  ts$taskId <-  tab$nodes %>%
    filter(grepl(x = taskId, pattern = 'pickup_') |
             grepl(x = taskId, pattern = 'dropoff_'),
           vehicleId == v) %>%
    select(taskId) %>% unlist %>% as.character()
  sr$routes[[length(sr$routes) + 1]] <- ts
}

sr$solveType <- 1 # 0 for optimise, 1 for evaluate, 2 for reoptimise

sr$toString() %>% cat

requestID <- api %>% postSolveRequest(sr)          
evalresp <- api %>% getResponse(requestID)          
tabeval <- respeval %>% tabulate(sr)

if(nrow(tabeval$nodes) != nrow(tab$nodes)){  
  stop("whoa, something isn't right in the world...") 
}