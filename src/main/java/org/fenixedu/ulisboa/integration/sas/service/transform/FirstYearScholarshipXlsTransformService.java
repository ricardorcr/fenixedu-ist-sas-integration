package org.fenixedu.ulisboa.integration.sas.service.transform;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.fenixedu.ulisboa.integration.sas.dto.ScholarshipStudentFirstYearBean;

public class FirstYearScholarshipXlsTransformService extends AbstractScholarshipXlsTransformService {

    static Integer FIRST_VALUE_ROW = 1;

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
    public static Integer COLUMN_REGISTERED = 11;
    public static Integer COLUMN_REGISTRATION_DATE = 12;
    public static Integer COLUMN_GRATUITY = 13;
    public static Integer COLUMN_NUMBER_OF_MONTHS_EXECUTION_YEAR = 14;
    public static Integer COLUMN_FIRST_MONTH_EXECUTION_YEAR = 15;
    public static Integer COLUMN_OWNER_CET = 16;
    public static Integer COLUMN_OWNER_CTSP = 17;
    public static Integer COLUMN_OWNER_BACHELOR = 18;
    public static Integer COLUMN_OWNER_MASTER = 19;
    public static Integer COLUMN_OWNER_PHD = 20;
    public static Integer COLUMN_OWNER_OF_HIGHER_QUALIFICATION = 21;
    public static Integer COLUMN_OBSERVATIONS = 22;
    public static Integer COLUMN_REGIME = 23;
    public static Integer COLUMN_NUMBER_OF_YEARS_DEGREE = 24;
    public static Integer COLUMN_NUMBER_OF_REGISTRATIONS_SINCE_REGISTRATION_START = 25;
    public static Integer COLUMN_NUMBER_OF_ECTS = 26;
    public static Integer COLUMN_FISCAL_CODE = 27;
    public static Integer COLUMN_DOCUMENT_BI = 28;

    @Override
    public boolean checkExcelYear(HSSFSheet sheet) throws IOException {
        // contains cells merged?
        // return sheet.getMergedRegion(0) == null;
        return sheet.getRow(0).getLastCellNum() == 29;
    }

    @Override
    public void readStudentLines(HSSFSheet sheet) {
        int i = FIRST_VALUE_ROW;
        HSSFRow row;
        while ((row = sheet.getRow(i)) != null && row.getCell(0) != null) {
            ScholarshipStudentFirstYearBean scholarshipStudentBean = new ScholarshipStudentFirstYearBean();
            readSpreadsheetRow(row, scholarshipStudentBean);
            studentLines.add(scholarshipStudentBean);
            i++;
        }
    }

    private void readSpreadsheetRow(final HSSFRow row, ScholarshipStudentFirstYearBean bean) {
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

        bean.setFiscalCode(getValueFromColumnMayBeNull(row, COLUMN_FISCAL_CODE));
        bean.setDocumentBINumber(getValueFromColumnMayBeNull(row, COLUMN_DOCUMENT_BI));
    }

    @Override
    public void writeExcelLines(HSSFSheet sheet) throws IOException {
        for (int i = 0; i < studentLines.size(); i++) {
            ScholarshipStudentFirstYearBean scholarshipStudentBean = (ScholarshipStudentFirstYearBean) studentLines.get(i);
            HSSFRow row = sheet.getRow(FIRST_VALUE_ROW + i);
            writeSpreadsheetRow(row, scholarshipStudentBean);
        }
    }

    private void writeSpreadsheetRow(HSSFRow row, ScholarshipStudentFirstYearBean bean) {
        writeCellString(row, COLUMN_INSTITUTION_CODE, bean.getInstitutionCode());
        writeCellInteger(row, COLUMN_STUDENT_NUMBER, bean.getStudentNumber());
        writeCellString(row, COLUMN_DEGREE_CODE, bean.getDegreeCode());
        writeCellString(row, COLUMN_REGISTERED, booleanToString(bean.getRegistered()));

        writeCellLocalDate(row, COLUMN_REGISTRATION_DATE, bean.getRegistered() ? bean.getRegistrationDate() : null);
        writeCellBigDecimal(row, COLUMN_GRATUITY, bean.getRegistered() ? bean.getGratuityAmount() : null);
        writeCellInteger(row, COLUMN_NUMBER_OF_MONTHS_EXECUTION_YEAR,
                bean.getRegistered() ? bean.getNumberOfMonthsExecutionYear() : null);
        writeCellString(row, COLUMN_FIRST_MONTH_EXECUTION_YEAR, bean.getRegistered() ? bean.getFirstMonthExecutionYear() : null);
        writeCellString(row, COLUMN_OWNER_CET, bean.getRegistered() ? booleanToString(bean.getCetQualificationOwner()) : null);
        writeCellString(row, COLUMN_OWNER_CTSP, bean.getRegistered() ? booleanToString(bean.getCtspQualificationOwner()) : null);
        writeCellString(row, COLUMN_OWNER_BACHELOR,
                bean.getRegistered() ? booleanToString(bean.getDegreeQualificationOwner()) : null);
        writeCellString(row, COLUMN_OWNER_MASTER,
                bean.getRegistered() ? booleanToString(bean.getMasterQualificationOwner()) : null);
        writeCellString(row, COLUMN_OWNER_PHD, bean.getRegistered() ? booleanToString(bean.getPhdQualificationOwner()) : null);
        writeCellString(row, COLUMN_OBSERVATIONS, bean.getRegistered() ? bean.getObservations() : null);
        writeCellString(row, COLUMN_REGIME, bean.getRegistered() ? bean.getRegime() : null);

        writeCellInteger(row, COLUMN_NUMBER_OF_YEARS_DEGREE,
                bean.getRegistered() ? bean.getNumberOfDegreeCurricularYears() : null);
        writeCellInteger(row, COLUMN_NUMBER_OF_REGISTRATIONS_SINCE_REGISTRATION_START,
                bean.getRegistered() ? bean.getCycleNumberOfEnrolmentYears() : null);
        writeCellBigDecimal(row, COLUMN_NUMBER_OF_ECTS, bean.getRegistered() ? bean.getNumberOfEnrolledECTS() : null);
    }

    public FirstYearScholarshipXlsTransformService(POIFSFileSystem poifsFileSystem) {
        this.studentLines = new ArrayList<>();
        this.poifsFileSystem = poifsFileSystem;
    }
}
