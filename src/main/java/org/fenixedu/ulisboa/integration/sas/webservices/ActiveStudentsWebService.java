package org.fenixedu.ulisboa.integration.sas.webservices;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
import org.fenixedu.ulisboa.specifications.domain.idcards.CgdCard;
import org.fenixedu.ulisboa.specifications.service.StudentActive;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

import com.qubit.solution.fenixedu.bennu.webservices.services.server.BennuWebService;

import edu.emory.mathcs.backport.java.util.Collections;

@WebService
public class ActiveStudentsWebService extends BennuWebService {

    private static Logger logger = LoggerFactory.getLogger(ActiveStudentsWebService.class);
    private static WeakReference<Collection<ActiveStudentBean>> cache = null;
    private static long timestamp = 0;

    private static class ActiveStudentCalculator implements CallableWithoutException<Object> {

        @Override
        public Object call() {
            cache = new WeakReference<Collection<ActiveStudentBean>>(parallelPopulateActiveStudents(calculateActiveStudents()));
            timestamp = System.currentTimeMillis();
            return null;
        }

    }

    static {
        logger.info("Launching ActiveStudentBean cache updater!");
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                boolean run = true;

                while (run) {
                    logger.info("Updating ActiveStudentsBean cache");
                    if (cache == null || cache.get() == null || System.currentTimeMillis() - timestamp > 3600 * 4) {
                        FenixFramework.getTransactionManager().withTransaction(new ActiveStudentCalculator());
                    }
                    logger.info("Updated ActiveStudentsBean cache");
                    try {
                        Thread.sleep(600 * 1000);
                    } catch (InterruptedException e) {
                        run = false;
                    }

                }
            }
        });
        thread.setName("ActiveStudenBean cache updater");
        thread.start();
    }

    @WebMethod
    public Collection<ActiveStudentBean> getActiveStudents() {
        Collection<ActiveStudentBean> collection = cache != null ? cache.get() : null;
        return collection == null ? Collections.emptyList() : collection;

    }

    @WebMethod
    public Collection<ActiveStudentBean> getDailyRegistration() {
        return parallelPopulateActiveStudents(calculateStudentsRegisteredInCurrentDay());
    }

    @WebMethod
    public Collection<ActiveStudentBean> getCurrentDayIssuedCards() {
        return parallelPopulateActiveStudents(getStudentsWithCardsIssuedToday());
    }

    private static List<ActiveStudentBean> parallelPopulateActiveStudents(List<Student> collect) {
        List<StudentDataCollector> collectors = new ArrayList<StudentDataCollector>();
        int size = collect.size();
        int split = size / 20 + ((size % 20) > 0 ? 1 : 0);
        for (int i = 0; i < 20; i++) {
            int start = i * split;
            int end = Math.min(start + split, size);
            if (start >= end) {
                // we don't need that many threads. let's break the cycle
                break;
            }
            collectors.add(new StudentDataCollector(collect.subList(start, end)));
        }

        List<Thread> threadList = new ArrayList<Thread>();
        for (StudentDataCollector collector : collectors) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    Collection<ActiveStudentBean> results = FenixFramework.getTransactionManager().withTransaction(collector);
                    collector.setBeans(results);
                }
            });
            threadList.add(t);
            t.start();
        }
        List<ActiveStudentBean> beans = new ArrayList<ActiveStudentBean>();
        for (Thread t : threadList) {
            try {
                t.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        for (StudentDataCollector collector : collectors) {
            beans.addAll(collector.getBeans());
        }
        return beans;
    }

    private static class StudentDataCollector implements CallableWithoutException<Collection<ActiveStudentBean>> {

        private final List<Student> students;
        private Collection<ActiveStudentBean> beans;

        public StudentDataCollector(List<Student> students) {
            this.students = students;
        }

        public void setBeans(Collection<ActiveStudentBean> beans) {
            this.beans = beans;
        }

        public Collection<ActiveStudentBean> getBeans() {
            return beans;
        }

        @Override
        public Collection<ActiveStudentBean> call() {
            return populateActiveStudents(students.stream());
        }

    }

    //Consider moving this logic to a different place
    public static Collection<ActiveStudentBean> populateActiveStudents(Stream<Student> students) {
        List<ActiveStudentBean> collect = students.map(student -> populateActiveStudent(student)).collect(Collectors.toList());
        return collect;
    }

    private static List<Student> calculateActiveStudents() {
        return Bennu.getInstance().getStudentsSet().stream().filter(student -> StudentActive.isActiveStudent(student))
                .collect(Collectors.toList());
    }

    private List<Student> calculateStudentsRegisteredInCurrentDay() {
        return Bennu.getInstance().getDailyEnrolmentsSet().stream().map(enrolment -> enrolment.getStudent()).distinct()
                .collect(Collectors.toList());
    }

    private static ActiveStudentBean populateActiveStudent(Student student) {
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
            activeStudentBean.setCardIssueDate(card.get().getLastMifareModication().toString());
            activeStudentBean.setCardNumber(card.get().getCardNumber());
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

    private static LocalDate getEnrolmentDate(Registration firstRegistration, ExecutionYear currentExecutionYear) {
        RegistrationDataByExecutionYear registrationDataByExecutionYear =
                firstRegistration.getRegistrationDataByExecutionYearSet().stream()
                        .filter(rdby -> rdby.getExecutionYear().equals(currentExecutionYear)).findFirst().orElse(null);
        return registrationDataByExecutionYear != null ? registrationDataByExecutionYear.getEnrolmentDate() : null;
    }

    private static Double getApprovedEcts(Registration firstRegistration, ExecutionYear previousExecutionYear) {
        return firstRegistration.getEnrolments(previousExecutionYear).stream().filter(e -> e.isApproved())
                .map(e -> e.getEctsCredits()).reduce((n1, n2) -> n1 + n2).orElse(0.0);
    }

    private static ArrayList<ExecutionYear> getSortedExecutionYears(Registration firstRegistration) {
        ArrayList<ExecutionYear> arrayList = new ArrayList<>();
        arrayList.addAll(firstRegistration.getEnrolmentsExecutionYears());
        arrayList.sort((e1, e2) -> e1.compareTo(e2));
        return arrayList;
    }

    private List<Student> getStudentsWithCardsIssuedToday() {
        // We are comparing the card modification date instead of the card issued date
        // This is required since the card issued date may be some day before the card insertion in the system
        return Bennu.getInstance().getCgdCardsSet().stream()
                .filter(card -> isToday(card.getLastMifareModication()) && card.getPerson().getStudent() != null)
                .map(card -> card.getPerson().getStudent()).collect(Collectors.toList());
    }

    private boolean isToday(LocalDate b) {
        LocalDate now = LocalDate.now();
        return now.year().get() == b.year().get() && now.monthOfYear().get() == b.monthOfYear().get()
                && now.dayOfMonth().get() == b.dayOfMonth().get();
    }

}
