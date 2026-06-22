package com.upiexpense.intel.parser;

import com.upiexpense.intel.common.error.BadRequestException;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractor {

    public String extract(byte[] fileBytes, String password) {
        try (PDDocument document = Loader.loadPDF(fileBytes, password == null ? "" : password)) {
            return new PDFTextStripper().getText(document);
        } catch (InvalidPasswordException exception) {
            throw new BadRequestException("Invalid PDF password");
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read PDF statement");
        }
    }
}
