package com.dvFabricio.VidaLongaFlix.infra.config;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.category.CategoryType;
import com.dvFabricio.VidaLongaFlix.domain.menu.Menu;
import com.dvFabricio.VidaLongaFlix.domain.user.Role;
import com.dvFabricio.VidaLongaFlix.domain.user.User;
import com.dvFabricio.VidaLongaFlix.domain.user.UserStatus;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.MenuRepository;
import com.dvFabricio.VidaLongaFlix.repositories.RoleRepository;
import com.dvFabricio.VidaLongaFlix.repositories.UserRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final VideoRepository videoRepository;
    private final MenuRepository menuRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        seedRolesAndAdmin();
        seedCategories();
        seedVideos();
        seedMenus();
    }

    // ── Roles & Admin ──────────────────────────────────────────────────────────

    private void seedRolesAndAdmin() {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User("Admin User", adminEmail,
                    passwordEncoder.encode(adminPassword), "(51)99999-9999");
            admin.setTaxId("987.654.321-00");
            admin.setProfileComplete(true);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setQueuePosition(null);
            admin.setRoles(List.of(userRole, adminRole));
            userRepository.save(admin);
        }
    }

    // ── Categories ─────────────────────────────────────────────────────────────

    private void seedCategories() {
        cat("Receitas",      CategoryType.VIDEO);
        cat("Almoço",        CategoryType.MENU);
        cat("Lanche",        CategoryType.MENU);
        cat("Jantar",        CategoryType.MENU);
        cat("Café da manhã", CategoryType.MENU);
        cat("Sobremesa",     CategoryType.MENU);
        cat("Bebida",        CategoryType.MENU);
    }

    private Category cat(String name, CategoryType type) {
        return categoryRepository.findByNameAndType(name, type)
                .orElseGet(() -> categoryRepository.save(new Category(name, type)));
    }

    // ── Videos ────────────────────────────────────────────────────────────────

    private void seedVideos() {
        if (videoRepository.count() > 0) return;
        Category c = cat("Receitas", CategoryType.VIDEO);
        videoRepository.saveAll(List.of(
            v(c, "Bolo de Cenoura Fácil",
                "Aprenda a fazer um bolo de cenoura simples e delicioso.",
                "assets/videos/Bolo-de-Cenoura-Facil.mp4",
                "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400&h=300&fit=crop",
                3.7, 21.2, 4.9, 1.6, 142.0,
                "Bata cenoura, ovo e óleo no liquidificador. Adicione açúcar, farinha e fermento. Asse a 180°C por 35 min."),
            v(c, "Bolo de Cenoura",
                "Receita tradicional de bolo de cenoura com cobertura.",
                "assets/videos/Bolo-de-Cenoura.mp4",
                "https://images.unsplash.com/photo-1621303837174-89787a7d4729?w=400&h=300&fit=crop",
                4.4, 22.4, 5.8, 2.2, 165.0,
                "Prepare a massa tradicional e finalize com cobertura de chocolate."),
            v(c, "Bolo de Chocolate",
                "Um bolo de chocolate super fofo e saboroso.",
                "assets/videos/Bolo-de-Chocolate.mp4",
                "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400&h=300&fit=crop",
                5.1, 23.6, 6.7, 1.0, 183.0,
                "Misture cacau, farinha, ovos e leite. Asse e cubra com ganache."),
            v(c, "Bolo de Laranja",
                "Delicioso bolo de laranja com sabor cítrico marcante.",
                "assets/videos/Bolo-de-Laranja.mp4",
                "https://images.unsplash.com/photo-1557925923-cd4648e211a0?w=400&h=300&fit=crop",
                5.8, 24.8, 4.0, 1.6, 168.0,
                "Bata suco de laranja com ovos e óleo. Adicione farinha e fermento. Asse a 180°C."),
            v(c, "Bolo de Limão",
                "Receita leve de bolo de limão com um toque cítrico.",
                "assets/videos/Bolo-de-Limao.mp4",
                "https://images.unsplash.com/photo-1519915028121-7d3463d20b13?w=400&h=300&fit=crop",
                3.0, 26.0, 4.9, 2.2, 161.0,
                "Misture raspas de limão, ovos e iogurte. Asse e finalize com calda de limão."),
            v(c, "Bolo de Milho com Goiabada",
                "Bolo delicioso de milho com pedaços de goiabada.",
                "assets/videos/Bolo-de-Milho-com-Goibada.mp4",
                "https://images.unsplash.com/photo-1586444248902-2f64eddc13df?w=400&h=300&fit=crop",
                3.7, 20.0, 5.8, 1.0, 148.0,
                "Bata milho verde, ovos e margarina. Adicione fubá, farinha e fermento. Recheie com goiabada."),
            v(c, "Bolo de Pote Proteico",
                "Uma opção saudável e prática de bolo proteico.",
                "assets/videos/Bolo-de-pote-proteico.mp4",
                "https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?w=400&h=300&fit=crop",
                4.4, 21.2, 6.7, 1.6, 166.0,
                "Monte camadas de bolo, creme proteico e frutas em potes individuais."),
            v(c, "Bolocuca de Banana",
                "Uma cuca deliciosa feita com bananas maduras.",
                "assets/videos/Bolocuca-de-banana.mp4",
                "https://images.unsplash.com/photo-1603569283847-aa295f0d016a?w=400&h=300&fit=crop",
                5.1, 22.4, 4.0, 2.2, 151.0,
                "Amasse bananas maduras e misture com farinha, ovos e canela. Adicione farofa por cima."),
            v(c, "Brownie no Pote",
                "Delicioso brownie no pote para sobremesa ou presente.",
                "assets/videos/Brownie-no-Pote.mp4",
                "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=400&h=300&fit=crop",
                5.8, 23.6, 4.9, 1.0, 163.0,
                "Prepare a massa de brownie e sirva em camadas com sorvete em potes."),
            v(c, "Brownie Proteico",
                "Uma versão saudável e deliciosa do brownie clássico.",
                "assets/videos/Brownie-proteico.mp4",
                "https://images.unsplash.com/photo-1607920591413-4ec007e70023?w=400&h=300&fit=crop",
                3.0, 24.8, 5.8, 1.6, 161.0,
                "Substitua farinha por proteína whey e açúcar por adoçante. Asse a 180°C por 20 min."),
            v(c, "Cupcake de Banana",
                "Cupcake fofinho de banana para o lanche da tarde.",
                "assets/videos/Cupcake-de-Banana.mp4",
                "https://images.unsplash.com/photo-1614707267537-b85aaf00c4b7?w=400&h=300&fit=crop",
                3.7, 26.0, 6.7, 2.2, 178.0,
                "Amasse banana, misture com farinha e ovos. Asse em forminhas e decore com cream cheese."),
            v(c, "Cuca de Uva e Banana",
                "Receita tradicional de cuca com uva e banana.",
                "assets/videos/Cuca-de-Uva-e-Banana.mp4",
                "https://images.unsplash.com/photo-1612929633738-8fe44f7ec841?w=400&h=300&fit=crop",
                4.4, 20.0, 4.0, 1.0, 140.0,
                "Prepare a massa de cuca, cubra com uva e banana. Faça a farofa e espalhe por cima.")
        ));
    }

    private Video v(Category cat, String title, String desc, String url, String cover,
                    double protein, double carbs, double fat, double fiber, double calories,
                    String recipe) {
        return Video.builder()
                .title(title).description(desc).url(url).cover(cover).category(cat)
                .protein(protein).carbs(carbs).fat(fat).fiber(fiber).calories(calories)
                .recipe(recipe).views(0).watchTime(0.0).likesCount(0).favorited(false)
                .build();
    }

    // ── Menus (Cardápios) ──────────────────────────────────────────────────────

    private void seedMenus() {
        if (menuRepository.count() > 0) return;
        Category almoco    = cat("Almoço",        CategoryType.MENU);
        Category lanche    = cat("Lanche",        CategoryType.MENU);
        Category jantar    = cat("Jantar",        CategoryType.MENU);
        Category cafe      = cat("Café da manhã", CategoryType.MENU);
        Category sobremesa = cat("Sobremesa",     CategoryType.MENU);
        Category bebida    = cat("Bebida",        CategoryType.MENU);

        menuRepository.saveAll(List.of(
            m(almoco, "Bowl Proteico de Frango",
                "Almoço equilibrado com fibras e proteína.",
                "https://images.unsplash.com/photo-1546069901-eacef0df6022?w=900&h=600&fit=crop",
                32.0, 45.0, 12.0, 8.0, 420.0,
                "Grelhe o frango, monte com arroz integral, brócolis e cenoura.",
                "Prefira arroz integral para maior saciedade."),
            m(lanche, "Lanche da Tarde Fit",
                "Sanduíche integral com pasta de atum.",
                "https://images.unsplash.com/photo-1550507992-eb63ffee0847?w=900&h=600&fit=crop",
                24.0, 30.0, 6.0, 5.0, 280.0,
                "Misture atum com iogurte e mostarda. Monte no pão integral.",
                "Troque maionese por iogurte para reduzir gordura."),
            m(jantar, "Jantar Leve",
                "Sopa de legumes com frango desfiado.",
                "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=900&h=600&fit=crop",
                20.0, 18.0, 4.0, 6.0, 190.0,
                "Cozinhe legumes e frango, bata metade no liquidificador.",
                "Evite exagerar no sal; use ervas."),
            m(cafe, "Café da Manhã Funcional",
                "Iogurte com granola e frutas vermelhas.",
                "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=900&h=600&fit=crop",
                15.0, 35.0, 7.0, 5.0, 260.0,
                "Monte camadas de iogurte, granola e frutas.",
                "Evite granolas com açúcar refinado."),
            m(almoco, "Salada Colorida",
                "Mix de folhas com legumes e castanhas.",
                "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=900&h=600&fit=crop",
                8.0, 20.0, 10.0, 6.0, 210.0,
                "Misture folhas, tomate, cenoura, pepino e castanhas.",
                "Adicione azeite extra virgem na hora."),
            m(lanche, "Panqueca de Aveia",
                "Panqueca leve com banana e mel.",
                "https://images.unsplash.com/photo-1504754524776-8f4f37790ca0?w=900&h=600&fit=crop",
                12.0, 40.0, 6.0, 4.0, 300.0,
                "Bata aveia, ovo e banana. Grelhe em frigideira antiaderente.",
                "Evite excesso de mel."),
            m(lanche, "Wrap Integral",
                "Wrap com frango, alface e creme leve.",
                "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=900&h=600&fit=crop",
                22.0, 28.0, 7.0, 5.0, 290.0,
                "Recheie o wrap com frango desfiado e legumes.",
                "Prefira wraps integrais."),
            m(jantar, "Peixe Grelhado",
                "Filé de peixe com legumes salteados.",
                "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&h=600&fit=crop",
                28.0, 15.0, 8.0, 4.0, 240.0,
                "Grelhe o peixe e salteie legumes com azeite.",
                "Use ervas frescas para temperar."),
            m(sobremesa, "Sobremesa Fit",
                "Mousse de cacau com abacate.",
                "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=900&h=600&fit=crop",
                4.0, 12.0, 14.0, 5.0, 180.0,
                "Bata abacate com cacau e adoçante.",
                "Cacau puro é rico em antioxidantes."),
            m(bebida, "Smoothie Verde",
                "Bebida nutritiva com couve e frutas.",
                "https://images.unsplash.com/photo-1497534446932-c925b458314e?w=900&h=600&fit=crop",
                3.0, 25.0, 2.0, 4.0, 140.0,
                "Bata couve, banana, maçã e água.",
                "Consuma logo após bater."),
            m(almoco, "Arroz Integral com Legumes",
                "Prato simples e nutritivo para o dia a dia.",
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=900&h=600&fit=crop",
                9.0, 48.0, 5.0, 6.0, 320.0,
                "Cozinhe o arroz integral e misture legumes cozidos.",
                "Use legumes da estação."),
            m(jantar, "Omelete de Legumes",
                "Omelete leve com tomate e espinafre.",
                "https://images.unsplash.com/photo-1504754524776-8f4f37790ca0?w=900&h=600&fit=crop",
                18.0, 6.0, 10.0, 2.0, 210.0,
                "Bata ovos e misture legumes, cozinhe em fogo baixo.",
                "Evite fritura pesada, use azeite."),
            m(almoco, "Bowl de Quinoa com Legumes",
                "Almoço leve e rico em fibras.",
                "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=900&h=600&fit=crop",
                14.0, 42.0, 9.0, 7.0, 320.0,
                "Cozinhe a quinoa e misture com legumes salteados.",
                "Use azeite extra virgem após o preparo."),
            m(almoco, "Frango com Batata Doce",
                "Combinação clássica para energia e saciedade.",
                "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&h=600&fit=crop",
                28.0, 35.0, 6.0, 5.0, 360.0,
                "Grelhe o frango e asse a batata doce.",
                "Tempere com ervas para reduzir o sal."),
            m(almoco, "Macarrão Integral ao Pesto",
                "Prato simples com fibras e gorduras boas.",
                "https://images.unsplash.com/photo-1526318896980-cf78c088247c?w=900&h=600&fit=crop",
                12.0, 48.0, 10.0, 6.0, 380.0,
                "Cozinhe o macarrão e misture com pesto.",
                "Prefira molho caseiro."),
            m(almoco, "Salada de Grão-de-bico",
                "Almoço leve e proteico.",
                "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=900&h=600&fit=crop",
                13.0, 30.0, 8.0, 9.0, 290.0,
                "Misture grão-de-bico com legumes e tempero.",
                "Use limão para realçar o sabor."),
            m(cafe, "Tapioca com Ovo e Queijo",
                "Café da manhã prático e nutritivo.",
                "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=900&h=600&fit=crop",
                18.0, 26.0, 7.0, 2.0, 260.0,
                "Prepare a tapioca e recheie com ovo mexido.",
                "Use queijo branco para reduzir gordura."),
            m(cafe, "Overnight Oats",
                "Aveia hidratada com frutas.",
                "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=900&h=600&fit=crop",
                12.0, 34.0, 6.0, 6.0, 240.0,
                "Misture aveia com iogurte e deixe na geladeira.",
                "Adicione chia para mais fibras."),
            m(lanche, "Sanduíche de Peru",
                "Lanche rápido e leve.",
                "https://images.unsplash.com/photo-1550507992-eb63ffee0847?w=900&h=600&fit=crop",
                20.0, 28.0, 5.0, 4.0, 250.0,
                "Use pão integral, peito de peru e folhas.",
                "Evite maionese; use pasta de ricota."),
            m(lanche, "Iogurte Proteico com Frutas",
                "Lanche prático pós-treino.",
                "https://images.unsplash.com/photo-1497534446932-c925b458314e?w=900&h=600&fit=crop",
                16.0, 24.0, 4.0, 3.0, 220.0,
                "Misture iogurte com frutas e granola.",
                "Prefira granola sem açúcar."),
            m(jantar, "Sopa de Abóbora",
                "Jantar leve e reconfortante.",
                "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=900&h=600&fit=crop",
                6.0, 22.0, 4.0, 5.0, 170.0,
                "Cozinhe abóbora e bata com temperos.",
                "Use gengibre para dar sabor."),
            m(jantar, "Omelete com Espinafre",
                "Jantar rápido e proteico.",
                "https://images.unsplash.com/photo-1504754524776-8f4f37790ca0?w=900&h=600&fit=crop",
                19.0, 6.0, 9.0, 2.0, 210.0,
                "Misture ovos com espinafre e cozinhe.",
                "Use pouco óleo."),
            m(sobremesa, "Mousse de Maracujá Fit",
                "Sobremesa leve com fruta.",
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=900&h=600&fit=crop",
                6.0, 18.0, 5.0, 3.0, 160.0,
                "Misture iogurte natural com maracujá.",
                "Adoce com mel moderadamente."),
            m(sobremesa, "Bolo de Banana Fit",
                "Sobremesa saudável sem açúcar refinado.",
                "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=900&h=600&fit=crop",
                7.0, 28.0, 6.0, 4.0, 230.0,
                "Amasse banana, aveia e ovo; asse.",
                "Use canela para adoçar.")
        ));
    }

    private Menu m(Category cat, String title, String desc, String cover,
                   double protein, double carbs, double fat, double fiber, double calories,
                   String recipe, String tips) {
        return Menu.builder()
                .title(title).description(desc).cover(cover).category(cat)
                .protein(protein).carbs(carbs).fat(fat).fiber(fiber).calories(calories)
                .recipe(recipe).nutritionistTips(tips)
                .build();
    }
}