java -classpath "$PROJECTPATH/bin/" \
     -Djava.security.policy="$PROJECTPATH/java.policy" \
     -Djava.rmi.server.codebase="file:$PROJECTPATH/bin/" \
     server.ResImpl.FlightManagerImpl 1099 &

java -classpath ../bin/ \
     -Djava.security.policy="$PROJECTPATH/java.policy" \
     -Djava.rmi.server.codebase="file:$PROJECTPATH/bin/" \
     server.ResImpl.HotelManagerImpl 1099 &

java -classpath ../bin/ \
     -Djava.security.policy="$PROJECTPATH/java.policy" \
     -Djava.rmi.server.codebase="file:$PROJECTPATH/bin/" \
     server.ResImpl.CarManagerImpl 1099 &
