# NS3 EXAMPLE SCRIPT (ADVANCED)

library(iceR)
library(dplyr)

source('ns3-model-helper.R')

# Create an apiHelper object with the model type you'd like to solve
api <- new("apiHelper", modelType = 'ns3-tbfvuwtge2iq', configFile = '../config.json')

# Create the model
sr <- new (NS3.SolveRequest)
sr$model <- new (NS3.Model)

# Add some data to the model (and add some geometry for distance calculations)
data <- read.csv('../sample_data/publist_large.csv')[1:100,]

data$geometry<- st_geometry(st_as_sf(data,coords = c("longitude", "latitude")))
data$geometry<- st_set_crs(data$geometry, "+proj=longlat +datum=WGS84")

sr$model$dimensions<- make_distance_time_user_dimensions("weight")
sr$model$dimensions$toString() %>% cat

productionNodes <- data %>% filter(demand == -1)
warehouseNodes <-  data %>% filter(demand == -2)
demandNodes <- data %>% filter(demand > 0)

p_nodes <- make_nodes(productionNodes)
w_nodes <- make_nodes(warehouseNodes)

for(i in 1:length(w_nodes)){
  w_nodes[[i]]$flow <- new (NS3.Node.Flow)
  w_nodes[[i]]$flow$FixedDimensionCosts <- new (NS3.FixedDimensionCost)
  w_nodes[[i]]$flow$FixedDimensionCosts[[1]]$dimensionIds <- 'weight'
  w_nodes[[i]]$flow$FixedDimensionCosts[[1]]$fixedCost <- 10000
}

d_nodes <- make_nodes(demandNodes)

p_nodes %>% display
w_nodes %>% display

sources <- data %>% filter(demand < 0) %>% select(id) %>% unlist() %>% as.character()

for(i in 1:length(d_nodes)){
  pf <- new (NS3.Node.ProductFlow)
  pf$productId <- "Beer"
  pf$dimensionRanges <- make_dimension_range("weight", demandNodes$demand[i], demandNodes$demand[i])
  d_nodes[[i]]$consumption <-pf
  allowableSources <- warehouseNodes$id
  if(as.numeric(
    st_distance(demandNodes$geometry[i], d$geometry[1], which = "Great Circle") / 1000) < 50){
    allowableSources <- c(allowableSources, productionNodes$id)
  }
  d_nodes[[i]]$allowableSources <- allowableSources
  d_nodes[[i]]$maximumSources <- 1
}

for(i in 1:length(p_nodes)){
  pf <- new (NS3.Node.ProductFlow)
  pf$productId <- "Beer"
  pf$dimensionRanges <-  make_dimension_range('weight', 0, 0.9 * sum(demandNodes$demand))
  pf$dimensionRanges[[1]]$flowPenalty <- 1e8  
  p_nodes[[i]]$production <- pf
}

sr$model$nodes <- c(p_nodes, w_nodes, d_nodes)

sr$model$laneRates <- c(make_lane_rate_distance(sources[1], sources[2], 0.1),
                 make_lane_rate_distance(sources[1], sources[3], 0.1))
sr$model$laneRates %>% display

sr$model$productGroups <- make_single_product_group("Beer", "weight")
sr$model$productGroups %>% display

sr$model$costModels <- list()
for(i in 1:length(sources)){
  sr$model$costModels <- append(sr$model$costModels, make_cost_model_distance(sources[i], 0.2))
}
sr$model$costModels %>% display

sr$geometryOutput <- 1

sr$solveType <- 0 # 0 for optimise, 1 for evaluate, 2 for reoptimise

# Submit the model to the API
requestID <- api %>% postSolveRequest(sr)

# Retrieve the model response
resp <- api %>% getResponse(requestID)

# Tabulate the data in native-R format
tab <- resp %>% tabulate(sr)
tab$nodeProductFlow %>% filter(consumptionPenalty > 0) #so we have 
tab$nodeProductFlow$consumptionAmount %>% sum # which is 90% of the total.

tab$nodeProductFlow %>% filter(productionPenalty > 0) 
tab$assignments %>% head
tab$nodeFlow %>% head 
tab$nodeProductFlow %>% head
tab$routes %>% head

# Plot the data 
resp %>% plotResponse(sr) # ggplot
resp %>% plotResponseLeaflet(sr) # leaflet (if it's mappable)
