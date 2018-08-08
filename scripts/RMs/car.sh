java -classpath "../../bin/" \
     -Djava.security.policy="../../java.policy" \
     -Djava.rmi.server.codebase="file:../../bin/" \
     server.ResImpl.CarManager 1099
