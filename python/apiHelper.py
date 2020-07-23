import json
import requests
import sys
import os.path
import importlib
import re
import time

class apiHelper:
  def __init__(self, modelType, configfile = "../config.json"): 
    if not os.path.isfile(configfile): 
        data = {}
        data['endpoint'] = 'https://api.icepack.ai'
        data['token'] = ''
        with open(configfile, 'w') as outfile:
          json.dump(data, outfile)
        sys.exit("Config file created at \"" + configfile + "\". Please populate with a valid api-token.")  
    
    with open(configfile) as json_file:
      self.config = json.load(json_file)
    if self.config['apiToken'] == "":
      sys.exit("A valid apiToken should be provided in the config.json file")

    if self.config['endpoint'] == "":
      sys.exit("A valid endpoint should be provided in the config.json file")

    if self.config['endpoint'][-1:] != "/":
      self.config['endpoint'] = self.config['endpoint'] + "/"

    self.models = [ 'tsp-mcvfz472gty6','tsptw-kcxbievqo879',
                    'cvrp-jkfdoctmp51n','cvrptw-acyas3nzweqb',
                    'ivr7-kt461v8eoaif','ivr8-yni1c9k2swof',
                    'nvd-hap0j2y4zlm1','ns3-tbfvuwtge2iq',
                    'matrix-vyv95n7wchpl','ivrdata-o43e0dvs78zq']
    self.modelRoutes = ['vehicle-router/solve/','vehicle-router/solve/',
                        'vehicle-router/solve/','vehicle-router/solve/',
                        'vehicle-router/solve/','vehicle-router/solve/',
                        'vehicle-router/solve/','network-sourcing/solve/',
                         'matrix/','vehicle-router/data/']    
    if modelType not in self.models:
        s = '","'
        sys.exit("Invalid model type provided, should be one of the list:\n \"" + s.join(self.models) + "\"")
    
    self.route = self.modelRoutes[self.models.index(modelType)]
        
    self.modelType = re.sub("-","_", modelType)
    
    
    global problem_pb2
    import problem_pb2 as problem_pb2
    globals()[self.modelType + "_pb2"] = __import__(self.modelType + "_pb2") # this one we do programatically.
  
  def EndPoint(self):
      return (self.config['endpoint'] + self.route)

  def Token(self):
      return (self.config['apiToken'])
  
  def Post(self, sr):
    if not sr.IsInitialized():
       sys.exit('Incomplete model provided. Did you set the values of all required fields?')

    p = problem_pb2.ProblemEnvelope()
    p.type = re.sub("_", "-", self.modelType)
    p.subType = 0    # set the subtype to 0 for INPUT
    p.content = sr.SerializeToString()
    req_head = { 'Content-Type': "application/protobuf", 'Authorization': "Apitoken " + self.Token()} 
    
    post_resp = requests.post(self.EndPoint(), p.SerializeToString(), headers = req_head)
    # if the POST request was successful, get the request ID
    if post_resp.status_code != 200:
        if post_resp.status_code == 405 and len(post_resp.content) == 0:
          sys.exit('Unexpected http error code: ', post_resp.status_code, "\nDid you specify the end-point correctly?\n")
        else:
          sys.exit('Unexpected http error code: ', post_resp.status_code, "\n", post_resp.content())
    else: 
        return(post_resp.json()['requestid'])

  def Get(self, reqId):
    time.sleep(0.15) # if you're calling this in a tight python loop it often helps to give the 150ms head-start of processing the query.
    while True:
      get_resp = requests.get(self.EndPoint() + reqId, 
                              headers = { 'Content-Type': "application/protobuf", 
                                         'Authorization': "Apitoken " + self.Token()})
      if get_resp.status_code != 200:
        print("Unable to get response payload. Error: " + get_resp.content())
        break
      else:
        p = problem_pb2.ProblemEnvelope()
        p.ParseFromString(get_resp.content)
        x = problem_pb2.SolverResponse
        solRes = x.FromString(p.content)

        lastmsg = 0
        for l in solRes.logs:
          print(l) # just pump out the logs to the cmd line that were received
          lastmsg = l
        
        if solRes.state != 1 and lastmsg.type != 2:
          time.sleep(1) # wait 1 second before reopening the connection (just to avoid rapid-message)
        else:
          # then we have som stuff we can retrieve
          if len(solRes.solution) == 0:
            return 
          else:
            if self.modelType == 'tsp_mcvfz472gty6': # python pb2 only does underscores :-/ sad.
              m = tsp_mcvfz472gty6_pb2.SolutionResponse()
              return (m.FromString(solRes.solution))
            if self.modelType == 'tsptw_kcxbievqo879':
              m = tsptw_kcxbievqo879_pb2.SolutionResponse()
              return (m.FromString(solRes.solution))
            if self.modelType == 'cvrp_jkfdoctmp51n':
              m = cvrp_jkfdoctmp51n_pb2.SolutionResponse()
              return (m.FromString(solRes.solution))
            if self.modelType == 'ivr7_kt461v8eoaif':
              m = ivr7_kt461v8eoaif_pb2.SolutionResponse()
              return(m.FromString(solRes.solution))
            if self.modelType == 'ivrdata_o43e0dvs78zq':
              m = ivrdata_o43e0dvs78zq_pb2.SolutionResponse()
              return(m.FromString(solRes.solution))
            if self.modelType == 'ivr8_yni1c9k2swof':
              m = ivr8_yni1c9k2swof_pb2.SolutionResponse()
              return(m.FromString(solRes.solution))
            if self.modelType == 'matrix_vyv95n7wchpl':
              m = matrix_vyv95n7wchpl_pb2.MatrixResponse()
              return(m.FromString(solRes.solution))
            return 

