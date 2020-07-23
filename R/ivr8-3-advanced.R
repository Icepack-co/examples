# IVR8 Advanced Example:
# Purpose: demonstrate how to use compartment constraints on a particular model.
# * Use a subset of the publist stops and configure a single vehicle
# * Use a simple one-rack comparment configuration
# * Add allowable-compartment assignments (i.e. which jobs may be assigned to which compartments)
rm(list= ls())
library(iceR)

source('../examples/R/ivr8-model-helper.R')
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

# We're going to simplify the config in this example slightly.
#  Lower Rack  [ ] [ ] [ ] [ ]  500kg per "compartment"  c1, c2, c3, c4

# create the compartments
model$compartments <- list()
for(i in 1:4){
  comp <- new (IVR8.Compartment)
  comp$id <- paste0("c", i)
  comp$capacities <- new (IVR8.Compartment.Capacity)
  comp$capacities[[1]]$dimensionId <- "capacity"
  comp$capacities[[1]]$capacity <- 500 # top rack
  model$compartments  <- append(model$compartments, comp)
}
# now we can go back through the tasks and allocate them to allowable compartments
# this is normal in fuel delivery systems where you have diesel/petrol constraints.
# we're just going to decide on which jobs may go in which compartments based on the
# index, and lets see if that's feasible. Obviously, you'll create it using proper logic
# based on the business rules.

for(i in 1:length(model$jobs)){
  relations <- new (IVR8.Job.CompartmentRelation)
  relations$type <- 0  # 0 for inclusions, 1 for exclusions
  if(i %% 2 == 0){ # just an alternating pattern
    relations$compartmentIds <- paste0("c", c(1,3))
  }else{
    relations$compartmentIds <- paste0("c", c(2,4))
  }
  model$jobs[[i]]$compartmentRelations <- relations   # set the compartment relation on the job
}

model$jobs[[1]]$toString() %>% cat # so here you can see we have c2 and c4 as allowable compartments


# now we can define a compartment set (a container for the individual compartments)
# which is attached to a vehicle.
cset <- new (IVR8.CompartmentSet)
cset$id <- 'tanker'
cset$compartmentIds <- paste0('c', 1:4) #indicates that this compartment set has c1:c8 available.
model$compartmentSets <- cset # add it to the model

# then we assign the "double-decker" compartment set to the vehicle class.
# we could have added it to each vehicle if we wanted, this is simply easier.
model$vehicleClasses[[1]]$compartmentSetId <- 'tanker'

sr <- new(IVR8.SolveRequest)
sr$model <- model
sr$solveType <- 0

requestID <- api %>% postSolveRequest(sr)
resp <- api %>% getResponse(requestID)
resp$objective
tab <- resp %>% tabulate(sr)
tab$compartmentSummary
# so the compartment summary is nice - but it doesn't tell us whether we stuck to the constraints
# around the relations for each of the jobs.
# so lets just build a data-frame of allowable compartments per job and join it to the normal response
jdata<- do.call(rbind, lapply(sr$model$jobs,
              function(i){data.frame(id = i$id,
                          allowableCompartments =
                            paste0(i$compartmentRelations$compartmentIds, collapse = ','),
                          stringsAsFactors = F)}))

tab$nodes %>% select(taskId, compartmentId, jobId) %>% left_join(jdata, by = c('jobId' = 'id'))
# so that's nice, we can see that at each task assignment we only used compartments
# which were in the allowable set we provided the api.
# it's probably worth noting that the default is all compartments in a compartment-set are allowed
# so you can either specify an inclusive sub-set, or excluded sub-set.
# if all compartments are excluded then it will let you know that there's no feasible allocation

# plotting still works (just for funzies)
resp %>% plotResponseLeaflet(sr)

