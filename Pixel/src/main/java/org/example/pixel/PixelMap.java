package org.example.pixel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class PixelMap {

    @PostMapping("/uploadImages")
    public ResponseEntity<String> uploadImages(
            @RequestParam("username") String username,
            @RequestParam("bankAccount") String bankAccount,
            @RequestParam("legalStamp") MultipartFile legalStampFile,
            @RequestParam("passportFront") MultipartFile passportFrontFile,
            @RequestParam("passportBack") MultipartFile passportBackFile) throws IOException {

        // Load the notarization PDF document
        Resource resource = new ClassPathResource("templates/Notarization.pdf");
        PDDocument document = PDDocument.load(resource.getInputStream());


        // Convert MultipartFile to PDImageXObject
        PDImageXObject legalStampImage = PDImageXObject.createFromByteArray(document, legalStampFile.getBytes(), legalStampFile.getOriginalFilename());
        PDImageXObject passportFrontImage = PDImageXObject.createFromByteArray(document, passportFrontFile.getBytes(), passportFrontFile.getOriginalFilename());
        PDImageXObject passportBackImage = PDImageXObject.createFromByteArray(document, passportBackFile.getBytes(), passportBackFile.getOriginalFilename());


        int pageIndex = 0;
        PDPage page = document.getPage(pageIndex);

        // Create content stream to add image to the PDF
        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);

        // Draw the images on the PDF
        contentStream.drawImage(legalStampImage, 170, 5410, 450, 450);
        contentStream.drawImage(legalStampImage, 2060, 40, 450, 450);
        contentStream.drawImage(passportFrontImage, 240, 2000, 2100, 1500);
        contentStream.drawImage(passportBackImage, 240, 500, 2100, 1500);

        // Cover the existing text and write new text
        // Coordinates and sizes for the cover rectangles and text placement
        float usernameX = 170, usernameY = 6220;
        float bankAccountX = 1120, bankAccountY = 6220;

        // Create content stream to add text to the PDF
        try (PDPageContentStream textContentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

            textContentStream.setNonStrokingColor(Color.WHITE);
            textContentStream.addRect(usernameX, usernameY, 380, 80); // Width and height should cover the old text
            textContentStream.fill();
            textContentStream.addRect(bankAccountX, bankAccountY, 660, 80);
            textContentStream.fill();

            float fontSize = 48;
            float textWidth = PDType1Font.HELVETICA.getStringWidth(username) / 1000 * fontSize;
            float textHeight = PDType1Font.HELVETICA.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;


            textContentStream.beginText();
            textContentStream.setFont(PDType1Font.HELVETICA, fontSize);
            textContentStream.setNonStrokingColor(Color.BLACK);

            // Calculate starting X position (center text in the rectangle)
            float startXUsername = usernameX + (390 - textWidth) / 2;
            float startYUsername = usernameY + (80 - textHeight) / 2;
            textContentStream.newLineAtOffset(startXUsername, startYUsername);
            textContentStream.showText(username);


            float textWidthAccount = PDType1Font.HELVETICA.getStringWidth(bankAccount) / 1000 * fontSize;
            float startXAccount = bankAccountX + (660 - textWidthAccount) / 2;
            float startYAccount = bankAccountY + (80 - textHeight) / 2;
            textContentStream.newLineAtOffset(startXAccount - startXUsername, startYAccount - startYUsername);
            textContentStream.showText(bankAccount);

            textContentStream.endText();
        } // The try-with-resources block ensures that the contentStream is closed automatically



        // Close the content stream
        contentStream.close();

        // Define the output directory relative to the project root
        String outputDirectoryPath = System.getProperty("user.dir") + "/output";

        // Create the directory if it does not exist
                Path outputDirectory = Paths.get(outputDirectoryPath);
                if (Files.notExists(outputDirectory)) {
                    Files.createDirectories(outputDirectory);
                }

        // Define the path for the modified PDF
                String outputFilePath = outputDirectoryPath + "/ModifiedNotarization.pdf";

        // Save the document to the output file path
                document.save(outputFilePath);
                document.close();

        return ResponseEntity.ok("PDF Modified Successfully");
    }
}

