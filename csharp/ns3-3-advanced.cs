
// An advanced network sourcing model
// uses one production, two intermediate and multiple consumption locations.
// We're moving one product "Beer" measured in the dimension "weight"
// How are movements in the network costed?
// - Same configuration as the intermediate-2 example:
//   * We have two lane rates between our main production center and the two warehouses
//   * and a distribution "Cost Model" between the sources (proudction + 2 x warehouses) and consumption nodes
//   * Lane rates between Production and Intermediate nodes are costed on a cost per km basis.
//   * Cost models to distribute the quantities further is also based on a (more expensive) cost per km.
//   * It's typical that high utilisation vehicles move between warehouses (and typically larger vehicles, achieving a lower cost per km / cost per ton)
//   * And that smaller vehicles handle the secondary distribution (at a higher cost per ton)
//   * adding a fixed cost trigger for using a warehouse
//   * the allows modelling selecting between the two warehouses, or potentially using both.
// - We're going to add a constraint where deliveries from the guiness storehouse may only be 
//   made withing a radius of 50 km's. This is typical when you need to restrict a fleet to 
//   a vehicle type (like a secondary delivery vehicle) which you don't want leaving a major city
// - we're also going to add a constraint where only 90% of the volume can be serviced by the production
//   node, which means we'll incur consumption penalties, but that's okay.

using System.Collections.Generic;
using System.Linq;
using System;

class ns3advanced : IRunner
{
  public ns3advanced(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data;
  }

  // Convert the degrees into radians.
  private double DegreesToRadians(double degrees)
  {
    return degrees * Math.PI / 180.0;
  }

  private double GreatCircleDistance(double lat1, double lon1, double lat2, double lon2)
  {
    const double radius = 6371; // Radius of the Earth in km.
    lat1 = DegreesToRadians(lat1);
    lon1 = DegreesToRadians(lon1);
    lat2 = DegreesToRadians(lat2);
    lon2 = DegreesToRadians(lon2);
    double d_lat = lat2 - lat1;
    double d_lon = lon2 - lon1;
    double h = Math.Sin(d_lat / 2) * Math.Sin(d_lat / 2) + Math.Cos(lat1) * Math.Cos(lat2) * Math.Sin(d_lon / 2) * Math.Sin(d_lon / 2);
    return 2 * radius * Math.Asin(Math.Sqrt(h));
  }

  private double GreatCircleDistance(Ns3.Node n1, Ns3.Node n2)
  {
    return GreatCircleDistance(n1.Geocode.Latitude, n1.Geocode.Longitude, n2.Geocode.Latitude, n2.Geocode.Longitude);
  }

  public void Run()
  {
    var api = new ApiHelper<Ns3.SolveRequest, Ns3.SolutionResponse>("ns3-tbfvuwtge2iq", configFile);

    // so here we're going to build the model 
    var m = new Ns3.Model();
    m.Dimensions = ns3helper.make_distance_time_user_dimensions("weight");

    var productionNodes = data.Where(q => q.quanity == -1).ToList();
    var warehouseNodes = data.Where(q => q.quanity == -2).ToList();
    var demandNodes = data.Where(q => q.quanity > 0).ToList();

    var p_nodes = ns3helper.make_nodes(productionNodes);
    var w_nodes = ns3helper.make_nodes(warehouseNodes);
    var d_nodes = ns3helper.make_nodes(demandNodes);

    // lets assume we can go factory-direct or through a warehouse!
    var sources = data.Where(q => q.quanity < 0).Select(t => t.id).ToList();


    // here is where we add the fixed costs to the warehouse nodes.
    foreach (var w in w_nodes)
    {
      w.flow = new Ns3.Node.Flow
      {
        FixedDimensionCosts = {
          new Ns3.FixedDimensionCost{
            fixedCost = 10000,
            dimensionIds = {"weight"}
            // so if any quantity passes through this warehouse, a cost of 10,000 is incurred.
          }
        }
      };
    }



    // lets continue to make some alterations.
    // we know that demand nodes require us to fulfill the demand at the node.
    // lets assume we have no production constraints

    // we have a reasonably even demand profile - something tells us this data-set is not real! :-) 
    // demandNodes$demand %>% hist
    // in order to specify which demands we should satisfy, lets place a flow requirement at each node.
    for (int i = 0; i < d_nodes.Count; i++)
    {
      // each demand node must have the quantity demand[i] delivered, so the range here
      // is actually [demand[i], demand[i]]. 
      // Not meeting this range incurs a large penalty cost.
      d_nodes[i].Consumptions.Add(
      new Ns3.Node.ProductFlow
      {
        productId = "Beer",
        dimensionRanges = { ns3helper.make_dimension_range("weight", demandNodes[i].quanity, demandNodes[i].quanity) }
      }
      );
      // we're only going to allow the source Guiness Storehouse if it's within 50km's (as the crow flies)
      // from the location of the node.
      var allowableSources = warehouseNodes.Select(t => t.id).ToList();
      if (GreatCircleDistance(p_nodes[0], d_nodes[i]) < 50)
      {
        allowableSources.Add(p_nodes[0].Id);
      }

      d_nodes[i].allowableSources.AddRange(allowableSources);
      d_nodes[i].maximumSources = 1;
    }

    for (int i = 0; i < p_nodes.Count; i++)
    {
      // the production node has no limit on the amount that can be produced. 
      // so we can simply set the upper bound to the sum of all demand, i.e. [0, sum(demands)]
      // this way we know that the facility can produce enough to satisfy all the demand
      p_nodes[i].Productions.Add(
        new Ns3.Node.ProductFlow
        {
          productId = "Beer",
          dimensionRanges = { ns3helper.make_dimension_range("weight", 0, 0.9f * demandNodes.Sum(t => t.quanity)) }
        }
      );
      p_nodes[i].Productions[0].dimensionRanges[0].flowPenalty = 1e8f;
    }

    m.Nodes.AddRange(p_nodes);
    m.Nodes.AddRange(w_nodes);
    m.Nodes.AddRange(d_nodes);

    // so  Guiness Storehouse -> Limerick
    // and Guinnes Storehouse -> Galway
    // each costed at 0.1 monetary units per km.
    m.laneRates.Add(ns3helper.make_lane_rate_distance(sources[0], sources[1], 0.1f));
    m.laneRates.Add(ns3helper.make_lane_rate_distance(sources[0], sources[2], 0.1f));

    m.productGroups.Add(ns3helper.make_single_product_group("Beer"));

    for (int i = 0; i < sources.Count; i++)
    {
      m.costModels.Add(ns3helper.make_cost_model_distance(sources[i], 0.2f));
    }

    Ns3.SolveRequest sr = new Ns3.SolveRequest
    {
      Model = m,
      geometryOutput = Ns3.SolveRequest.GeometryOutput.Aggregate,
      solveType = Ns3.SolveRequest.SolveType.Optimise
    };


    // now it's just sending the model to the api
    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)

    ns3helper.printSolution(Solution, sr);
    // you'll notice now that both warehouses are being used with the changes made.
    // There are some consumption penalties incurred and the production amount is exactly at 90% of the total demand (as requested)

    // for nice visualisations see the R/python notebook for the same example.
    return;
  }

  public Ns3.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}