# VidaLongaFlix — Especificación Funcional del Sistema

> **Versión:** 1.0 — 17/05/2026
> **Alcance:** Sistema completo (backend Spring Boot + frontend Angular)
> **Complementa:** `VidaLongaFlix-Backend-EN.md` (especificación técnica de API)

---

## 1. Visión General del Sistema

VidaLongaFlix es una plataforma de streaming de contenido orientada a la salud y la longevidad. Los usuarios ven vídeos de recetas saludables, acceden a menús nutricionales, marcan favoritos, comentan y reciben notificaciones de novedades.

### Arquitectura

```
Usuario (navegador)
    │
    ▼
Angular SPA (CloudFront / S3)
    │  HTTP + JWT Bearer
    ▼
Spring Boot API (Elastic Beanstalk — puerto 8090, context /api)
    │
    ├── PostgreSQL (RDS — producción)
    ├── S3 (medios — vídeos e imágenes)
    └── WhatsApp Business API (Meta — notificaciones de bienvenida)
```

---

## 2. Perfiles de Acceso

| Perfil        | Quién es                                      | Cómo obtiene acceso         |
|---------------|-----------------------------------------------|-----------------------------|
| **Anónimo**   | Visitante sin cuenta o no autenticado         | Sin login                   |
| **ROLE_USER** | Usuario registrado y activo                   | Registro + login con JWT    |
| **ROLE_ADMIN**| Administrador de la plataforma                | Creado por el sistema al iniciar |

---

## 3. Matriz de Funcionalidades

| Funcionalidad                            | Anónimo | ROLE_USER | ROLE_ADMIN |
|------------------------------------------|:-------:|:---------:|:----------:|
| Ver lista de vídeos                      | ✅      | ✅        | ✅         |
| Ver detalle de un vídeo                  | ✅      | ✅        | ✅         |
| Ver lista de menús                       | ✅      | ✅        | ✅         |
| Ver detalle de un menú                   | ✅      | ✅        | ✅         |
| Ver categorías                           | ✅      | ✅        | ✅         |
| Ver comentarios de un vídeo              | ✅      | ✅        | ✅         |
| Registrar visualización en vídeo         | ✅      | ✅        | ✅         |
| Iniciar sesión                           | ✅      | ✅        | ✅         |
| Registrarse                              | ✅      | —         | —          |
| Verificar estado de apertura de plazas   | ✅      | —         | —          |
| Salir de la lista de espera              | ✅      | —         | —          |
| Comentar en vídeos                       | ❌      | ✅        | ✅         |
| Agregar/quitar favoritos                 | ❌      | ✅        | ✅         |
| Ver lista de favoritos                   | ❌      | ✅        | ✅         |
| Ver notificaciones                       | ❌      | ✅        | ✅         |
| Marcar notificaciones como leídas        | ❌      | ✅        | ✅         |
| Ver y editar propio perfil               | ❌      | ✅        | ✅         |
| Crear vídeo                              | ❌      | ❌        | ✅         |
| Editar vídeo                             | ❌      | ❌        | ✅         |
| Eliminar vídeo                           | ❌      | ❌        | ✅         |
| Crear menú                               | ❌      | ❌        | ✅         |
| Editar menú                              | ❌      | ❌        | ✅         |
| Eliminar menú                            | ❌      | ❌        | ✅         |
| Crear/editar/eliminar categorías         | ❌      | ❌        | ✅         |
| Eliminar comentarios                     | ❌      | ❌        | ✅         |
| Gestionar usuarios (crear/eliminar)      | ❌      | ❌        | ✅         |
| Gestionar lista de espera                | ❌      | ❌        | ✅         |
| Ver analytics                            | ❌      | ❌        | ✅         |
| Cambiar límite de usuarios activos       | ❌      | ❌        | ✅         |

---

## 4. Jornadas del Usuario Común (ROLE_USER)

### 4.1 Registro y Primer Acceso

**Precondición:** El usuario no tiene cuenta.

**Flujo principal — hay plaza disponible:**

1. El usuario accede a la pantalla de registro y completa: nombre, correo, contraseña y teléfono.
2. El sistema verifica disponibilidad de plazas (`count(ACTIVE) < MAX_ACTIVE_USERS`).
3. El backend crea al usuario con estado `ACTIVE`, genera el token JWT y devuelve HTTP 201.
4. El frontend guarda el token y redirige a la pantalla principal.
5. WhatsApp Business envía un mensaje de bienvenida al número indicado.

**Flujo alternativo — límite de usuarios alcanzado:**

1. Mismos pasos 1–2.
2. El backend crea al usuario con estado `QUEUED`, asigna posición en la cola y devuelve HTTP 202 sin token.
3. El frontend muestra: *"Estás en la posición #N de la lista de espera."*
4. El usuario no puede iniciar sesión hasta ser promovido a `ACTIVE`.

