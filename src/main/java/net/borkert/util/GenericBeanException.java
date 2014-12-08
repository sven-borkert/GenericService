package net.borkert.util;

public class GenericBeanException
    extends RuntimeException {

  public GenericBeanException(String message) {
    super(message);
  }

  public GenericBeanException(String message, Throwable cause) {
    super(message, cause);
  }

}
