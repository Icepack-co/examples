# TSPTW EXAMPLE SCRIPT (BASIC)

library(iceR)

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'tsptw-kcxbievqo879', configFile = '../config.json')

# Create the model
sr <- new (TSPTW.SolveRequest)
sr$model <- new (TSPTW.TSP)

# Add some data to the model
data <- read.csv('../sample_data/publist.csv')[1:10,]

rupper <- 2500
data <- data[sample(1:nrow(data), size = nrow(data), replace = F),]
data$WindowStart <- runif(nrow(data), 0, rupper)
data$WindowEnd <- data$WindowStart + rupper

sr$model$points <- apply(X = data,
                         MARGIN = 1,
                         FUN = function(i){
                           g <- new (TSPTW.Geocode)
                           g$id <- as.character(i['id'])
                           g$x <- as.numeric(i['X'])
                           g$y <- as.numeric(i['Y'])
                           g$windowStart <- as.numeric(i['WindowStart'])
                           g$windowEnd <- as.numeric(i['WindowEnd'])
                           return(g)
                         }) # there are many ways to build protobuf. This is one simple way using old-school R functions.
sr$model$points[[1]]$windowStart <- sr$model$points[[1]]$windowEnd <- c()

# Set the distance type to use the road network
sr$model$distancetype <- 1 # 1 for road network, 2 for euclidean, 3 for haversine      

# View the model in plain-text format (string or JSON)
sr$model$toString() %>% cat
sr$model$toJSON() %>% cat

# Submit the model to the API
requestID <- api %>% postSolveRequest(sr)   

# Retrieve the model response
resp <- api %>% getResponse(requestID) 
resp$arrivalTimes

# Tabulate the data in native-R format
tab <- resp %>% tabulate(sr)               
tab$nodes %>% head # just inspect the head of the output
tab$edges %>% head # just inspect the head of the output

tab$edges %>% arrange(sequence)

# Plot the data 
resp %>% plotResponse(sr) # ggplot
resp %>% plotResponseLeaflet(sr) # leaflet (if it's mappable)