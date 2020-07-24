# Icepack api usage examples

This repo has a series of examples in different langauges of implementations of different model requests against the icepack api.

This repo should be used in conjunction with the documentation on [docs.icepack.ai](https://docs.icepack.ai) which provide technical descriptions of the interpretation and use cases for each of the model fields.

The schemas for examples in `java`, `csharp` and `python` are all precomiled. The `R` examples require installing the [r-package](https://github.com/icepack-co/iceR) (and consequently, `protoc` if you're on Linux or Mac) which handles the compilation of the schemas and some very fast parsing of geometries into [sf](https://github.com/r-spatial/sf) for plots using `ggplot` and `sf`.

In order to run the examples you'll require an api-key which can be obtained by registering a free account on the icepack [client-portal](https://portal.icepack.ai). There are limits on free tier accounts but free accounts are sufficient to run the examples herein. See the specific readme file for each of the language samples for more information.

If you're looking to compile the schemas from source for your own language implementations check out the [model-schemas](https://github.com/icepack-co/model-schemas) repo.
