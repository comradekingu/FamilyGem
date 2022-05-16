// Family situation

package app.familygem.constants;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;

public enum Status {

	NONE, // Generic relationship
	MARRIED, DIVORCED, SEPARATED;

	// Find the status of a Family
	public static Status getStatus(Family family) {
		Status status = NONE;
		if( family != null ) {
			for( EventFact event : family.getEventsFacts() ) {
				String tag = event.getTag();
				switch( tag ) {
					case "MARR":
						String type = event.getType();
						if( type == null || type.isEmpty() || type.equals("marriage")
								|| type.equals("civil") || type.equals("religious") || type.equals("common law") )
							status = MARRIED;
						else
							status = NONE;
						break;
					case "MARB":
					case "MARC":
					case "MARL":
					case "MARS":
						status = MARRIED;
						break;
					case "DIV":
						status = status == MARRIED ? DIVORCED : SEPARATED;
						break;
				}
			}
		}
		return status;
	}
}
