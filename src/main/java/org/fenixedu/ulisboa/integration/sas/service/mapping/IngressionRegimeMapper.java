package org.fenixedu.ulisboa.integration.sas.service.mapping;

import org.fenixedu.academic.domain.student.Registration;

public class IngressionRegimeMapper {

    public static String map(Registration registration) {
        if (registration.getIngressionType() == null) {
            return "";
        }
        
        

        return "";
    }
    
    /*
    "1 - Concurso nacional de acesso"
    "2 - Concurso local"
    "3 - Concurso institucional"
    "4 - Concurso de acesso a CTeSP"
    "5 -Concurso especial maiores de 23 anos"
    "6 - Concurso especial titulares de DET"
    "7 - Concurso especial titulares de diploma CTeSP"
    "8 - Concurso especial titulares outros cursos superiores"
    "9 - Concurso especial estudantes internacionais"
    "10 - Concurso especial acesso a medicina por licenciados"
    "11 - Regimes especiais "
    "12 - Mudança de curso"
    "13 - Mudança de instituição"
    "14 - Reingresso"
    */
}
