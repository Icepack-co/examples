# CVRPTW EXAMPLE SCRIPT

library(iceR)

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'cvrptw-acyas3nzweqb', configFile = '../config.json')

# Create your model
sr <- new (CVRPTW.SolveRequest)
sr$model <- new (CVRPTW.CVRPTW)

# Add some data to the model
data <- read.csv('../sample_data/publist_orders.csv')[1:10,]

makePoint <- function(i){
  g <- new (CVRPTW.Geocode)
  g$id <- data$id[i]
  g$x <- data$X[i]
  g$y <- data$Y[i]
  g$quantity <- data$quantity[i]
  return (g)
}
sr$model$points <-sapply(2:nrow(data), makePoint)

sr$model$depot <- makePoint(1) # we're not going to add any windows on the depot.

morning <- c(8*60, 12*60)
afternoon <- c(12*60, 16 * 60)
for(i in 1:length(sr$model$points)){
  if(i %% 2 == 0){
    sr$model$points[[i]]$windowStart <-morning[1]
    sr$model$points[[i]]$windowEnd <- morning[2]
  }else{
    sr$model$points[[i]]$windowStart <-afternoon[1]
    sr$model$points[[i]]$windowEnd <- afternoon[2]
  }
}

sr$model$NumberOfVehicles <- 3
sr$model$VehicleCapacity <- 100
sr$solveType <- 0  # 0 for optimise, 1 for evaluate, 2 for reoptimise

# Set the distance type to use the road network
sr$model$distancetype <- 1 # 1 for road network, 2 for euclidean, 3 for haversine      

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

# Plot the data 
resp %>% plotResponse(sr) # ggplot
resp %>% plotResponseLeaflet(sr) # leaflet (if it's mappable)