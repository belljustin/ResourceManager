java -classpath "$PROJECTPATH/bin/" \
     -Djava.security.policy="$PROJECTPATH/java.policy" \
     -Djava.rmi.server.codebase="file:$PROJECTPATH/bin/" \
     server.ResImpl.FlightManager 1099 &

java -classpath ../bin/ \
     -Djava.security.policy="$PROJECTPATH/java.policy" \
     -Djava.rmi.server.codebase="file:$PROJECTPATH/bin/" \
     server.ResImpl.HotelManager 1099 &

java -classpath ../bin/ \
     -Djava.security.policy="$PROJECTPATH/java.policy" \
     -Djava.rmi.server.codebase="file:$PROJECTPATH/bin/" \
     server.ResImpl.CarManager 1099 &
