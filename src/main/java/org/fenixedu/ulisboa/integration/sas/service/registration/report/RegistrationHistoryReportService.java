package org.fenixedu.ulisboa.integration.sas.service.registration.report;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.curriculum.Curriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.ulisboa.integration.sas.service.process.AbstractFillScholarshipService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

//TODO: add boolean indicating if student has concluded in analysis year
public class RegistrationHistoryReportService {

    private static class NormalEnrolmentsPredicate implements Predicate<Enrolment> {

        @Override
        public boolean test(Enrolment enrolment) {

            if (!belongsToDegreeCurricularPlanOfStudent(enrolment)) {
                return false;
            }

            if (enrolment.getCurriculumGroup().isNoCourseGroupCurriculumGroup()
                    && !isInternalCreditsSourceGroup(enrolment.getCurriculumGroup())) {
                return false;
            }

            return true;
        }

        private boolean isInternalCreditsSourceGroup(CurriculumGroup curriculumGroup) {
            //TODO implement
            return false;
        }

        private boolean belongsToDegreeCurricularPlanOfStudent(Enrolment enrolment) {
            return enrolment.getDegreeModule() != null && enrolment.getDegreeModule().getParentDegreeCurricularPlan() == enrolment
                    .getStudentCurricularPlan().getDegreeCurricularPlan();
        }
    }

    public Collection<RegistrationHistoryReport> generateReport(final ExecutionInterval startInterval,
            final ExecutionInterval endInterval, final DegreeType degreeType) {

        final ExecutionYear startYear = ExecutionInterval.assertExecutionIntervalType(ExecutionYear.class, startInterval);
        final ExecutionYear endYear = ExecutionInterval.assertExecutionIntervalType(ExecutionYear.class, endInterval);

        if (startYear == null || endYear == null) {
            throw new DomainException("error.RegistrationHistoryReportService.startInterval.and.endInterval.must.be.defined");
        }

        if (startYear.isAfter(endYear)) {
            throw new DomainException("error.RegistrationHistoryReportService.startInterval.must.be.before.endInterval");
        }

        final SortedSet<ExecutionYear> toProcess = new TreeSet<>(ExecutionYear.COMPARATOR_BY_BEGIN_DATE);
        toProcess.addAll(ExecutionYear.readExecutionYears(startYear, endYear));

        final List<RegistrationHistoryReport> result = new ArrayList<>();
        for (final ExecutionYear executionYear : toProcess) {
            result.addAll(process(executionYear, degreeType));
        }

        return result;

    }

    public RegistrationHistoryReport generateReport(final Registration registration, final ExecutionInterval executionInterval) {
        return processRegistration(registration,
                ExecutionInterval.assertExecutionIntervalType(ExecutionYear.class, executionInterval));
    }

    private Collection<RegistrationHistoryReport> process(ExecutionYear executionYear, DegreeType degreeType) {

        final Set<Registration> registrationsToProcess = getRegistrationsToProcess(executionYear, degreeType);

        final Collection<RegistrationHistoryReport> result = new HashSet<>();
        for (final Registration registration : registrationsToProcess) {
            result.add(processRegistration(registration, executionYear));
        }

        return result;

    }

    private RegistrationHistoryReport processRegistration(Registration registration, ExecutionYear executionYear) {
        final RegistrationHistoryReport result = new RegistrationHistoryReport(registration, executionYear);

        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);
        if (studentCurricularPlan == null) {
            throw new DomainException("error.RegistrationHistoryReportService.unable.to.find.student.curricular.plan.for.year",
                    registration.getStudent().getNumber().toString(), executionYear.getQualifiedName());
        }
        result.setStudentCurricularPlan(studentCurricularPlan);

        final RegistrationState registrationState = registration.getLastRegistrationState(executionYear);
        result.setRegistrationState(registrationState);

        result.setStatuteTypes(registration.getStudent().getStatutesTypesValidOnAnyExecutionSemesterFor(executionYear));

        final ExecutionSemester firstExecutionPeriod = executionYear.getFirstExecutionPeriod();
        result.setFirstSemesterEnrolmentsCount(
                calculateEnrolmentsCount(studentCurricularPlan, firstExecutionPeriod, new NormalEnrolmentsPredicate()));
        result.setFirstSemesterEnroledCredits(
                calculateEnroledCredits(studentCurricularPlan, firstExecutionPeriod, new NormalEnrolmentsPredicate()));
        result.setFirstSemesterApprovedCredits(
                calculateApprovedCredits(studentCurricularPlan, firstExecutionPeriod, new NormalEnrolmentsPredicate()));