def makeCompartmentSummary(m, ns):
  res = {}
  # we need the unique vehicles
  vids = list(set(ns['vehicleId']))
  # and capacity dimensions
  cdims = list()
  for i,e in enumerate(m.dimensions.capacityDimensions):
      cdims.append(e.id)

  for _, v in enumerate(vids):
      vindex = -1
      for j, vs in enumerate(m.vehicles):
          if vs.id == v:
              vindex = j
              break
      # the catch here is that the compartment set could have been configured on the class
      # or on the individual vehicle (which will override the class configuration)
      
      csetid = m.vehicles[vindex].compartmentSetId
      
      if csetid == '':
          # then it's on the class level (if it exists)
          for _, vc in enumerate(m.vehicleClasses):
              if vc.id == m.vehicles[vindex].classId:
                  csetid = vc.compartmentSetId
                  break
      
      if csetid == '':
          continue
      else:
          cset = list()
          for _, cs in enumerate(m.compartmentSets):
              if cs.id == csetid:
                  cset = list(cs.compartmentIds)
      
          # great, then for each vehicle, for each dimension, build a table of the state of cset at each stop.
          idx = 0
          cols = ['compartmentId', 'capacity']
          compCaps = pandas.DataFrame(columns=cols) # first grab all the compartment details for this dimension
          
          for _, d in enumerate(cdims):
              for _, c in enumerate(m.compartments):
                  if c.id in cset:
                      for _, cd in enumerate(c.capacities):
                          if cd.dimensionId == d:
                              compCaps.loc[idx] = list([c.id, cd.capacity])
                              idx+=1
              ns['tmp_delta'] = ns[d + '_end'] - ns[d + '_start']
              
              stab = ns[ns['vehicleId'] == v]
              if stab.shape[0] > 2:
                  for i, r in enumerate(stab):
                      if (i != 0) & (i < (stab.shape[0] - 1)):
                          stage = 'stop.' + str(stab['stopId'][i])
                          tmp = stab[1:(i+1)].groupby('compartmentId', 
                                                      as_index = False).agg({"tmp_delta": "sum"})
                          tmp.columns = ['compartmentId',stage]
                          compCaps = pandas.merge(compCaps,tmp,
                            left_on='compartmentId', right_on='compartmentId', how = 'left')
                  compCaps = compCaps.transpose()
                  cnames = compCaps.iloc[0]
                  compCaps = compCaps[1:compCaps.shape[0]]
                  compCaps.columns = list(cnames)
                  # then just join on the stop sequence from stab
                  compCaps['taskId'] = [None] + list(stab['taskId'][1:stab.shape[0]-1])
                  compCaps['vehilceId'] = v
                  compCaps['dimension'] = d
                  compCaps = compCaps.fillna(0)
                  res[v + ' dimension:' + d] = compCaps
  return res

