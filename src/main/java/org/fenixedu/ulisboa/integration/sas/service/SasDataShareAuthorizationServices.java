package org.fenixedu.ulisboa.integration.sas.service;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academicextensions.domain.person.dataShare.DataShareAuthorizationType;
import org.fenixedu.academicextensions.domain.services.person.DataShareAuthorizationServices;

public class SasDataShareAuthorizationServices {

    private static final String SAS_AUTHORIZATION_CODE = "SAS";

    static public boolean isAuthorizationTypeConfigured() {
        return getAuthorizationType() != null;
    }

    static public boolean isAnswered(Person person) {
        final DataShareAuthorizationType authorizationType = getAuthorizationType();
        return authorizationType == null ? false : DataShareAuthorizationServices.findLatest(person) != null;
    }

    static public boolean isDataShareAllowed(Person person) {
        final DataShareAuthorizationType authorizationType = getAuthorizationType();
        return authorizationType == null ? false : DataShareAuthorizationServices.isDataShareAllowed(person, authorizationType);
    }

    private static DataShareAuthorizationType getAuthorizationType() {
        return DataShareAuthorizationType.findUnique(SAS_AUTHORIZATION_CODE);
    }

}
