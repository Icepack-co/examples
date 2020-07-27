# test script
library(iceR)

# create an api-helper object with the model type you'd like to solve.
api <- new("apiHelper", modelType = 'tsp-mcvfz472gty6', configFile = '../config.json')

# create your model
sr <- new (TSP.SolveRequest)

sr$model <- new (TSP.TSP)

# add some data to the model
data <- read.csv('../sample_data/publist.csv', stringsAsFactors = F)

# there are many ways to build protobuf. This is one simple way using old-school R functions.
sr$model$points <- apply(X = data,
                         MARGIN = 1,
                         FUN = function(i){
                                          g <- new (TSP.Geocode)
                                          g$id <- as.character(i['id'])
                                          g$x <- as.numeric(i['X'])
                                          g$y <- as.numeric(i['Y'])
                                          return(g)
                                        })
sr$model$distancetype <- 1      # set the distance type to use the road-network

# if you'd like to view the model in plain-text format simply
sr$model$toString() %>% cat
sr$model$toJSON() %>% cat

requestID <- api %>% postSolveRequest(sr)         # submit the model to the api
resp <- api %>% getResponse(requestID)            # retrieve the model response

tab <- resp %>% tabulate(sr)                # tabulate the data in native-R format.
tab$edges %>% head
#tab$nodes %>% head                              # just inspect the head of the output data
#tab$edges %>% head                              # just inspect the head of the output data

resp %>% plotResponse(sr)                       # plot the data using ggplot. always looks nice.
resp %>% plotResponseLeaflet(sr)                # plot the data using leaflet (assuming it's map-able)
