package icepackai;

import icepackai.problem.Problem;
import icepackai.problem.Problem.ProblemEnvelope.SubType;
import icepackai.problem.Problem.SolverInfo.SolverMessageType;
import icepackai.problem.Problem.SolverResponse.SolveState;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.io.File;

import com.google.gson.Gson;
import com.google.protobuf.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.io.IOUtils;

public class apiHelper<T> {
  public String EndPoint;

  public String BaseURI;

  public String ModelType;

  public String ApiToken;

  public apiHelper(Class<T> typeClass, String modeltype, String configFile) throws Exception {
    outputType = typeClass;
    if (!models.containsKey(modeltype)) {
      throw new Exception("model type not recognised: \"" + modeltype
          + "\". should be one of:" + models.keySet().toString());
    }
    ModelType = modeltype;

    File f = new File(configFile);
    if (!f.exists()) {
      Config badconf = new Config();
      badconf.endpoint = "https://api.icepack.ai/";
      badconf.apiToken = "";
      Gson gson = new Gson();
      String json = gson.toJson(badconf);
      try {
        FileWriter myWriter = new FileWriter(configFile);
        myWriter.write(json);
        myWriter.close();
      } catch (Exception e) {
        throw e;
      }
      throw new Exception(
          "config.json not found: creating a default file. please populate with an apiToken!");
    } else {
      try {
        String path = configFile;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
        Gson gson = new Gson();
        Config json = gson.fromJson(bufferedReader, Config.class);
        BaseURI = json.endpoint;
        EndPoint = json.endpoint + models.get(modeltype);
        ApiToken = json.apiToken;
        System.out.println("Endpoint: " + json.endpoint);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  static HttpURLConnection con;

  private String postProblem(Problem.ProblemEnvelope p) throws IOException {
    try {
      URL myurl = new URL(EndPoint);
      con = (HttpURLConnection) myurl.openConnection();
      con.setDoOutput(true);
      con.setDoInput(true);
      con.setUseCaches(false);

      con.setRequestMethod("POST");
      con.setRequestProperty("Content-Type", "application/protobuf");
      con.setRequestProperty("Authorization", "Apitoken " + ApiToken);
      
      try (java.io.DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
        // byte[] b = p.toByteArray();
        // System.out.println("Problem payload bytes: " + b.length); // for the curious reader
        wr.write(p.toByteArray());
      }
      if(con.getResponseCode() == 200){
        StringBuilder content = new StringBuilder();
        try (java.io.BufferedReader br =
                new BufferedReader(new InputStreamReader(con.getInputStream()))) {
          String line;
          while ((line = br.readLine()) != null) {
            content.append(line);
            content.append(System.lineSeparator());
          }
        } catch (Exception e) {
          System.out.println("Exception occurred: " + e.toString());
          throw e;
        }
        Gson gson = new Gson();
        PostResponse response = gson.fromJson(content.toString(), PostResponse.class);
        return (response.requestid);
      }else{
        StringBuilder content = new StringBuilder();
        try (java.io.BufferedReader br =
                new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
          String line;
          while ((line = br.readLine()) != null) {
            content.append(line);
            content.append(System.lineSeparator());
          }
        } catch (Exception e) {
          System.out.println("Exception occurred: " + e.toString());
          throw e;
        }
        System.out.println("Unexpected response code " + con.getResponseCode() + " from the api: " +content.toString()); 
        return "";
      }
    } finally {
      con.disconnect();
    }
  }

  public String Post(Object solveRequest) {
    try {
      byte[] b = (byte[]) solveRequest.getClass()
                     .getMethod("toByteArray")
                     .invoke(solveRequest, new Object[] {});
      //System.out.println("Serialised model in " + b.length + " bytes."); // for the curious reader
      //System.out.println("converting to problem envelope");
      Problem.ProblemEnvelope p = Problem.ProblemEnvelope.newBuilder()
                                      .setType(ModelType)
                                      .setSubType(SubType.INPUT)
                                      .setContent(ByteString.copyFrom(b))
                                      .build();
      //System.out.println("serialising envelope");
      String requestId = postProblem(p);
      return (requestId);
    } catch (Exception e) {
      System.out.println("An exception occurred: " + e.toString());
    }
    return "";
  }

  public T Get(String requestId) throws Exception {
    if (requestId == ""){
      throw new Exception("No request ID provided. Did you get a valid post response? Are you rate limited or is the service enabled on your key?");
    }
    try {
      System.out.println("Getting response");
      while (true) {
        URL myurl = new URL(EndPoint + requestId);
        con = (HttpURLConnection) myurl.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setUseCaches(false);

        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/protobuf");
        con.setRequestProperty("Authorization", "Apitoken " + ApiToken);
        byte[] bytes = IOUtils.toByteArray(con.getInputStream());
        //System.out.println("Retrieved response with " + bytes.length + " bytes"); // again, for those interested.
        Problem.ProblemEnvelope p = Problem.ProblemEnvelope.parseFrom(bytes);
        //System.out.println("Parsed problem envelope");
        Problem.SolverResponse solRes = Problem.SolverResponse.parseFrom(p.getContent());
        con.disconnect();
        Problem.SolverInfo lastInfo = null;
        for (Problem.SolverInfo info : solRes.getLogsList()) {
          System.out.printf(info.toString());
          lastInfo = info;
        }
        if (solRes.getState() != SolveState.COMPLETED
            && lastInfo.getType() != SolverMessageType.ERROR) {
          Thread.sleep(1000); // Snooze for a moment
        } else {
          if (solRes.hasSolution()) {
            //System.out.printf("Returning solution.");
            T solution =
                (T) outputType
                    .getMethod("parseFrom", new Class[] {com.google.protobuf.ByteString.class})
                    .invoke(outputType, solRes.getSolution());
            // a lol-worthy line of code. Basically get the method from the target class and attempt
            // to deserialise it as the target object type. I guess if the language supports this
            // kind of tom-foolery it's okay?
            return solution;
          } else {
            System.out.printf("No solution returned.");
            return null;
          }
        }
      }
    } finally {
      con.disconnect();
    }
  }

  Class<T> outputType;

  Map<String, String> models = new HashMap<String, String>() {
    {
      put("tsp-mcvfz472gty6", "vehicle-router/solve/");
      put("tsptw-kcxbievqo879", "vehicle-router/solve/");
      put("matrix-vyv95n7wchpl", "matrix/");
      put("cvrp-jkfdoctmp51n", "vehicle-router/solve/");
      put("cvrptw-acyas3nzweqb", "vehicle-router/solve/");
      put("ivr7-kt461v8eoaif", "vehicle-router/solve/");
      put("ivr8-yni1c9k2swof", "vehicle-router/solve/");
      put("ivrdata-o43e0dvs78zq", "vehicle-router/data/");
      put("ns3-tbfvuwtge2iq", "network-sourcing/solve/");
    }
  };
}
