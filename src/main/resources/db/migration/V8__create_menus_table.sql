-- Tabela de menus (pratos/receitas)
CREATE TABLE menus (
                       id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                       title VARCHAR(150) NOT NULL,
                       description TEXT NOT NULL,
                       cover VARCHAR(255),
                       category_id UUID NOT NULL,
                       recipe TEXT,
                       nutritionist_tips TEXT,
                       protein DOUBLE PRECISION,
                       carbs DOUBLE PRECISION,
                       fat DOUBLE PRECISION,
                       fiber DOUBLE PRECISION,
                       calories DOUBLE PRECISION,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP,
                       FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- Categorias de menu
INSERT INTO categories (id, name, type) VALUES
                                            ('9d8aafd4-9222-4150-aadb-5167405a7730', 'Café da Manhã', 'MENU'),
                                            ('9d8aafd4-9222-4150-aadb-5167405a7731', 'Almoço', 'MENU'),
                                            ('9d8aafd4-9222-4150-aadb-5167405a7732', 'Jantar', 'MENU'),
                                            ('9d8aafd4-9222-4150-aadb-5167405a7733', 'Lanches', 'MENU');

-- Seed de menus para teste
INSERT INTO menus (
    id, title, description, cover, category_id,
    recipe, nutritionist_tips,
    protein, carbs, fat, fiber, calories,
    created_at, updated_at
) VALUES
      (
          'b23e4567-e89b-12d3-a456-426614174010',
          'Frango Grelhado',
          'Prato rico em proteína, leve e nutritivo',
          NULL,
          '9d8aafd4-9222-4150-aadb-5167405a7731',
          'Tempere o frango e grelhe por 20 minutos em fogo médio.',
          'Prefira azeite de oliva extravirgem.',
          40.0, 10.0, 5.0, 2.0, 250.0,
          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
      ),
      (
          'b23e4567-e89b-12d3-a456-426614174011',
          'Vitamina de Banana com Aveia',
          'Café da manhã energético e saudável',
          NULL,
          '9d8aafd4-9222-4150-aadb-5167405a7730',
          'Bata banana, aveia e leite no liquidificador.',
          'Adicione mel a gosto para adoçar naturalmente.',
          8.0, 45.0, 3.0, 5.0, 220.0,
          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
      );