package psl.xues.ep;

/**
 * Abstract interface for all EP plugin types.  Inputs, outputs, stores,
 * and transforms should all implement this interface so as to allow a simple
 * uniform interface.
 * <p>
 * Copyright (c) 2000-2002: The Trustees of Columbia University and the
 * City of New York.  All Rights Reserved.
 * 
 * <!--
 * TODO:
 * - Consider making it an abstract class instead of a plugin.  We don't,
 *   right now, because it seems overly excessive (the implementors of this
 *   interface are already abstract classes...)
 * - Have events be EPPlugins?  Does it make any sense?
 * -->
 *
 * @author Janak J Parekh
 * @version $Revision$
 */
public interface EPPlugin {
  /** Constant representing an EPInput. */
  public static final short INPUT = 1;
  /** Constant representing an EPOutput. */
  public static final short OUTPUT = 2;
  /** Constant representing an EPStore. */
  public static final short STORE = 3;
  /** Constant representing an EPTransform. */
  public static final short TRANSFORM = 4;
  
  /**
   * Get the plugin "type" as String.
   */
  public String getType();
  
  /**
   * Get the plugin "name" as String.
   */
  public String getName();
  
  /**
   * Get the number of times this plugin has "fired".
   */
  public long getCount();
  
  /**
   * Shutdown the plugin.
   */
  public void shutdown();
}  
  