
import siena.*;

/**
 * Proposed KX base notification class.  Multiple instances of these
 * may exist right now, since different people have worked on Siena
 * integration asynchronously.  It is expected that all of these will
 * be integrated into this class (or this class integrated into them)
 * sometime in the future.  In any case the minimum functionality here
 * is needed for EventPackager, Distiller.
 *
 * TODO: 
 * - Should we put individual KX components' convenience CTOR's in
 *   a separate place?
 * - Should we have an "ImmediateSource" to distinguish indivdiual KX
 *   components from the ORIGINATOR of the data (e.g. probe)?
 *
 * @author Janak J Parekh
 * @version 0.5
 */
public class KXNotification extends Notification {
  /**
   * CTOR for EventPackager.
   *
   * @param srcID The ID of this source.  Can be used by others to
   * disambiguate multiple streams coming into an EventPackager.
   * @param dataSourceURL Location where additional information about
   * this event can be obtained, possibly including data.  If it's
   * known beforehand no such data will exist, this URL will be null
   * and the corresponding Siena attribute will not exist.
   */
  public KXNotification(int srcID, String dataSourceURL) {
    super();
    putAttribute("Source","EventPackager");
    putAttribute("SourceID",(long)srcID);
    if(dataSourceURL != null) {
      putAttribute("dataSourceURL",dataSourceURL);
    }
  }
}
