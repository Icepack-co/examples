# IVR7 Intermediate example 2:
# Purpose: illustrate the usage of the evaluate end-point and inline model manipulations.
# Builds a simple pickup/dropoff (similar to the basic model).
# Runs an eval on a sub-sequence to illustrate how to call the endpoint and
# interpret the responses in terms of infeasibility messages.

rm(list = ls())
library(iceR)

# just up some pre-defined functions which wrap the data into pbf
source('ivr7-model-helper.R')

api <- new("apiHelper", modelType = 'ivr7-kt461v8eoaif', configFile = '../config.json')
sr <- new (IVR7.SolveRequest)
sr$model <- new (IVR7.Model)
data <- read.csv('../sample_data/publist_orders.csv')
data$id %<>% as.character()

#see ivr7 basic for notes around each of these methods.
sr$model$dimensions <- make_distance_time_cap_dims()
sr$model$locations <- make_locations(data)
sr$model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
sr$model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)
sr$model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
sr$model$vehicles <- lapply(1:4, function(i){
  make_vehicle_cap( paste0("vehicle_", i), 'vc1', 'vcc1',
                    2000, # the vehicle capacity
                    data$id[1], # start location
                    data$id[1], # end location
                    7*60,  # 7 AM
                    18*60) # 6 PM
})

# now just configure the solve request to the solve type
sr$solveType <- 0  # 0 optimise, 1 evaluate, 2 reoptimise
requestID <- api %>% postSolveRequest(sr)         # submit the model to the api
resp <- api %>% getResponse(requestID)            # retrieve the model response

tab <- resp %>% tabulate(sr)

# so we can do a few things here, we can add constraints which weren't in the original
# model, evaluate the same sequence and see if any constraints are broken?
# sure, this sounds like fun. Lets add some time windows to all the locations and see
# what that does.

for(i in 1:length(sr$model$locations)){
  la <- new (IVR7.Location.Attribute)
  la$dimensionId <- 'time'
  la$quantity <- 0
  w <- new (IVR7.Window)
  w$start <- 8*60 # 08:00 in our world, because we're measuring in minutes.
  w$end <- 14*60  # 14:00
  la$arrivalWindows <- w  # adding this as an arrival window. We could also add it as a departure window
  sr$model$locations[[i]]$attributes <- la
}
# okay, so now we've added a 08:00 - 14:00 window on all the locations.
# in order to evaluate our current solution against this new solution
# we need to convert our current solution to a task sequence (which is done
# by vehicle)

# so the only catch here is that the shift-start and shift-end nodes are implicitly already there
# so all we really need to do is pull out the nodes inbetween. So we filter on only the tasks we're
# scheduling in the last solution we received.
sr$model$taskSequence <- list()
vs <- tab$nodes$vehicleId %>% unique() # the unique set of vehicles in the solution
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
# so the definition is, for a vehicle: list the tasks that vehicle should perform
# each item in the list is another vehicle. vehicles with no tasks can be omitted
sr$model$taskSequence[[1]]$toString() %>% cat

#Great; so at this point the locations have new windows
# and we have a location sequence which the solver must use.
# The last step is to tell the solver to USE the evaluation solve process.

sr$solveType <- 1 # 1 for Evaluate

requestID <- api %>% postSolveRequest(sr)             # submit the evaluate model to the api
evalresp <- api %>% getResponse(requestID)            # save the response elsewhere so we can refer back

tabeval <- evalresp %>% tabulate(sr)

tabeval$infeasibilities %>% head
# so here we should have some constraints which have been broken.
# We get told which dimension is related (if the constraint is related to a dimension)
# we also get told which type of constraint (if known) and the degree to which the constraint is broken.

# if the constraints are tardy constraints being broken, this means the task starts AFTER the
# allowable window. the limit will be often be zero, the value will be the amount by which the
# vehicle is late. we can check the arrival time of the task to verify this:

