package com.dvFabricio.VidaLongaFlix.domain.address;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum State {
    ACRE("AC", "Acre"),
    ALAGOAS("AL", "Alagoas"),
    AMAPA("AP", "Amapá"),
    AMAZONAS("AM", "Amazonas"),
    BAHIA("BA", "Bahia"),
    CEARA("CE", "Ceará"),
    DISTRITO_FEDERAL("DF", "Distrito Federal"),
    ESPIRITO_SANTO("ES", "Espírito Santo"),
    GOIAS("GO", "Goiás"),
    MARANHAO("MA", "Maranhão"),
    MATO_GROSSO("MT", "Mato Grosso"),
    MATO_GROSSO_DO_SUL("MS", "Mato Grosso do Sul"),
    MINAS_GERAIS("MG", "Minas Gerais"),
    PARA("PA", "Pará"),
    PARAIBA("PB", "Paraíba"),
    PARANA("PR", "Paraná"),
    PERNAMBUCO("PE", "Pernambuco"),
    PIAUI("PI", "Piauí"),
    RIO_DE_JANEIRO("RJ", "Rio de Janeiro"),
    RIO_GRANDE_DO_NORTE("RN", "Rio Grande do Norte"),
    RIO_GRANDE_DO_SUL("RS", "Rio Grande do Sul"),
    RONDONIA("RO", "Rondônia"),
    RORAIMA("RR", "Roraima"),
    SANTA_CATARINA("SC", "Santa Catarina"),
    SAO_PAULO("SP", "São Paulo"),
    SERGIPE("SE", "Sergipe"),
    TOCANTINS("TO", "Tocantins");

    private final String abbreviation;
    private final String name;

    State(String abbreviation, String name) {
        this.abbreviation = abbreviation;
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public static State fromName(String input) {
        String normalized = Normalizer
                .normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toUpperCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(estado ->
                        estado.name().equalsIgnoreCase(normalized) ||
                                estado.abbreviation.equalsIgnoreCase(normalized) ||
                                estado.name.equalsIgnoreCase(input.trim()))
                .findFirst()
                .orElseThrow(() -> {
                    System.err.println("❌ Estado inválido: " + input);
                    System.err.println("📌 Estados disponíveis (siglas): " +
                            Arrays.stream(values()).map(State::getAbbreviation).collect(Collectors.joining(", ")));
                    return new IllegalArgumentException("Estado inválido: " + input);
                });
    }
}
