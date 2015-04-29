package org.fenixedu.ulisboa.integration.sas.dto;

import java.io.Serializable;

public class ActiveDegreeBean implements Serializable {
    String degreeCode;
    String designation;
    String schoolLevel;
    String cycle;
    String duration;
    String oficialCode;

    public String getDegreeCode() {
        return degreeCode;
    }

    public void setDegreeCode(String degreeCode) {
        this.degreeCode = degreeCode;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getSchoolLevel() {
        return schoolLevel;
    }

    public void setSchoolLevel(String schoolLevel) {
        this.schoolLevel = schoolLevel;
    }

    public String getCycle() {
        return cycle;
    }

    public void setCycle(String cycle) {
        this.cycle = cycle;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getOficialCode() {
        return oficialCode;
    }

    public void setOficialCode(String oficialCode) {
        this.oficialCode = oficialCode;
    }

}
