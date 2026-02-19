@ApplicationModule(
        displayName = "Reservation",
        allowedDependencies = {
                "com.mykulle.booking.system.catalog",
                "com.mykulle.booking.system.useraccount::identity"
        }
)
package com.mykulle.booking.system.reservation;

import org.springframework.modulith.ApplicationModule;
