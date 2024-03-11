# CVRPTW sample script
library(iceR)

# # create an api-helper object with the model type you'd like to solve.
api <- new("apiHelper", modelType = 'cvrptw-acyas3nzweqb', '../config.json')
sr <- new (CVRPTW.SolveRequest)
sr$model <- new (CVRPTW.CVRPTW)

# A classic cvrptw has a heterogeneous fleet. This means we need only specify the size of the
# vehicle and the number of vehicles available. The other aspect of this model is to include
# the location of the depot. The cvrptw is costed differently to the cvrp. The objective is still
# to minimise the number of vehicles used, but also to minimise the total time. The classic cvrp
# aims to minimise the number of vehicles, then the total distance travelled.
# The cvrptw has time windows on each point. In this schema, we allow you to omit windows from 
# points if needed.

# let's make the first location the depot, and the balance of the locations the visit points.
data <- read.csv('../sample_data/publist_orders.csv')[1:10,]
makePoint <- function(i){
  g <- new (CVRPTW.Geocode)
  g$id <- data$id[i]
  g$x <- data$X[i]
  g$y <- data$Y[i]
  g$quantity <- data$quantity[i] # this is just zero by default, we reuse the schema component but this isn't used
  return (g)
}
sr$model$depot <- makePoint(1) # we're not going to add any windows on the depot.
#sr$model$toString() %>% cat  
#sr$model$toJSON() %>% cat # if you prefer the look and feel of json

# NOTE: quantity in Geocode above is zero by default. 
# In this example, the quantity is provided with the data but can be set for each point here too.
# For example, each point can be set to take up 20 units. If the maximum capacity of the vehicle is set to 100
# we know that we don't need more than 2 vehicles but also that we can't use less than 2 vehicles.
# data$quantity <- 20

sr$model$points <-sapply(2:nrow(data), makePoint)
# let's add windows just using a traditional loop
# let's randomly split the windows into "morning" and "afternoon" windows
# note that the windows are measured in minutes in this schema.

morning <- c(8*60, 12*60) #08:00 to 12:00
afternoon <- c(12*60, 16 * 60) #12:00 to 16:00

for(i in 1:length(sr$model$points)){
  if(i %% 2 == 0){
    sr$model$points[[i]]$windowStart <-morning[1]
    sr$model$points[[i]]$windowEnd <- morning[2]
  }else{
    sr$model$points[[i]]$windowStart <-afternoon[1]
    sr$model$points[[i]]$windowEnd <- afternoon[2]
  }
}

# sr$model$toString() %>% cat # if you want to display what the model looks like at this point.
sr$model$NumberOfVehicles <- 3
sr$model$VehicleCapacity <- 100
sr$model$distancetype <- 1 # 1 for road network, 2 for euclidean, 3 for haversine.

sr$solveType <- 0  # 0 for optimise, 1 for evaluate, 2 for reoptimise

requestID <- api %>% postSolveRequest(sr)         # submit the model to the api
resp <- api %>% getResponse(requestID)            # retrieve the model response

tab <- resp %>% tabulate(sr)                      # tabulate the data in native-R format.
tab$nodes
tab$edges

resp %>% plotResponse(sr)
resp %>% plotResponseLeaflet(sr)

