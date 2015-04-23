package org.fenixedu.ulisboa.integration.sas.tasks;

import java.util.Set;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.ulisboa.integration.sas.domain.ScholarshipReportRequest;
import org.fenixedu.ulisboa.integration.sas.service.process.ScholarshipService;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

//Force task to be read only and process each report on its own transaction to avoid errors in a report affecting other reports
@Task(englishTitle = "Process Scholarship Report Requests", readOnly = true)
public class ProcessScholarshipReportRequests extends CronTask {

    @Override
    public void runTask() throws Exception {
        Set<ScholarshipReportRequest> pendingScholarshipReportRequestsSet =
                Bennu.getInstance().getPendingScholarshipReportRequestsSet();
        for (ScholarshipReportRequest pendingRequest : pendingScholarshipReportRequestsSet) {
            try {
                processReport(pendingRequest);
            } catch (Exception e) {
                taskLog("Error processing scholarship request with oid " + pendingRequest.getExternalId());
                e.printStackTrace(getTaskLogWriter());
                removeReport(pendingRequest);
            }
        }
    }

    @Atomic(mode = TxMode.WRITE)
    private void processReport(ScholarshipReportRequest pendingRequest) {
        ScholarshipService.createScholarshipService(pendingRequest);
    }

    @Atomic(mode = TxMode.WRITE)
    private void removeReport(ScholarshipReportRequest pendingRequest) {
        pendingRequest.setBennuForWhichIsPending(null);
        pendingRequest.setWhenProcessed(new DateTime());
    }
}
