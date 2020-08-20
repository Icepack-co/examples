def make_distance_time_user_dimensions(name):
    dimensions = ns3_tbfvuwtge2iq_pb2.DimensionConfiguration()
    dimensions.timeConfig.id = 'time'
    dimensions.timeConfig.measurementUnit = 2
    dimensions.distanceConfig.id = 'distance'
    dimensions.distanceConfig.measurementUnit = 4

    if name != '':
        capdim = ns3_tbfvuwtge2iq_pb2.UserDimension()
        capdim.id = name
        capdim.units = 'unknown'
        dimensions.userDimensions.extend([capdim])

    return dimensions

def make_node(id, lng, lat):
    n = ns3_tbfvuwtge2iq_pb2.Node()
    n.geocode.longitude = lng
    n.geocode.latitude = lat
    n.id = id
    return n

def make_nodes(data):
    p = list()
    for index, row in data.iterrows():
        p.append(make_node(row['id'], row['longitude'], row['latitude']))
    return p

def make_dimension_range(dimid, _min, _max):
    dr = ns3_tbfvuwtge2iq_pb2.DimensionRange()
    dr.dimensionId = dimid
    dr.maxRange = _max
    dr.minRange = _min
    dr.flowPenalty = 1e6
    return (dr)


def make_udc(dimid, coef, costperunit):
    udc = ns3_tbfvuwtge2iq_pb2.UnitDimensionCost()
    udc.dimensionIds.append(dimid)
    udc.dimensionCoefficients.append(coef)
    udc.costPerUnit = costperunit
    return (udc)  


def make_lane_rate_distance(src, dest, costperkm):
    lr = ns3_tbfvuwtge2iq_pb2.LaneRate()
    lr.id = "lr:" + src + "->" + dest
    lr.source = src           # from location
    lr.destination = dest     # to location
    lr.unitDimensionCosts.append(make_udc("distance", 1.0, costperkm)) # cost per km.
    return (lr)


def make_lane_rate_distance_weight(src, dest, costperkm, weightdim, costperunit):
    lr = ns3_tbfvuwtge2iq_pb2.LaneRate()
    lr.id = "lr:"+ src + "->" + dest
    lr.source = src
    lr.destination = dest
    if costperkm != 0:
    # we can have multiple cost coefficients on a paritcular transaction
        lr.unitDimensionCosts.append(make_udc(weightdim, 1.0, costperunit))
        lr.unitDimensionCosts.append(make_udc("distance", 1.0, costperkm))
    else:
        lr.unitDimensionCosts.append(make_udc(weightdim, 1.0, costperunit))
    return (lr)  

def make_single_product_group(product, dimension):
    pg = ns3_tbfvuwtge2iq_pb2.ProductGroup()
    pg.productId = product
    pg.productGroupId = product
    return(pg)

def make_cost_model_distance(src, costperkm):
    cm = ns3_tbfvuwtge2iq_pb2.CostModel()
    cm.id = "costmodel: " + src + ":Beer"
    cm.source = src
    cm.productGroupIds.append("Beer") # you can also leave this blank if all products can be used by this costmodel
    cm.unitDimensionCost.append(make_udc("distance", 1.0, costperkm))
    return(cm)  

