package org.fenixedu.ulisboa.integration.sas.webservices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.ulisboa.integration.sas.domain.SchoolLevelTypeMapping;
import org.fenixedu.ulisboa.integration.sas.dto.ActiveStudentBean;
import org.fenixedu.ulisboa.specifications.domain.idcards.CgdCard;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

import com.qubit.solution.fenixedu.bennu.webservices.services.server.BennuWebService;

@WebService
public class ActiveStudentsWebService extends BennuWebService {

    @WebMethod
    public Collection<ActiveStudentBean> getActiveStudents() {
        return populateActiveStudents(calculateActiveStudents());
    }

    @WebMethod
    public Collection<ActiveStudentBean> getDailyRegistration() {
        return populateActiveStudents(calculateStudentsRegisteredInCurrentDay());
    }

    //Consider moving this logic to a different place
    private Collection<ActiveStudentBean> populateActiveStudents(Stream<Student> students) {
        return students.map(student -> populateActiveStudent(student)).collect(Collectors.toList());
    }

    private Stream<Student> calculateActiveStudents() {
        return Bennu.getInstance().getStudentsSet().stream().filter(student -> isActive(student));
    }

    private boolean isActive(Student student) {
        //TODO review the requirements since the concept of active student for SAS is not the same as in fenix
        return !student.getActiveRegistrations().isEmpty();
    }

    private Stream<Student> calculateStudentsRegisteredInCurrentDay() {
        return Bennu.getInstance().getDailyEnrolmentsSet().stream().map(enrolment -> enrolment.getStudent()).distinct();
    }

    private ActiveStudentBean populateActiveStudent(Student student) {
        // TODO review which registration to use
        // Return the first Registration found

        ActiveStudentBean activeStudentBean = new ActiveStudentBean();
        activeStudentBean.setName(student.getName());
        activeStudentBean.setGender(student.getPerson().getGender().toLocalizedString(Locale.getDefault()));
        //information still not available

        String mifareCode =
                student.getPerson().getCgdCardsSet().stream().filter(CgdCard::isValid).map(CgdCard::getMifareCode).findAny()
                        .orElse("");
        activeStudentBean.setMifare(mifareCode);

        activeStudentBean.setIdentificationNumber(student.getPerson().getDocumentIdNumber());
        activeStudentBean.setFiscalIdentificationNumber(student.getPerson().getSocialSecurityNumber());
        YearMonthDay dateOfBirthYearMonthDay = student.getPerson().getDateOfBirthYearMonthDay();
        activeStudentBean.setDateOfBirth(dateOfBirthYearMonthDay != null ? dateOfBirthYearMonthDay.toString() : "");
        activeStudentBean.setStudentCode(student.getNumber().toString());
        Country country = student.getPerson().getCountry();
        activeStudentBean.setOriginCountry(country != null ? country.getLocalizedName().getContent(Locale.getDefault()) : "");

        if (!student.getActiveRegistrations().isEmpty()) {
            Registration registration = student.getActiveRegistrations().iterator().next();
            SchoolLevelTypeMapping schoolLevelTypeMapping = registration.getDegreeType().getSchoolLevelTypeMapping();
            if (schoolLevelTypeMapping == null) {
                //Consider all courses without school level type mapping as the free course 
                activeStudentBean.setDegreeCode(ActiveDegreesWebService.FREE_COURSES_CODE);
            } else {
                activeStudentBean.setDegreeCode(registration.getDegree().getCode());
                activeStudentBean.setOficialDegreeCode(registration.getDegree().getMinistryCode());

                ArrayList<ExecutionYear> sortedExecutionYears = getSortedExecutionYears(registration);
                if (sortedExecutionYears.size() > 0) {
                    ExecutionYear currentExecutionYear = sortedExecutionYears.get(sortedExecutionYears.size() - 1);
                    activeStudentBean.setCurrentExecutionYear(currentExecutionYear.getName());
                    activeStudentBean.setEnroledECTTotal(Double.toString(registration.getEnrolmentsEcts(currentExecutionYear)));
                    activeStudentBean.setDateOfRegistration(getEnrolmentDate(registration, currentExecutionYear).toString());
                    activeStudentBean.setRegime(registration.getRegimeType(currentExecutionYear).toString());
                }

                if (sortedExecutionYears.size() > 1) {
                    ExecutionYear previousExecutionYear = sortedExecutionYears.get(sortedExecutionYears.size() - 1);
                    activeStudentBean.setPreviousExecutionYear(previousExecutionYear.getName());
                    activeStudentBean.setEnroledECTTotalInPreviousYear(Double.toString(registration
                            .getEnrolmentsEcts(previousExecutionYear)));
                    activeStudentBean.setApprovedECTTotalInPreviousYear(getApprovedEcts(registration, previousExecutionYear)
                            .toString());
                }

                activeStudentBean.setCurricularYear(Integer.toString(registration.getCurricularYear()));
            }
        } else {
            return null;
        }
        //information will only be available during implementation of academic-treasury
        activeStudentBean.setIsPayingSchool(true);

        return activeStudentBean;
    }

    private LocalDate getEnrolmentDate(Registration firstRegistration, ExecutionYear currentExecutionYear) {
        return firstRegistration.getRegistrationDataByExecutionYearSet().stream()
                .filter(rdby -> rdby.getExecutionYear().equals(currentExecutionYear)).findFirst().get().getEnrolmentDate();
    }

    private Double getApprovedEcts(Registration firstRegistration, ExecutionYear previousExecutionYear) {
        return firstRegistration.getEnrolments(previousExecutionYear).stream().filter(e -> e.isApproved())
                .map(e -> e.getEctsCredits()).reduce((n1, n2) -> n1 + n2).orElse(0.0);
    }

    private ArrayList<ExecutionYear> getSortedExecutionYears(Registration firstRegistration) {
        ArrayList<ExecutionYear> arrayList = new ArrayList<>();
        arrayList.addAll(firstRegistration.getEnrolmentsExecutionYears());
        arrayList.sort((e1, e2) -> e1.compareTo(e2));
        return arrayList;
    }
}