# we know that we set all the location windows to be between 08:00 and 14:00
# filter on all tasks which appear in the infeasibility set and lets check their windows.
tabeval$nodes %>% filter(taskId %in% tabeval$infeasibilities$taskId) %>%
  select(time_start) %>% mutate(windowBroken = time_start > 14*60)
# well that's cool. So all the tasks flagged by the infeasibilities table correspond
# to tasks in the schedule which will arrive after the permissable window.

# we could try other things: how about we take out all tasks which are infeasible?
for(i in 1:length(sr$model$taskSequence)){
  ts <- sr$model$taskSequence[[i]]$taskId
  sr$model$taskSequence[[i]]$taskId <- ts[!(ts %in% tabeval$infeasibilities$taskId)]
  # just remove any tasks seen in the tabeval$infeasibilities set.
}
# okay, but kinda obviously, the pickups were mostly feasible on their time windows, it was
# just the dropoffs that were a problem. So now we have pickups without a dropoff?
# what will happen?

requestID <- api %>% postSolveRequest(sr)             # submit the evaluate model to the api
evalresp <- api %>% getResponse(requestID)            # save the response elsewhere so we can refer back
tabeval <- evalresp %>% tabulate(sr)
tabeval$infeasibilities %>% head
# so this is again, very intuitive. We find that there are a whole bunch of precendence
# constraints which are then broken, cumul-pair constraints and task-pair constraints.
# this is because there's a relation between the pickup and dropoff and either they're BOTH scheduled
# or BOTH unscheduled. Having one task assigned to a vehicle in the schedule without the other breaks
# a bunch of constraints.
# so lets apply the same trick and remove these stops.

for(i in 1:length(sr$model$taskSequence)){
  ts <- sr$model$taskSequence[[i]]$taskId
  sr$model$taskSequence[[i]]$taskId <- ts[!(ts %in% tabeval$infeasibilities$taskId)]
  # just remove any tasks seen in the tabeval$infeasibilities set.
}

requestID <- api %>% postSolveRequest(sr)             # submit the evaluate model to the api
evalresp <- api %>% getResponse(requestID)            # save the response elsewhere so we can refer back
tabeval <- evalresp %>% tabulate(sr)

tabeval$infeasibilities # great, this is actually NULL now, which means there aren't any
# infeasibilities left in the schedule
# but at what cost did that come.
if( !(evalresp$objective >resp$objective) ){
  stop("Only true if the cost of dropping a job is close to zero.")
}
# so clearly the new schedule costs more - because a bunch of jobs have been left off.
# Just for your own interest, the evaluate still comes back with all the same
# stuff as before, so plotting is identical and the interpretation of the schedule
# is also the same. The only thing is that it will permit infeasible actions but then
# report on where constraints are being broken (such as slack (earlyness), tardiness (lateness)),
# precedence, capacity and task/location windows.


resp %>%  plotResponseLeaflet(sr)     # so this is all the jobs
evalresp %>%  plotResponseLeaflet(sr) # this is just the feasible subset with the evaluate endpoint.
# just illustrating that responses from an optimise request
# and a evaluate request come back with everything you need.

# we can switch back to a optimise request. drop the task-sequence and let the optimiser do its
# thing with the new window constraints.

sr$model$taskSequence <- list()

sr$solveType <- 0 # switching back to the optimise request.
requestID <- api %>% postSolveRequest(sr)                 # submit the evaluate model to the api
window_constrained_resp <- api %>% getResponse(requestID) # save the response
wtab <- window_constrained_resp %>% tabulate(sr)

window_constrained_resp$objective > resp$objective # so we expect the new solution to cost
# more because of the additional constraints
window_constrained_resp$infeasibilities            # there won't be any infeasibilities because the
# request was to "optimise" not "evaluate" and this problem instance is largely unconstrained (i.e. it's
# easy for the solver to find a way to satisfy all the tasks)
