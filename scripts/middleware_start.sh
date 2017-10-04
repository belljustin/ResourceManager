java -classpath "$PROJECTPATH/bin/" \
  -Djava.security.policy="$PROJECTPATH/java.policy" \
  -Djava.rmi.server.codebase="file:$PROJECTPATH/bin/" \
  server.ResImpl.MiddleWare 1099