        final ExecutionSemester lastExecutionPeriod = executionYear.getLastExecutionPeriod();
        result.setSecondSemesterEnrolmentsCount(
                calculateEnrolmentsCount(studentCurricularPlan, lastExecutionPeriod, new NormalEnrolmentsPredicate()));
        result.setSecondSemesterEnroledCredits(
                calculateEnroledCredits(studentCurricularPlan, lastExecutionPeriod, new NormalEnrolmentsPredicate()));
        result.setSecondSemesterApprovedCredits(
                calculateApprovedCredits(studentCurricularPlan, lastExecutionPeriod, new NormalEnrolmentsPredicate()));

        //TODO: change logic when degrees are not organized in years and semesters
        result.setTotalEnrolmentsCount(
                result.getFirstSemesterEnrolmentsCount().intValue() + result.getSecondSemesterEnrolmentsCount().intValue());
        result.setTotalEnroledCredits(result.getFirstSemesterEnroledCredits().add(result.getSecondSemesterEnroledCredits()));
        result.setTotalApprovedCredits(result.getFirstSemesterApprovedCredits().add(result.getSecondSemesterApprovedCredits()));

        result.setRegimeType(registration.getRegimeType(executionYear) == null ? RegistrationRegimeType.FULL_TIME : registration
                .getRegimeType(executionYear));

        final Curriculum curriculum = studentCurricularPlan.getCurriculum(new DateTime(), executionYear);
        result.setApprovedCredits(curriculum.getSumEctsCredits());

        result.setEquivalenceCredits(calculateEquivalenceCredits(studentCurricularPlan, executionYear));
        result.setCurricularYear(curriculum.getCurricularYear());
        LocalDate enrolmentDate = getEnrolmentDate(registration, executionYear);
        result.setEnrolmentDate(enrolmentDate);

        // calling getPrecedentDegreeRegistrations here is poor design.
        // That method should be refactored to the registration
        Set<ExecutionYear> collect = getEnrolmentYearsIncludingPrecedentRegistrations(registration);
        int size = collect.size();
        result.setEnrolmentYearsCount(size);
        result.setEnrolmentYearsInFullRegimeCount(calculateEnrolmentYearsInFullRegimeCount(registration, executionYear));
        result.setLastAcademicActDate(calculateLastAcademicActDate(registration, executionYear));

