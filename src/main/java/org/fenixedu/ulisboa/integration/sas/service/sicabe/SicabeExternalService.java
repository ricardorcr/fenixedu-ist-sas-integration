package org.fenixedu.ulisboa.integration.sas.service.sicabe;

import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;

import org.apache.commons.lang.math.NumberUtils;
import org.datacontract.schemas._2004._07.sicabe_contracts.AlterarDadosAcademicosContratualizacaoRequest;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.SasSpringConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.treasury.services.integration.erp.ERPExternalServiceImplementation.SOAPLoggingHandler;
import org.fenixedu.ulisboa.integration.sas.domain.CandidacyState;
import org.fenixedu.ulisboa.integration.sas.domain.SasScholarshipCandidacy;
import org.fenixedu.ulisboa.integration.sas.domain.SasScholarshipCandidacyState;
import org.fenixedu.ulisboa.integration.sas.domain.SasScholarshipData;
import org.fenixedu.ulisboa.integration.sas.domain.SasScholarshipDataChangeLog;
import org.fenixedu.ulisboa.integration.sas.dto.AbstractScholarshipStudentBean;
import org.fenixedu.ulisboa.integration.sas.dto.ScholarshipStudentFirstYearBean;
import org.fenixedu.ulisboa.integration.sas.dto.ScholarshipStudentOtherYearBean;
import org.fenixedu.ulisboa.integration.sas.service.process.AbstractFillScholarshipService;
import org.fenixedu.ulisboa.integration.sas.service.process.FillScholarshipException;
import org.fenixedu.ulisboa.integration.sas.service.process.FillScholarshipFirstYearService;
import org.fenixedu.ulisboa.integration.sas.service.process.FillScholarshipServiceOtherYearService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.util.StringUtils;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.qubit.solution.fenixedu.bennu.webservices.services.client.BennuWebServiceClient;
import com.sun.xml.ws.fault.ServerSOAPFaultException;

