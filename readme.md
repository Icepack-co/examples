# Icepack api usage examples

This Repo has a series of examples which show how model requests and responses to the Icepack API can be constructed and interpreted using a few different languages.

Read these examples in conjunction with the documentation on [docs.icepack.ai](https://docs.icepack.ai), which provides detailed technical descriptions and use cases for each model field.

The schemas for examples in Java, C# and Python have been precompiled. 

The R examples require installing the [iceR](https://github.com/icepack-co/iceR) R package (and consequently, [protoc](https://grpc.io/docs/protoc-installation/) if you're on Linux or Mac), which handles the compilation of the schemas and very fast parsing of solution reponse route geometries into [sf](https://github.com/r-spatial/sf) for plots using ggplot2.

To run the examples, you'll require an api-key. There are limits on free tier accounts, but they are sufficient to run the examples herein. See the specific readme file for each language sample for more information.

If you want to compile the schemas from source for your language implementations, check out the [model-schemas](https://github.com/icepack-co/model-schemas) repo.

# Catalog of Model Examples

## TSP Model

## TSPTW Model

## CVRP Model

## IVR7 Model

## IVR8 Model

## NS3 Model

## NVD Model

## Matrix Model
