package org.fenixedu.ulisboa.integration.sas.domain;

import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.bennu.core.domain.Bennu;

public class SasIngressionRegimeMapping extends SasIngressionRegimeMapping_Base {

    public SasIngressionRegimeMapping() {
        super();
    }

    public static SasIngressionRegimeMapping create(Set<IngressionType> ingressionTypes, String regimeCode, String
            regimeCodeWithDescription) {
        checkPreConditions(regimeCode, regimeCodeWithDescription);
        
        SasIngressionRegimeMapping ingressionRegimeMapping = new SasIngressionRegimeMapping();
        ingressionRegimeMapping.getIngressionTypeSet().clear();
        ingressionRegimeMapping.getIngressionTypeSet().addAll(ingressionTypes);

        ingressionRegimeMapping.setRegimeCode(regimeCode);
        ingressionRegimeMapping.setRegimeCodeWithDescription(regimeCodeWithDescription);
        ingressionRegimeMapping.setBennu(Bennu.getInstance());
        return ingressionRegimeMapping;
    }

    public void edit(Set<IngressionType> ingressionTypes, String regimeCode, String regimeCodeWithDescription) {
        checkPreConditions(regimeCode, regimeCodeWithDescription);
        getIngressionTypeSet().clear();
        getIngressionTypeSet().addAll(ingressionTypes);
        setRegimeCode(regimeCode);
        setRegimeCodeWithDescription(regimeCodeWithDescription);
    }

    // Check if the arguments are not null, and if the degree type has no associated schoolLevel.
    // First argument can be null (for constructor case)
    public static void checkPreConditions(String regimeCode, String regimeCodeWithDescription) {

        if (regimeCode == null) {
            throw new RuntimeException("Regime Code");
        }
        if (regimeCodeWithDescription == null) {
            throw new RuntimeException("Regime Code With Description");
        }
    }

    public void delete() {
        getIngressionTypeSet().clear();
        setBennu(null);
        super.deleteDomainObject();
    }

    public static Stream<SasIngressionRegimeMapping> findAll() {
        return Bennu.getInstance().getSasIngressionRegimeMappingsSet().stream();
    }

    /*public static List<String> readAllRegimes() {
        return Arrays.asList("1 - Concurso nacional de acesso", "2 - Concurso local", "3 - Concurso institucional",
                "4 - Concurso de acesso a CTeSP", "5 - Concurso especial maiores de 23 anos",
                "6 - Concurso especial titulares de DET", "7 - Concurso especial titulares de diploma CTeSP",
                "8 - Concurso especial titulares outros cursos superiores", "9 - Concurso especial estudantes internacionais",
                "10 - Concurso especial acesso a medicina por licenciados", "11 - Regimes especiais", "12 - Mudança de curso",
                "13 - Mudança de instituição", "14 - Reingresso");
    }*/
}