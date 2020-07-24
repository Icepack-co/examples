### An easy example of how to use the matrix api to generate distance/time matricies
library(iceR)

api <- new("apiHelper", modelType = 'matrix-vyv95n7wchpl', configFile = '../config.json')

data <- read.csv('../sample_data/publist.csv', stringsAsFactors = F)
data<- data[1:5,] #just grab the first 5 locations for this example
data$id %<>% as.character()

model <- new (Matrix.MatrixRequest)

model$locations <- apply(X = data,
                         MARGIN = 1,
                         FUN = function(i){
                           l <- new (Matrix.Location)
                           l$id <- i['id']
                           l$geocode$longitude <- as.numeric(i['X'])
                           l$geocode$latitude <- as.numeric(i['Y'])
                           return(l)
                         })

model$sources <- data$id #note we don't specify the destinations, indicating we want all sources to sources.
model$distanceUnit <- 0
model$durationUnit <- 2
model$toString() %>% cat

requestID <- api %>% postSolveRequest(model)         # submit the model to the api
resp <- api %>% getResponse(requestID)               # retrieve the model response. Takes around a minute to solve
resp$toString() %>% cat                              # the pbf in string format.
tab <- resp %>% tabulate(model)   # this tabulates the response data into a native r-format.

tab$elements %>% head
tab$elements %>% nrow # we get 20 elements back, which is 5*(5-1) (because the diagonal is always zero.)

# the response is tabulated in long form (i.e. "tidy").
# You might want it in matrix-form (or a wide-ish form), just use reshape2 to do the heavy lifting here.
tab$elements$fromId %>% unique
tab$elements$toId %>% unique
tab$elements %>% reshape2::dcast(fromId ~ toId, value.var = 'distance', fill = 0) #distance matrix #install.packages("reshape2") if not installed.
tab$elements %>% reshape2::dcast(fromId ~ toId, value.var = 'duration', fill = 0) #time matrix
#Note, if you leave the fill=0 off, you'll get NA's on the diagonal, which may or may not be what you want.

