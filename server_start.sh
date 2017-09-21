java -classpath `pwd`/bin/ \
     -Djava.security.policy=java.policy \
     -Djava.rmi.server.codebase=file:`pwd`/bin/ server.ResImpl.ResourceManagerImpl 1099
