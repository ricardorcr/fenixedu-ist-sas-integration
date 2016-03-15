package org.fenixedu.ulisboa.integration.sas.service.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.io.domain.GenericFile;
import org.fenixedu.ulisboa.integration.sas.domain.ScholarshipReportRequest;
import org.fenixedu.ulisboa.integration.sas.service.transform.FirstYearScholarshipXlsTransformService;
import org.fenixedu.ulisboa.integration.sas.service.transform.OtherYearScholarshipXlsTransformService;
import org.fenixedu.ulisboa.integration.sas.util.SASDomainException;

public class ScholarshipService {

    static public GenericFile createScholarshipService(ScholarshipReportRequest request) {

        GenericFile file = request.getParameterFile();

        POIFSFileSystem poifsFileSystem;
        try {
            poifsFileSystem = new POIFSFileSystem(file.getStream());

            HSSFWorkbook hssfWorkbook;

            if (request.getFirstYearOfCycle()) {
                FirstYearScholarshipXlsTransformService spreadsheetScholarshipFirstYear =
                        new FirstYearScholarshipXlsTransformService(poifsFileSystem);
                spreadsheetScholarshipFirstYear.readExcelFile();
                final FillScholarshipFirstYearService service = new FillScholarshipFirstYearService();
                service.fillAllInfo(spreadsheetScholarshipFirstYear.getStudentLines(), request);
                hssfWorkbook = spreadsheetScholarshipFirstYear.writeExcelFile(poifsFileSystem);
            } else {
                OtherYearScholarshipXlsTransformService spreadsheetScholarshipOtherYearService =
                        new OtherYearScholarshipXlsTransformService(poifsFileSystem);
                spreadsheetScholarshipOtherYearService.readExcelFile();
                final FillScholarshipServiceOtherYearService service = new FillScholarshipServiceOtherYearService();
                service.fillAllInfo(spreadsheetScholarshipOtherYearService.getStudentLines(), request);
                hssfWorkbook = spreadsheetScholarshipOtherYearService.writeExcelFile(poifsFileSystem);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                hssfWorkbook.write(outputStream);
                final byte[] content = outputStream.toByteArray();
                return request.createResultFile(request.getParameterFile().getFilename(), content);
            } catch (final Exception e) {
                throw new DomainException("error.ScholarshipService.spreadsheet.generation.failed", e);
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    throw new DomainException("error.ScholarshipService.spreadsheet.generation.failed", e);
                }
            }

        } catch (SASDomainException e1) {
            throw e1;
        } catch (IOException e1) {
            throw new DomainException(e1.getMessage());
        }

    }

}
