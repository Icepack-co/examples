# Icepack api usage examples

This Repo has a series of examples which show how model requests and responses to the Icepack API can be constructed and interpreted using a few different languages.

Read these examples in conjunction with the documentation on [docs.icepack.ai](https://docs.icepack.ai), which provides detailed technical descriptions and use cases for each model field.

The schemas for examples in Java, C# and Python have been precompiled. 

The R examples require installing the [iceR](https://github.com/icepack-co/iceR) R package (and consequently, [protoc](https://grpc.io/docs/protoc-installation/) if you're on Linux or Mac), which handles the compilation of the schemas and very fast parsing of solution reponse route geometries into [sf](https://github.com/r-spatial/sf) for plots using ggplot2.

To run the examples, you'll require an api-key. There are limits on free tier accounts, but they are sufficient to run the examples herein. See the specific readme file for each language sample for more information.

If you want to compile the schemas from source for your language implementations, check out the [model-schemas](https://github.com/icepack-co/model-schemas) repo.

# Catalog of Model Examples

## TSP Model

### R

* [Basic](https://github.com/Icepack-co/examples/blob/master/R/tsp-1-basic.R)

### C#

* [Basic](https://github.com/Icepack-co/examples/blob/master/csharp/tsp-1-basic.cs)

### Java

* [Basic](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/tsp1basic.java)

### Python

* [Basic](https://github.com/Icepack-co/examples/blob/master/python/tsp-1-basic.ipynb)


## TSPTW Model

### R

* [Basic](https://github.com/Icepack-co/examples/blob/master/R/tsptw-1-basic.R)

### C#

* [Basic](https://github.com/Icepack-co/examples/blob/master/csharp/tsptw-1-basic.cs)

### Java

* [Basic](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/tsptw1basic.java)

### Python

* [Basic](https://github.com/Icepack-co/examples/blob/master/python/tsptw-1-basic.ipynb)


## CVRP Model

### R

* [Basic](https://github.com/Icepack-co/examples/blob/master/R/cvrp-1-basic.R)

### C#

* [Basic](https://github.com/Icepack-co/examples/blob/master/csharp/cvrp-1-basic.cs)

### Java

* [Basic](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/cvrp1basic.java)

### Python

* [Basic](https://github.com/Icepack-co/examples/blob/master/python/cvrp-1-basic.ipynb)


## CVRPTW Model

### R

* [Basic](https://github.com/Icepack-co/examples/blob/master/R/cvrptw-1-basic.R)

### C#

* [Basic](https://github.com/Icepack-co/examples/blob/master/csharp/cvrptw-1-basic.cs)

### Java

* [Basic](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/cvrptw1basic.java)

### Python

* [Basic](https://github.com/Icepack-co/examples/blob/master/python/cvrptw-1-basic.ipynb)


## IVR7 Model

### R

* [Basic](https://github.com/Icepack-co/examples/blob/master/R/ivr7-1-basic.R)
* [Intermediate-1](https://github.com/Icepack-co/examples/blob/master/R/ivr7-2-intermediate.R)
* [Intermediate-2](https://github.com/Icepack-co/examples/blob/master/R/ivr7-3-intermediate2.R)
* [Advanced-1](https://github.com/Icepack-co/examples/blob/master/R/ivr7-4-advanced.R)
* [Advanced-2](https://github.com/Icepack-co/examples/blob/master/R/ivr7-5-advanced2.R)

### C#

* [Basic](https://github.com/Icepack-co/examples/blob/master/csharp/ivr7-1-basic.cs)
* [Intermediate-1](https://github.com/Icepack-co/examples/blob/master/csharp/ivr7-2-intermediate.cs)
* [Intermediate-2](https://github.com/Icepack-co/examples/blob/master/csharp/ivr7-3-intermediate2.cs)
* [Advanced-1](https://github.com/Icepack-co/examples/blob/master/csharp/ivr7-4-advanced1.cs)
* [Advanced-2](https://github.com/Icepack-co/examples/blob/master/csharp/ivr7-5-advanced2.cs)

### Java

* [Basic](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ivr7_1_basic.java)
* [Intermediate-1](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ivr7_2_intermediate.java)
* [Intermediate-2](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ivr7_3_intermediate2.java)
* [Advanced-1](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ivr7_4_advanced1.java)
* [Advanced-2](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ivr7_5_advanced2.java)

### Python

* [Basic](https://github.com/Icepack-co/examples/blob/master/python/ivr7-1-basic.ipynb)
* [Intermediate-1](https://github.com/Icepack-co/examples/blob/master/python/ivr7-2-intermediate.ipynb)
* [Intermediate-2](https://github.com/Icepack-co/examples/blob/master/python/ivr7-3-intermediate2.ipynb)
* [Advanced-1](https://github.com/Icepack-co/examples/blob/master/python/ivr7-4-advanced.ipynb)
* [Advanced-2](https://github.com/Icepack-co/examples/blob/master/python/ivr7-5-advanced2.ipynb)


## IVR8 Model

### R

* [Basic](https://github.com/Icepack-co/examples/blob/master/R/ivr8-1-basic.R)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/R/ivr8-2-intermediate.R)
* [Advanced](https://github.com/Icepack-co/examples/blob/master/R/ivr8-3-advanced.R)

### C#

* [Basic](https://github.com/Icepack-co/examples/blob/master/csharp/ivr8-1-basic.cs)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/csharp/ivr8-2-intermediate.cs)
* [Advanced](https://github.com/Icepack-co/examples/blob/master/csharp/ivr8-3-advanced.cs)

### Java

* [Basic](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ivr8_1_basic.java)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ivr8_2_intermediate.java)
* [Advanced](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ivr8_3_advanced.java)

### Python

* [Basic](https://github.com/Icepack-co/examples/blob/master/python/ivr8-1-basic.ipynb)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/python/ivr8-2-intermediate.ipynb)
* [Advanced](https://github.com/Icepack-co/examples/blob/master/python/ivr8-3-advanced.ipynb)


## NS3 Model

### R

* [Basic](https://github.com/Icepack-co/examples/blob/master/R/ns3-1-basic.R)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/R/ns3-2-intermediate.R)
* [Advanced](https://github.com/Icepack-co/examples/blob/master/R/ns3-3-advanced.R)

### C#

* [Basic](https://github.com/Icepack-co/examples/blob/master/csharp/ns3-1-basic.cs)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/csharp/ns3-2-intermediate.cs)
* [Advanced](https://github.com/Icepack-co/examples/blob/master/csharp/ns3-3-advanced.cs)

### Java

* [Basic](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ns3_1_basic.java)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ns3_2_intermediate.java)
* [Advanced](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/ns3_3_advanced.java)

### Python

* [Basic](https://github.com/Icepack-co/examples/blob/master/python/ns3-1-basic.ipynb)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/python/ns3-2-intermediate.ipynb)
* [Advanced](https://github.com/Icepack-co/examples/blob/master/python/ns3-3-advanced.ipynb)


## NVD Model

### R

* [Basic](https://github.com/Icepack-co/examples/blob/master/R/nvd-1-basic.R)

### C#

### Java

### Python


## Matrix Model

### R

* [Basic](https://github.com/Icepack-co/examples/blob/master/R/matrix-1-basic.R)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/R/matrix-2-intermediate.R)

### C#

* [Basic](https://github.com/Icepack-co/examples/blob/master/csharp/matrix1basic.cs)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/csharp/matrix1intermediate.cs)

### Java

* [Basic](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/matrix1basic.java)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/java/src/main/java/icepackai/matrix1intermediate.java)

### Python

* [Basic](https://github.com/Icepack-co/examples/blob/master/python/matrix-1-basic.ipynb)
* [Intermediate](https://github.com/Icepack-co/examples/blob/master/python/matrix-2-intermediate.ipynb)