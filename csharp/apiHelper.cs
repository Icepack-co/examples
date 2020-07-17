// simple api-helper class.
using Newtonsoft.Json;
using System.Collections.Generic;
using System;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;


class config{
  public config(){

  }
  public string apiToken { get; set; }
  public string endpoint { get; set; }
}

// takes two types as instantiation arguements. An input type (solve request) and output type (solution response)
public class ApiHelper<iT, oT>
{

  public string EndPoint { get; private set; }

  public string BaseURI { get; private set; }

  public string ModelType { get; private set; }

  public string ApiToken { get; private set; }

  public ApiHelper(string modeltype, string configFile = "../config.json")
  {

    ModelType = modeltype;

    if (!models.ContainsKey(modeltype))
    {
      throw new System.Exception("model type not recognised: \"" + modeltype + "\". should be one of:" + models.Keys.ToString());
    }
    if (!System.IO.File.Exists(configFile))
    {
      config badconf = new config();
      badconf.endpoint = "https://api.icepack.ai/";
      badconf.apiToken = "";
      File.WriteAllText(configFile, JsonConvert.SerializeObject(badconf));
      throw new System.Exception("config.json not found: creating a default file. please populate with an apiToken!");
    }
    config conf = JsonConvert.DeserializeObject<config>(File.ReadAllText(configFile));
    ApiToken = conf.apiToken;
    if (ApiToken == "")
    {
      throw new System.Exception("invalid apiToken in config.json: please update with a valid token");
    }
    BaseURI = conf.endpoint;
    EndPoint = conf.endpoint + models[modeltype];
  }

  private class postResponse
  {
    public string requestId { get; set; }
  }

  public string Post(iT solveRequest)
  {
    var p = new Problem.ProblemEnvelope();
    p.Type = ModelType;
    p.subType = 0; // for a post;
    p.Content = SerialiseObject<iT>(solveRequest);
    string res = PostProblem(p).Result;
    if (res == "")
    {
      throw new Exception("unable to retrieve a request id from the api");
    }
    else
    {
      postResponse resJson = JsonConvert.DeserializeObject<postResponse>(res);
      System.Threading.Thread.Sleep(100); // Feel free to remove this
      return resJson.requestId;
    }
  }

  /// Feel free to cast this into the target solution response object on the other side
  public oT Get(string requestID)
  {
    var response = GetResponse(requestID).Result;
    if (response == null || response.Length == 0)
    {
      return default(oT);
    }
    return (DeserialiseObject<oT>(response));
  }

  private async Task<byte[]> GetResponse(string requestID)
  {
    using (var client = new HttpClient())
    {
      configureClient(client);
      while (true)
      {
        var result = client.GetAsync(EndPoint + requestID).Result;
        if (result.StatusCode == System.Net.HttpStatusCode.OK)
        {
          byte[] byteResposne = await result.Content.ReadAsByteArrayAsync();

          var p = DeserialiseObject<Problem.ProblemEnvelope>(byteResposne);
          var solRes = DeserialiseObject<Problem.SolverResponse>(p.Content);
          Problem.SolverInfo lastLog = new Problem.SolverInfo();
          foreach (var l in solRes.Logs)
          {
            Console.WriteLine("unixDateTime: " + l.unixDateTime + "\nType:" + l.Type + "\ninfoMessage:" + l.infoMessage);
            lastLog = l;
          }
          if (solRes.State != Problem.SolverResponse.SolveState.Completed &&
             lastLog.Type != Problem.SolverInfo.SolverMessageType.Error)
          {
            System.Threading.Thread.Sleep(1000); // snooze for a moment
          }
          else
          {
            return solRes.Solution;
          }
        }
        else
        {
          Console.WriteLine("Error connecting to api; http error: " + result.ReasonPhrase);
        }
      }
    }
  }


  private async Task<string> PostProblem(Problem.ProblemEnvelope envelope)
  {
    // we need to construct a http call.
    using (var client = new HttpClient())
    {
      configureClient(client);
      byte[] envBytes = SerialiseObject<Problem.ProblemEnvelope>(envelope);
      var content = new ByteArrayContent(envBytes);
      content.Headers.ContentType = new MediaTypeHeaderValue("application/protobuf");

      var result = client.PostAsync(EndPoint, content).Result;
      if (result.StatusCode == System.Net.HttpStatusCode.OK)
      {
        string resultContent = await result.Content.ReadAsStringAsync();
        return resultContent;
      }
      else
      {
        string resultContent = await result.Content.ReadAsStringAsync();
        Console.WriteLine("Error connecting to api; http error: " + result.ReasonPhrase + "\n" + resultContent);
      }
    }
    return "";
  }


  private void configureClient(HttpClient client)
  {
    client.BaseAddress = new Uri(BaseURI);
    client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Apitoken", ApiToken);
    client.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/protobuf"));
  }

  private Dictionary<string, string> models = new Dictionary<string, string>(){
            {"tsp-mcvfz472gty6","vehicle-router/solve/"} ,
            {"tsptw-kcxbievqo879","vehicle-router/solve/"},
            {"matrix-vyv95n7wchpl","matrix/"},
            {"cvrp-jkfdoctmp51n","vehicle-router/solve/"},
            {"cvrptw-acyas3nzweqb","vehicle-router/solve/"},
            {"ivr7-kt461v8eoaif","vehicle-router/solve/"},
            {"ivr8-yni1c9k2swof","vehicle-router/solve/"},
            {"ivrdata-o43e0dvs78zq","vehicle-router/data/"},
            {"ns3-tbfvuwtge2iq","network-sourcing/solve/"},
  };

  public static byte[] SerialiseObject<T>(T instance)
  {
    System.IO.MemoryStream ms = new System.IO.MemoryStream();
    ProtoBuf.Serializer.Serialize(ms, instance);
    return ms.ToArray();
  }

  public static T DeserialiseObject<T>(byte[] bytes)
  {
    var ms = new System.IO.MemoryStream(bytes);
    // if you're getting a deserialisation error here then you may want to check you're running .net framework 4.1+
    // which is required by the protobuf.net package. :-/ sorry. 
    return ProtoBuf.Serializer.Deserialize<T>(ms);
  }
}