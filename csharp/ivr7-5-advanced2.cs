// IVR7 Advanced example:
// Purpose: illustrate the usage of the data-upload as well as model versioning.
// * Builds a simple pickup/dropoff (similar to the basic model).
// * Configures "open routing" - i.e. do not cost the return legs for the vehicles to the end-location
// * Illustrates how to reference matrix data in a (potentially) versioned model.
// * or load overriding elements on a particular matrix directly.
//     (one can provide a complete matrix in this manner too)


using System.Collections.Generic;
using System;

class ivr7advanced2 : IRunner
{
  public ivr7advanced2(List<dataRow> data, string configFile = "../config.json")
  {
    this.configFile = configFile;
    this.data = data;
  }

  public void Run()
  {
    var api = new ApiHelper<Ivr7.SolveRequest, Ivr7.SolutionResponse>("ivr7-kt461v8eoaif", configFile);
    // so here we're going to build the model 

    var m = new Ivr7.Model(); // initialise the model container

    // we're going to reuse the helpers described in the ivr7basic example. Please see that for an initial reference.
    // We want "Open Routing" which basically means that if a vehicle finishes it's day at any node,
    // it is then not supposed to cost the return trip home. So as a start, we need to separate the
    // depot (Guiness Storehouse) from the vehicle-home location by name - it's okay if it's at the
    // same point (long/lat), but we're going to separate the identifier so we can control the
    // distance/time matrix nicely.

    ivr7helper.makeDistanceTimeCapDims(m); // adds distance, time & capacity

    ivr7helper.makeLocations(m, data);     // adds all the locations to the model
    // we're going to add an exta location; the "vehicle-site"
    m.Locations.Add(new Ivr7.Location
    {
      Id = "vehicle-site",
      Geocode = m.Locations[0].Geocode  // use the geocode of the Guiness storehouse.
    });

    ivr7helper.makeJobTimeCap(m, data, ivr7helper.Rep(0, data.Count - 1), ivr7helper.Seq(1, data.Count));
    m.vehicleCostClasses.Add(ivr7helper.makeVccSimple("vcc1", 1000, 0.01f, 0.01f, 0.01f, 1, 3));
    m.vehicleClasses.Add(ivr7helper.makeVcSimple("vc1", 1, 1, 1, 1));
    for (int i = 0; i < 5; i++)
    {
      m.Vehicles.Add(ivr7helper.makeVehicleCap("vehicle_" + i, // unique id for the vehicle.
                                                      "vc1",  // the vehicle class
                                                      "vcc1", // the vehicle cost class
                                                      2000,  // the capacity of the vehicle
                                                      "vehicle-site", // start location for the vehicle //NOTE the change here.
                                                      "vehicle-site", // end location for the vehicle
                                                      7 * 60,  // start time: 7 AM
                                                      18 * 60  // end time: 6 PM
                                                      ));
    }
    // so we've created vehicles which need to start/end at "vehicle-site". Now we can make
    // the last change which is to override the distance between locations and the "vehicle-site".
    // we're only going to modify the distances FROM locations TO "vehicle-site". If you wanted to
    // do complete line-haul outsourcing-style modelling, you could also do this for "vehicle-site"
    // TO all alocations. For now, we'll just demonstrate the open routing case.

    // you have two ways of doing this. Upload it via the data-api, or upload it as part of the model.
    bool dataUpload = true;
    // it's nice to illustrate this if you've enabled the services on your key
    // but you can set this to false to get a feel for the other code path if needed.

    if (dataUpload)
    {
      var ts = new IVRData.TransitSet();
      for (int i = 0; i < m.Locations.Count; i++)
      {
        ts.Transits.Add(new IVRData.TransitSet.TransitValue
        {
          fromId = m.Locations[i].Id,
          toId = "vehicle-site",
          Value = 0
        });
      }

      var datamodel = new IVRData.CachedTransitSet
      {
        transitSet = ts
      };
      var data_api = new ApiHelper<IVRData.CachedTransitSet, object>("ivrdata-o43e0dvs78zq", configFile);
      var transitModelID = data_api.Post(datamodel);
      // now create the additional transit generators and link them to the data that has been uploaded
      var tgen_d = new Ivr7.TransitGenerator
      {
        Id = "custom_distance",
        requestId = transitModelID // Note, we're telling the API where to find the Transit-set data.
      };
      var tgen_t = new Ivr7.TransitGenerator
      {
        Id = "custom_time",
        requestId = transitModelID  // we can use the same one here as before, why? because it's all zero :-)
                                    // so we could upload another column of zeros, but there isn't much point.
      };
      m.transitGenerators.Add(tgen_d);
      m.transitGenerators.Add(tgen_t);
    }
    else
    {
      // embed the zero elements in the matrix in the payload directly (rather than through a data-upload)
      var ts = new Ivr7.TransitSet();
      for (int i = 0; i < m.Locations.Count; i++)
      {
        ts.Transits.Add(new Ivr7.TransitSet.TransitValue
        {
          fromId = m.Locations[i].Id,
          toId = "vehicle-site",
          Value = 0
        });
      }
      var tgen_d = new Ivr7.TransitGenerator
      {
        Id = "custom_distance",
        transitSet = ts // or we're explicily providing all the data.
      };
      var tgen_t = new Ivr7.TransitGenerator
      {
        Id = "custom_time",
        transitSet = ts // or we're explicily providing all the data.
      };
      m.transitGenerators.Add(tgen_d);
      m.transitGenerators.Add(tgen_t);
    }

    // now the last step, we need to tell the vehicles that they should use these
    // transit generators. Note. we're appending the transit generators here, so keeping 
    // the roadnetwork distance/time in the list (the order of the list IS important when
    // layering matricies). There are 4 attributes in the vehicle-class list now.
    m.vehicleClasses[0].Attributes.Add(new Ivr7.VehicleClass.Attribute
    {
      dimensionId = "time",
      transitGeneratorId = "custom_time",
      transitCoef = 1.0f,
      locationCoef = 1.0f,
      taskCoef = 1.0f
    });
    m.vehicleClasses[0].Attributes.Add(new Ivr7.VehicleClass.Attribute
    {
      dimensionId = "distance",
      transitGeneratorId = "custom_distance",
      transitCoef = 1.0f,
    });
    // so now we have a roadnetwork distance + time generator
    // followed by a custom time and custom distance generator.
    // the api will execute the transit generators in the order they appear and will
    // override previous values with new values (if they exist). So in this case, it will
    // build a transit matrix using the road network, then overlay the custom matrix which
    // was uploaded with our data.


    var sr = new Ivr7.SolveRequest();
    sr.Model = m; // could have instantiated the solve request up-front if we'd wanted to
    sr.solveType = Ivr7.SolveRequest.SolveType.Optimise; // Optimise the solve request.

    // now it's just sending the model to the api
    string requestId = api.Post(sr); // send the model to the api
    Solution = api.Get(requestId);   // get the response (which it typed, so that's cool)
    ivr7helper.printSolution(Solution);

    // lets confirm that the distances and times between the last stop and the vehicle-site are indeed zero.
    Dictionary<int, string> stopToLocation = new Dictionary<int, string>();
    foreach (var r in Solution.Routes)
    {
      foreach (var s in r.Stops)
      {
        stopToLocation.Add(s.Id, s.locationId);
      }

      foreach (var e in r.interStops)
      {
        if (stopToLocation[e.fromStopId] != "vehicle-site" &&
            stopToLocation[e.toStopId] == "vehicle-site")
        {
          foreach (var a in e.Attributes)
          {
            if (a.dimId == "time" || a.dimId == "distance")
            {
              if (a.endValue - a.startValue != 0)
              {
                throw new Exception("distance and/or time is supposed to be zero!");
              }
            }
          }
        }
      }
    }
    // for visualisations see the R/python notebook for plots on the same example.
    // You'll see in the visuals that the stops that are farthest from the depot are typically 
    // selected to be the route (because it results in the largest saving).

    return;
  }

  public Ivr7.SolutionResponse Solution { get; set; }

  private string configFile;

  private List<dataRow> data;
}