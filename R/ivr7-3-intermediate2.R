# IVR7 EXAMPLE SCRIPT (INTERMEDIATE 2)

library(iceR)

source('ivr7-model-helper.R')

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'ivr7-kt461v8eoaif', configFile = '../config.json')

# Create the model
sr <- new (IVR7.SolveRequest)
sr$model <- new (IVR7.Model)

# Add some data to the model
data <- read.csv('../sample_data/publist_orders.csv')

sr$model$dimensions <- make_distance_time_cap_dims()
sr$model$locations <- make_locations(data)
sr$model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
sr$model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)
sr$model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
sr$model$vehicles <- lapply(1:4, function(i){
  make_vehicle_cap( paste0("vehicle_", i), 'vc1', 'vcc1',
                    2000,
                    data$id[1],
                    data$id[1],
                    7*60,
                    18*60)
})

sr$solveType <- 0 # 0 for optimise, 1 for evaluate, 2 for reoptimise

# View the model in plain-text format (string or JSON)
sr$model$toString() %>% cat
sr$model$toJSON() %>% cat

# Submit the model to the API
requestID <- api %>% postSolveRequest(sr)

# Retrieve the model response
resp <- api %>% getResponse(requestID)

# Tabulate the data in native-R format
tab <- resp %>% tabulate(sr)       
tab$nodes %>% head # just inspect the head of the output
tab$edges %>% head # just inspect the head of the output

# Plot the data (all jobs)
resp %>% plotResponse(sr) # ggplot
resp %>% plotResponseLeaflet(sr) # leaflet (if it's mappable)

# Manipulate the model and evaluate
for(i in 1:length(sr$model$locations)){
  la <- new (IVR7.Location.Attribute)
  la$dimensionId <- 'time'
  la$quantity <- 0
  w <- new (IVR7.Window)
  w$start <- 8*60
  w$end <- 14*60
  la$arrivalWindows <- w
  sr$model$locations[[i]]$attributes <- la
}

sr$model$taskSequence <- list()
vs <- tab$nodes$vehicleId %>% unique()
for(v in vs){
  ts <- new (IVR7.TaskSequence)
  ts$vehicleId <- v
  ts$taskId <-  tab$nodes %>%
    filter(grepl(x = taskId, pattern = 'pickup_') |
             grepl(x = taskId, pattern = 'dropoff_'),
           vehicleId == v) %>%
    select(taskId) %>% unlist %>% as.character()
  sr$model$taskSequence[[length(sr$model$taskSequence) + 1]] <- ts
}

sr$model$taskSequence[[1]]$toString() %>% cat

sr$solveType <- 1 # 0 for optimise, 1 for evaluate, 2 for reoptimise

requestID <- api %>% postSolveRequest(sr)          
evalresp <- api %>% getResponse(requestID)          

tabeval <- evalresp %>% tabulate(sr)
tabeval$infeasibilities %>% head

tabeval$nodes %>% filter(taskId %in% tabeval$infeasibilities$taskId) %>%
  select(time_start) %>% mutate(windowBroken = time_start > 14*60)

for(i in 1:length(sr$model$taskSequence)){
  ts <- sr$model$taskSequence[[i]]$taskId
  sr$model$taskSequence[[i]]$taskId <- ts[!(ts %in% tabeval$infeasibilities$taskId)]
}

requestID <- api %>% postSolveRequest(sr)             
evalresp <- api %>% getResponse(requestID)      

tabeval <- evalresp %>% tabulate(sr)
tabeval$infeasibilities %>% head

for(i in 1:length(sr$model$taskSequence)){
  ts <- sr$model$taskSequence[[i]]$taskId
  sr$model$taskSequence[[i]]$taskId <- ts[!(ts %in% tabeval$infeasibilities$taskId)]
}

requestID <- api %>% postSolveRequest(sr)
evalresp <- api %>% getResponse(requestID)

tabeval <- evalresp %>% tabulate(sr)
tabeval$infeasibilities 

if( !(evalresp$objective > resp$objective) ){
  stop("Only true if the cost of dropping a job is close to zero.")
}

# Plot the data (feasible subset)
evalresp %>%  plotResponseLeaflet(sr)

# Run the request (with the added time windows)
sr$model$taskSequence <- list()

sr$solveType <- 0 # 0 for optimise, 1 for evaluate, 2 for reoptimise

requestID <- api %>% postSolveRequest(sr)    
window_constrained_resp <- api %>% getResponse(requestID)

wtab <- window_constrained_resp %>% tabulate(sr)

window_constrained_resp$objective > resp$objective
window_constrained_resp$infeasibilities