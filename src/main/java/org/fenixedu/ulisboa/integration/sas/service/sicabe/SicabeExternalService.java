package org.fenixedu.ulisboa.integration.sas.service.sicabe;

import javax.xml.ws.BindingProvider;

import com.qubit.solution.fenixedu.bennu.webservices.services.client.BennuWebServiceClient;

import pt.dges.schemas.data.sicabe.v1.ObterCandidaturasSubmetidasRequest;
import pt.dges.schemas.data.sicabe.v1.ObterCandidaturasSubmetidasResponse;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicos;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosObterCandidaturasSubmetidasSicabeBusinessMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosObterCandidaturasSubmetidasSicabeErrorMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosObterCandidaturasSubmetidasSicabeValidationMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicos_Service;
import pt.ist.fenixframework.Atomic;

public class SicabeExternalService extends BennuWebServiceClient<DadosAcademicos> {

    public SicabeExternalService() {
    }

    @Atomic
    public static void init() {
        new SicabeExternalService();
    }

    @Override
    protected BindingProvider getService() {
        return (BindingProvider) new DadosAcademicos_Service().getCustomBindingDadosAcademicos();
    }

    public void dummy() {
        final ObterCandidaturasSubmetidasRequest parameters = new ObterCandidaturasSubmetidasRequest();
        parameters.setAnoLectivo(2017);

        try {
            final ObterCandidaturasSubmetidasResponse obterCandidaturasSubmetidas =
                    getClient().obterCandidaturasSubmetidas(parameters);
            obterCandidaturasSubmetidas.getCandidaturas().getCandidaturaSubmetida().stream().forEach(c -> {

                System.out.println(c.getCodigoCurso() + "\t" + c.getCodigoInstituicaoEnsino() + "\t" + c.getNif() + "\t"
                        + c.getNumeroCandidatura() + "\t" + c.getEstadoCandidatura().getResultadoEstadoCandidatura().name());

            });
        } catch (DadosAcademicosObterCandidaturasSubmetidasSicabeBusinessMessageFaultFaultMessage e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DadosAcademicosObterCandidaturasSubmetidasSicabeErrorMessageFaultFaultMessage e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DadosAcademicosObterCandidaturasSubmetidasSicabeValidationMessageFaultFaultMessage e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
