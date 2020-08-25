# ns3 model helper script

display<- function(L){ str <- lapply(L, function(i){ paste0(i$toString(), '\n')  }); str %>% unlist %>% cat }

make_distance_time_user_dimensions<- function(name){
  dimconfig <- new (NS3.DimensionConfiguration)
  
  timeConfig <- new (NS3.InternalDimension)
  timeConfig$id <- "time"
  timeConfig$measurementUnit <- 2
  
  distConfig <- new (NS3.InternalDimension)
  distConfig$id <- 'distance'
  distConfig$measurementUnit <- 4
  dimconfig$timeConfig <- timeConfig
  dimconfig$distanceConfig <- distConfig
  
  res <- list()  
  for(n in as.list(name)){
    dim <- new (NS3.UserDimension)
    dim$id <- n
    dim$units <- 'unknown'
    res <- append(res, dim)
  }
  dimconfig$userDimensions <- res
  dimconfig$toString() %>% cat
  return (dimconfig)
}

make_node <- function(id, lng, lat){
  n<- new(NS3.Node)
  n$geocode <- new (NS3.Geocode)
  n$geocode$longitude = as.numeric(lng)
  n$geocode$latitude = as.numeric(lat)
  n$id = as.character(id)
  return (n)
}

make_nodes <- function(rows){
  return(apply(rows, MARGIN = 1, 
              function(i){make_node(i['id'],i['longitude'],i['latitude'])}))
}

make_dimension_range <- function(dimid, min, max){
  dr <- new (NS3.DimensionRange)
  dr$dimensionId <- dimid
  dr$maxRange <- max
  dr$minRange <- min
  dr$flowPenalty <- 1e6
  return (dr)  
}
make_udc <- function(dimid, coef, costperunit){
  udc <- new (NS3.UnitDimensionCost)
  udc$dimensionIds <- dimid
  udc$dimensionCoefficients <- coef
  udc$costPerUnit <- costperunit
  return (udc)  
}

make_lane_rate_distance <- function(src, dest, costperkm){
  lr <- new (NS3.LaneRate)
  lr$id <- paste0("lr:", src, "->", dest)
  lr$source <- src           # from location
  lr$destination <- dest     # to location
  lr$unitDimensionCosts <- make_udc("distance", 1.0, costperkm) # cost per km.
  return (lr)
}

make_lane_rate_distance_weight <- function(src, dest, costperkm, weightdim, costperunit){
  lr <- new (NS3.LaneRate)
  lr$id <- paste0("lr:", src, "->", dest)
  lr$source <- src
  lr$destination <- dest
  if(costperkm != 0){
    # we can have multiple cost coefficients on a paritcular transaction
    lr$unitDimensionCosts<- c(make_udc(weightdim, 1.0, costperunit),
                              make_udc("distance", 1.0, costperkm))
  }else{
    lr$unitDimensionCosts <- make_udc(weightdim, 1.0, costperunit)
  }
  return (lr)  
}
make_single_product_group <- function(product, dimension){
  pg <- new(NS3.ProductGroup)
  pg$productId <- product
  pg$productGroupId <- product
  return(pg)
}
make_cost_model_distance <- function(src, costperkm){
  cm <- new (NS3.CostModel)
  cm$id <- paste0("costmodel: ", src, ":Beer")
  cm$source <- src
  cm$productGroupIds <- "Beer" # you can also leave this blank if all products can be used by this costmodel
  cm$unitDimensionCost <- make_udc("distance", 1.0, costperkm)
  return(cm)  
}
