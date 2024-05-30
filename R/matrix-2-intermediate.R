# MATRIX EXAMPLE SCRIPT (INTERMEDIATE)

library(iceR)

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'matrix-vyv95n7wchpl', configFile = '../config.json')

# Create the model
model <- new (Matrix.MatrixRequest)

# Add some data to the model
data <- read.csv('../sample_data/publist.csv', stringsAsFactors = F)[1:6,]

model$locations <- apply(X = data,
                         MARGIN = 1,
                         FUN = function(i){
                           l <- new (Matrix.Location)
                           l$id <- i['id']
                           l$geocode$longitude <- as.numeric(i['X'])
                           l$geocode$latitude <- as.numeric(i['Y'])
                           return(l)
                         })

model$sources <- data$id[1:2]
model$destinations <- data$id[3:6]
model$distanceUnit <- 1
model$durationUnit <- 1

# View the model in plain-text format (string or JSON)
model$toString() %>% cat
model$toJSON() %>% cat

# Submit the model to the API
requestID <- api %>% postSolveRequest(model)

# Retrieve the model response
resp <- api %>% getResponse(requestID)
resp$toString() %>% cat # the pbf in string format

# Get the solve time
solveTime <- api %>% getSolveTime(requestID)

# Tabulate the data in native-R format
tab <- resp %>% tabulate(model)     
tab$elements %>% head
tab$elements %>% nrow 

# Visualise using reshape2
library(reshape2)

tab$elements$fromId %>% unique
tab$elements$toId %>% unique
tab$elements %>% reshape2::dcast(fromId ~ toId, value.var = 'distance', fill = 0) 
tab$elements %>% reshape2::dcast(fromId ~ toId, value.var = 'duration', fill = 0)