# IVR7 Advanced example:
# Purpose: illustrate the usage of the data-upload as well as model versioning.
# * Builds a simple pickup/dropoff (similar to the basic model).
# * Configures "open routing" - i.e. do not cost the return legs for the vehicles to the end-location
# * Illustrates how to reference matrix data in a (potentially) versioned model.
# * or load overriding elements on a particular matrix directly.
#     (one can provide a complete matrix in this manner too)

rm(list = ls()) #clean up
library(iceR)

# just up some pre-defined functions which wrap the data into pbf
source('../examples/R/ivr7-model-helper.R')

api <- new("apiHelper", modelType = 'ivr7-kt461v8eoaif')
sr <- new (IVR7.SolveRequest)
sr$model <- new (IVR7.Model)
data <- read.csv('../sample_data/publist_orders.csv')
data$id %<>% as.character()

# We want "Open Routing" which basically means that if a vehicle finishes it's day at any node,
# it is then not supposed to cost the return trip home. So as a start, we need to separate the
# depot (Guiness Storehouse) from the vehicle-home location by name - it's okay if it's at the
# same point (long/lat), but we're going to separate the identifier so we can control the
# distance/time matrix nicely.

# see ivr7 basic for notes around each of these methods.
sr$model$dimensions <- make_distance_time_cap_dims()
sr$model$locations <- make_locations(data)
# add the vehicle-home location
sr$model$locations[[length(sr$model$locations) + 1]] <- sr$model$locations[[1]]
sr$model$locations[[length(sr$model$locations)]]$id <- "vehicle-site"
# okay, so this just coppies the guiness storehouse object and creates a new id.

sr$model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
sr$model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)
sr$model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
sr$model$vehicles <- lapply(1:5, function(i){
  make_vehicle_cap(paste0("vehicle_", i), 'vc1', 'vcc1',
                   2000, # the vehicle capacity
                   "vehicle-site", # start location
                   "vehicle-site", # end location
                   7*60,  # 7 AM
                   18*60) # 6 PM
})
# so we've created vehicles which need to start/end at "vehicle-site". Now we can make
# the last change which is to override the distance between locations and the "vehicle-site".
# we're only going to modify the distances FROM locations TO "vehicle-site". If you wanted to
# do complete line-haul outsourcing-style modelling, you could also do this for "vehicle-site"
# TO all alocations. For now, we'll just demonstrate the open routing case.

# you have two ways of doing this. Upload it via the data-api, or upload it as part of the model.

dataUpload <- TRUE # it's nice to illustrate this if you've enabled the services on your key
# but you can set this to false to get a feel for the other code path if needed.
if(dataUpload){
  ts <- new (IVRData.TransitSet)
  for(i in 1:(length(sr$model$locations) - 1)){
    tv <- new (IVRData.TransitSet.TransitValue)
    tv$fromId <- sr$model$locations[[i]]$id
    tv$toId <- "vehicle-site"
    tv$value <- 0
    ts$transits<- append(ts$transits, tv)
  }
  # lets upload this transit set.
  data_api <- new("apiHelper", modelType = 'ivrdata-o43e0dvs78zq')
  data_model <- new (IVRData.CachedTransitSet)
  data_model$transitSet <-ts
  rm(ts) # delete this to illustrate the point
  # lets push the model data to the api and see what happens.
  modelID <-  data_api %>% postSolveRequest(data_model)
}else{
  # we can pop the transit-set directly in the model.
  ts <-new (IVR7.TransitSet)
  for(i in 1:(length(sr$model$locations) - 1)){
    tv <- new (IVR7.TransitSet.TransitValue)
    tv$fromId <- sr$model$locations[[i]]$id
    tv$toId <- "vehicle-site"
    tv$value <- 0
    ts$transits<- append(ts$transits, tv)
  }
}

# then add the new transit generators linked to those transit-sets

# or the other way, build them all inline here.

tgen_d <- new (IVR7.TransitGenerator)
tgen_d$id <- 'custom_distance'
tgen_t <- new (IVR7.TransitGenerator)
tgen_t$id <- 'custom_time'
if(dataUpload){
  tgen_d$requestId <- modelID # Note, we're telling the API where to find the Transit-set data.
  tgen_t$requestId <- modelID # we can use the same one here as before, why? because it's all zero :-)
  # so we could upload another column of zeros, but there isn't much point.
}else{
  tgen_d$transitSet <- ts # or we're explicily providing all the data.
  tgen_t$transitSet <- ts
}
tgen_d$toString() %>% cat

sr$model$transitGenerators <- c(tgen_d, tgen_t) # add the custom transit generators
# sr$model$transitGenerators[[1]]$toString() %>% cat

# now the last step, we need to tell the vehicles that they should use these
# transit generators.
sr$model$vehicleClasses[[1]]$toString() %>% cat

t_attr <- new (IVR7.VehicleClass.Attribute)
t_attr$dimensionId <- "time"
t_attr$transitGeneratorId <- 'custom_time'
t_attr$transitCoef <- 1.0
t_attr$locationCoef <- 1.0
t_attr$taskCoef <- 1.0
sr$model$vehicleClasses[[1]]$attributes<- append(sr$model$vehicleClasses[[1]]$attributes, t_attr)

d_attr <- new (IVR7.VehicleClass.Attribute)
d_attr$dimensionId <- "distance"
d_attr$transitGeneratorId <- 'custom_distance'
d_attr$transitCoef <- 1.0
sr$model$vehicleClasses[[1]]$attributes<- append(sr$model$vehicleClasses[[1]]$attributes, d_attr)

sr$model$vehicleClasses[[1]]$toString() %>% cat
# so now we have a roadnetwork distance + time generator
# followed by a custom time and custom distance generator.
# the api will execute the transit generators in the order they appear and will
# override previous values with new values (if they exist). So in this case, it will
# build a transit matrix using the road network, then overlay the custom matrix which
# was uploaded with our data.

# now just configure the solve request to the solve type
sr$solveType <- 0  # 0 optimise, 1 evaluate, 2 reoptimise

requestID <- api %>% postSolveRequest(sr)         # submit the model to the api
resp <- api %>% getResponse(requestID)            # retrieve the model response

tab <- resp %>% tabulate(sr)
tab$edges %>%
  filter(toLocationId == 'vehicle-site') %>%
  select(fromStopId, toStopId,
         fromLocationId, toLocationId,
         distance_start, distance_end,
         time_start, time_end) %>%
  mutate(distance_delta = distance_end - distance_start,
         time_delta = time_end - time_start)

# and there you have it: the distance and time from the last stop to the vehicle-end point is zero.
# you'll notice there is however still a geometry associated with this movement, which is fine
# since you know that you can ignore these in whatever visualisation you put together, like this:
l <- tab
l$edges$geometry[l$edges$toLocationId == 'vehicle-site'] <- NULL
# we can probably highlight the first and last stops for each route too.
stopColors <- rep('black', nrow(l$nodes))
stopColors[l$edges$toStopId[l$edges$toLocationId == 'vehicle-site']] <- 'red'
leaflet() %>%
  addTiles() %>%
  addCircleMarkers(data = l$nodes, lng = ~x, lat = ~y, color = stopColors, radius = 5) %>%
  addPolylines(data = l$edges$geometry)

# and this kinda makes sense, because the stops which are farthest away from all the
# other nodes are typically the points at which the route should end (because you're saving the
# most in terms of distance and time) - indicated by the red markers in this plot