# a tabulate function for a solve request and a solve-response
def tabulate(sr, resp):
  # so we may as well tabulate matrix models here as well. The only catch is we may not have 
  # loaded the matrix type, so to avoid a runtime error, lets stringify the type and check that instead
  if str(type(resp)) == "<class 'matrix_vyv95n7wchpl_pb2.MatrixResponse'>":
    matrix = pandas.DataFrame(columns = ['fromId', 'toId', 'distance', 'duration'])
    idx = 0
    for _, e in enumerate(resp.elements):
        matrix.loc[idx] = list([e.fromId, e.toId, e.distance, e.duration])
        idx+=1
    return matrix
  
  m = sr.model
  # we have certain fields we can guarentee:
  # stopId, sequence, locationId, taskId, jobId, vehicleId, x, y
  # the balance of the field are a function of the dimension:
  # dim_start, dim_end, dim_slackval, dim_slackcost, dim_tardyval, dim_tardycost, dim_cost
  # which repeat for each dimension.
  
  # check if this model has compartments (ivr8)
  hascompartments = False
  for i, e in enumerate(resp.routes):
    vid = e.vehicleId
    if hascompartments:
      break
    for j, s in enumerate(e.stops):
      if hasattr(s, 'compartmentId'):
        hascompartments = True
        break

  if hascompartments:
    cols = ['stopId', 'sequence', 'compartmentId', 'locationId', 'taskId', 'jobId', 'vehicleId']
  else:
    cols = ['stopId', 'sequence', 'locationId', 'taskId', 'jobId', 'vehicleId']
  
  def nodeDimList(name):
    return [name + '_start', name +'_end', 
            name + '_slackval', name + '_slackcost',
            name + '_tardyval', name + '_tardycost', 
            name + '_cost' ] # apologies for the dirty python here, it's a tad-unclear how to write this more compactly?
  
  # scan the response for the embedded dimensions
  
  dims = list()
  for _, e in enumerate(resp.routes):
    for _, s in enumerate(e.stops):
      for _, att in enumerate(s.attributes):
        dims.append(att.dimId)
  
   
  dims = list(set(dims))
  for _,d in enumerate(dims):
    for f in enumerate(nodeDimList(d)):
      cols.append(f[1])
  
  # we're going to quickly tabulate all the location information so we can tack on the x's and y's for plotting
  # once we've formed the whole data-frame.
  locations = pandas.DataFrame(columns = ['loc_id', 'x', 'y'])
  idx = 0
  for _, e in enumerate(m.locations):
    locations.loc[idx] = list([e.id, e.geocode.longitude, e.geocode.latitude])
    idx+=1
  if idx == 0:
    print('warning: no geocodes found in input model payload, x,y,fx,fy,tx,ty will be NaN')

  nodes = pandas.DataFrame(columns=cols)
  
  g = list() # for storing the geometries
  idx = 0
  for i, e in enumerate(resp.routes):
    vid = e.vehicleId
    for j, s in enumerate(e.stops):
      if hascompartments:
        row = [s.id,s.sequence, s.compartmentId, s.locationId, s.taskId, s.jobId, vid]
      else:
        row = [s.id,s.sequence, s.locationId, s.taskId, s.jobId, vid]
      # loop through the attributes and grab the dimensional values.
      for dim in dims:
        for _, att in enumerate(s.attributes):
          if att.dimId == dim: # we do a bit of extra work here, but it's not massive tbh. Perhaps change this to use a python dictionary?
            row.append(att.startValue)
            row.append(att.endValue)
            row.append(att.slackValue)
            row.append(att.slackCost)
            row.append(att.tardyValue)
            row.append(att.tardyCost)
            row.append(att.cost)
      nodes.loc[idx] = list(row)
      idx+=1
  
  
  #left-join on the location information to the nodes.
  nodes = pandas.merge(nodes,locations,left_on='locationId', right_on='loc_id', how = 'left').drop(['loc_id'], axis=1)


  def edgeDimList(name):
    return [name + '_start', name +'_end', name + '_cost' ] 

  # edges follow a similar convention: 
  cols = ['fromStopId', 'toStopId', 'vehicleId', 'geometry']
  for _, d in enumerate(dims):
    for e in enumerate(edgeDimList(d)):
      cols.append(e[1])
  
  # followed by dimension+ _start, _end, _cost
  # there are only three attribute fields for edges because all slack/tardy is applied at a node level for simplicity.
  edges = pandas.DataFrame(columns=cols)
  idx = 0
  for i, e in enumerate(resp.routes):
    vid = e.vehicleId
    for j, s in enumerate(e.interStops):
      gs = list()
      for _, rs in enumerate(s.routeSegments):
        gs.append([rs.latitude, rs.longitude])
      row = [s.fromStopId, s.toStopId, vid, gs]
      for dim in dims:
        for _, att in enumerate(s.attributes):
          if att.dimId == dim: # we do a bit of extra work here, but it's not massive tbh. Perhaps change this to use a python dictionary?
            row.append(att.startValue)
            row.append(att.endValue)
            row.append(att.cost)

      edges.loc[idx] = list(row)
      idx+=1
  
  #lets grab just the stopid and sequence number from the nodes table as well as the from-x from-y, to-x and to-y
  edges = pandas.merge(edges,nodes[['stopId', 'sequence','locationId', 'x', 'y']],
                      left_on='fromStopId', right_on='stopId', how = 'left').drop(['stopId'], axis=1)
  edges = edges.rename(columns={"locationId": "fromLocationId", 'x': 'fx', 'y': 'fy'})
  edges = pandas.merge(edges,nodes[['stopId','locationId','x','y']],
                      left_on='toStopId', right_on='stopId', how = 'left').drop(['stopId'], axis=1)
  edges = edges.rename(columns={"locationId": "toLocationId",'x': 'tx', 'y': 'ty'})
  
  # Lets grab any transit rules that might apply here.
  cols = ['vehicleId', 'ruleId', 'dimId', 'fromStopId', 'toStopId', 'startValue', 'endValue', 'cost']
  trules = pandas.DataFrame(columns=cols)
  idx = 0
  for i, e in enumerate(resp.routes):
    vid = e.vehicleId
    if len(e.transitRuleAttributes) > 0:
      for j, t in enumerate(e.transitRuleAttributes):
        trules.loc[idx] = list([vid, t.ruleId, t.dimId, t.fromStopId, t.toStopId, t.startValue, t.endValue, t.cost])
        idx+=1

  # Lastly, tabulate any infeasibilities which may have occured (if this was an evaluate call or if there are dropped loads with tightness)
  idx = 0
  cols = ['dimId', 'message', 'limit', 'value', 'count', 'taskId', 'constrainingTask']
  infeas = pandas.DataFrame(columns=cols)
  for i, e in enumerate(resp.infeasibilities):
    taskid = e.taskId
    for j, f in enumerate(e.infeasibilityInfo):
      row = [f.dimId, f.message,f.limit, f.value, f.count, taskid]
      ctasks = list()
      for si, s in enumerate(f.constrainingTaskIds):
        ctasks.append(s)
      if len(ctasks) > 0:
        row.append(','.join(ctasks))
      else:
        row.append('')
      infeas.loc[idx] = row
      idx+=1
  
  if hascompartments and (len(m.compartmentSets) != 0): # we can't tabulate this if we don't have the master data (need the context of the compartment set allocations)
    # go head and build a compartment summary table (which is nice to look at) - which is designed per vehicle.
    return {"nodes": nodes, 
            "edges": edges, 
            'transitrules': trules, 
            "infeasibilities": infeas, 
            'compartmentSummary': makeCompartmentSummary(m, nodes)}
  else:
    return {"nodes": nodes, 
            "edges": edges, 
            'transitrules': trules, 
            "infeasibilities": infeas}
    
