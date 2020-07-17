# Java examples

The Java package requires maven. Once maven is installed and the specified JRE you can compile the examples using:

```
mvn package
```

If the compilation is successful, you can run the examples by modifying which examples you want to run in the `App.java` and then execute the run using maven:

```
mvn exec:java -Dexec.mainClass="icepackai.App"
```
