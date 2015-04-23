package org.fenixedu.ulisboa.integration.sas.service.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.fenixedu.academic.domain.SchoolLevelType;
import org.fenixedu.academic.domain.degree.DegreeType;

import com.google.common.collect.Maps;

//TODO: delete this class when degreetypes generate schoolleveltypes and schoolleveltypes can be grouped
public class SchoolLevelTypeMapping {

//    private static final List<SchoolLevelType> CTSP_SCHOOL_LEVELS = Arrays.asList(SchoolLevelType.HIGHER_TECHNICAL_PROFESSIONALS);
    private static final List<SchoolLevelType> CTSP_SCHOOL_LEVELS = new ArrayList<>();

    private static final List<SchoolLevelType> CET_SCHOOL_LEVELS = Arrays.asList(SchoolLevelType.TECHNICAL_SPECIALIZATION);

    private static final List<SchoolLevelType> DEGREE_SCHOOL_LEVELS = Arrays.asList(SchoolLevelType.DEGREE,
            SchoolLevelType.DEGREE_PRE_BOLOGNA, SchoolLevelType.BACHELOR_DEGREE_PRE_BOLOGNA);

    private static final List<SchoolLevelType> MASTER_DEGREE_SCHOOL_LEVELS = Arrays.asList(SchoolLevelType.MASTER_DEGREE,
            SchoolLevelType.MASTER_DEGREE_PRE_BOLOGNA, SchoolLevelType.MASTER_DEGREE_INTEGRATED);

    private static final List<SchoolLevelType> PHD_SCHOOL_LEVELS = Arrays.asList(SchoolLevelType.DOCTORATE_DEGREE,
            SchoolLevelType.DOCTORATE_DEGREE_PRE_BOLOGNA);

    private static final Map<DegreeType, SchoolLevelType> DEGREE_TYPE_TO_SCHOOL_LEVEL_TYPE = Maps.newHashMap();

    static {
        DegreeType p;

        DEGREE_TYPE_TO_SCHOOL_LEVEL_TYPE.put(DegreeType.matching(DegreeType::isBolonhaDegree).get(), SchoolLevelType.DEGREE);
        DEGREE_TYPE_TO_SCHOOL_LEVEL_TYPE.put(DegreeType.matching(DegreeType::isDegree).get(), SchoolLevelType.DEGREE_PRE_BOLOGNA);
        DEGREE_TYPE_TO_SCHOOL_LEVEL_TYPE.put(DegreeType.matching(DegreeType::isBolonhaMasterDegree).get(),
                SchoolLevelType.MASTER_DEGREE);
        DEGREE_TYPE_TO_SCHOOL_LEVEL_TYPE.put(DegreeType.matching(DegreeType::isMasterDegree).get(),
                SchoolLevelType.MASTER_DEGREE_PRE_BOLOGNA);

        //DEGREE_TYPE_TO_SCHOOL_LEVEL_TYPE.put(DegreeType.BOLONHA_PHD, SchoolLevelType.DOCTORATE_DEGREE);

    }

    public static SchoolLevelType getSchoolLevelTypeFor(final DegreeType degreeType) {
        if (DEGREE_TYPE_TO_SCHOOL_LEVEL_TYPE.containsKey(degreeType)) {
            return DEGREE_TYPE_TO_SCHOOL_LEVEL_TYPE.get(degreeType);
        }

        return null;
    }

    public static boolean isCTSP(final SchoolLevelType schoolLevelType) {
        return CTSP_SCHOOL_LEVELS.contains(schoolLevelType);
    }

    public static boolean isCET(final SchoolLevelType schoolLevelType) {
        return CET_SCHOOL_LEVELS.contains(schoolLevelType);
    }

    public static boolean isDegree(final SchoolLevelType schoolLevelType) {
        return DEGREE_SCHOOL_LEVELS.contains(schoolLevelType);
    }

    public static boolean isMasterDegree(final SchoolLevelType schoolLevelType) {
        return MASTER_DEGREE_SCHOOL_LEVELS.contains(schoolLevelType);
    }

    public static boolean isPhd(final SchoolLevelType schoolLevelType) {
        return PHD_SCHOOL_LEVELS.contains(schoolLevelType);
    }

}
