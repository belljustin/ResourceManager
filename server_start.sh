java -classpath `pwd`/bin/ \
     -Djava.security.policy=java.policy \
     -Djava.rmi.server.codebase=file:`pwd`/bin/ server.ResImpl.FlightManagerImpl 1099 &

java -classpath `pwd`/bin/ \
     -Djava.security.policy=java.policy \
     -Djava.rmi.server.codebase=file:`pwd`/bin/ server.ResImpl.HotelManagerImpl 1099 &

java -classpath `pwd`/bin/ \
     -Djava.security.policy=java.policy \
     -Djava.rmi.server.codebase=file:`pwd`/bin/ server.ResImpl.CarManagerImpl 1099 &


