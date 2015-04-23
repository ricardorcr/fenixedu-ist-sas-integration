package org.fenixedu.ulisboa.integration.sas.service.process;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.ulisboa.integration.sas.domain.ScholarshipReportRequest;
import org.fenixedu.ulisboa.integration.sas.domain.SocialServicesConfiguration;
import org.fenixedu.ulisboa.integration.sas.dto.AbstractScholarshipStudentBean;
import org.fenixedu.ulisboa.integration.sas.dto.ScholarshipStudentOtherYearBean;
import org.fenixedu.ulisboa.integration.sas.service.registration.report.RegistrationHistoryReport;
import org.fenixedu.ulisboa.integration.sas.service.registration.report.RegistrationHistoryReportService;
import org.joda.time.DateTime;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class FillScholarshipServiceOtherYearService extends AbstractFillScholarshipService {

    @Override
    protected void fillSpecificInfo(AbstractScholarshipStudentBean inputBean,
            RegistrationHistoryReport currentYearRegistrationReport, ScholarshipReportRequest request) {

        final Registration registration = currentYearRegistrationReport.getRegistration();
        final ScholarshipStudentOtherYearBean bean = (ScholarshipStudentOtherYearBean) inputBean;

        bean.setNumberOfDegreeChanges(calculateNumberOfDegreeChanges(registration.getStudent(), request));
        bean.setHasMadeDegreeChangeOnCurrentYear(calculateDegreeChangeForCurrentYear(registration, request));

        bean.setCurricularYear(currentYearRegistrationReport.getCurricularYear());

        bean.setCycleIngressionYear(calculateCycleIngressionYear(bean, registration));

        final Collection<RegistrationHistoryReport> cycleRegistrationReports =
                calculateRegistrationReports(registration.getStudent(), registration.getDegreeType(), request.getExecutionYear());

        bean.setCycleNumberOfEnrolmentYears(calculateCycleNumberOfEnrolmentYears(cycleRegistrationReports, request));
        bean.setCycleNumberOfEnrolmentsYearsInIntegralRegime(calculateCycleNumberOfEnrolmentYearsInIntegralRegime(
                cycleRegistrationReports, request));

        final RegistrationHistoryReport lastEnrolmentYearHistory = calculateCycleLastEnrolmentYearHistory(registration, request);

        if (lastEnrolmentYearHistory != null) {
            checkIfHasDismissals(bean, lastEnrolmentYearHistory);
            bean.setLastEnrolmentYear(lastEnrolmentYearHistory.getExecutionInterval().getBeginDateYearMonthDay().getYear());
            bean.setLastEnrolmentCurricularYear(lastEnrolmentYearHistory.getCurricularYear());

            final ExecutionYear executionYear =
                    ExecutionInterval.assertExecutionIntervalType(ExecutionYear.class,
                            lastEnrolmentYearHistory.getExecutionInterval());

            if (!isInMobility(registration, executionYear)) {
                bean.setNumberOfEnrolledEctsLastYear(lastEnrolmentYearHistory.getTotalEnroledCredits());
                bean.setNumberOfApprovedEctsLastYear(lastEnrolmentYearHistory.getTotalApprovedCredits());
            } else {
                // TODO add the approved and enrolled credits obtained in erasmus (information to be extracted from mobility - roadmap)
                bean.setNumberOfEnrolledEctsLastYear(lastEnrolmentYearHistory.getTotalEnroledCredits());
                bean.setNumberOfApprovedEctsLastYear(lastEnrolmentYearHistory.getTotalApprovedCredits());
            }

            bean.setLastAcademicActDateLastYear(lastEnrolmentYearHistory.getLastAcademicActDate());
        }

        bean.setNumberOfApprovedEcts(calculateNumberOfApprovedEcts(registration, request));
    }

    private BigDecimal calculateNumberOfApprovedEcts(Registration registration, ScholarshipReportRequest request) {
        ExecutionYear executionYear = request.getExecutionYear();

        final StudentCurricularPlan studentCurricularPlan;
        final Curriculum curriculum;
        if (executionYear.hasNextExecutionYear()) {
            // the executionYear has next year, so it gets the curriculum plan of the beginning of next year)
            studentCurricularPlan = registration.getStudentCurricularPlan(executionYear.getNextExecutionYear());

            if (studentCurricularPlan == null) {
                throw new DomainException(
                        "error.RegistrationHistoryReportService.unable.to.find.student.curricular.plan.for.year", registration
                                .getStudent().getNumber().toString(), executionYear.getQualifiedName());
            }

            curriculum = studentCurricularPlan.getCurriculum(new DateTime(), executionYear.getNextExecutionYear());
        } else {
            // current execution year
            // get the curriculum with date = null
            studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

            if (studentCurricularPlan == null) {
                throw new DomainException(
                        "error.RegistrationHistoryReportService.unable.to.find.student.curricular.plan.for.year", registration
                                .getStudent().getNumber().toString(), executionYear.getQualifiedName());
            }

            curriculum = studentCurricularPlan.getCurriculum(new DateTime(), null);

        }
        return curriculum.getSumEctsCredits();
    }

    private void checkIfHasDismissals(ScholarshipStudentOtherYearBean bean, RegistrationHistoryReport lastEnrolmentYearHistory) {

        final ExecutionYear executionYear =
                ExecutionInterval.assertExecutionIntervalType(ExecutionYear.class,
                        lastEnrolmentYearHistory.getExecutionInterval());

        for (final StudentCurricularPlan studentCurricularPlan : lastEnrolmentYearHistory.getRegistration()
                .getStudentCurricularPlansSet()) {
            for (final Credits credits : studentCurricularPlan.getCreditsSet()) {
                if (credits.getExecutionPeriod().getExecutionYear() == executionYear) {
                    addWarning(bean, "A matrícula tem creditações no ano lectivo " + executionYear.getQualifiedName() + ".");
                    return;
                }
            }
        }

    }

    private Integer calculateCycleNumberOfEnrolmentYearsInIntegralRegime(
            Collection<RegistrationHistoryReport> cycleRegistrationReports, ScholarshipReportRequest request) {

        Set<ExecutionYear> executionYears = Sets.newHashSet();
        for (RegistrationHistoryReport registrationHistoryReport : cycleRegistrationReports) {

            Registration registration = registrationHistoryReport.getRegistration();
            Collection<ExecutionYear> enrolmentYearsIncludingPrecedentRegistrations =
                    getEnrolmentYearsIncludingPrecedentDegrees(request.getExecutionYear(), registration);

            for (ExecutionYear executionYear : enrolmentYearsIncludingPrecedentRegistrations) {
                if (!registrationHistoryReport.getRegistration().isPartialRegime(executionYear))
                    executionYears.add(executionYear);
            }
        }
        return executionYears.size();
    }

    private Collection<ExecutionYear> getEnrolmentYearsIncludingPrecedentDegrees(ExecutionYear executionYear,
            Registration registration) {

        Collection<Registration> precedentDegreeRegistrations = getPrecedentDegreeRegistrations(registration);
        precedentDegreeRegistrations.add(registration);

        Collection<ExecutionYear> enrolmentYearsIncludingPrecedentRegistrations =
                precedentDegreeRegistrations.stream().flatMap(r -> r.getEnrolmentsExecutionYears().stream())
                        .filter(ey -> ey.isBefore(executionYear)).collect(Collectors.toSet());
        return enrolmentYearsIncludingPrecedentRegistrations;
    }

    private Integer calculateCycleNumberOfEnrolmentYears(Collection<RegistrationHistoryReport> cycleRegistrationReports,
            ScholarshipReportRequest request) {

        Set<ExecutionYear> executionYears = Sets.newHashSet();
        for (RegistrationHistoryReport registrationHistoryReport : cycleRegistrationReports) {
            executionYears.addAll(getEnrolmentYearsIncludingPrecedentDegrees(request.getExecutionYear(),
                    registrationHistoryReport.getRegistration()));
        }
        return executionYears.size();
    }

    private Collection<RegistrationHistoryReport> calculateRegistrationReports(Student student, DegreeType degreeType,
            ExecutionYear executionYear) {
        final Set<RegistrationHistoryReport> result = Sets.newHashSet();
        final RegistrationHistoryReportService service = new RegistrationHistoryReportService();
        for (final Registration registration : student.getRegistrationsByDegreeTypes(degreeType)) {
            result.add(service.generateReport(registration, executionYear));
        }

        return result;
    }

    private Integer calculateCycleIngressionYear(ScholarshipStudentOtherYearBean bean, Registration registration) {

        final Registration firstRegistration =
                Collections.min(registration.getStudent().getRegistrationsByDegreeTypes(registration.getDegreeType()),
                        Registration.COMPARATOR_BY_START_DATE);

        Integer firstRegistrationYear =
                getRootRegistration(firstRegistration).getStartExecutionYear().getBeginDateYearMonthDay().getYear();

        if (bean.getCycleIngressionYear() != null && !bean.getCycleIngressionYear().equals(firstRegistrationYear)) {
            String message =
                    "o ano de ingresso no ciclo de estudos declarado no ficheiro (" + bean.getCycleIngressionYear()
                            + ") não corresponde ao ano de início do sistema (" + firstRegistrationYear + ").";
            addWarning(bean, message);
        }

        return firstRegistrationYear;
    }

    private RegistrationHistoryReport calculateCycleLastEnrolmentYearHistory(Registration currentYearRegistration,
            ScholarshipReportRequest request) {

        final SortedSet<ExecutionYear> allEnrolmentYears = Sets.newTreeSet(ExecutionYear.COMPARATOR_BY_BEGIN_DATE);

        final Multimap<Registration, ExecutionYear> enrolmentYearsByRegistration = ArrayListMultimap.create();

        for (final Registration registration : currentYearRegistration.getStudent().getRegistrationsByDegreeTypes(
                currentYearRegistration.getDegreeType())) {

            final Collection<ExecutionYear> enrolmentYears =
                    getEnrolmentYearsIncludingPrecedentDegrees(request.getExecutionYear(), registration);
            enrolmentYearsByRegistration.putAll(registration, enrolmentYears);
            allEnrolmentYears.addAll(enrolmentYears);
        }

        allEnrolmentYears.remove(request.getExecutionYear());

        if (allEnrolmentYears.isEmpty()) {
            return null;
        }

        final ExecutionYear lastEnrolmentYear = allEnrolmentYears.last();
        for (final Map.Entry<Registration, Collection<ExecutionYear>> entry : enrolmentYearsByRegistration.asMap().entrySet()) {
            if (entry.getValue().contains(lastEnrolmentYear)) {
                final Registration registration = entry.getKey();
                final RegistrationHistoryReportService service = new RegistrationHistoryReportService();

                return service.generateReport(registration, lastEnrolmentYear);
            }
        }

        return null;

    }

    private Boolean calculateDegreeChangeForCurrentYear(Registration registration, ScholarshipReportRequest request) {

        Registration rootRegistration = getRootRegistration(registration);
        if (rootRegistration.getStartExecutionYear() == request.getExecutionYear()) {
            final IngressionType ingression = rootRegistration.getStudentCandidacy().getIngressionType();

            return ingression != null
                    && SocialServicesConfiguration.getInstance().getIngressionTypeWhichAreDegreeTransferSet()
                            .contains(ingression);

        }

        return false;
    }

    private Integer calculateNumberOfDegreeChanges(Student student, ScholarshipReportRequest request) {

        final SortedSet<Registration> allRegistrations =
                Sets.newTreeSet(Collections.reverseOrder(Registration.COMPARATOR_BY_START_DATE));
        allRegistrations.addAll(student.getRegistrationsSet());

        final Set<Registration> headRegistrations = Sets.newHashSet();

        while (!allRegistrations.isEmpty()) {
            final Registration registration = allRegistrations.first();
            headRegistrations.add(registration);
            allRegistrations.remove(registration);
            allRegistrations.removeAll(getPrecedentDegreeRegistrations(registration));
        }

        int degreeChangeCount = 0;
        for (final Registration headRegistration : headRegistrations) {
            if (hasAnyDegreeChange(headRegistration, request)) {
                degreeChangeCount++;
            }
        }

        return degreeChangeCount;

    }

    private boolean hasAnyDegreeChange(Registration headRegistration, ScholarshipReportRequest request) {
        final Set<Registration> allRegistrations = Sets.newHashSet();
        allRegistrations.add(headRegistration);
        allRegistrations.addAll(getPrecedentDegreeRegistrations(headRegistration));

        for (final Registration registration : allRegistrations) {
            final IngressionType ingression = registration.getStudentCandidacy().getIngressionType();

            if (ingression != null
                    && SocialServicesConfiguration.getInstance().getIngressionTypeWhichAreDegreeTransferSet()
                            .contains(ingression)) {
                return true;
            }
        }

        return false;
    }
}