**Reglas de contraseña:** mínimo 8 caracteres con mayúscula, minúscula, número y carácter especial.

---

### 4.2 Inicio de Sesión

**Flujo principal:**

1. El usuario ingresa correo y contraseña.
2. El backend valida las credenciales y devuelve token JWT (vigencia: 2 horas) + datos del usuario.
3. El frontend guarda el token y redirige a la home.

**Flujos alternativos:**

| Situación               | Respuesta del backend         | Comportamiento del frontend                        |
|-------------------------|-------------------------------|----------------------------------------------------|
| Credenciales inválidas  | 401                           | Muestra "Correo o contraseña incorrectos"          |
| Estado `QUEUED`         | 403 `ACCOUNT_QUEUED`          | Muestra posición en la cola y orienta a esperar    |
| Estado `DISABLED`       | 403 `ACCOUNT_DISABLED`        | Muestra mensaje de cuenta desactivada              |

---

### 4.3 Navegación en el Catálogo de Vídeos

1. La pantalla principal carga las categorías de vídeo (`GET /categories?type=VIDEO`).
2. Para cada categoría se muestra un carrusel horizontal con los vídeos correspondientes.
3. Cada tarjeta muestra: imagen de portada, título, contador de visualizaciones y de me gusta.

**Categorías predeterminadas (seed):**

| Categoría          | Cant. de vídeos |
|--------------------|:--------------:|
| Bolos Clássicos    | 5              |
| Bolos Especiais    | 4              |
| Receitas Proteicas | 3              |

---

### 4.4 Ver un Vídeo

1. El usuario hace clic en una tarjeta → pantalla de detalle del vídeo (`GET /videos/{id}`).
2. El reproductor carga el vídeo. Al iniciar la reproducción → `PATCH /videos/{id}/view` (contador +1).
3. La pantalla muestra: reproductor, título, descripción, receta e información nutricional.
4. Sección de comentarios debajo del reproductor (lectura pública, escritura requiere login).
5. Botón de favorito (❤️) visible solo para usuarios autenticados.

---

### 4.5 Comentar en un Vídeo

1. El usuario autenticado escribe el comentario y hace clic en "Enviar".
2. El backend crea el comentario (`POST /comments`).
3. El comentario aparece inmediatamente en la lista.

**Protección contra duplicados:** mismo usuario + mismo texto + mismo vídeo → backend devuelve 409. El frontend muestra aviso de duplicado.

---

### 4.6 Marcar Favoritos

1. El usuario hace clic en el ícono ❤️ en un vídeo o menú.
2. El sistema llama a `POST /favorites/{type}/{itemId}`.
3. Comportamiento toggle: si no es favorito → agrega; si ya es favorito → elimina.
4. El ícono y el contador de me gusta se actualizan inmediatamente.

---

### 4.7 Notificaciones

1. El ícono de campana en el encabezado muestra la cantidad de no leídas (`GET /notifications/unread-count`).
2. Al hacer clic → panel de notificaciones paginado (`GET /notifications?page=0&size=20`).
3. Las notificaciones no leídas aparecen resaltadas.
4. Al abrir el panel → `POST /notifications/mark-all-read` → el contador se pone en cero.

**Generación automática:** cada vez que un admin publica un vídeo o menú, el sistema genera una notificación para todos los usuarios.

---

### 4.8 Gestionar Perfil

1. El usuario accede a "Mi Perfil" → datos propios (`GET /auth/me`).
2. Puede editar: nombre, teléfono, CNPJ/CPF (opcional), dirección, foto.
3. Guarda (`PUT /users/{id}`) → solo el propio usuario o un ROLE_ADMIN puede modificar.
4. Los datos actualizados se reflejan de inmediato.

---

### 4.9 Salir de la Lista de Espera

1. El usuario con estado `QUEUED` accede a la pantalla de cancelación de cola.
2. Ingresa el correo y confirma.
3. El sistema llama a `DELETE /auth/waitlist/me?email={email}`.
4. La cuenta se elimina y las posiciones de los demás se recalculan.

---

## 5. Jornadas del Administrador (ROLE_ADMIN)

### 5.1 Acceso Administrativo

1. El admin inicia sesión con las credenciales configuradas en el boot (`ADMIN_EMAIL` / `ADMIN_PASSWORD`).
2. El frontend detecta `ROLE_ADMIN` y habilita funcionalidades exclusivas:
   - Ícono de lápiz (✏️) en vídeos y menús.
   - Menú o pestaña de administración.
   - Acceso a las pantallas de analytics, gestión de usuarios y cola.

