# IVR7 Advanced example:
# Purpose: illustrate the usage of the data-upload as well as model versioning.
# * Builds a simple pickup/dropoff (similar to the basic model).
# * Illustrates how to use the parent solve-request container for evaluate requests against a versioned model
# ** This is a quicker manner in which to run evaluate requests against the api where a large chunk
#    of model content is required to define the model. Send it once, then just run requests against that model,
#    and we'll handle moving the data around on our side.

rm(list = ls())
library(iceR)

# just up some pre-defined functions which wrap the data into pbf
source('../examples/R/ivr7-model-helper.R')

api <- new("apiHelper", modelType = 'ivr7-kt461v8eoaif', configFile = '../config.json')
model <- new (IVR7.Model)
data <- read.csv('../sample_data/publist_orders.csv')
data$id %<>% as.character()

#see ivr7 basic for notes around each of these methods.
model$dimensions <- make_distance_time_cap_dims()
model$locations <- make_locations(data)
model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)
model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
model$vehicles <- lapply(1:5, function(i){
  make_vehicle_cap( paste0("vehicle_", i), 'vc1', 'vcc1',
                    2000, # the vehicle capacity
                    data$id[1], # start location
                    data$id[1], # end location
                    7*60,  # 7 AM
                    18*60) # 6 PM
})

# okay, so that's a basic model. Lets now use the objects, but submit them to the api
# through a different mechanism.
data_api <- new("apiHelper", modelType = 'ivrdata-o43e0dvs78zq', configFile = '../config.json')
# so the data api allows us to push just the data, without anything else.
data_model <- new (IVRData.CachedModel)
data_model$model <- model$serialize(NULL) # epic: we just saved our model as a byte stream
# into this data payload.

# lets push the model data to the api and see what happens.
modelID <-  data_api %>% postSolveRequest(data_model)
# if you get an error at this point, just enable the ivr-data services on the client-portal.
# the modelID is now a guid reference to model we can reference in a solve request.
rm(model) # wham, we've deleted all the model information. But we've already uploaded it so it's in the cloud.
sr = new (IVR7.SolveRequest)
sr$modelID <- modelID # tell the solve request to use the model we uploaded.
sr$solveType <- 0
sr$toString() %>% cat # so basically no information at all, just the model guid. Compare this
# to all the data we had to send in the basic/intermediate examples.
requestID <- api %>% postSolveRequest(sr)
resp <- api %>% getResponse(requestID)
tab <- resp %>% tabulate(sr)

# this also means that because we have a model which is versioned separately from the
# solve request, we can use the solve request with the task-sequence and have that apply
# to a model. So lets extract the task sequence from the solved model.

sr$routes <- list() # note, in the intermediate example we used the model$taskSequence.
# we're going to use sr$routes here (because it's outside the $model)
vs <- tab$nodes$vehicleId %>% unique() # the unique set of vehicles in the solution
for(v in vs){
  ts <- new (IVR7.TaskSequence)
  ts$vehicleId <- v
  ts$taskId <-  tab$nodes %>%
    filter(grepl(x = taskId, pattern = 'pickup_') |
             grepl(x = taskId, pattern = 'dropoff_'), # you can use whatever convention you want for pickups/dropoffs
           vehicleId == v) %>%
    select(taskId) %>% unlist %>% as.character()
  sr$routes[[length(sr$routes) + 1]] <- ts
}
sr$solveType <- 1 # set to evaluate
sr$toString() %>% cat # now contains the model guid along with the task sequence for each vehicle
sRequestID <- api %>% postSolveRequest(sr)
respeval <- api %>% getResponse(sRequestID)            # retrieve the model response
tabeval <- respeval %>% tabulate(sr)

if(nrow(tabeval$nodes) != nrow(tab$nodes)){  stop("whoa, something isn't right in the world...") }

# so this is pretty nice when we think about it. It means that if you want to evaluate
# several permutations (i.e. modifications on a UI) then you don't have to resend the model each
# time, you can send it only when the master data is modified (i.e. times, locations, tasks etc)
# and then just use an evaluate solve request against a particular task-sequence.
