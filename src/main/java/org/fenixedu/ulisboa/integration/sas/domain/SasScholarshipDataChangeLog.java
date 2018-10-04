package org.fenixedu.ulisboa.integration.sas.domain;

import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class SasScholarshipDataChangeLog extends SasScholarshipDataChangeLog_Base {

    protected SasScholarshipDataChangeLog() {
        super();
        super.setBennu(Bennu.getInstance());
    }

    public SasScholarshipDataChangeLog(SasScholarshipCandidacy candidacy, DateTime date, String description) {
        this();
        setSasScholarshipCandidacy(candidacy);
        setDate(date);
        setStudentNumber(candidacy.getStudentNumber());
        setStudentName(candidacy.getCandidacyName());
        setDescription(description);
    }

    @Atomic
    public void delete() {
        setSasScholarshipCandidacy(null);
        setBennu(null);
        deleteDomainObject();
    }

}