**Importante:** el ícono de edición y los botones de gestión se muestran **únicamente** para ROLE_ADMIN. Los usuarios comunes nunca ven estos controles.

---

### 5.2 Crear Vídeo

1. Admin accede a Gestionar Vídeos → "Nuevo Vídeo".
2. Completa el formulario:

| Campo                     | Obligatorio |
|---------------------------|:-----------:|
| Título                    | ✅          |
| Descripción               | ✅          |
| Categoría                 | ✅          |
| Archivo de vídeo o URL    | ✅          |
| Imagen de portada o URL   | ✅          |
| Receta                    | ❌          |
| Información nutricional   | ❌          |

3. El frontend envía `POST /admin/videos` (JSON o `multipart/form-data` si hay archivo).
4. El backend almacena el archivo en S3 (producción), guarda el vídeo y **genera notificación para todos los usuarios**.
5. El admin regresa a la lista de vídeos.

---

### 5.3 Editar Vídeo

1. Admin hace clic en el ícono ✏️ en la tarjeta o en la pantalla de detalle.
2. Formulario precargado con los datos actuales.
3. Admin modifica solo los campos deseados (actualización parcial).
4. Guarda → `PUT /admin/videos/{id}`.

---

### 5.4 Eliminar Vídeo

1. Admin hace clic en eliminar → el sistema muestra confirmación.
2. Admin confirma → `DELETE /admin/videos/{id}`.
3. El vídeo desaparece de la lista.

---

### 5.5 Gestionar Menús

Flujo idéntico al de vídeos (secciones 5.2–5.4) usando los endpoints `/admin/menus`.

**Campo adicional exclusivo de los menús:** `nutritionistTips` (consejos del nutricionista, texto libre, opcional).

---

### 5.6 Gestionar Categorías

1. Admin accede a Gestionar Categorías.
2. Puede crear (nombre + tipo: `VIDEO` o `MENU`), renombrar o eliminar categorías.
3. Restricción: dos categorías con mismo nombre y tipo devuelven 409.

---

### 5.7 Gestionar Lista de Espera

La pantalla muestra: límite actual, cantidad de usuarios activos y lista de la cola por posición.

| Acción                         | Endpoint                                  | Efecto                                                |
|--------------------------------|-------------------------------------------|-------------------------------------------------------|
| Activar usuario manualmente    | `POST /admin/waitlist/{userId}/activate`  | Promueve QUEUED → ACTIVE (si hay plaza)               |
| Eliminar usuario de la cola    | `DELETE /admin/waitlist/{userId}`         | Elimina y recalcula posiciones                        |
| Cambiar límite de activos      | `PUT /admin/config/max-users`             | Si se abren plazas, el backend promueve automáticamente (FIFO) |

---

### 5.8 Gestionar Usuarios

| Acción                   | Endpoint              | Quién puede usarlo          |
|--------------------------|-----------------------|-----------------------------|
| Buscar por ID            | `GET /users/{id}`     | ADMIN o el propio usuario   |
| Crear directamente       | `POST /users`         | Solo ADMIN                  |
| Editar cualquier perfil  | `PUT /users/{id}`     | ADMIN o el propio usuario   |
| Eliminar usuario         | `DELETE /users/{id}`  | Solo ADMIN                  |

---

### 5.9 Eliminar Comentarios

1. Admin visualiza los comentarios de cualquier vídeo.
2. Hace clic en eliminar en un comentario inapropiado → `DELETE /comments/{commentId}`.
3. Comentario eliminado permanentemente.

---

### 5.10 Analytics

| Sección      | Dato                                   | Endpoint                                              |
|--------------|----------------------------------------|-------------------------------------------------------|
| Vídeos       | Más vistos                             | `GET /admin/videos/most-viewed`                       |
| Vídeos       | Menos vistos                           | `GET /admin/videos/least-viewed`                      |
| Vídeos       | Visualizaciones por categoría          | `GET /admin/videos/views-by-category`                 |
| Vídeos       | Tiempo promedio visto                  | `GET /admin/videos/{id}/tempo-medio-assistido`        |
| Vídeos       | Con más comentarios                    | `GET /admin/videos/mais-comentados`                   |
| Comentarios  | Total general                          | `GET /admin/comentarios/total`                        |
| Comentarios  | Total por vídeo                        | `GET /admin/comentarios/total-por-video`              |

---

## 6. Pantallas del Sistema

### 6.1 Pantallas Públicas

