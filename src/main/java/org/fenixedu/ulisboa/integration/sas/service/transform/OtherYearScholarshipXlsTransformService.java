package org.fenixedu.ulisboa.integration.sas.service.transform;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.fenixedu.ulisboa.integration.sas.dto.ScholarshipStudentOtherYearBean;

public class OtherYearScholarshipXlsTransformService extends AbstractScholarshipXlsTransformService {

    static Integer FIRST_VALUE_ROW = 2;

    public static Integer COLUMN_INSTITUTION_CODE = 0;
    public static Integer COLUMN_INSTITUTION_NAME = 1;
    public static Integer COLUMN_CANDIDACY_NUMBER = 2;
    public static Integer COLUMN_STUDENT_NUMBER = 3;
    public static Integer COLUMN_STUDENT_NAME = 4;
    public static Integer COLUMN_DOCUMENT_TYPE_NAME = 5;
    public static Integer COLUMN_DOCUMENT_NUMBER = 6;
    public static Integer COLUMN_DEGREE_CODE = 7;
    public static Integer COLUMN_DEGREE_NAME = 8;
    public static Integer COLUMN_DEGREE_TYPE_NAME = 9;
    public static Integer COLUMN_CODE = 10;
    public static Integer COLUMN_COUNT_NUMBER_OF_DEGREE_CHANGES = 11;
    public static Integer COLUMN_CURRENT_YEAR_HAS_MADE_DEGREE_CHANGE = 12;
    public static Integer COLUMN_REGISTERED = 13;
    public static Integer COLUMN_REGISTRATION_DATE = 14;
    public static Integer COLUMN_REGIME = 15;
    public static Integer COLUMN_CODE1 = 16;
    public static Integer COLUMN_CYCLE_INGRESSION_YEAR = 17;
    public static Integer COLUMN_CYCLE_NUMBER_OF_ENROLMENT_YEARS = 18;
    public static Integer COLUMN_CYCLE_COUNT_NUMBER_OF_ENROLMENTS_YEARS_IN_INTEGRAL_REGIME = 19;
    public static Integer COLUMN_NUMBER_OF_APPROVED_ECTS = 20;
    public static Integer COLUMN_NUMBER_OF_YEARS_DEGREE = 21;
    public static Integer COLUMN_LAST_ENROLMENT_CURRICULAR_YEAR = 22;
    public static Integer COLUMN_NUMBER_OF_ENROLLED_ECTS_LAST_YEAR = 23;
    public static Integer COLUMN_NUMBER_OF_APPROVED_ECTS_LAST_YEAR = 24;
    public static Integer COLUMN_CURRICULAR_YEAR = 25;
    public static Integer COLUMN_NUMBER_OF_ECTS = 26;
    public static Integer COLUMN_GRATUITY = 27;
    public static Integer COLUMN_NUMBER_OF_MONTHS_EXECUTION_YEAR = 28;
    public static Integer COLUMN_FIRST_MONTH_EXECUTION_YEAR = 29;
    public static Integer COLUMN_OWNER_CET = 30;
    public static Integer COLUMN_OWNER_CSTP = 31;
    public static Integer COLUMN_OWNER_BACHELOR = 32;
    public static Integer COLUMN_OWNER_MASTER = 33;
    public static Integer COLUMN_OWNER_PHD = 34;
    public static Integer COLUMN_OWNER_OF_HIGHER_QUALIFICATION = 35;
    public static Integer COLUMN_OBSERVATIONS = 36;
    public static Integer COLUMN_LAST_ENROLMENT_EXECUTION_YEAR = 37;
    public static Integer COLUMN_FISCAL_CODE = 38;
    public static Integer COLUMN_LAST_ACADEMIC_ACT_DATE_LAST_YEAR = 39;
    public static Integer COLUMN_DOCUMENT_BI = 40;

    @Override
    public boolean checkExcelYear(HSSFSheet sheet) throws IOException {
//since we are editing the files by hand, the number of columns won't be right
//        return sheet.getRow(0).getLastCellNum() == 41;
        return true;
    }

