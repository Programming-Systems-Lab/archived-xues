package psl.xues.ed;

import siena.Notification;

/**
 * This interface must be implemented by objects that handle EDBus
 * callbacks.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 *
 * @author Enrico Buonnano
 * @version $Revision$
 */
public interface EDNotifiable {
  /**
   * Handles the callbacks.
   * @param n the dispatched event
   * @return whether the notification is absorbed here
   */
  public boolean notify(Notification n);
}
