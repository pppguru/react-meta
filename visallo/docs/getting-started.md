# Getting Started

While we recommend reading all the pages in this Developer Guide, the steps below are the fastest path to getting an instance of Visallo up and running in no time. Please make sure you have all [required dependencies](getting-started/dependencies.md) installed before attempting any of the steps below.

Clone the source code.

      git clone git://github.com/visallo/visallo.git

Change directories to the checked out code. This is your `$PROJECT_DIR` directory.

      cd visallo

Compile the application (optionally run tests.)
      
      mvn -DskipTests compile      

Run the web application.

      mvn -am -pl dev/tomcat-server -P dev-tomcat-run compile

Once the log output stops, your server will be available at [http://localhost:8888](http://localhost:8888).

