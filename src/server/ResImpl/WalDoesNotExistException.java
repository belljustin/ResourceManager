package server.ResImpl;

public class WalDoesNotExistException extends Exception {

  public WalDoesNotExistException() {
    super("Write Ahead Log does not exist");
  }
}