        return result;
    }

    static private Set<ExecutionYear> getEnrolmentYearsIncludingPrecedentRegistrations(final Registration input) {

        return AbstractFillScholarshipService.getExecutionYears(input, r -> r.getEnrolmentsExecutionYears().stream(), ey -> true);
    }

    private LocalDate getEnrolmentDate(Registration registration, ExecutionYear executionYear) {
        return registration.getRegistrationDataByExecutionYearSet().stream()
                .filter(rdbey -> rdbey.getExecutionYear() == executionYear).map(rdbey -> rdbey.getEnrolmentDate())
                .filter(x -> x != null).findAny().orElse(null);
    }

    //TODO: this should be moved to registration (including normal students predicate)
    private LocalDate calculateLastAcademicActDate(final Registration registration, final ExecutionYear executionYear) {

        if (registration.hasConcluded()) {

            final RegistrationConclusionBean conclusionBean;
            if (registration.getDegreeType().hasAnyCycleTypes()) {
                final CycleCurriculumGroup cycleCurriculumGroup =
                        registration.getLastStudentCurricularPlan().getLastConcludedCycleCurriculumGroup();
                conclusionBean = new RegistrationConclusionBean(registration, cycleCurriculumGroup);
            } else {
                conclusionBean = new RegistrationConclusionBean(registration);
            }

            if (conclusionBean.getConclusionYear() == executionYear) {
                return conclusionBean.getConclusionDate().toLocalDate();
            }
        }

        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);
        final Collection<Enrolment> normalEnrolments =
                getFilteredEnrolments(studentCurricularPlan, executionYear, new NormalEnrolmentsPredicate());

        final Collection<Enrolment> normalEnrolmentsEvaluated = normalEnrolments.stream()
                .filter(enrolment -> enrolment.getFinalEnrolmentEvaluation() != null).collect(Collectors.toSet());

        if (!normalEnrolmentsEvaluated.isEmpty()) {
            Comparator<? super Enrolment> compareByFinalEnrolmentEvalution = (x, y) -> EnrolmentEvaluation.COMPARATOR_BY_EXAM_DATE
                    .compare(x.getFinalEnrolmentEvaluation(), y.getFinalEnrolmentEvaluation());
            return Collections.max(normalEnrolmentsEvaluated, compareByFinalEnrolmentEvalution).getFinalEnrolmentEvaluation()
                    .getExamDateYearMonthDay().toLocalDate();
        }

        return registration.getRegistrationDataByExecutionYearSet().stream().filter(r -> r.getExecutionYear() == executionYear)
                .map(rdbey -> rdbey.getEnrolmentDate()).filter(x -> x != null).findAny().orElse(null);
    }

    private Integer calculateEnrolmentYearsInFullRegimeCount(final Registration registration, final ExecutionYear executionYear) {

        final Collection<ExecutionYear> enrolmentYears = getEnrolmentYearsIncludingPrecedentRegistrations(registration);
        return enrolmentYears.stream().filter(enrolmentYear -> !registration.isPartialRegime(enrolmentYear))
                .collect(Collectors.toSet()).size();

    }

    private BigDecimal calculateEquivalenceCredits(final StudentCurricularPlan studentCurricularPlan,
            final ExecutionYear executionYear) {
        BigDecimal result = BigDecimal.ZERO;

        final Curriculum curriculum = studentCurricularPlan.getCurriculum(new DateTime(), null);
        for (final ICurriculumEntry entry : curriculum.getCurricularYearEntries()) {
            if (entry.getExecutionYear() == executionYear && entry instanceof Dismissal
                    && ((Dismissal) entry).getCredits().isEquivalence()) {
                result = result.add(entry.getEctsCreditsForCurriculum());
            }
        }

        return result;
    }

    private BigDecimal calculateApprovedCredits(StudentCurricularPlan studentCurricularPlan, ExecutionSemester executionSemester,
            Predicate<Enrolment> predicate) {
        BigDecimal result = BigDecimal.ZERO;
        for (final Enrolment enrolment : getFilteredEnrolments(studentCurricularPlan, executionSemester, predicate)) {

            if (!enrolment.isApproved()) {
                continue;
            }

            result = result.add(enrolment.getEctsCreditsForCurriculum());
        }

        return result;
    }

    private BigDecimal calculateEnroledCredits(StudentCurricularPlan studentCurricularPlan, ExecutionSemester executionSemester,
            Predicate<Enrolment> predicate) {
        BigDecimal result = BigDecimal.ZERO;
        for (final Enrolment enrolment : getFilteredEnrolments(studentCurricularPlan, executionSemester, predicate)) {
            result = result.add(enrolment.getEctsCreditsForCurriculum());
        }

        return result;
    }

    private Integer calculateEnrolmentsCount(StudentCurricularPlan studentCurricularPlan, ExecutionSemester executionSemester,
            Predicate<Enrolment> predicate) {
        return getFilteredEnrolments(studentCurricularPlan, executionSemester, predicate).size();
    }

    private Collection<Enrolment> getFilteredEnrolments(StudentCurricularPlan studentCurricularPlan,
            ExecutionSemester executionSemester, Predicate<Enrolment> predicate) {
        final List<Enrolment> enrolments = studentCurricularPlan.getEnrolmentsByExecutionPeriod(executionSemester);
        return enrolments.stream().filter(predicate).collect(Collectors.toSet());
    }

    private Collection<Enrolment> getFilteredEnrolments(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear,
            Predicate<Enrolment> predicate) {
        final List<Enrolment> enrolments = studentCurricularPlan.getEnrolmentsByExecutionYear(executionYear);
        return enrolments.stream().filter(predicate).collect(Collectors.toSet());
    }

    protected Set<Registration> getRegistrationsToProcess(ExecutionYear executionYear, DegreeType degreeType) {

        final Set<Registration> result = new HashSet<>();

        for (final ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
            for (final Enrolment enrolment : executionSemester.getEnrolmentsSet()) {
                if (enrolment.getRegistration().getDegreeType() == degreeType) {
                    result.add(enrolment.getRegistration());
                }
            }
        }

        return result;
    }

}