package net.borkert.util;

public class GenericServiceException
    extends RuntimeException {

  public GenericServiceException(String message) {
    super(message);
  }

  public GenericServiceException(String message, Throwable cause) {
    super(message, cause);
  }

}
