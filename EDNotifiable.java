package psl.xues;
import siena.Notification;

/** This interface must be implemented by objects that handle EDBus callbacks. */
interface EDNotifiable {
    /**
     * Handles the callbacks.
     * @param n the dispatched event
     * @return whether the notification is absorbed here
     */
    public boolean notify(Notification n);
}
