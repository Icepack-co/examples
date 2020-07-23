### An intermediate example of how to use the matrix api to generate distance/time matricies
library(iceR)

api <- new("apiHelper", modelType = 'matrix-vyv95n7wchpl', configFile = '../config.json')

data <- read.csv('../sample_data/publist.csv', stringsAsFactors = F)
data<- data[1:6,] #just grab the first 6 locations for this example
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
# create a 2x4 matrix. locations 1:2 to locations 3:6. Returns a total of 2*4 elements.
model$sources <- data$id[1:2]
model$destinations <- data$id[3:6]
model$distanceUnit <- 1
model$durationUnit <- 1
model$toString() %>% cat

requestID <- api %>% postSolveRequest(model)         # submit the model to the api
resp <- api %>% getResponse(requestID)               # retrieve the model response. Takes around a minute to solve
resp$toString() %>% cat                              # the pbf in string format.
tab <- resp %>% tabulate(model)   # this tabulates the response data into a native r-format.

tab$elements %>% head
# the response is tabulated in long form (i.e. "tidy").
# You might want it in matrix-form (or a wide-ish form), just use reshape2 to do the heavy lifting here.
tab$elements$fromId %>% unique
tab$elements$toId %>% unique
# note there is no overlap between the from's and to's based on the way we constructed the query.
tab$elements %>% reshape2::dcast(fromId ~ toId, value.var = 'distance') #distance matrix #install.packages("reshape2") if not installed.
tab$elements %>% reshape2::dcast(fromId ~ toId, value.var = 'duration') #time matrix

