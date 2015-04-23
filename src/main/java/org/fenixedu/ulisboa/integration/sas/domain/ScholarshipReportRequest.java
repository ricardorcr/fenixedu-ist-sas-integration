package org.fenixedu.ulisboa.integration.sas.domain;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

public class ScholarshipReportRequest extends ScholarshipReportRequest_Base {

    public ScholarshipReportRequest(ExecutionYear executionYear, boolean firstYearOfCycle, String fileName, byte[] content) {
        super();
        setExecutionYear(executionYear);
        setFirstYearOfCycle(firstYearOfCycle);
        ScholarshipReportFile scholarshipReportFile = new ScholarshipReportFile(fileName, content);
        setParameterFile(scholarshipReportFile);
        Bennu bennu = Bennu.getInstance();
        setBennu(bennu);
        setBennuForWhichIsPending(bennu);
        setWhenRequested(new DateTime());
    }

    public ScholarshipReportFile createResultFile(String fileName, byte[] content) {
        if (getResultFile() != null) {
            throw new DomainException("label.error.scholarship.requestAlreadyHasResult");
        }
        ScholarshipReportFile scholarshipReportFile = new ScholarshipReportFile(fileName, content);
        setResultFile(scholarshipReportFile);
        setWhenProcessed(new DateTime());
        return scholarshipReportFile;
    }

    //Overrides to change visibility
    @Override
    public boolean getFirstYearOfCycle() {
        return super.getFirstYearOfCycle();
    }

    @Override
    public ScholarshipReportFile getParameterFile() {
        return super.getParameterFile();
    }

    @Override
    public ScholarshipReportFile getResultFile() {
        return super.getResultFile();
    }

}