    @Override
    public void readStudentLines(HSSFSheet sheet) {
        int i = FIRST_VALUE_ROW;
        HSSFRow row;
        while ((row = sheet.getRow(i)) != null && row.getCell(0) != null) {
            ScholarshipStudentOtherYearBean bean = new ScholarshipStudentOtherYearBean();
            readSpreadsheetRow(row, bean);
            studentLines.add(bean);
            i++;
        }
    }

    private void readSpreadsheetRow(HSSFRow row, ScholarshipStudentOtherYearBean bean) {
        bean.setInstitutionCode(getValueFromColumnMayBeNull(row, COLUMN_INSTITUTION_CODE));
        bean.setInstitutionName(getValueFromColumnMayBeNull(row, COLUMN_INSTITUTION_NAME));
        bean.setCandidacyNumber(getValueFromColumnMayBeNull(row, COLUMN_CANDIDACY_NUMBER));
        try {
            bean.setStudentNumber(Integer.parseInt(getValueFromColumnMayBeNull(row, COLUMN_STUDENT_NUMBER)));
        } catch (NumberFormatException e) {
            bean.setStudentNumber(null);
        }
        bean.setStudentName(getValueFromColumnMayBeNull(row, COLUMN_STUDENT_NAME));
        bean.setDocumentTypeName(getValueFromColumnMayBeNull(row, COLUMN_DOCUMENT_TYPE_NAME));
        bean.setDocumentNumber(getValueFromColumnMayBeNull(row, COLUMN_DOCUMENT_NUMBER));
        bean.setDegreeCode(getValueFromColumnMayBeNull(row, COLUMN_DEGREE_CODE));
        bean.setDegreeName(getValueFromColumnMayBeNull(row, COLUMN_DEGREE_NAME));
        bean.setDegreeTypeName(getValueFromColumnMayBeNull(row, COLUMN_DEGREE_TYPE_NAME));
        bean.setCode(getValueFromColumnMayBeNull(row, COLUMN_CODE));
        try {
            bean.setCycleIngressionYear(Integer.parseInt(getValueFromColumnMayBeNull(row, COLUMN_CYCLE_INGRESSION_YEAR)));
        } catch (NumberFormatException e) {
            bean.setCycleIngressionYear(null);
        }
        bean.setDocumentBINumber(getValueFromColumnMayBeNull(row, COLUMN_DOCUMENT_BI));
    }

    @Override
    public void writeExcelLines(HSSFSheet sheet) throws IOException {
        for (int i = 0; i < studentLines.size(); i++) {
            ScholarshipStudentOtherYearBean scholarshipStudentBean = (ScholarshipStudentOtherYearBean) studentLines.get(i);
            HSSFRow row = sheet.getRow(FIRST_VALUE_ROW + i);
            writeSpreadsheetRow(row, scholarshipStudentBean);
        }
    }