import pt.dges.schemas.data.sicabe.v1.AlterarCursoInsituicaoRequest;
import pt.dges.schemas.data.sicabe.v1.AlterarDadosAcademicosPrimeiraVezRequest;
import pt.dges.schemas.data.sicabe.v1.AlterarDadosAcademicosPrimeiraVezResponse;
import pt.dges.schemas.data.sicabe.v1.AlterarDadosAcademicosRestantesCasosRequest;
import pt.dges.schemas.data.sicabe.v1.CandidaturaSubmetida;
import pt.dges.schemas.data.sicabe.v1.IdentificadorCandidatura;
import pt.dges.schemas.data.sicabe.v1.ObjectFactory;
import pt.dges.schemas.data.sicabe.v1.ObterCandidaturasSubmetidasRequest;
import pt.dges.schemas.data.sicabe.v1.ObterCandidaturasSubmetidasResponse;
import pt.dges.schemas.data.sicabe.v1.ObterEstadoCandidaturaRequest;
import pt.dges.schemas.data.sicabe.v1.RegistarMatriculaAlunoRequest;
import pt.dges.schemas.data.sicabe.v1.ResultadoEstadoCandidatura;
import pt.dges.schemas.data.sicabe.v1.TipoDocumentoIdentificacao;
import pt.dges.schemas.data.sicabe.v1.TipoRegime;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicos;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosAlterarDadosAcademicosContratualizacaoSicabeBusinessMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosAlterarDadosAcademicosContratualizacaoSicabeErrorMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosAlterarDadosAcademicosContratualizacaoSicabeValidationMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosAlterarDadosAcademicosPrimeiraVezSicabeBusinessMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosAlterarDadosAcademicosPrimeiraVezSicabeErrorMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosAlterarDadosAcademicosPrimeiraVezSicabeValidationMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosAlterarDadosAcademicosRestantesCasosSicabeBusinessMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosAlterarDadosAcademicosRestantesCasosSicabeErrorMessageFaultFaultMessage;
import pt.dges.schemas.services.sicabe.v1.DadosAcademicosAlterarDadosAcademicosRestantesCasosSicabeValidationMessageFaultFaultMessage;
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

    @Atomic
    public void fillAllSasScholarshipCandidacies(ExecutionYear executionYear)
            throws DadosAcademicosObterCandidaturasSubmetidasSicabeBusinessMessageFaultFaultMessage,
            DadosAcademicosObterCandidaturasSubmetidasSicabeErrorMessageFaultFaultMessage,
            DadosAcademicosObterCandidaturasSubmetidasSicabeValidationMessageFaultFaultMessage, ServerSOAPFaultException {
        final ObterCandidaturasSubmetidasRequest parameters = new ObterCandidaturasSubmetidasRequest();
        parameters.setAnoLectivo(executionYear.getBeginCivilYear());

        final ObterCandidaturasSubmetidasResponse obterCandidaturasSubmetidas =
                getClient().obterCandidaturasSubmetidas(parameters);
        obterCandidaturasSubmetidas.getCandidaturas().getCandidaturaSubmetida().stream()
                .filter(c -> String.valueOf(c.getCodigoInstituicaoEnsino())
                        .equalsIgnoreCase(Bennu.getInstance().getSocialServicesConfiguration().getInstitutionCode()))
                .forEach(c -> {

                    updateOrCreateSasScholarshipCandidacy(c, executionYear);

                });
    }

    @Atomic
    public void fillSasScholarshipCandidacies(List<SasScholarshipCandidacy> list2Update, ExecutionYear executionYear)
            throws DadosAcademicosObterCandidaturasSubmetidasSicabeBusinessMessageFaultFaultMessage,
            DadosAcademicosObterCandidaturasSubmetidasSicabeErrorMessageFaultFaultMessage,
            DadosAcademicosObterCandidaturasSubmetidasSicabeValidationMessageFaultFaultMessage, ServerSOAPFaultException {
        final ObterCandidaturasSubmetidasRequest parameters = new ObterCandidaturasSubmetidasRequest();
        parameters.setAnoLectivo(executionYear.getBeginCivilYear());

        Set<Integer> candidacyNumbersSet = Sets.newHashSet();
        list2Update.stream().forEach(s -> candidacyNumbersSet.add(s.getCandidacyNumber()));

        final ObterCandidaturasSubmetidasResponse obterCandidaturasSubmetidas =
                getClient().obterCandidaturasSubmetidas(parameters);

        obterCandidaturasSubmetidas.getCandidaturas().getCandidaturaSubmetida().stream()
                .filter(c -> String.valueOf(c.getCodigoInstituicaoEnsino())
                        .equalsIgnoreCase(Bennu.getInstance().getInstitutionUnit().getCode())
                        && candidacyNumbersSet.contains(c.getNumeroCandidatura()))
                .forEach(c -> {

                    updateOrCreateSasScholarshipCandidacy(c, executionYear);

                });
    }

    private void updateOrCreateSasScholarshipCandidacy(CandidaturaSubmetida input, ExecutionYear executionYear) {

        SasScholarshipCandidacy candidacy = SasScholarshipCandidacy.findAll().stream()
                .filter(c -> c.getExecutionYear() == executionYear && (c.getFiscalNumber().equalsIgnoreCase(input.getNif())
                        || (c.getDocIdType().equalsIgnoreCase(input.getTipoDocumentoIdentificacao().name()))
                                && c.getDocIdNumber().equalsIgnoreCase(input.getNumeroDocumentoIdentificacao())))
                .findFirst().orElse(null);

        if (candidacy == null) {
            
            candidacy = new SasScholarshipCandidacy();
            fillCandidacyInfos(input, executionYear, true, candidacy);
            

        } else if (!equalsDataBetweenCandidacyAndInput(candidacy, input)) {

            fillCandidacyInfos(input, executionYear, false, candidacy);

        }

    }

    private boolean equalsDataBetweenCandidacyAndInput(SasScholarshipCandidacy candidacy, CandidaturaSubmetida input) {
        if (Objects.equal(candidacy.getDegreeCode(), input.getCodigoCurso())
                && Objects.equal(candidacy.getInstitutionCode(), input.getCodigoInstituicaoEnsino())
                && Objects.equal(candidacy.getInstitutionName(), input.getInstituicaoEnsino())
                && Objects.equal(candidacy.getDegreeName(), input.getCurso())
                && Objects.equal(candidacy.getFiscalNumber(), input.getNif()) &&

                Objects.equal((candidacy.getSubmissionDate() != null ? candidacy.getSubmissionDate().toString() : null),
                        (input.getDataSubmissao() != null ? new DateTime(input.getDataSubmissao().toGregorianCalendar().getTime())
                                .toString() : null))
                &&

                Objects.equal(candidacy.getTechnicianEmail(), input.getEmailTecnico().getValue()) &&

                Objects.equal((candidacy.getAssignmentDate() != null ? candidacy.getAssignmentDate().toString() : null),
                        (input.getEstadoCandidatura().getDataAtribuicao() != null ? new DateTime(
                                input.getEstadoCandidatura().getDataAtribuicao().toGregorianCalendar().getTime())
                                        .toString() : null))
                &&

                Objects.equal(candidacy.getDescription(), input.getEstadoCandidatura().getDescricao().getValue())
                && Objects.equal(candidacy.getGratuityAmount(), input.getEstadoCandidatura().getValorBolsa()) &&

                Objects.equal(candidacy.getCandidacyName(), input.getNomeCandidato().getValue())
                && Objects.equal(candidacy.getTechnicianName(), input.getNomeTecnico().getValue())
                && Objects.equal(candidacy.getStudentNumber(), input.getNumeroAluno().getValue())
                && Objects.equal(candidacy.getCandidacyNumber(), input.getNumeroCandidatura())
                && Objects.equal(candidacy.getDocIdNumber(), input.getNumeroDocumentoIdentificacao())
                && Objects.equal(candidacy.getDocIdType(), input.getTipoDocumentoIdentificacao().name()) &&

                Objects.equal(candidacy.getCetQualificationOwner(), input.getTitularidade().getValue().isTitularCET())
                && Objects.equal(candidacy.getCstpQualificationOwner(), input.getTitularidade().getValue().isTitularCSTP())
                && Objects.equal(candidacy.getPhdQualificationOwner(), input.getTitularidade().getValue().isTitularDoutoramento())
                && Objects.equal(candidacy.getDegreeQualificationOwner(),
                        input.getTitularidade().getValue().isTitularLicenciatura())
                && Objects.equal(candidacy.getMasterQualificationOwner(), input.getTitularidade().getValue().isTitularMestrado())

        ) {
            return true;
        }

        return false;

    }

    private void fillCandidacyInfos(CandidaturaSubmetida input, ExecutionYear executionYear, boolean isNewCandidacy,
            SasScholarshipCandidacy candidacy) {
        
        candidacy.setDegreeCode(input.getCodigoCurso());
        candidacy.setInstitutionCode(input.getCodigoInstituicaoEnsino());
        candidacy.setDegreeName(input.getCurso());
        candidacy.setSubmissionDate(
                input.getDataSubmissao() != null ? new DateTime(input.getDataSubmissao().toGregorianCalendar().getTime()) : null);
        candidacy.setTechnicianEmail(input.getEmailTecnico().getValue());

        candidacy.setAssignmentDate(input.getEstadoCandidatura().getDataAtribuicao() != null ? new DateTime(
                input.getEstadoCandidatura().getDataAtribuicao().toGregorianCalendar().getTime()) : null);
        candidacy.setDescription(input.getEstadoCandidatura().getDescricao().getValue());
        candidacy.setCandidacyState(convertCandidacyState(input.getEstadoCandidatura().getResultadoEstadoCandidatura()));
        candidacy.setGratuityAmount(input.getEstadoCandidatura().getValorBolsa());

        candidacy.setInstitutionName(input.getInstituicaoEnsino());
        candidacy.setFiscalNumber(input.getNif());
        candidacy.setCandidacyName(input.getNomeCandidato().getValue());
        candidacy.setTechnicianName(input.getNomeTecnico().getValue());
        candidacy.setStudentNumber(input.getNumeroAluno().getValue());
        candidacy.setCandidacyNumber(input.getNumeroCandidatura());
        candidacy.setDocIdNumber(input.getNumeroDocumentoIdentificacao());
        candidacy.setDocIdType(input.getTipoDocumentoIdentificacao().name());

        // Ownership
        candidacy.setCetQualificationOwner(input.getTitularidade().getValue().isTitularCET());
        candidacy.setCstpQualificationOwner(input.getTitularidade().getValue().isTitularCSTP());
        candidacy.setPhdQualificationOwner(input.getTitularidade().getValue().isTitularDoutoramento());

        candidacy.setDegreeQualificationOwner(input.getTitularidade().getValue().isTitularLicenciatura());
        candidacy.setMasterQualificationOwner(input.getTitularidade().getValue().isTitularMestrado());

        if (executionYear == null) {
            throw new DomainException("error.SicabeExternalService.ExecutionYear.not.found");
        }

        if (isNewCandidacy) {
            executionYear.addSasScholarshipCandidacies(candidacy);
        }

        candidacy.changeState(SasScholarshipCandidacyState.PENDING);

        writeLog(candidacy,
                isNewCandidacy ? BundleUtil.getString(SasSpringConfiguration.BUNDLE,
                        "message.fillCandidacyInfos.new") : BundleUtil.getString(SasSpringConfiguration.BUNDLE,
                                "message.fillCandidacyInfos.update"),
                candidacy.getStateDate());

        fillCandidacyInfos(candidacy);

        candidacy.setImportDate(candidacy.getStateDate());

    }

    private void fillCandidacyInfos(SasScholarshipCandidacy c) {

        final AbstractFillScholarshipService service = new FillScholarshipFirstYearService();

        AbstractScholarshipStudentBean tempBean = new ScholarshipStudentFirstYearBean();
        tempBean.setStudentNumber(c.getStudentNumber() != null && NumberUtils.isNumber(c.getStudentNumber()) ? Integer
                .valueOf(c.getStudentNumber()) : null);
        tempBean.setFiscalCode(c.getFiscalNumber());
        tempBean.setStudentName(c.getCandidacyName());
        tempBean.setDegreeCode(c.getDegreeCode());
        tempBean.setDocumentNumber(c.getDocIdNumber());
        IDDocumentType candidacyDocumentType = convertCandidacyDocumentType(c.getDocIdType());
        tempBean.setDocumentTypeName(candidacyDocumentType != null ? candidacyDocumentType.name() : null);

        try {
            Registration registration = service.getRegistrationByAbstractScholarshipStudentBean(tempBean, c.getExecutionYear());
            if (registration != null) {
                c.setRegistration(registration);
                c.setFirstYear(AbstractFillScholarshipService.isFirstTimeInCycle(registration, c.getExecutionYear()));
            }

        } catch (FillScholarshipException e) {
            c.changeState(SasScholarshipCandidacyState.PROCESSED_ERRORS);
            writeLog(c, service.formatObservations(tempBean), c.getStateDate());

        }
    }

    protected TipoRegime convertRegimeCandidacy(String regime) {

        if (regime.equals(AbstractFillScholarshipService.REGIME_FULL_TIME)) {
            return TipoRegime.TEMPO_INTEGRAL;
        } else if (regime.equals(AbstractFillScholarshipService.REGIME_FULL_TIME_WORKING_STUDENT)) {
            return TipoRegime.TRABALHADOR_ESTUDANTE_TEMPO_INTEGRAL;
        } else if (regime.equals(AbstractFillScholarshipService.REGIME_PARTIAL_TIME)) {
            return TipoRegime.TEMPO_PARCIAL;
        } else if (regime.equals(AbstractFillScholarshipService.REGIME_PROFESSIONAL_INTERNSHIP)) {
            return TipoRegime.ESTAGIO_PROFISSIONAL;
        } else if (regime.equals(AbstractFillScholarshipService.REGIME_PARTIAL_TIME_WORKING_STUDENT)) {
            return TipoRegime.TRABALHADOR_ESTUDANTE_TEMPO_PARCIAL;
        }

        return null;
    }

    protected IDDocumentType convertCandidacyDocumentType(String idDocumentType) {
        if (idDocumentType.equalsIgnoreCase(TipoDocumentoIdentificacao.BI.name())) {
            return IDDocumentType.IDENTITY_CARD;
        } else if (idDocumentType.equalsIgnoreCase(TipoDocumentoIdentificacao.PASSAPORTE.name())) {
            return IDDocumentType.PASSPORT;
        } else if (idDocumentType.equalsIgnoreCase(TipoDocumentoIdentificacao.AUTORIZACAO_RESIDENCIA.name())) {
            return IDDocumentType.RESIDENCE_AUTHORIZATION;
        } else if (idDocumentType.equalsIgnoreCase(TipoDocumentoIdentificacao.BI_NAO_NACIONAL.name())) {
            return IDDocumentType.FOREIGNER_IDENTITY_CARD;
        } else if (idDocumentType.equalsIgnoreCase(TipoDocumentoIdentificacao.OUTROS.name())) {
            return IDDocumentType.OTHER;
        }

        return null;
    }

    protected CandidacyState convertCandidacyState(ResultadoEstadoCandidatura candidacyState) {
        if (candidacyState == ResultadoEstadoCandidatura.INDEFERIDA) {
            return CandidacyState.DISMISSED;
        } else if (candidacyState == ResultadoEstadoCandidatura.DEFERIDA) {
            return CandidacyState.DEFERRED;
        } else if (candidacyState == ResultadoEstadoCandidatura.NAO_DETERMINADO) {
            return CandidacyState.UNDEFINED;
        }

        return null;
    }

    @Atomic
    public void processAllSasScholarshipCandidacies(ExecutionYear executionYear) {
        SasScholarshipCandidacy.findAll().stream().filter(c -> c.getExecutionYear() == executionYear)
                .forEach(c -> fillCandidacyData(c));
    }

    @Atomic
    public void processSasScholarshipCandidacies(List<SasScholarshipCandidacy> list2Process) {
        list2Process.stream().forEach(c -> fillCandidacyData(c));
    }

    private void fillCandidacyData(SasScholarshipCandidacy c) {
        final AbstractFillScholarshipService service;

        final AbstractScholarshipStudentBean bean;

        if (c.getFirstYear() == null) {
            c.changeState(SasScholarshipCandidacyState.PROCESSED_ERRORS);
            return;
        }

        if (c.getFirstYear()) {
            bean = new ScholarshipStudentFirstYearBean();
            service = new FillScholarshipFirstYearService();
        } else {
            bean = new ScholarshipStudentOtherYearBean();
            service = new FillScholarshipServiceOtherYearService();

        }

        if (c.getRegistration() == null) {
            return;
        }

        // commons bean fields
        bean.setStudentNumber(c.getStudentNumber() != null && NumberUtils.isNumber(c.getStudentNumber()) ? Integer
                .valueOf(c.getStudentNumber()) : null);
        bean.setGratuityAmount(c.getGratuityAmount());
        bean.setCetQualificationOwner(c.getCetQualificationOwner());
        bean.setCtspQualificationOwner(c.getCetQualificationOwner());
        bean.setDegreeQualificationOwner(c.getDegreeQualificationOwner());
        bean.setMasterQualificationOwner(c.getMasterQualificationOwner());
        bean.setPhdQualificationOwner(c.getPhdQualificationOwner());
        bean.setFiscalCode(c.getFiscalNumber());
        bean.setInstitutionCode(c.getInstitutionCode() != null ? String.valueOf(c.getInstitutionCode()) : null);
        bean.setInstitutionName(c.getInstitutionName());
        bean.setCandidacyNumber(c.getCandidacyNumber() != null ? String.valueOf(c.getCandidacyNumber()) : null);
        bean.setStudentName(c.getCandidacyName());
        bean.setDegreeCode(c.getDegreeCode());
        bean.setDegreeName(c.getDegreeName());
        bean.setDocumentNumber(c.getDocIdNumber());
        IDDocumentType candidacyDocumentType = convertCandidacyDocumentType(c.getDocIdType());
        bean.setDocumentTypeName(candidacyDocumentType != null ? candidacyDocumentType.name() : null);

        service.fillAllInfo(bean, c.getRegistration(), c.getExecutionYear(), c.getFirstYear());

        if (c.getSasScholarshipData() == null || dataHasChanged(c.getSasScholarshipData(), bean)) {
            updateSasSchoolarshipCandidacyData(bean, c);
        }

    }

    private boolean dataHasChanged(SasScholarshipData sasScholarshipData, AbstractScholarshipStudentBean bean) {

        boolean value = !equal(bean, bean.getCetQualificationOwner(), sasScholarshipData.getCetQualificationOwner(),
                "cetQualificationOwner")

                || !equal(bean, bean.getCtspQualificationOwner(), sasScholarshipData.getCtspQualificationOwner(),
                        "ctspQualificationOwner")

                || !equal(bean, bean.getPhdQualificationOwner(), sasScholarshipData.getPhdQualificationOwner(),
                        "phdQualificationOwner")

                || !equal(bean, bean.getDegreeQualificationOwner(), sasScholarshipData.getDegreeQualificationOwner(),
                        "degreeQualificationOwner")

                || !equal(bean, bean.getMasterQualificationOwner(), sasScholarshipData.getMasterQualificationOwner(),
                        "masterQualificationOwner")

                || !equal(bean, bean.getFirstMonthExecutionYear(), sasScholarshipData.getFirstMonthExecutionYear(),
                        "firstMonthExecutionYear")

                || !equal(bean, bean.getGratuityAmount(), sasScholarshipData.getGratuityAmount(), "gratuityAmount")

                || !equal(bean, bean.getCycleNumberOfEnrolmentsYears(), sasScholarshipData.getNumberOfEnrolmentsYears(),
                        "numberOfEnrolmentsYears")

                || !equal(bean, bean.getNumberOfEnrolledECTS(), sasScholarshipData.getNumberOfEnrolledECTS(),
                        "numberOfEnrolledECTS")

                || !equal(bean, bean.getNumberOfMonthsExecutionYear(), sasScholarshipData.getNumberOfMonthsExecutionYear(),
                        "numberOfMonthsExecutionYear")

                || !equal(bean, bean.getRegime(), sasScholarshipData.getRegime(), "regime")

                || !equal(bean, bean.getEnroled(), sasScholarshipData.getEnroled(), "enroled")

                || !equal(bean, bean.getNumberOfDegreeCurricularYears(), sasScholarshipData.getNumberOfDegreeCurricularYears(),
                        "numberOfDegreeCurricularYears")

                || !equal(bean, bean.getEnrolmentDate(), sasScholarshipData.getEnrolmentDate(), "enrolmentDate");

        if (bean instanceof ScholarshipStudentOtherYearBean) {

            final ScholarshipStudentOtherYearBean otherYearBean = (ScholarshipStudentOtherYearBean) bean;

            value &= !equal(otherYearBean, otherYearBean.getNumberOfApprovedEcts(), sasScholarshipData.getNumberOfApprovedEcts(),
                    "numberOfApprovedEcts")

                    || !equal(otherYearBean, otherYearBean.getNumberOfApprovedEctsLastYear(),
                            sasScholarshipData.getNumberOfApprovedEctsLastYear(), "numberOfApprovedEctsLastYear")
                    
                    || !equal(otherYearBean, otherYearBean.getNumberOfEnrolledEctsLastYear(),
                            sasScholarshipData.getNumberOfEnrolledEctsLastYear(), "numberOfEnrolledEctsLastYear")

                    || !equal(otherYearBean, otherYearBean.getNumberOfDegreeChanges(),
                            sasScholarshipData.getNumberOfDegreeChanges(), "numberOfDegreeChanges")

                    || !equal(otherYearBean, otherYearBean.getHasMadeDegreeChangeOnCurrentYear(),
                            sasScholarshipData.getHasMadeDegreeChangeOnCurrentYear(), "hasMadeDegreeChangeOnCurrentYear")

                    || !equal(otherYearBean, otherYearBean.getLastEnrolmentYear(), sasScholarshipData.getLastEnrolmentYear(),
                            "lastEnrolmentYear")

                    || !equal(otherYearBean, otherYearBean.getLastAcademicActDateLastYear(),
                            sasScholarshipData.getLastAcademicActDateLastYear(), "lastAcademicActDateLastYear")

                    || !equal(otherYearBean, otherYearBean.getCurricularYear(), sasScholarshipData.getCurricularYear(),
                            "curricularYear")

                    || !equal(otherYearBean, otherYearBean.getCycleNumberOfEnrolmentsYearsInIntegralRegime(),
                            sasScholarshipData.getCycleNumberOfEnrolmentsYearsInIntegralRegime(), "cycleNumberOfEnrolmentsYearsInIntegralRegime")

                    || !equal(otherYearBean, bean.getCycleIngressionYear(), sasScholarshipData.getCycleIngressionYear(),
                            "cycleIngressionYear");
        }

        return value;

    }

    private boolean equal(AbstractScholarshipStudentBean bean, Object left, Object right, String fieldName) {
        if (Objects.equal(left, right)) {
            return true;
        }

        bean.setObservations((bean.getObservations() != null ? bean.getObservations() + "\n" : "")
                + BundleUtil.getString(SasSpringConfiguration.BUNDLE, "message.warning.data.has.changed",
                        BundleUtil.getString(SasSpringConfiguration.BUNDLE, "label.SasScholarshipData." + fieldName)));
        return false;
    }

    private void updateSasSchoolarshipCandidacyData(AbstractScholarshipStudentBean bean, SasScholarshipCandidacy candidacy) {

        boolean isNewData = false;

        SasScholarshipData data;
        if (candidacy.getSasScholarshipData() == null) {
            data = new SasScholarshipData();
            candidacy.setSasScholarshipData(data);

            isNewData = true;
        } else {
            data = candidacy.getSasScholarshipData();
        }

        data.setGratuityAmount(bean.getGratuityAmount());
        data.setNumberOfMonthsExecutionYear(bean.getNumberOfMonthsExecutionYear());
        data.setFirstMonthExecutionYear(bean.getFirstMonthExecutionYear());
        data.setRegime(bean.getRegime());
        data.setEnroled(bean.getEnroled());
        data.setEnrolmentDate(bean.getEnrolmentDate());
        data.setNumberOfEnrolledECTS(bean.getNumberOfEnrolledECTS());

        data.setCetQualificationOwner(bean.getCetQualificationOwner());
        data.setCtspQualificationOwner(bean.getCtspQualificationOwner());
        data.setDegreeQualificationOwner(bean.getDegreeQualificationOwner());
        data.setMasterQualificationOwner(bean.getMasterQualificationOwner());
        data.setPhdQualificationOwner(bean.getPhdQualificationOwner());

        data.setCycleIngressionYear(bean.getCycleIngressionYear());
        data.setNumberOfEnrolmentsYears(bean.getCycleNumberOfEnrolmentsYears());
        data.setNumberOfDegreeCurricularYears(bean.getNumberOfDegreeCurricularYears());
        data.setObservations(bean.getObservations());

        if (bean instanceof ScholarshipStudentOtherYearBean) {

            final ScholarshipStudentOtherYearBean otherYearBean = (ScholarshipStudentOtherYearBean) bean;

            data.setNumberOfDegreeChanges(otherYearBean.getNumberOfDegreeChanges());
            data.setHasMadeDegreeChangeOnCurrentYear(otherYearBean.getHasMadeDegreeChangeOnCurrentYear());

            data.setNumberOfEnrolledEctsLastYear(otherYearBean.getNumberOfEnrolledEctsLastYear());
            data.setNumberOfApprovedEctsLastYear(otherYearBean.getNumberOfApprovedEctsLastYear());
            data.setLastEnrolmentYear(String.valueOf(otherYearBean.getLastEnrolmentYear()));
            data.setCurricularYear(otherYearBean.getCurricularYear());
            data.setLastAcademicActDateLastYear(otherYearBean.getLastAcademicActDateLastYear());
            data.setCycleNumberOfEnrolmentsYearsInIntegralRegime(otherYearBean.getCycleNumberOfEnrolmentsYearsInIntegralRegime());
            data.setNumberOfApprovedEcts(otherYearBean.getNumberOfApprovedEcts());

        }

        if (data.getObservations().contains(AbstractFillScholarshipService.ERROR_OBSERVATION)) {
            candidacy.changeState(SasScholarshipCandidacyState.PROCESSED_ERRORS);
        } else if (data.getObservations().contains(AbstractFillScholarshipService.WARNING_OBSERVATION)) {
            candidacy.changeState(SasScholarshipCandidacyState.PROCESSED_WARNINGS);
        } else {
            candidacy.changeState(SasScholarshipCandidacyState.PROCESSED);
        }

        writeLog(candidacy,
                !StringUtils.isEmpty(data.getObservations()) ? data
                        .getObservations() : isNewData ? BundleUtil.getString(SasSpringConfiguration.BUNDLE,
                                "message.updateSasSchoolarshipCandidacyData.new") : BundleUtil.getString(
                                        SasSpringConfiguration.BUNDLE, "message.updateSasSchoolarshipCandidacyData.update")
                                        + (!StringUtils.isEmpty(data.getObservations()) ? data.getObservations() : ""),
                candidacy.getStateDate());
    }

    @Atomic
    public void sendAllSasScholarshipCandidacies2Sicabe(ExecutionYear executionYear) {
        SasScholarshipCandidacy.findAll().stream().filter(c -> c.getExecutionYear() == executionYear)
                .forEach(c -> sendCandidacy2Sicabe(c));
    }

    @Atomic
    public void sendSasScholarshipsCandidacies2Sicabe(List<SasScholarshipCandidacy> list2Process) {
        list2Process.stream().forEach(c -> sendCandidacy2Sicabe(c));
    }

    private void sendCandidacy2Sicabe(SasScholarshipCandidacy c) {

        if (!stateAllow2SendCandicacy(c.getState())) {
            writeLog(c, BundleUtil.getString(SasSpringConfiguration.BUNDLE, "message.error.sendCandidacy2Sicabe",
                    c.getState().getLocalizedName()), new DateTime());
            throw new RuntimeException(c.getState().name());
        }

        try {
            if (c.getFirstYear()) {
                sendFirstTimeAcademicData(c);
            } else {
                sendOtherAcademicData(c);
                sendContratualizationAcademicData(c);
            }
            final DateTime exportDate = new DateTime();
            c.setExportDate(exportDate);
            writeLog(c, BundleUtil.getString(SasSpringConfiguration.BUNDLE, "message.success.sendCandidacy2Sicabe"), exportDate);
        } catch (DadosAcademicosAlterarDadosAcademicosPrimeiraVezSicabeBusinessMessageFaultFaultMessage
                | DadosAcademicosAlterarDadosAcademicosPrimeiraVezSicabeErrorMessageFaultFaultMessage
                | DadosAcademicosAlterarDadosAcademicosPrimeiraVezSicabeValidationMessageFaultFaultMessage
                | DadosAcademicosAlterarDadosAcademicosRestantesCasosSicabeBusinessMessageFaultFaultMessage
                | DadosAcademicosAlterarDadosAcademicosRestantesCasosSicabeErrorMessageFaultFaultMessage
                | DadosAcademicosAlterarDadosAcademicosRestantesCasosSicabeValidationMessageFaultFaultMessage
                | DadosAcademicosAlterarDadosAcademicosContratualizacaoSicabeBusinessMessageFaultFaultMessage
                | DadosAcademicosAlterarDadosAcademicosContratualizacaoSicabeErrorMessageFaultFaultMessage
                | DadosAcademicosAlterarDadosAcademicosContratualizacaoSicabeValidationMessageFaultFaultMessage e) {
            throw new RuntimeException(e);
        }
    }

    private boolean stateAllow2SendCandicacy(SasScholarshipCandidacyState state) {
        return state == SasScholarshipCandidacyState.PROCESSED || state == SasScholarshipCandidacyState.PROCESSED_WARNINGS;
    }

    @Atomic
    public boolean removeAllSasScholarshipsCandidacies(ExecutionYear executionYear) {
        final List<SasScholarshipCandidacy> candidacies2Remove = SasScholarshipCandidacy.findAll().stream()
                .filter(c -> c.getExecutionYear() == executionYear).collect(Collectors.toList());

        boolean warningMessage = false;
        for (SasScholarshipCandidacy current : candidacies2Remove) {
            try {
                removeSasScholarshipsCandidacy(current);
            } catch (FillScholarshipException e) {
                warningMessage = true;
            }
        }

        return warningMessage;
    }

    @Atomic
    public void removeSasScholarshipsCandidacy(SasScholarshipCandidacy c) {
        if (c.getExportDate() == null) {
            c.delete();
        } else {
            throw new FillScholarshipException("label.error.delete.already.sent");
        }
    }

    private void sendFirstTimeAcademicData(SasScholarshipCandidacy candidacy)
            throws DadosAcademicosAlterarDadosAcademicosPrimeiraVezSicabeBusinessMessageFaultFaultMessage,
            DadosAcademicosAlterarDadosAcademicosPrimeiraVezSicabeErrorMessageFaultFaultMessage,
            DadosAcademicosAlterarDadosAcademicosPrimeiraVezSicabeValidationMessageFaultFaultMessage {

        final SasScholarshipData data = candidacy.getSasScholarshipData();

        AlterarDadosAcademicosPrimeiraVezRequest request = new AlterarDadosAcademicosPrimeiraVezRequest();

        request.setAnoInscricaoCurso(data.getCycleIngressionYear());
        request.setCodigoCurso(data.getSasScholarshipCandidacy().getDegreeCode());
        request.setCodigoInstituicaoEnsino(candidacy.getInstitutionCode());

        request.setDataInscricaoAnoLectivo(createXMLGregorianCalendar(data.getEnrolmentDate()));

        IdentificadorCandidatura idCandidatura =
                createIdentificadorCandidaturaData(candidacy.getExecutionYear().getBeginCivilYear(), candidacy.getDocIdNumber(),
                        candidacy.getDocIdType(), candidacy.getFiscalNumber());
        request.setIdentificadorCandidatura(idCandidatura);

        request.setIInscritoAnoLectivoActual(data.getEnroled());
        request.setMesPrimeiroPagamento(data.getFirstMonthExecutionYear());

        request.setNumeroAluno(candidacy.getStudentNumber());
        request.setNumeroAnosCurso(data.getNumberOfDegreeCurricularYears());
        request.setNumeroECTSActualInscrito(data.getNumberOfEnrolledECTS());
        request.setNumeroMatriculas(1);
        request.setNumeroMesesPropina(data.getNumberOfMonthsExecutionYear());
        request.setObservacoes(null);

        request.setRegime(convertRegimeCandidacy(data.getRegime()));
        request.setTitularCET(data.getCetQualificationOwner());
        request.setTitularCSTP(data.getCtspQualificationOwner());
        request.setTitularDoutoramento(data.getPhdQualificationOwner());
        request.setTitularLicenciatura(data.getDegreeQualificationOwner());
        request.setTitularMestrado(data.getMasterQualificationOwner());

        request.setValorPropina(data.getGratuityAmount());

        candidacy.changeState(SasScholarshipCandidacyState.SENT);

        DadosAcademicos client = getClient();
        
        final SOAPLoggingHandler loggingHandler = SOAPLoggingHandler.createLoggingHandler((BindingProvider) client);
                
        client.alterarDadosAcademicosPrimeiraVez(request);
        
        //System.out.println("SENT== FIRST-TIME \n" + loggingHandler.getOutboundMessage());
        //System.out.println("RECEIVED==  FIRST-TIME \n" + loggingHandler.getInboundMessage());
        

    }

    protected XMLGregorianCalendar createXMLGregorianCalendar(LocalDate localDate) {
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(localDate.toDate().getTime());

        DatatypeFactory datatypeFactory;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
            XMLGregorianCalendar newXMLGregorianCalendar = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
            return newXMLGregorianCalendar;
        } catch (DatatypeConfigurationException e) {
            return null;
        }

    }

    protected IdentificadorCandidatura createIdentificadorCandidaturaData(Integer beginExecutionYear, String docIdNumber,
            String docIdType, String fiscalNumber) {
        ObjectFactory factory = new ObjectFactory();

        IdentificadorCandidatura idCandidatura = new IdentificadorCandidatura();
        idCandidatura.setAnoLectivo(beginExecutionYear);
        idCandidatura.setDocumentoIdentificacao(factory.createIdentificadorCandidaturaDocumentoIdentificacao(docIdNumber));
        idCandidatura.setTipoDocumentoIdentificacao(TipoDocumentoIdentificacao.fromValue(docIdType));
        idCandidatura.setNif(factory.createIdentificadorCandidaturaNif(fiscalNumber));
        return idCandidatura;
    }

    private void sendContratualizationAcademicData(SasScholarshipCandidacy candidacy)
            throws DadosAcademicosAlterarDadosAcademicosContratualizacaoSicabeBusinessMessageFaultFaultMessage,
            DadosAcademicosAlterarDadosAcademicosContratualizacaoSicabeErrorMessageFaultFaultMessage,
            DadosAcademicosAlterarDadosAcademicosContratualizacaoSicabeValidationMessageFaultFaultMessage {

        final SasScholarshipData data = candidacy.getSasScholarshipData();

        org.datacontract.schemas._2004._07.sicabe_contracts.ObjectFactory factory =
                new org.datacontract.schemas._2004._07.sicabe_contracts.ObjectFactory();

        AlterarDadosAcademicosContratualizacaoRequest request = new AlterarDadosAcademicosContratualizacaoRequest();

        request.setCodigoCurso(factory.createAlterarDadosAcademicosContratualizacaoRequestCodigoCurso(
                data.getSasScholarshipCandidacy().getDegreeCode()));
        request.setCodigoInstituicaoEnsino(candidacy.getInstitutionCode());
        request.setDataInscricaoAnoLectivo(createXMLGregorianCalendar(data.getEnrolmentDate()));

        IdentificadorCandidatura idCandidatura =
                createIdentificadorCandidaturaData(candidacy.getExecutionYear().getBeginCivilYear(), candidacy.getDocIdNumber(),
                        candidacy.getDocIdType(), candidacy.getFiscalNumber());
        request.setIdentificadorCandidatura(idCandidatura);

        request.setIInscritoAnoLectivoActual(data.getEnroled());
        request.setMesPrimeiroPagamento(Integer.valueOf(data.getFirstMonthExecutionYear()));
        request.setNumeroAluno(factory.createAlterarDadosAcademicosContratualizacaoRequestNumeroAluno(
                candidacy.getRegistration().getNumber().toString()));
        request.setNumeroECTSActualmenteInscrito(candidacy.getSasScholarshipData().getNumberOfEnrolledECTS());
        request.setNumeroMesesPropina(data.getNumberOfMonthsExecutionYear());
        request.setNumeroOcorrenciasMudancaCurso(data.getNumberOfDegreeChanges());
        request.setPresenteAnoMudouDeCurso(data.getHasMadeDegreeChangeOnCurrentYear());
        request.setRegime(convertRegimeCandidacy(data.getRegime()));
        request.setValorPropina(data.getGratuityAmount());

        candidacy.changeState(SasScholarshipCandidacyState.SENT);

        getClient().alterarDadosAcademicosContratualizacao(request);
    }
       

    private void sendOtherAcademicData(SasScholarshipCandidacy candidacy)
            throws DadosAcademicosAlterarDadosAcademicosRestantesCasosSicabeBusinessMessageFaultFaultMessage,
            DadosAcademicosAlterarDadosAcademicosRestantesCasosSicabeErrorMessageFaultFaultMessage,
            DadosAcademicosAlterarDadosAcademicosRestantesCasosSicabeValidationMessageFaultFaultMessage {

        final SasScholarshipData data = candidacy.getSasScholarshipData();

        AlterarDadosAcademicosRestantesCasosRequest request = new AlterarDadosAcademicosRestantesCasosRequest();

        request.setAnoInscricaoCurso(data.getCycleIngressionYear());
        request.setAnoLectivoActual(data.getCurricularYear());
        request.setCodigoCurso(candidacy.getDegreeCode());
        request.setCodigoInstituicaoEnsino(candidacy.getInstitutionCode());
        request.setDataConclusaoAtosAcademicosUltimoAnoLectivoInscrito(
                createXMLGregorianCalendar(data.getLastAcademicActDateLastYear()));
        request.setDataInscricaoAnoLectivo(createXMLGregorianCalendar(data.getEnrolmentDate()));

        IdentificadorCandidatura idCandidatura =
                createIdentificadorCandidaturaData(candidacy.getExecutionYear().getBeginCivilYear(), candidacy.getDocIdNumber(),
                        candidacy.getDocIdType(), candidacy.getFiscalNumber());
        request.setIdentificadorCandidatura(idCandidatura);

        request.setIdentificadorCandidatura(idCandidatura);
        request.setIInscritoAnoLectivoActual(data.getEnroled());
        request.setMesPrimeiroPagamento(data.getFirstMonthExecutionYear());

        request.setNumeroAluno(candidacy.getStudentNumber());
        request.setNumeroAnosCurso(data.getNumberOfDegreeCurricularYears());
        request.setNumeroECTSActualmenteInscrito(data.getNumberOfEnrolledECTS());
        request.setNumeroECTSObtidosUltimoAnoInscrito(data.getNumberOfApprovedEctsLastYear());
        request.setNumeroECTSUltimoAnoInscrito(data.getNumberOfEnrolledEctsLastYear());
        request.setNumeroInscricoesCicloEstudosTempoIntegral(data.getCycleNumberOfEnrolmentsYearsInIntegralRegime());
        request.setNumeroMatriculas(data.getNumberOfEnrolmentsYears());

        request.setNumeroMesesPropina(data.getNumberOfMonthsExecutionYear());
        request.setNumeroOcorrenciasMudancaCurso(data.getNumberOfDegreeChanges());
        request.setObservacoes(null);

        request.setPresenteAnoMudouDeCurso(data.getHasMadeDegreeChangeOnCurrentYear());
        request.setRegime(convertRegimeCandidacy(data.getRegime()));
        request.setTitularCET(data.getCetQualificationOwner());
        request.setTitularCSTP(data.getCtspQualificationOwner());
        request.setTitularDoutoramento(data.getPhdQualificationOwner());
        request.setTitularLicenciatura(data.getDegreeQualificationOwner());
        request.setTitularMestrado(data.getMasterQualificationOwner());
        request.setTotalECTScursoAtingirGrau(data.getNumberOfApprovedEcts());
        request.setUltimoAnoInscrito(Integer.valueOf(data.getLastEnrolmentYear()));
        request.setValorPropina(data.getGratuityAmount());

        candidacy.changeState(SasScholarshipCandidacyState.SENT);
        
        DadosAcademicos client = getClient();
        
        final SOAPLoggingHandler loggingHandler = SOAPLoggingHandler.createLoggingHandler((BindingProvider) client);
                
        client.alterarDadosAcademicosRestantesCasos(request);
        
        //System.out.println("SENT OTHER== \n" + loggingHandler.getOutboundMessage());
        //System.out.println("RECEIVED OTHER == \n" + loggingHandler.getInboundMessage());

    }

    /** TODO: to be done **/
    private void sendRegistrationStudent(SasScholarshipCandidacy candidacy) {

        RegistarMatriculaAlunoRequest request = new RegistarMatriculaAlunoRequest();

        request.setCodigoCurso(candidacy.getRegistration().getDegree().getMinistryCode());
        request.setCodigoInstituicaoEnsino(-1); // TODO
        request.setDataMatricula(null); // TODO

        IdentificadorCandidatura idCandidatura = createIdentificadorCandidaturaData(null /*TODO*/, candidacy.getDocIdNumber(),
                candidacy.getDocIdType(), candidacy.getFiscalNumber());
        request.setIdentificadorCandidatura(idCandidatura);

    }

    private void updateCandidacyState(SasScholarshipCandidacy candidacy) {
        //TODO

        ObterEstadoCandidaturaRequest request = new ObterEstadoCandidaturaRequest();

        IdentificadorCandidatura idCandidatura = createIdentificadorCandidaturaData(null /*TODO*/, candidacy.getDocIdNumber(),
                candidacy.getDocIdType(), candidacy.getFiscalNumber());
        request.setIdentificadorCandidatura(idCandidatura);

    }

    private void getSubmittedCandidacies(SasScholarshipCandidacy candidacy) {
        //TODO

        ObterEstadoCandidaturaRequest request = new ObterEstadoCandidaturaRequest();

        IdentificadorCandidatura idCandidatura = createIdentificadorCandidaturaData(null /*TODO*/, candidacy.getDocIdNumber(),
                candidacy.getDocIdType(), candidacy.getFiscalNumber());
        request.setIdentificadorCandidatura(idCandidatura);

    }

    private void changeInstitutionDegree(SasScholarshipCandidacy candidacy) {
        //TODO
        ObjectFactory factory = new ObjectFactory();
        AlterarCursoInsituicaoRequest request = new AlterarCursoInsituicaoRequest();

        IdentificadorCandidatura idCandidatura = createIdentificadorCandidaturaData(null /*TODO*/, candidacy.getDocIdNumber(),
                candidacy.getDocIdType(), candidacy.getFiscalNumber());
        request.setIdentificadorCandidatura(idCandidatura);

        request.setIdentificadorCandidatura(idCandidatura);
        request.setCodigoCurso(candidacy.getRegistration().getDegree().getMinistryCode());
        request.setCodigoInstituicaoEnsino(-1);
        request.setDataMudanca(null);

    }

    private void writeLog(SasScholarshipCandidacy candidacy, String message, DateTime date) {
        SasScholarshipDataChangeLog log = new SasScholarshipDataChangeLog();
        log.setDate(date);
        log.setDescription(message);
        log.setStudentName(candidacy.getCandidacyName());
        log.setStudentNumber(candidacy.getStudentNumber());
        candidacy.addSasScholarshipDataChangeLogs(log);
    }
}
