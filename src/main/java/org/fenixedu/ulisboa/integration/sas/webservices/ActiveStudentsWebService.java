package org.fenixedu.ulisboa.integration.sas.webservices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.ulisboa.integration.sas.domain.SchoolLevelTypeMapping;
import org.fenixedu.ulisboa.integration.sas.dto.ActiveStudentBean;
import org.fenixedu.ulisboa.integration.sas.dto.StudentIssuedCardBean;
import org.fenixedu.ulisboa.specifications.domain.idcards.CgdCard;
import org.fenixedu.ulisboa.specifications.domain.idcards.CgdCard_Base;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.springframework.cglib.core.Local;

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

    @WebMethod
    public Collection<ActiveStudentBean> getCurrentDayIssuedCards() {
        return populateActiveStudents(getStudentsWithCardsIssuedToday());
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

        Optional<CgdCard> card = student.getPerson().getCgdCardsSet().stream().filter(CgdCard::isValid).findAny();
        if (card.isPresent()) {
            String mifareCode = card.get().getMifareCode();
            activeStudentBean.setMifare(mifareCode);
            activeStudentBean.setIsTemporaryCard(Boolean.toString(card.get().getTemporary()));
            activeStudentBean.setCardIssueDate(card.get().getIssueDate().toString());
            //TODO add card number
            //activeStudentBean.setCardNumber(cardNumber);
        }

        activeStudentBean.setIdentificationNumber(student.getPerson().getDocumentIdNumber());
        activeStudentBean.setFiscalIdentificationNumber(student.getPerson().getSocialSecurityNumber());
        YearMonthDay dateOfBirthYearMonthDay = student.getPerson().getDateOfBirthYearMonthDay();
        activeStudentBean.setDateOfBirth(dateOfBirthYearMonthDay != null ? dateOfBirthYearMonthDay.toString() : "");

        Country country = student.getPerson().getCountry();
        activeStudentBean.setOriginCountry(country != null ? country.getLocalizedName().getContent(Locale.getDefault()) : "");

        if (!student.getActiveRegistrations().isEmpty()) {
            Registration registration = student.getActiveRegistrations().iterator().next();
            activeStudentBean.setStudentCode(Integer.toString(registration.getNumber()));
            SchoolLevelTypeMapping schoolLevelTypeMapping = registration.getDegreeType().getSchoolLevelTypeMapping();
            if (schoolLevelTypeMapping == null) {
                //Consider all courses without school level type mapping as the free course 
                activeStudentBean.setDegreeCode(ActiveDegreesWebService.FREE_COURSES_CODE);
            } else {
                activeStudentBean.setDegreeCode(registration.getDegree().getCode());
                activeStudentBean.setOficialDegreeCode(registration.getDegree().getMinistryCode());
            }
            ArrayList<ExecutionYear> sortedExecutionYears = getSortedExecutionYears(registration);
            if (sortedExecutionYears.size() > 0) {
                ExecutionYear currentExecutionYear = sortedExecutionYears.get(sortedExecutionYears.size() - 1);
                activeStudentBean.setCurrentExecutionYear(currentExecutionYear.getName());
                activeStudentBean.setEnroledECTTotal(Double.toString(registration.getEnrolmentsEcts(currentExecutionYear)));
                LocalDate enrolmentDate = getEnrolmentDate(registration, currentExecutionYear);
                activeStudentBean.setDateOfRegistration(enrolmentDate != null ? enrolmentDate.toString() : "");
                activeStudentBean.setRegime(registration.getRegimeType(currentExecutionYear).toString());
                boolean toPayTuition =
                        TreasuryBridgeAPIFactory.implementation().isToPayTuition(registration, currentExecutionYear);
                activeStudentBean.setIsPayingSchool(toPayTuition);
            }

            if (sortedExecutionYears.size() > 1) {
                ExecutionYear previousExecutionYear = sortedExecutionYears.get(sortedExecutionYears.size() - 2);
                activeStudentBean.setPreviousExecutionYear(previousExecutionYear.getName());
                activeStudentBean.setEnroledECTTotalInPreviousYear(Double.toString(registration
                        .getEnrolmentsEcts(previousExecutionYear)));
                activeStudentBean.setApprovedECTTotalInPreviousYear(getApprovedEcts(registration, previousExecutionYear)
                        .toString());
            }

            activeStudentBean.setCurricularYear(Integer.toString(registration.getCurricularYear()));
        } else {
            return null;
        }

        return activeStudentBean;
    }

    private LocalDate getEnrolmentDate(Registration firstRegistration, ExecutionYear currentExecutionYear) {
        RegistrationDataByExecutionYear registrationDataByExecutionYear =
                firstRegistration.getRegistrationDataByExecutionYearSet().stream()
                        .filter(rdby -> rdby.getExecutionYear().equals(currentExecutionYear)).findFirst().orElse(null);
        return registrationDataByExecutionYear != null ? registrationDataByExecutionYear.getEnrolmentDate() : null;
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

    private Stream<Student> getStudentsWithCardsIssuedToday() {
        // TODO we should not be comparing the card issued date, but the card modification date
        // This is required since the card issued date may be some day before the card insertion in the system
        return Bennu.getInstance().getCgdCardsSet().stream()
                .filter(card -> isToday(card.getIssueDate()) && card.getPerson().getStudent() != null)
                .map(card -> card.getPerson().getStudent());
    }

    public boolean isToday(LocalDate b) {
        LocalDate now = LocalDate.now();
        return now.year() == b.year() && now.monthOfYear() == b.monthOfYear() && now.dayOfMonth() == b.dayOfMonth();
    }

}
