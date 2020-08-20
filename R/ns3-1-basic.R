### A basic network sourcing model
### uses one production, two intermediate and multiple consumption locations.
### We're moving one product "Beer" measured in the dimension "weight"
### How are movements in the network costed?
### - We have two lane rates between our main production center and the two warehouses
### - and a distribution "Cost Model" between the sources (proudction + 2 x warehouses) and consumption nodes
### - Lane rates between Production and Intermediate nodes are costed on a cost per km basis.
### - Cost models to distribute the quantities further is also based on a (more expensive) cost per km.
### - It's typical that high utilisation vehicles move between warehouses (and typically larger vehicles, achieving a lower cost per km / cost per ton)
### - And that smaller vehicles handle the secondary distribution (at a higher cost per ton)

rm(list =ls())
library(iceR)
library(dplyr)
source('ns3-model-helper.R')

# d is going to be our main demand set (it's a list of pubs)
d <- read.csv('../sample_data/publist_large.csv')

d<- d[1:100,] # free tier key, model too large
d %>% nrow

api <- new("apiHelper", modelType = 'ns3-tbfvuwtge2iq', configFile = '../config.json')

#lets instantiate a model container so that we can build out a model
m <- new (NS3.Model)

# we're going to employ the model helper here
# lets use a single dimension for this model of weight.
m$dimensions<- make_distance_time_user_dimensions("weight")
m$dimensions$toString() %>% cat

productionNodes <- d %>% filter(demand == -1)
warehouseNodes <-  d %>% filter(demand == -2)
demandNodes <- d %>% filter(demand > 0)

p_nodes <- make_nodes(productionNodes)
w_nodes <- make_nodes(warehouseNodes)
d_nodes <- make_nodes(demandNodes)

p_nodes %>% display # so we've just defined the Guiness storehouse
w_nodes %>% display # and here we have to warehouses, one in gallway, one in limerick

#lets assume we can go factory-direct or through a warehouse!
sources <- d %>% filter(demand < 0) %>% select(id) %>% unlist() %>% as.character()

# lets continue to make some alterations.
# we know that demand nodes require us to fulfill the demand at the node.
# lets assume we have no production constraints

# we have a reasonably even demand profile - something tells us this data-set is not real! :-) 
# demandNodes$demand %>% hist
# in order to specify which demands we should satisfy, lets place a flow requirement at each node.

for(i in 1:length(d_nodes)){
  pf <- new (NS3.Node.ProductFlow)
  pf$productId <- "Beer"
  # each demand node must have the quantity demand[i] delivered, so the range here
  # is actually [demand[i], demand[i]]. 
  # Not meeting this range incurs a large penalty cost.
  pf$dimensionRanges <- make_dimension_range("weight", demandNodes$demand[i], demandNodes$demand[i])
  d_nodes[[i]]$consumption <-pf
  d_nodes[[i]]$allowableSources <- sources # all sources are allowable.
}
for(i in 1:length(p_nodes)){
  pf <- new (NS3.Node.ProductFlow)
  pf$productId <- "Beer"
  # the production node has no limit on the amount that can be produced. 
  # so we can simply set the upper bound to the sum of all demand, i.e. [0, sum(demands)]
  # this way we know that the facility can produce enough to satisfy all the demand
  pf$dimensionRanges <-  make_dimension_range('weight', 0, sum(demandNodes$demand))
  pf$dimensionRanges[[1]]$flowPenalty <- 0  
  p_nodes[[i]]$production <- pf
}

# d_nodes %>% display
# w_nodes %>% display
# p_nodes %>% display 

m$nodes <- c(p_nodes, w_nodes, d_nodes)

# so  Guiness Storehouse -> Limerick
# and Guinnes Storehouse -> Galway
# each costed at 0.1 monetary units per km.
m$laneRates <- c(make_lane_rate_distance(sources[1], sources[2], 0.1),
                 make_lane_rate_distance(sources[1], sources[3], 0.1))

m$laneRates %>% display

m$productGroups <- make_single_product_group("Beer", "weight")

m$productGroups %>% display

m$costModels <- list()
for(i in 1:length(sources)){
  m$costModels <- append(m$costModels, make_cost_model_distance(sources[i], 0.2))
}
m$costModels %>% display

sr <- new (NS3.SolveRequest)
sr$model <- m
sr$geometryOutput <- 1
sr$solveType <- 0

requestID <- api %>% postSolveRequest(sr)
resp <- api %>% getResponse(requestID)
resp %>% plotResponse(sr)
resp %>% plotResponseLeaflet(sr)
tab$assignments %>% head

tab <- resp %>% tabulate(sr)
tab$assignments %>% head
tab$nodeFlow %>% head
tab$nodeProductFlow %>% head
tab$routes %>% head
