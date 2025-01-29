package com.dvFabricio.VidaLongaFlix;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
        "com.dvFabricio.VidaLongaFlix.videoTest.domain",
        "com.dvFabricio.VidaLongaFlix.videoTest.repository",
        "com.dvFabricio.VidaLongaFlix.videoTest.service",
        "com.dvFabricio.VidaLongaFlix.videoTest.controller"
})
public class AllTestsSuite {
    // Este Test Suite executará todos os testes dos pacotes especificados.
}
