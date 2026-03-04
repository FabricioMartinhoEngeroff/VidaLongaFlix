package com.dvFabricio.VidaLongaFlix.domain.imports;

import java.util.List;

public record ImportResultDTO(int imported, int skipped, List<String> errors) {
}
