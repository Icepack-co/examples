# TSPTW - test script
# devtools::load_all()
library(iceR)

# create an api-helper object with the model type you'd like to solve.
api <- new("apiHelper", modelType = 'tsptw-kcxbievqo879', configFile = '../config.json')

# create your model
sr <- new (TSPTW.SolveRequest)
sr$model <- new (TSPTW.TSP)

# add some data to the model
data <- read.csv('../sample_data/publist.csv')[1:10,]
# there are many ways to build protobuf. This is one simple way using old-school R functions.
rupper <- 2500
data <- data[sample(1:nrow(data), size = nrow(data), replace = F),]
# just randomly shuffle the selected points. We're making somewhat random windows below
# and if you run the script again you'll want to see a different result

data$WindowStart <- runif(nrow(data), 0, rupper)
                      #this creates some random time windows, with starts in [0,2500] and ends in
                      # [0,5000]. These are hard windows that have to be respected in terms of arrival
                      # so a "vehicle" will wait for the window to start if it arrives early at a location.

data$WindowEnd <- data$WindowStart + rupper
# we don't accept backwards windows, so we'll just set these to some positive width upper amount.

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
                         })
sr$model$points[[1]]$windowStart <- sr$model$points[[1]]$windowEnd <- c()
sr$model$distancetype <- 1      # set the distance type to use the road-network

# if you'd like to view the model in plain-text format simply
sr$model$toString() %>% cat

requestID <- api %>% postSolveRequest(sr)         # submit the model to the api

resp <- api %>% getResponse(requestID)            # retrieve the model response

# a quick note. The objective with a tsp-tw is to minimise the total travel time.
# this means that a slightly longer route might be taken in terms of km-distance in order to
# effectively use the available time. Helps if you're working with real-world data for this example.

tab <- resp %>% tabulate(sr)                    # tabulate the data in native-R format.
tab$edges %>% arrange(sequence)
# What can happen now, which doesn't with a tsp, is that we may end up with unsatisfiable
# windows. In other words, try to execute all these orders, but the windows don't permit it
# to be done within the constraints. In this situation, we do as many of the stops as possible.
# Just randomise the data again if this happens.
resp$arrivalTimes

tab$nodes %>% head                              # just inspect the head of the output data
tab$edges %>% head                              # just inspect the head of the output data

resp %>% plotResponse(sr)                       # plot the data using ggplot. always looks nice.
resp %>% plotResponseLeaflet(sr)                # plot the data using leaflet (assuming it's mappable)

# because of the time windows, some stops may be ommitted (infeasible) and the sequence is probably
# not going to be the shortest in distance, but will be the shortest in time.
