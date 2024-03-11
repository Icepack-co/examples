### An advanced network sourcing model
### uses one production, two intermediate and multiple consumption locations.
### We're moving one product "Beer" measured in the dimension "weight"
### How are movements in the network costed?
### - Same configuration as the intermediate-2 example:
###   * We have two lane rates between our main production center and the two warehouses
###   * and a distribution "Cost Model" between the sources (proudction + 2 x warehouses) and consumption nodes
###   * Lane rates between Production and Intermediate nodes are costed on a cost per km basis.
###   * Cost models to distribute the quantities further is also based on a (more expensive) cost per km.
###   * It's typical that high utilisation vehicles move between warehouses (and typically larger vehicles, achieving a lower cost per km / cost per ton)
###   * And that smaller vehicles handle the secondary distribution (at a higher cost per ton)
###   * adding a fixed cost trigger for using a warehouse
###   * the allows modelling selecting between the two warehouses, or potentially using both.
### - We're going to add a constraint where deliveries from the guiness storehouse may only be 
###   made withing a radius of 50 km's. This is typical when you need to restrict a fleet to 
###   a vehicle type (like a secondary delivery vehicle) which you don't want leaving a major city
### - we're also going to add a constraint where only 90% of the volume can be serviced by the production
###   node, which means we'll incur consumption penalties, but that's okay.

rm(list =ls())
library(iceR)
library(dplyr)
source('ns3-model-helper.R')


# d is going to be our main demand set (it's a list of pubs)
d <- read.csv('../sample_data/publist_large.csv')

d<- d[1:100,] # free tier key, model too large
d$id %<>% as.character()
d %>% nrow
d$geometry<- st_geometry(st_as_sf(d,coords = c("longitude", "latitude")))
d$geometry<- st_set_crs(d$geometry, "+proj=longlat +datum=WGS84")
# just adding some geometry here for distance calculations

api <- new("apiHelper", modelType = 'ns3-tbfvuwtge2iq', configFile = '../config.json')

#let's instantiate a model container so that we can build out a model
m <- new (NS3.Model)

# we're going to employ the model helper here
# let's use a single dimension for this model of weight.
m$dimensions<- make_distance_time_user_dimensions("weight")
m$dimensions$toString() %>% cat

productionNodes <- d %>% filter(demand == -1)
warehouseNodes <-  d %>% filter(demand == -2)
demandNodes <- d %>% filter(demand > 0)

p_nodes <- make_nodes(productionNodes)
w_nodes <- make_nodes(warehouseNodes)

# here is where we add the fixed costs to the warehouse nodes.
for(i in 1:length(w_nodes)){
  w_nodes[[i]]$flow <- new (NS3.Node.Flow)
  w_nodes[[i]]$flow$FixedDimensionCosts <- new (NS3.FixedDimensionCost)
  w_nodes[[i]]$flow$FixedDimensionCosts[[1]]$dimensionIds <- 'weight'
  w_nodes[[i]]$flow$FixedDimensionCosts[[1]]$fixedCost <- 10000
}

d_nodes <- make_nodes(demandNodes)

p_nodes %>% display # so we've just defined the Guiness storehouse
w_nodes %>% display # and here we have to warehouses, one in gallway, one in limerick

#let's assume we can go factory-direct or through a warehouse!
sources <- d %>% filter(demand < 0) %>% select(id) %>% unlist() %>% as.character()

# let's continue to make some alterations.
# we know that demand nodes require us to fulfill the demand at the node.
# let's assume we have no production constraints

# we have a reasonably even demand profile - something tells us this data-set is not real! :-) 
# demandNodes$demand %>% hist
# in order to specify which demands we should satisfy, let's place a flow requirement at each node.

for(i in 1:length(d_nodes)){
  pf <- new (NS3.Node.ProductFlow)
  pf$productId <- "Beer"
  # each demand node must have the quantity demand[i] delivered, so the range here
  # is actually [demand[i], demand[i]]. 
  # Not meeting this range incurs a large penalty cost.
  pf$dimensionRanges <- make_dimension_range("weight", demandNodes$demand[i], demandNodes$demand[i])
  d_nodes[[i]]$consumption <-pf
  allowableSources <- warehouseNodes$id
  if(as.numeric(
    st_distance(demandNodes$geometry[i,], d$geometry[1], which = "Great Circle") / 1000) < 50){
    allowableSources <- c(allowableSources, productionNodes$id)
  }
  d_nodes[[i]]$allowableSources <- allowableSources
  d_nodes[[i]]$maximumSources <- 1
}

for(i in 1:length(p_nodes)){
  pf <- new (NS3.Node.ProductFlow)
  pf$productId <- "Beer"
  # We're going to limit production to 90% of the total so that we pick the cheapest
  # subset of demand which gets us close to that 90% of the total at minimum cost.
  pf$dimensionRanges <-  make_dimension_range('weight', 0, 0.9 * sum(demandNodes$demand))
  pf$dimensionRanges[[1]]$flowPenalty <- 1e8  
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
# This is again quite interesting. 
# We're essentally opening two warehouses now and moving product back through the network
# to get to the outskirts of Dublin (which we expected by construction)
# We're also using both warehouses now. If the fixed cost was higher, we may find that we 
# would go back to selecting just one. Clearly we should explore a warehouse in Dublin that 
# could reach more of the surrounding area at a lower cost. But this is simply modelling details now.
# Note that some nodes go unserviced - this is because we don't have production capacity anymore 
# to service all nodes.

tab <- resp %>% tabulate(sr)
tab$nodeProductFlow %>% filter(consumptionPenalty > 0) #so we have 
tab$nodeProductFlow$consumptionAmount %>% sum # which is 90% of the total.

tab$nodeProductFlow %>% filter(productionPenalty > 0) 
tab$assignments %>% head
tab$nodeFlow %>% head 
tab$nodeProductFlow %>% head
tab$routes %>% head

