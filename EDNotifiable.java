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

/** Constants in ED. */
interface EDConst {

    // Cryteria for instantiating state machines.

    /** only one machine is instantiated. */
    public static final int ONE_ONLY = 0; 
    /** a new machine is instantiated as one dyes. */
    public static final int ONE_AT_A_TIME = 1;
    /** a new machine is instantiated as one starts. 
     *  this is the default value. */
    public static final int MULTIPLE = 2;

    /** 
     * Reap fudge factor.  IMPORTANT to take care of non-realtime event
     * buses (can anyone say Siena?)  XXX - should be a better way to do this.
     */
    public static final int REAP_FUDGE = 3000;
    /** Frequency for releasing events internally - in millisec. */
    public static int EVENT_PROCESSING = 500;
    /** Frequency for releasing events internally - in millisec. */
    public static int REAP_INTERVAL = 1000;
}

