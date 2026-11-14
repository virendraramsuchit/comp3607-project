# COMP 3607 BlueJ Telemetry Plugin

A BlueJ extension that records telemetry (edits, compilations, executions) and exports a JSON file and a PDF report.

## Setup 

1. **Java 21.** Maven must run on JDK 21 (the same version BlueJ uses).
   Check with `mvn -version` and make sure it says Java 21.
   If not, set `JAVA_HOME` to your JDK 21 folder.

2. **Register the BlueJ API jar with Maven.** It isn't on Maven Central.
   Adjust the path if BlueJ is installed somewhere else:

   ```
   mvn install:install-file -Dfile="C:\Program Files\BlueJ\lib\bluej.jar" -DgroupId=org.bluej -DartifactId=bluej -Dversion=5.0 -Dpackaging=jar
   ```

3. **Build and run the tests:**

   ```
   mvn clean package
   ```

## Install the plugin in BlueJ

1. Build with `mvn clean package`.
2. Copy the jar from `target/` into `%USERPROFILE%\bluej\extensions2\` (create the folder if needed).
3. Restart BlueJ. Check Help, then Installed Extensions.