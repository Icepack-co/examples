# IVR7 EXAMPLE SCRIPT (ADVANCED 2)

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
sr$model$locations[[length(sr$model$locations) + 1]] <- sr$model$locations[[1]]
sr$model$locations[[length(sr$model$locations)]]$id <- "vehicle-site"
sr$model$jobs <- make_job_time_cap(data, src = rep(1, nrow(data) - 1), dest = 2:nrow(data))
sr$model$vehicleCostClasses <- make_vcc_simple('vcc1', 1000, 0.01, 0.01, 0.01, 1, 3)
sr$model$vehicleClasses <- make_vc_simple('vc1', 1, 1, 1, 1)
sr$model$vehicles <- lapply(1:4, function(i){
  make_vehicle_cap(paste0("vehicle_", i), 'vc1', 'vcc1',
                   2000,
                   "vehicle-site",
                   "vehicle-site",
                   7*60,
                   18*60)
})


dataUpload <- TRUE # Set to FALSE if you don't want to use the data upload API
if(dataUpload){
  data_api <- new("apiHelper", modelType = 'ivrdata-o43e0dvs78zq', configFile = '../config.json')
  ts <- new (IVRData.TransitSet)
  for(i in 1:(length(sr$model$locations) - 1)){
    tv <- new (IVRData.TransitSet.TransitValue)
    tv$fromId <- sr$model$locations[[i]]$id
    tv$toId <- "vehicle-site"
    tv$value <- 0
    ts$transits<- append(ts$transits, tv)
  }
  data_model <- new (IVRData.CachedTransitSet)
  data_model$transitSet <-ts
  rm(ts) 
  modelID <-  data_api %>% postSolveRequest(data_model)
}else{
  ts <-new (IVR7.TransitSet)
  for(i in 1:(length(sr$model$locations) - 1)){
    tv <- new (IVR7.TransitSet.TransitValue)
    tv$fromId <- sr$model$locations[[i]]$id
    tv$toId <- "vehicle-site"
    tv$value <- 0
    ts$transits<- append(ts$transits, tv)
  }
}

tgen_d <- new (IVR7.TransitGenerator)
tgen_d$id <- 'custom_distance'
tgen_t <- new (IVR7.TransitGenerator)
tgen_t$id <- 'custom_time'
if(dataUpload){
  tgen_d$requestId <- modelID
  tgen_t$requestId <- modelID
}else{
  tgen_d$transitSet <- ts
  tgen_t$transitSet <- ts
}
tgen_d$toString() %>% cat

sr$model$transitGenerators <- c(tgen_d, tgen_t)

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

sr$solveType <- 0 # 0 for optimise, 1 for evaluate, 2 for reoptimise

# Submit the model to the API
requestID <- api %>% postSolveRequest(sr)

# Retrieve the model response
resp <- api %>% getResponse(requestID)

# Tabulate the data in native-R format
tab <- resp %>% tabulate(sr)
tab$edges %>%
  filter(toLocationId == 'vehicle-site') %>%
  select(fromStopId, toStopId,
         fromLocationId, toLocationId,
         distance_start, distance_end,
         time_start, time_end) %>%
  mutate(distance_delta = distance_end - distance_start,
         time_delta = time_end - time_start)

l <- tab
l$edges$geometry[l$edges$toLocationId == 'vehicle-site'] <- NULL
stopColors <- rep('black', nrow(l$nodes))
stopColors[l$edges$toStopId[l$edges$toLocationId == 'vehicle-site']] <- 'red'
leaflet() %>%
  addTiles() %>%
  addCircleMarkers(data = l$nodes, lng = ~x, lat = ~y, color = stopColors, radius = 5) %>%
  addPolylines(data = l$edges$geometry)