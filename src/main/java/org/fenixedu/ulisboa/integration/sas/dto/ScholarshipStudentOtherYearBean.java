package org.fenixedu.ulisboa.integration.sas.dto;

import java.math.BigDecimal;

import org.joda.time.LocalDate;

public class ScholarshipStudentOtherYearBean extends AbstractScholarshipStudentBean {
    
    static final long serialVersionUID = 1L;

    private Integer numberOfDegreeChanges;
    private Boolean hasMadeDegreeChangeOnCurrentYear;
    private Integer cycleIngressionYear;
    private Integer cycleNumberOfEnrolmentsYearsInIntegralRegime;
    private Integer lastEnrolmentCurricularYear;
    private BigDecimal numberOfEnrolledEctsLastYear;
    private BigDecimal numberOfApprovedEctsLastYear;
    private BigDecimal numberOfApprovedEcts;
    private Integer curricularYear;
    private Integer lastEnrolmentYear;
    private LocalDate lastAcademicActDateLastYear;

    public Integer getNumberOfDegreeChanges() {
        return numberOfDegreeChanges;
    }

    public void setNumberOfDegreeChanges(Integer numberOfDegreeChanges) {
        this.numberOfDegreeChanges = numberOfDegreeChanges;
    }

    public Integer getCycleNumberOfEnrolmentsYearsInIntegralRegime() {
        return cycleNumberOfEnrolmentsYearsInIntegralRegime;
    }

    public void setCycleNumberOfEnrolmentsYearsInIntegralRegime(Integer numberOfEnrolmentsYearsInIntegralRegime) {
        this.cycleNumberOfEnrolmentsYearsInIntegralRegime = numberOfEnrolmentsYearsInIntegralRegime;
    }

    public Boolean getHasMadeDegreeChangeOnCurrentYear() {
        return hasMadeDegreeChangeOnCurrentYear;
    }

    public void setHasMadeDegreeChangeOnCurrentYear(Boolean hasMadeDegreeChangeOnCurrentYear) {
        this.hasMadeDegreeChangeOnCurrentYear = hasMadeDegreeChangeOnCurrentYear;
    }

    public Integer getCycleIngressionYear() {
        return cycleIngressionYear;
    }

    public void setCycleIngressionYear(Integer ingressionYear) {
        this.cycleIngressionYear = ingressionYear;
    }

    public Integer getLastEnrolmentCurricularYear() {
        return lastEnrolmentCurricularYear;
    }

    public void setLastEnrolmentCurricularYear(Integer lastEnrolmentCurricularYear) {
        this.lastEnrolmentCurricularYear = lastEnrolmentCurricularYear;
    }

    public BigDecimal getNumberOfEnrolledEctsLastYear() {
        return numberOfEnrolledEctsLastYear;
    }

    public void setNumberOfEnrolledEctsLastYear(BigDecimal numberOfEnrolledEctsLastYear) {
        this.numberOfEnrolledEctsLastYear = numberOfEnrolledEctsLastYear;
    }

    public BigDecimal getNumberOfApprovedEctsLastYear() {
        return numberOfApprovedEctsLastYear;
    }

    public void setNumberOfApprovedEctsLastYear(BigDecimal numberOfApprovedEctsLastYear) {
        this.numberOfApprovedEctsLastYear = numberOfApprovedEctsLastYear;
    }
    
    public BigDecimal getNumberOfApprovedEcts() {
        return numberOfApprovedEcts;
    }

    public void setNumberOfApprovedEcts(BigDecimal numberOfApprovedEcts) {
        this.numberOfApprovedEcts = numberOfApprovedEcts;
    }

    public Integer getCurricularYear() {
        return curricularYear;
    }

    public void setCurricularYear(Integer curricularYear) {
        this.curricularYear = curricularYear;
    }

    public LocalDate getLastAcademicActDateLastYear() {
        return lastAcademicActDateLastYear;
    }

    public void setLastAcademicActDateLastYear(LocalDate lastAcademicActDateLastYear) {
        this.lastAcademicActDateLastYear = lastAcademicActDateLastYear;
    }

    public Integer getLastEnrolmentYear() {
        return lastEnrolmentYear;
    }

    public void setLastEnrolmentYear(Integer lastEnrolmentYear) {
        this.lastEnrolmentYear = lastEnrolmentYear;
    }

}
