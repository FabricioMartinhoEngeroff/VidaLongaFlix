package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.imports.ImportResultDTO;
import com.dvFabricio.VidaLongaFlix.domain.menu.Menu;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.MenuRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ImportService {

    private final VideoRepository videoRepository;
    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;

    public ImportService(VideoRepository videoRepository,
                         MenuRepository menuRepository,
                         CategoryRepository categoryRepository) {
        this.videoRepository = videoRepository;
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
    }

    public ImportResultDTO importVideos(MultipartFile file) throws IOException, CsvValidationException {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;
        int rowNum = 1;

        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                rowNum++;
                String title = trim(row.get("title"));
                String description = trim(row.get("description"));
                String url = trim(row.get("url"));
                String cover = trim(row.get("cover"));
                String categoryName = trim(row.get("categoryName"));

                if (isBlank(title)) {
                    errors.add("Linha " + rowNum + ": 'title' é obrigatório");
                    skipped++;
                    continue;
                }
                if (isBlank(description)) {
                    errors.add("Linha " + rowNum + ": 'description' é obrigatório");
                    skipped++;
                    continue;
                }
                if (isBlank(url)) {
                    errors.add("Linha " + rowNum + ": 'url' é obrigatório");
                    skipped++;
                    continue;
                }
                if (isBlank(categoryName)) {
                    errors.add("Linha " + rowNum + ": 'categoryName' é obrigatório");
                    skipped++;
                    continue;
                }

                Optional<Category> categoryOpt = categoryRepository.findByNameAndType(categoryName, CategoryType.VIDEO);
                if (categoryOpt.isEmpty()) {
                    errors.add("Linha " + rowNum + ": categoria '" + categoryName + "' não encontrada (tipo VIDEO). Crie a categoria antes de importar.");
                    skipped++;
                    continue;
                }

                Video video = Video.builder()
                        .title(title)
                        .description(description)
                        .url(url)
                        .cover(cover)
                        .category(categoryOpt.get())
                        .recipe(trim(row.get("recipe")))
                        .protein(parseDouble(row.get("protein")))
                        .carbs(parseDouble(row.get("carbs")))
                        .fat(parseDouble(row.get("fat")))
                        .fiber(parseDouble(row.get("fiber")))
                        .calories(parseDouble(row.get("calories")))
                        .views(0)
                        .watchTime(0.0)
                        .likesCount(0)
                        .favorited(false)
                        .build();

                videoRepository.save(video);
                imported++;
            }
        }

        return new ImportResultDTO(imported, skipped, errors);
    }

    public ImportResultDTO importMenus(MultipartFile file) throws IOException, CsvValidationException {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;
        int rowNum = 1;

        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                rowNum++;
                String title = trim(row.get("title"));
                String description = trim(row.get("description"));
                String categoryName = trim(row.get("categoryName"));

                if (isBlank(title)) {
                    errors.add("Linha " + rowNum + ": 'title' é obrigatório");
                    skipped++;
                    continue;
                }
                if (isBlank(description)) {
                    errors.add("Linha " + rowNum + ": 'description' é obrigatório");
                    skipped++;
                    continue;
                }
                if (isBlank(categoryName)) {
                    errors.add("Linha " + rowNum + ": 'categoryName' é obrigatório");
                    skipped++;
                    continue;
                }

                Optional<Category> categoryOpt = categoryRepository.findByNameAndType(categoryName, CategoryType.MENU);
                if (categoryOpt.isEmpty()) {
                    errors.add("Linha " + rowNum + ": categoria '" + categoryName + "' não encontrada (tipo MENU). Crie a categoria antes de importar.");
                    skipped++;
                    continue;
                }

                Menu menu = Menu.builder()
                        .title(title)
                        .description(description)
                        .cover(trim(row.get("cover")))
                        .category(categoryOpt.get())
                        .recipe(trim(row.get("recipe")))
                        .nutritionistTips(trim(row.get("nutritionistTips")))
                        .protein(parseDouble(row.get("protein")))
                        .carbs(parseDouble(row.get("carbs")))
                        .fat(parseDouble(row.get("fat")))
                        .fiber(parseDouble(row.get("fiber")))
                        .calories(parseDouble(row.get("calories")))
                        .build();

                menuRepository.save(menu);
                imported++;
            }
        }

        return new ImportResultDTO(imported, skipped, errors);
    }

    private String trim(String value) {
        return value != null ? value.trim() : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Double parseDouble(String value) {
        if (isBlank(value)) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