    private void writeSpreadsheetRow(HSSFRow row, ScholarshipStudentOtherYearBean bean) {
        writeCellString(row, COLUMN_INSTITUTION_CODE, bean.getInstitutionCode());
        writeCellInteger(row, COLUMN_STUDENT_NUMBER, bean.getStudentNumber());
        writeCellString(row, COLUMN_DEGREE_CODE, bean.getDegreeCode());
        writeCellInteger(row, COLUMN_COUNT_NUMBER_OF_DEGREE_CHANGES, bean.getNumberOfDegreeChanges());
        writeCellString(row, COLUMN_CURRENT_YEAR_HAS_MADE_DEGREE_CHANGE,
                booleanToString(bean.getHasMadeDegreeChangeOnCurrentYear()));
        writeCellString(row, COLUMN_REGISTERED, booleanToString(bean.getRegistered()));
        writeCellLocalDate(row, COLUMN_REGISTRATION_DATE, bean.getRegistered() ? bean.getRegistrationDate() : null);
        writeCellString(row, COLUMN_REGIME, bean.getRegistered() ? bean.getRegime() : null);
        writeCellInteger(row, COLUMN_CYCLE_INGRESSION_YEAR, bean.getRegistered() ? bean.getCycleIngressionYear() : null);
        writeCellInteger(row, COLUMN_CYCLE_NUMBER_OF_ENROLMENT_YEARS,
                bean.getRegistered() ? bean.getCycleNumberOfEnrolmentYears() : null);
        writeCellInteger(row, COLUMN_CYCLE_COUNT_NUMBER_OF_ENROLMENTS_YEARS_IN_INTEGRAL_REGIME,
                bean.getRegistered() ? bean.getCycleNumberOfEnrolmentsYearsInIntegralRegime() : null);
        writeCellBigDecimal(row, COLUMN_NUMBER_OF_APPROVED_ECTS, bean.getRegistered() ? bean.getNumberOfApprovedEcts() : null);
        writeCellInteger(row, COLUMN_NUMBER_OF_YEARS_DEGREE,
                bean.getRegistered() ? bean.getNumberOfDegreeCurricularYears() : null);
        writeCellInteger(row, COLUMN_LAST_ENROLMENT_CURRICULAR_YEAR,
                bean.getRegistered() ? bean.getLastEnrolmentCurricularYear() : null);
        writeCellBigDecimal(row, COLUMN_NUMBER_OF_ENROLLED_ECTS_LAST_YEAR,
                bean.getRegistered() ? bean.getNumberOfEnrolledEctsLastYear() : null);
        writeCellBigDecimal(row, COLUMN_NUMBER_OF_APPROVED_ECTS_LAST_YEAR,
                bean.getRegistered() ? bean.getNumberOfApprovedEctsLastYear() : null);
        writeCellInteger(row, COLUMN_CURRICULAR_YEAR, bean.getRegistered() ? bean.getCurricularYear() : null);
        writeCellBigDecimal(row, COLUMN_NUMBER_OF_ECTS, bean.getRegistered() ? bean.getNumberOfEnrolledECTS() : null);
        writeCellBigDecimal(row, COLUMN_GRATUITY, bean.getRegistered() ? bean.getGratuityAmount() : null);
        writeCellInteger(row, COLUMN_NUMBER_OF_MONTHS_EXECUTION_YEAR,
                bean.getRegistered() ? bean.getNumberOfMonthsExecutionYear() : null);
        writeCellString(row, COLUMN_FIRST_MONTH_EXECUTION_YEAR, bean.getRegistered() ? bean.getFirstMonthExecutionYear() : null);
        writeCellString(row, COLUMN_OWNER_CET, bean.getRegistered() ? booleanToString(bean.getCetQualificationOwner()) : null);
        writeCellString(row, COLUMN_OWNER_CSTP, bean.getRegistered() ? booleanToString(bean.getCtspQualificationOwner()) : null);
        writeCellString(row, COLUMN_OWNER_BACHELOR,
                bean.getRegistered() ? booleanToString(bean.getDegreeQualificationOwner()) : null);
        writeCellString(row, COLUMN_OWNER_MASTER,
                bean.getRegistered() ? booleanToString(bean.getMasterQualificationOwner()) : null);
        writeCellString(row, COLUMN_OWNER_PHD, bean.getRegistered() ? booleanToString(bean.getPhdQualificationOwner()) : null);
        writeCellString(row, COLUMN_OBSERVATIONS, bean.getRegistered() ? bean.getObservations() : null);
        writeCellInteger(row, COLUMN_LAST_ENROLMENT_EXECUTION_YEAR, bean.getRegistered() ? bean.getLastEnrolmentYear() : null);
        writeCellLocalDate(row, COLUMN_LAST_ACADEMIC_ACT_DATE_LAST_YEAR,
                bean.getRegistered() ? bean.getLastAcademicActDateLastYear() : null);

    }

    public OtherYearScholarshipXlsTransformService(POIFSFileSystem poifsFileSystem) {
        scholarshipStudentBean = new ScholarshipStudentOtherYearBean();
        this.studentLines = new ArrayList<>();
        this.poifsFileSystem = poifsFileSystem;
    }

}
