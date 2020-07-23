# .proto
# CVRP - test script
library(iceR)

# create an api-helper object with the model type you'd like to solve.
api <- new("apiHelper", modelType = 'cvrp-jkfdoctmp51n', configFile = '../config.json')

sr <- new (CVRP.SolveRequest)
sr$model <- new (CVRP.CVRP)

# so a classic cvrp has a heterogeneous fleet. This means we need only specify the size of the
# vehicle and the number of vehicles available. The other aspect of this model is to include
# the location of the depot.

# lets make the first location the depot, and the balance of the locations the visit points.

data <- read.csv('../sample_data/publist.csv')[1:10,]
data$id %<>% as.character()
data$quantity <- 0
makePoint <- function(i){
  g <- new (CVRP.Geocode)
  g$id <- data$id[i]
  g$x <- data$X[i]
  g$y <- data$Y[i]
  g$quantity <- data$quantity[i] # this is just zero by default, we reuse the schema component but this isn't used
  return (g)
}
sr$model$depot <- makePoint(1)
#sr$model$toString() %>% cat  # or sr$model$toJSON() %>% cat # if you prefer the look and feel of json

# now we just need to select some quantities for the points.
# lets make each point take up 20 units, and we'll set the maximum capacity of the vehicle to 100
# that way we know that we don't need more than 2 vehicles but also that we can't use less than 2 vehicles.
data$quantity <- 20
sr$model$points <-sapply(2:nrow(data), makePoint)
# sr$model$toString() %>% cat # if you want to display what the model looks like at this point.

sr$model$NumberOfVehicles <- 3
sr$model$VehicleCapacity <- 100
sr$model$distancetype <- 1 # 1 for road network, 2 for euclidean, 3 for haversine.

sr$solveType <- 0  # 0 for optimise, 1 for evaluate, 2 for reoptimise

requestID <- api %>% postSolveRequest(sr)         # submit the model to the api
resp <- api %>% getResponse(requestID)            # retrieve the model response
resp$objective

tab <- resp %>% tabulate(sr)                      # tabulate the data in native-R format.
tab$nodes
tab$edges

resp %>% plotResponse(sr)
resp %>% plotResponseLeaflet(sr)



