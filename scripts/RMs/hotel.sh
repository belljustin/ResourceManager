java -classpath ../../bin/ \
     -Djava.security.policy="../../java.policy" \
     -Djava.rmi.server.codebase="file:../../bin/" \
     server.ResImpl.HotelManager 1099
