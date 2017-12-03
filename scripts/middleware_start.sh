java -classpath "../bin/" \
  -Djava.security.policy="../java.policy" \
  -Djava.rmi.server.codebase="file:../../bin/" \
  server.ResImpl.MiddleWare 1099
