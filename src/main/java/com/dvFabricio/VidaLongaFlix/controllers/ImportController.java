package com.dvFabricio.VidaLongaFlix.controllers;

import com.dvFabricio.VidaLongaFlix.domain.imports.ImportResultDTO;
import com.dvFabricio.VidaLongaFlix.services.ImportService;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/admin/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/videos")
    public ResponseEntity<ImportResultDTO> importVideos(@RequestParam("file") MultipartFile file)
            throws IOException, CsvValidationException {
        ImportResultDTO result = importService.importVideos(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/menus")
    public ResponseEntity<ImportResultDTO> importMenus(@RequestParam("file") MultipartFile file)
            throws IOException, CsvValidationException {
        ImportResultDTO result = importService.importMenus(file);
        return ResponseEntity.ok(result);
    }
}
