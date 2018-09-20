package org.fenixedu.ulisboa.integration.sas.domain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.bennu.core.domain.Bennu;

public class SasIngressionRegimeMapping extends SasIngressionRegimeMapping_Base {

    public SasIngressionRegimeMapping() {
        super();
    }

    public static SasIngressionRegimeMapping create(IngressionType ingression, String regime) {
        checkPreConditions(null, ingression, regime);
        SasIngressionRegimeMapping ingressionRegimeMapping = new SasIngressionRegimeMapping();
        ingressionRegimeMapping.setIngressionType(ingression);
        ingressionRegimeMapping.setRegime(regime);
        ingressionRegimeMapping.setBennu(Bennu.getInstance());
        return ingressionRegimeMapping;
    }

    public void edit(IngressionType ingression, String regime) {
        checkPreConditions(this, ingression, regime);
        setIngressionType(ingression);
        setRegime(regime);
    }

    // Check if the arguments are not null, and if the degree type has no associated schoolLevel.
    // First argument can be null (for constructor case)
    public static void checkPreConditions(SasIngressionRegimeMapping ingressionRegimeMapping, IngressionType ingression,
            String regime) {

        //TODO localize messages
        if (ingression == null) {
            throw new RuntimeException("Ingression Type");
        }
        if (regime == null) {
            throw new RuntimeException("Regime");
        }

        SasIngressionRegimeMapping regimeCurrentIngressionRegimeMapping = ingression.getSasIngressionRegimeMapping();
        if (regimeCurrentIngressionRegimeMapping != null && regimeCurrentIngressionRegimeMapping != ingressionRegimeMapping) {
            throw new RuntimeException("Ingression type already has associated Regime");
        }

    }

    public void delete() {
        setIngressionType(null);
        setBennu(null);
        super.deleteDomainObject();
    }

    public static Stream<SasIngressionRegimeMapping> findAll() {
        return Bennu.getInstance().getSasIngressionRegimeMappingsSet().stream();
    }

    public static List<String> readAllRegimes() {

        return Arrays.asList("1 - Concurso nacional de acesso", "2 - Concurso local", "3 - Concurso institucional",
                "4 - Concurso de acesso a CTeSP", "5 - Concurso especial maiores de 23 anos",
                "6 - Concurso especial titulares de DET", "7 - Concurso especial titulares de diploma CTeSP",
                "8 - Concurso especial titulares outros cursos superiores", "9 - Concurso especial estudantes internacionais",
                "10 - Concurso especial acesso a medicina por licenciados", "11 - Regimes especiais", "12 - Mudança de curso",
                "13 - Mudança de instituição", "14 - Reingresso");

    }
}
