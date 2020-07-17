# Python examples

In order to run the python code examples here you'll need to install a few libraries.
Some of the libraries are useful for plotting or wrangling data, others, like `protobuf`, are necessary to work with the schema files which create a tight-binding to meet the model standards expected by the api. 

## Requirements

* `python3` should be installed (sorry, python2.7 looks like it's en-route to deprecation-town).
* [jupyter-notebook](https://jupyter.org/install) in order to visualise the output from the samples (or run things step by step)
* `protoc`: This is the google protobuf compiler, it compiles the protobuf schema files into classes that can be used in python. It's a command line tool, so either you install it at an os-level (linux/mac), or download a binary (windows) from the [releases](https://github.com/protocolbuffers/protobuf/releases) page.
* `protobuf`: is the package used by python to handle the serialisation/deserialisation of python classes against the schema files produced by `protoc`. It's worth mentioning that you'll get a bunch of strange python errors loading the schema files if your version of `protoc` > `protobuf`.  
 `protobuf` is typically backward compatible, but `protoc` is not. So make sure `protoc --version` <= `protobuf.__version__`
* `numpy` (installation is system dependent)
* `pip3 install requests`
* `pip3 install protobuf` (typically the most recent version is perfect - just check you version of `protoc` is a bit older)

## Getting going

* Once everything is installed, you can run the notebook by running: `jupyter notebook`
  * Open the link provided in the command line from there.