| Pantalla                | Ruta                    | Descripción                                      |
|-------------------------|-------------------------|--------------------------------------------------|
| Home / Catálogo         | `/`                     | Carruseles de vídeos por categoría               |
| Detalle del Vídeo       | `/videos/:id`           | Reproductor + info + comentarios                 |
| Catálogo de Menús       | `/menus`                | Lista de menús                                   |
| Detalle del Menú        | `/menus/:id`            | Receta + info nutricional + consejos             |
| Iniciar sesión          | `/login`                | Formulario de inicio de sesión                   |
| Registro                | `/register`             | Formulario de registro + feedback de cola        |
| Estado de Plazas        | `/registration-status`  | Indicador de apertura de plazas                  |

### 6.2 Pantallas Autenticadas

| Pantalla        | Ruta              | Descripción                                   |
|-----------------|-------------------|-----------------------------------------------|
| Perfil          | `/profile`        | Datos del usuario + formulario de edición     |
| Favoritos       | `/favorites`      | Vídeos y menús marcados como favoritos        |
| Notificaciones  | `/notifications`  | Historial con badge de no leídas              |

### 6.3 Pantallas Administrativas

| Pantalla                | Ruta                    | Descripción                                   |
|-------------------------|-------------------------|-----------------------------------------------|
| Gestionar Vídeos        | `/admin/videos`         | CRUD de vídeos con carga de medios            |
| Gestionar Menús         | `/admin/menus`          | CRUD de menús                                 |
| Gestionar Categorías    | `/admin/categories`     | CRUD de categorías                            |
| Gestionar Usuarios/Cola | `/admin/waitlist`       | Cola de espera + promoción de usuarios        |
| Analytics               | `/admin/analytics`      | Dashboards de visualizaciones y comentarios   |

---

## 7. Seguridad en el Frontend

| Situación                                          | Comportamiento esperado                                |
|----------------------------------------------------|--------------------------------------------------------|
| Token JWT expirado (2h)                            | Redirección automática a `/login`                      |
| Usuario sin token accede a ruta privada            | Redirección a `/login`                                 |
| ROLE_USER intenta acceder a ruta `/admin/**`       | Redirección a home o página 403                        |
| Ícono de edición (✏️) en vídeos y menús            | Visible **solo** para ROLE_ADMIN                       |
| Botón de eliminar comentario                       | Visible **solo** para ROLE_ADMIN                       |
| Botón de favorito (❤️)                             | Visible **solo** para usuarios autenticados            |
| Campo de comentario                                | Habilitado **solo** para usuarios autenticados         |
| Todas las solicitudes autenticadas                 | Header `Authorization: Bearer {token}` vía interceptor Angular |

---

## 8. Estados del Usuario

```
[Registro]
    │
    ├── hay plaza ──────► ACTIVE ◄──── Admin activa desde la cola
    │                       │
    └── sin plaza ──► QUEUED ──────── Admin elimina ──► (eliminado)
                            │
                            └── Admin activa ──► ACTIVE

ACTIVE   → puede iniciar sesión, ver vídeos, comentar, marcar favoritos
QUEUED   → no puede iniciar sesión; espera promoción
DISABLED → no puede iniciar sesión; cuenta desactivada
```

---

## 9. Reglas de Negocio Consolidadas

| Código | Regla                                                                                                  |
|--------|--------------------------------------------------------------------------------------------------------|
| RN-01  | El token JWT tiene vigencia de 2 horas. Al expirar, el usuario debe volver a iniciar sesión.          |
| RN-02  | Las contraseñas se almacenan con hash BCrypt. Nunca en texto plano.                                   |
| RN-03  | El límite de usuarios activos es configurable por el admin sin redeploy.                              |
| RN-04  | Cuando el límite sube, el backend promueve automáticamente a los primeros de la cola (FIFO).          |
| RN-05  | Un usuario no puede comentar el mismo texto dos veces en el mismo vídeo.                              |
| RN-06  | Favorito es toggle: la segunda llamada elimina; no existe estado duplicado.                           |
| RN-07  | Las notificaciones son globales: todos los usuarios las reciben cuando se publica nuevo contenido.    |
| RN-08  | Una notificación es "leída" si fue creada antes del campo `notificationsLastReadAt` del usuario.      |
| RN-09  | El admin creado al iniciar usa credenciales de variables de entorno (`ADMIN_EMAIL` / `ADMIN_PASSWORD`).|
| RN-10  | ROLE_USER solo puede editar y consultar su propio perfil. ROLE_ADMIN gestiona cualquier usuario.      |
| RN-11  | El upload de vídeo/portada va a S3 en producción; la URL definitiva la devuelve el backend.           |
| RN-12  | El fallo en el envío de WhatsApp no bloquea el registro del usuario.                                  |

---

## 10. Historial de Revisiones

| Fecha      | Descripción                                              | Responsable |
|------------|----------------------------------------------------------|-------------|
| 17/05/2026 | Creación del documento — cobertura completa del sistema  | Fabricio    |