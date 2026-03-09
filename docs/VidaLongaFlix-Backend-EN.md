# VidaLongaFlix - Backend System Documentation

## 1. Revision History

| Date       | Revision Summary                                                             | Author  | Development Notes |
|------------|------------------------------------------------------------------------------|---------|-------------------|
| 02/28/2026 | Initial document creation                                                    | Fabricio| -                 |
| 03/03/2026 | Fixes: CommentResponseDTO shape, configurable CORS, CategoryController       | Fabricio| -                 |
| 03/07/2026 | CPF (taxId) becomes optional on user registration                            | Fabricio| -                 |
| 03/08/2026 | Automated CD pipeline: GitHub Actions deploy job to Elastic Beanstalk        | Fabricio| -                 |
| 03/08/2026 | GitHub Environment "production" with manual approval gate before deploy       | Fabricio| -                 |
| 03/09/2026 | Active user limit, waitlist support, and waitlist admin endpoints            | Fabricio| Backend implemented; frontend must handle `201/202` registration responses |

---

## 2. System Overview

VidaLongaFlix is a video and meal plan streaming platform focused on health and longevity. The system allows authenticated users to watch videos, like content, post comments, and receive notifications about newly published content. Administrators manage all platform content.

**Technologies:** Spring Boot 3.x, Java 17, JWT, H2 (development), PostgreSQL (production), WhatsApp Business API (Meta).

**API base URL:** `/api`

**Default port (development):** `8090`

---

## 3. Access Profiles

| Profile    | Description                                                    |
|------------|----------------------------------------------------------------|
| ROLE_USER  | Authenticated user with access to platform content            |
| ROLE_ADMIN | Administrator with full access, including content management  |
| Anonymous  | Limited access to read videos, menus, comments, and categories|

---

## 4. General Rules

**GR01** - Authentication uses JWT (JSON Web Token) with a 2-hour validity period. The token must be sent in the `Authorization: Bearer {token}` header on all requests requiring authentication.

**GR02** - Passwords must contain at least 8 characters, including: an uppercase letter, a lowercase letter, a number, and a special character.

**GR03** - The initial admin user is automatically created on system startup with credentials defined by environment variables (`ADMIN_EMAIL`, `ADMIN_PASSWORD`).

**GR04** - All entity IDs are auto-generated UUIDs.

**GR05** - Dates and times are stored in UTC timezone.

**GR06** - Required fields return HTTP 400 with a list of invalid fields when not provided.

**GR07** - Requests for non-existent resources return HTTP 404.

**GR08** - Unauthenticated requests to protected endpoints return HTTP 401. Requests with insufficient roles return HTTP 403.

**GR09** - The system has a configurable limit of `ACTIVE` users, stored in `app_config.MAX_ACTIVE_USERS`. Once the limit is reached, new registrations are stored as `QUEUED`.

**GR10** - Users with status `QUEUED` do not receive a JWT token during registration and cannot log in until they are promoted to `ACTIVE`.

---

## 5. Authentication Module

**Base endpoint:** `/auth`

**Access:** Public

---

### 5.1 Login

**Endpoint:** `POST /auth/login`

**Description:** Authenticates the user with email and password. Returns a JWT token and user data.

**Input fields (LoginRequestDTO):**

| Field    | Type   | Required | Validation               |
|----------|--------|----------|--------------------------|
| email    | String | Yes      | Valid email format       |
| password | String | Yes      | Minimum 8 characters     |

**Response (AuthResponseDTO):**

| Field | Type            | Description             |
|-------|-----------------|-------------------------|
| token | String          | JWT token               |
| user  | UserResponseDTO | Data of the logged user |

**Rules:**

**BR-AUTH-01** - The email must be registered in the system. If not found, the system returns HTTP 401 with an invalid credentials message.

**BR-AUTH-02** - The password is checked against the stored BCrypt hash. Incorrect passwords return HTTP 401.

**BR-AUTH-03** - The generated token is valid for 2 hours from the login moment.

**BR-AUTH-04** - If the user status is `QUEUED`, the system returns HTTP 403 with `error = ACCOUNT_QUEUED`, a user-facing message, and `queuePosition`.

**BR-AUTH-05** - If the user status is `DISABLED`, the system returns HTTP 403 with `error = ACCOUNT_DISABLED`.

---

### 5.2 Registration

**Endpoint:** `POST /auth/register`

**Description:** Registers a new user with the ROLE_USER profile. If there is available capacity, the user becomes `ACTIVE` and receives a JWT token. If the active user limit is already reached, the user is stored as `QUEUED` and no JWT is issued.

**Input fields (RegisterRequestDTO):**

| Field    | Type   | Required | Validation                                                        |
|----------|--------|----------|-------------------------------------------------------------------|
| name     | String | Yes      | Not blank                                                         |
| email    | String | Yes      | Valid email format                                                |
| password | String | Yes      | Min 8 characters, with uppercase, lowercase, number and special  |
| phone    | String | Yes      | Format (XX) XXXXX-XXXX                                           |

**Response:** `RegistrationResponseDTO`.

**Response fields (RegistrationResponseDTO):**

| Field         | Type            | Description |
|---------------|-----------------|-------------|
| token         | String          | JWT token when the user is created as `ACTIVE`; `null` when queued |
| user          | UserResponseDTO | User data, including `status` and `queuePosition` |
| queued        | Boolean         | Indicates whether the user entered the waitlist |
| queuePosition | Integer         | Waitlist position when `queued = true` |
| message       | String          | UI-friendly status message |

**Rules:**

**BR-REG-01** - Duplicate emails are not allowed. The system returns HTTP 409 if the email is already registered.

**BR-REG-02** - Passwords are never stored in plain text. BCrypt hashing is applied before persisting.

**BR-REG-03** - After registration, the system attempts to send a welcome message via the WhatsApp Business API (Meta) to the provided phone number.

**BR-REG-04** - The phone number is normalized to the international standard with country code 55 (Brazil) before sending.

**BR-REG-05** - If `count(status = ACTIVE) < MAX_ACTIVE_USERS`, the system saves the user as `ACTIVE`, generates a JWT token, and returns HTTP 201.

**BR-REG-06** - If `count(status = ACTIVE) >= MAX_ACTIVE_USERS`, the system saves the user as `QUEUED`, assigns `queuePosition`, does not generate a token, and returns HTTP 202.

**BR-REG-07** - If the email already exists with status `QUEUED`, the system returns HTTP 409 and informs the current waitlist position in the error message.

**BR-REG-08** - Queue, activation, and removal emails are currently log-only placeholders in the backend. Real email delivery is not implemented yet.

**Example response - active registration (201 Created):**

```json
{
  "token": "jwt-token",
  "user": {
    "id": "uuid",
    "name": "Maria Silva",
    "email": "maria@gmail.com",
    "phone": "(11) 98765-4321",
    "status": "ACTIVE",
    "queuePosition": null,
    "profileComplete": false,
    "roles": ["ROLE_USER"]
  },
  "queued": false,
  "queuePosition": null,
  "message": null
}
```

**Example response - queued registration (202 Accepted):**

```json
{
  "token": null,
  "user": {
    "id": "uuid",
    "name": "Maria Silva",
    "email": "maria@gmail.com",
    "phone": "(11) 98765-4321",
    "status": "QUEUED",
    "queuePosition": 5,
    "profileComplete": false,
    "roles": ["ROLE_USER"]
  },
  "queued": true,
  "queuePosition": 5,
  "message": "Limite de usuarios atingido. Voce foi adicionado a fila de espera na posicao #5."
}
```

---

### 5.3 Registration Status

**Endpoint:** `GET /auth/registration-status`

**Description:** Returns the current public registration status, including active user count, configured limit, and waitlist size.

**Response (RegistrationStatusDTO):**

| Field       | Type    | Description |
|-------------|---------|-------------|
| open        | Boolean | `true` when there is still room for new `ACTIVE` users |
| activeUsers | Long    | Current number of `ACTIVE` users |
| limit       | Integer | Current active user limit |
| queueSize   | Long    | Current number of queued users |

---

### 5.4 Waitlist Cancellation

**Endpoint:** `DELETE /auth/waitlist/me?email={email}`

**Description:** Removes a `QUEUED` user from the waitlist by email.

**Response:** `WaitlistMessageDTO` with `Voce foi removido da fila de espera.`

---

### 5.5 Authenticated User Data

**Endpoint:** `GET /auth/me`

**Description:** Returns the data of the currently authenticated user.

**Access:** Requires authentication (ROLE_USER or ROLE_ADMIN).

**Response:** UserResponseDTO with id, name, email, taxId, phone, address, photo, profileComplete, status, queuePosition, roles.

---

## 5.6 Waitlist Administration

**Base endpoint:** `/admin`

**Access:** Requires `ROLE_ADMIN`

### 5.6.1 List waitlist

**Endpoint:** `GET /admin/waitlist`

**Description:** Returns the current limit, active user count, and the queue ordered by position.

### 5.6.2 Manually activate queued user

**Endpoint:** `POST /admin/waitlist/{userId}/activate`

**Description:** Promotes a `QUEUED` user to `ACTIVE` when a slot is available.

### 5.6.3 Remove queued user

**Endpoint:** `DELETE /admin/waitlist/{userId}`

**Description:** Removes a queued user and recalculates the remaining positions.

### 5.6.4 Update active user limit

**Endpoint:** `PUT /admin/config/max-users`

**Description:** Updates the `ACTIVE` user limit. If the new limit opens capacity, queued users are promoted automatically.

---

## 6. User Module

**Base endpoint:** `/users`

---

### 6.1 Get User by ID

**Endpoint:** `GET /users/{id}`

**Access:** Public.

**Response:** UserDTO with id, name, email, roles, taxId, phone, address.

---

### 6.2 Create User

**Endpoint:** `POST /users`

**Access:** Public.

**Input fields (UserRequestDTO):**

| Field   | Type    | Required | Validation                              |
|---------|---------|----------|-----------------------------------------|
| name    | String  | Yes      | Not blank                               |
| email   | String  | Yes      | Valid email format                      |
| password| String  | Yes      | Minimum 8 characters, complexity rules  |
| taxId   | String  | No       | Format XXX.XXX.XXX-XX (CPF)            |
| phone   | String  | Yes      | Format (XX) XXXXX-XXXX                 |
| address | Address | Yes      | Street, neighborhood, city, state, zip  |

**Rules:**

**BR-USR-01** - Email is unique per user. Duplicates return HTTP 409.

**BR-USR-02** - Tax ID (taxId/CPF) is optional. When provided, it must be unique per user. Duplicates return HTTP 409.

**BR-USR-03** - New users are automatically assigned the ROLE_USER profile.

---

### 6.3 Update User

**Endpoint:** `PUT /users/{id}`

**Access:** Requires authentication.

**Rules:**

**BR-USR-04** - Only provided fields are updated (partial update).

---

### 6.4 Delete User

**Endpoint:** `DELETE /users/{id}`

**Access:** Requires authentication.

---

## 7. Video Module

**Base endpoint:** `/videos` (public reads) | `/admin/videos` (writes, ROLE_ADMIN)

---

### 7.1 List Videos

**Endpoint:** `GET /videos`

**Access:** Public.

**Response:** List of VideoDTO with id, title, description, url, cover, category, comments, commentCount, views, watchTime, recipe, nutritional info (protein, carbs, fat, fiber, calories), likesCount, favorited.

---

### 7.2 Get Video by ID

**Endpoint:** `GET /videos/{id}`

**Access:** Public.

---

### 7.3 Register View

**Endpoint:** `PATCH /videos/{id}/view`

**Access:** Requires authentication.

**Rules:**

**BR-VID-01** - Each call increments the video view counter by 1.

---

### 7.4 Most Watched Videos

**Endpoint:** `GET /videos/most-viewed?limit=10`

**Access:** Public.

**Response:** List of VideoDTO ordered by views descending, limited by the `limit` parameter (default 10).

---

### 7.5 Least Watched Videos

**Endpoint:** `GET /videos/least-viewed?limit=10`

**Access:** Public.

---

### 7.6 Views by Category

**Endpoint:** `GET /videos/views-by-category`

**Access:** Public.

**Response:** Map with category name and total views.

---

### 7.7 Create Video (Admin)

**Endpoint:** `POST /admin/videos`

**Access:** ROLE_ADMIN.

**Input fields (VideoRequestDTO):**

| Field      | Type   | Required | Description                  |
|------------|--------|----------|------------------------------|
| title      | String | Yes      | Video title                  |
| description| String | Yes      | Full description             |
| url        | String | Yes      | Video file URL               |
| cover      | String | Yes      | Thumbnail image URL          |
| categoryId | UUID   | Yes      | ID of the associated category|
| recipe     | String | No       | Related recipe               |
| protein    | Double | No       | Protein (g)                  |
| carbs      | Double | No       | Carbohydrates (g)            |
| fat        | Double | No       | Fat (g)                      |
| fiber      | Double | No       | Fiber (g)                    |
| calories   | Double | No       | Calories (kcal)              |

**Rules:**

**BR-VID-02** - When a video is created, the system automatically generates a VIDEO type notification for all users.

**BR-VID-03** - The provided category must already exist in the system.

---

### 7.8 Update Video (Admin)

**Endpoint:** `PUT /admin/videos/{id}`

**Access:** ROLE_ADMIN.

**Rules:**

**BR-VID-04** - Only provided fields are updated (partial update).

---

### 7.9 Delete Video (Admin)

**Endpoint:** `DELETE /admin/videos/{id}`

**Access:** ROLE_ADMIN.

---

## 8. Menu (Meal Plan) Module

**Base endpoint:** `/menus` (public reads) | `/admin/menus` (writes, ROLE_ADMIN)

---

### 8.1 List Menus

**Endpoint:** `GET /menus`

**Access:** Public.

**Response:** List of MenuDTO with id, title, description, cover, category, recipe, nutritionistTips, nutritional info.

---

### 8.2 Get Menu by ID

**Endpoint:** `GET /menus/{id}`

**Access:** Public.

---

### 8.3 Create Menu (Admin)

**Endpoint:** `POST /admin/menus`

**Access:** ROLE_ADMIN.

**Input fields (MenuRequestDTO):**

| Field           | Type   | Required | Description                |
|-----------------|--------|----------|----------------------------|
| title           | String | Yes      | Menu title                 |
| description     | String | Yes      | Full description           |
| cover           | String | No       | Thumbnail image URL        |
| categoryId      | UUID   | Yes      | ID of associated category  |
| recipe          | String | No       | Menu recipe                |
| nutritionistTips| String | No       | Nutritionist tips          |
| protein         | Double | No       | Protein (g)                |
| carbs           | Double | No       | Carbohydrates (g)          |
| fat             | Double | No       | Fat (g)                    |
| fiber           | Double | No       | Fiber (g)                  |
| calories        | Double | No       | Calories (kcal)            |

**Rules:**

**BR-MNU-01** - When a menu is created, the system automatically generates a MENU type notification for all users.

---

### 8.4 Update Menu (Admin)

**Endpoint:** `PUT /admin/menus/{id}`

**Access:** ROLE_ADMIN.

---

### 8.5 Delete Menu (Admin)

**Endpoint:** `DELETE /admin/menus/{id}`

**Access:** ROLE_ADMIN.

---

## 9. Category Module

**Base endpoint:** `/categories`

---

### 9.1 List Categories

**Endpoint:** `GET /categories?type=VIDEO|MENU`

**Access:** Public.

**Parameters:**

| Parameter | Type         | Description                         |
|-----------|--------------|-------------------------------------|
| type      | CategoryType | Filters by VIDEO or MENU (required) |

---

### 9.2 Create, Update and Delete Categories

**Endpoints:** `POST /categories`, `PUT /categories/{id}`, `DELETE /categories/{id}`

**Access:** ROLE_ADMIN.

**Rules:**

**BR-CAT-01** - Two categories with the same name and type are not allowed.

---

## 10. Comment Module

**Base endpoint:** `/comments`

---

### 10.1 List Comments by Video

**Endpoint:** `GET /comments/video/{videoId}`

**Access:** Public.

**Response:** List of CommentResponseDTO with id, text, date (ISO 8601), user: { id, name }.

---

### 10.2 List Comments by User

**Endpoint:** `GET /comments/user/{userId}`

**Access:** Public.

---

### 10.3 Create Comment

**Endpoint:** `POST /comments`

**Access:** Requires authentication (ROLE_USER).

**Input fields (CreateCommentDTO):**

| Field   | Type   | Required | Validation                                    |
|---------|--------|----------|-----------------------------------------------|
| text    | String | Yes      | Not blank. Message: "Comment text is required."|
| videoId | UUID   | Yes      | ID of an existing video                       |

**Rules:**

**BR-COM-01** - The same user cannot post identical comment text on the same video (duplicate protection).

---

### 10.4 Delete Comment

**Endpoint:** `DELETE /comments/{commentId}`

**Access:** ROLE_ADMIN.

---

## 11. Favorites Module

**Base endpoint:** `/favorites`

**Access:** Requires authentication for all endpoints.

---

### 11.1 Toggle Favorite

**Endpoint:** `POST /favorites/{type}/{itemId}`

**Description:** Adds the item to favorites if not already favorited. Removes it if already favorited. Acts as a like/unlike button.

**Path parameters:**

| Parameter | Type                | Description       |
|-----------|---------------------|-------------------|
| type      | FavoriteContentType | VIDEO or MENU     |
| itemId    | String              | UUID of the item  |

**Response:** `{ favorited: boolean, itemId: string, itemType: string }`

**Rules:**

**BR-FAV-01** - A user cannot favorite the same item twice. The second call removes the favorite.

---

### 11.2 List All Favorites

**Endpoint:** `GET /favorites`

**Response:** List of FavoriteDTO with itemId, itemType, createdAt.

---

### 11.3 List Favorites by Type

**Endpoint:** `GET /favorites/{type}`

---

### 11.4 Check Favorite Status

**Endpoint:** `GET /favorites/{type}/{itemId}/status`

**Response:** `{ favorited: boolean, likesCount: long }`

---

## 12. Notification Module

**Base endpoint:** `/notifications`

**Access:** Requires authentication for all endpoints.

---

### 12.1 List Notifications

**Endpoint:** `GET /notifications?page=0&size=20`

**Description:** Returns paginated notifications. Each notification indicates whether it has been read based on the date the user last accessed notifications.

**Response (NotificationsPageDTO):**

| Field   | Type                      | Description                       |
|---------|---------------------------|-----------------------------------|
| items   | List<NotificationItemDTO> | List of notifications for the page|
| hasMore | boolean                   | Indicates if there are more pages |

**NotificationItemDTO:**

| Field     | Type             | Description                                |
|-----------|------------------|--------------------------------------------|
| id        | UUID             | Notification identifier                    |
| type      | NotificationType | VIDEO or MENU                              |
| title     | String           | Notification title                         |
| contentId | UUID             | ID of the related content                  |
| createdAt | Instant          | Creation date and time                     |
| read      | boolean          | True if created before user's last read    |

**Rules:**

**BR-NOT-01** - A notification is considered read if it was created before the user's `notificationsLastReadAt` field value.

**BR-NOT-02** - Notifications are created automatically when a new video or menu is published.

---

### 12.2 Unread Count

**Endpoint:** `GET /notifications/unread-count`

**Response:** `{ unreadCount: long }`

---

### 12.3 Mark All as Read

**Endpoint:** `POST /notifications/mark-all-read`

**Description:** Updates the user's `notificationsLastReadAt` field to the current moment. The next notification query will return `unreadCount = 0`.

---

## 13. Analytics Module (Admin)

**Access:** ROLE_ADMIN for all endpoints.

---

### 13.1 Video Analytics

**Base endpoint:** `/admin/videos`

| Endpoint                                            | Description                                     |
|-----------------------------------------------------|-------------------------------------------------|
| `GET /admin/videos/most-viewed?limit=10`            | Most watched videos                             |
| `GET /admin/videos/least-viewed?limit=10`           | Least watched videos                            |
| `GET /admin/videos/views-by-category`               | Total views grouped by category                 |
| `GET /admin/videos/{videoId}/tempo-medio-assistido` | Average watch time for a video                  |
| `GET /admin/videos/mais-comentados?limit=10`        | Videos with the most comments                   |

---

### 13.2 Comment Analytics

**Base endpoint:** `/admin/comentarios`

| Endpoint                                                | Description                                  |
|---------------------------------------------------------|----------------------------------------------|
| `GET /admin/comentarios/quantidade/video/{videoId}`     | Number of comments on a video                |
| `GET /admin/comentarios/usuarios/video/{videoId}`       | Names of users who commented on a video      |
| `GET /admin/comentarios/total`                          | Total comments across the entire platform    |
| `GET /admin/comentarios/total-por-video`                | Total comments grouped by video              |

---

## 14. WhatsApp Business API Integration

**Provider:** Meta (Facebook) Graph API v22.0

**Webhook endpoint:** `GET /whatsapp/webhook`, `POST /whatsapp/webhook` (public, Meta verification)

**Rules:**

**BR-WPP-01** - Message sending only occurs when the `whatsapp.enabled=true` property is set. In development mode, messages are simulated in the console log.

**BR-WPP-02** - The destination number is normalized: non-numeric characters are removed and country code 55 (Brazil) is added if not already present.

**BR-WPP-03** - Message content is defined by a template approved in Meta's WhatsApp Manager, not by the application code.

**BR-WPP-04** - If sending fails (HTTP error from Meta API), the message status is set to `SEND_ERROR` and the error is logged. The user registration is not affected.

---

## 15. Data Model

### Entity: User

| Field                   | Type          | Required | Description                           |
|-------------------------|---------------|----------|---------------------------------------|
| id                      | UUID          | Yes      | Unique identifier (auto-generated)    |
| name                    | String        | Yes      | Full name (unique)                    |
| email                   | String        | Yes      | Email address (unique)                |
| password                | String        | Yes      | Encrypted password (BCrypt)           |
| taxId                   | String (14)   | No       | CPF format XXX.XXX.XXX-XX (unique)    |
| phone                   | String (15)   | Yes      | Phone format (XX) XXXXX-XXXX          |
| address                 | Address       | No       | Embedded address                      |
| photo                   | String        | No       | Profile picture URL                   |
| profileComplete         | boolean       | -        | Indicates if profile is complete      |
| createdAt               | LocalDateTime | -        | Creation date (auto-populated)        |
| updatedAt               | LocalDateTime | -        | Update date (auto-populated)          |
| notificationsLastReadAt | Instant       | No       | Last time user read notifications     |
| roles                   | List<Role>    | -        | Access profiles (ManyToMany)          |

### Entity: Video

| Field       | Type          | Required | Description              |
|-------------|---------------|----------|--------------------------|
| id          | UUID          | Yes      | Unique identifier        |
| title       | String (150)  | Yes      | Title                    |
| description | String (text) | Yes      | Description              |
| url         | String        | Yes      | Video URL                |
| cover       | String        | No       | Thumbnail image URL      |
| category    | Category      | Yes      | Category (ManyToOne)     |
| views       | int           | -        | View counter             |
| watchTime   | double        | -        | Total watch time         |
| recipe      | String (text) | No       | Recipe                   |
| protein     | Double        | No       | Protein (g)              |
| carbs       | Double        | No       | Carbohydrates (g)        |
| fat         | Double        | No       | Fat (g)                  |
| fiber       | Double        | No       | Fiber (g)                |
| calories    | Double        | No       | Calories (kcal)          |
| likesCount  | int           | -        | Total likes              |

### Entity: Notification

| Field     | Type             | Description                                 |
|-----------|------------------|---------------------------------------------|
| id        | UUID             | Unique identifier (auto-generated if null)  |
| type      | NotificationType | VIDEO or MENU                               |
| title     | String (255)     | Notification title                          |
| contentId | UUID             | ID of the related content                   |
| createdAt | Instant          | Creation date (auto-populated)              |

---

## 16. CI/CD Pipeline

The project uses GitHub Actions for continuous integration and delivery. The pipeline is triggered on every push to the `main` branch or `feat/*` branches.

### Flow

```
push main → test → docker (build/push Docker Hub) → deploy (Elastic Beanstalk)
```

### Jobs

| Job    | Trigger              | Description                                                       |
|--------|----------------------|-------------------------------------------------------------------|
| test   | push main / feat/*   | Runs all tests with Maven (./mvnw test)                           |
| docker | after test (main)    | Builds Docker image and pushes to Docker Hub with SHA tag         |
| deploy | after docker (main)  | Generates the EB bundle with Dockerrun.aws.json + .platform, uploads to S3, updates EB env |

### Elastic Beanstalk Deploy

EB uses `Dockerrun.aws.json` to know which Docker image to run. On each deploy:

1. The job generates the file with image `{DOCKERHUB_USERNAME}/vidalongaflix:{SHA}`
2. The `deploy.zip` bundle is created with `Dockerrun.aws.json` and `.platform/`
3. The `.platform/hooks/predeploy/01_create_db.sh` hook reaches the EB instance and ensures the database from `DB_URL` exists before the container starts
4. The bundle is uploaded to the EB S3 bucket
5. A new "application version" is created in EB
6. The environment is updated to use the new version

**Required GitHub Secrets:**

| Secret                | Description                                     |
|-----------------------|-------------------------------------------------|
| `DOCKERHUB_USERNAME`  | Docker Hub username                             |
| `DOCKERHUB_TOKEN`     | Docker Hub access token                         |
| `AWS_ACCESS_KEY_ID`   | IAM access key for user vidalongaflix-ci        |
| `AWS_SECRET_ACCESS_KEY`| IAM secret key                                 |

**Elastic Beanstalk Configuration:**

| Parameter        | Value                               |
|------------------|-------------------------------------|
| Application name | vidalongaflix-backend               |
| Environment name | Vidalongaflix-backend-env           |
| Region           | us-east-2                           |
| Container port   | 8090                                |

### Rationale for Job Order

The `test → docker → deploy` order may seem inverted compared to the industry standard, where the build typically happens before tests. In this project, the unit tests (Maven) do not depend on a running application or a Docker image — they simulate the application in memory (H2). Therefore, it makes sense to validate the code first and only then build and publish the image.

In projects with end-to-end tests (that access a fully running application), the flow would be:

```
build image → start container → run e2e tests → push Docker Hub → deploy
```

### IAM Permissions (vidalongaflix-ci user)

Create in AWS Console → IAM → Users → `vidalongaflix-ci` → Attach policies → JSON:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "elasticbeanstalk:CreateApplicationVersion",
        "elasticbeanstalk:UpdateEnvironment",
        "elasticbeanstalk:DescribeEnvironments",
        "elasticbeanstalk:DescribeApplicationVersions",
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": "*"
    }
  ]
}
```

### Operational Steps to Activate CD

| # | Step | Status |
|---|---|---|
| 1 | Create IAM user `vidalongaflix-ci` in AWS Console with the policy above | ✅ Done |
| 2 | Generate Access Key for the user (IAM → Security credentials → Create access key) | ✅ Done |
| 3 | Register `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` in GitHub Environment `production` | ✅ Done |
| 4 | Confirm names in AWS Console: `application_name` and `environment_name` in `ci.yml` | ✅ Done |
| 5 | Create GitHub Environment `production` with required reviewer | ✅ Done |

### GitHub Environment: production

The `deploy` job references the `production` environment in `ci.yml`. This adds a manual approval gate before any deploy reaches production.

**How to configure (one time only):**

```
GitHub → Settings → Environments → New environment
Name: production
```

Inside the `production` environment:

- Check **"Required reviewers"** and add your user → requires manual approval before the deploy runs
- Optionally move the AWS secrets to the environment scope (recommended):
  - `AWS_ACCESS_KEY_ID`
  - `AWS_SECRET_ACCESS_KEY`

**Flow after configuration:**

```
push main → test ✅ → docker ✅ → deploy ⏸ waiting for approval
                                          ↓ you receive an email notification
                                          ↓ you click "Approve" on GitHub
                                          ✅ deploy runs on Elastic Beanstalk
```

**Benefits:**

| Benefit | Description |
|---|---|
| Deploy control | No code goes to production without conscious approval |
| Auditable history | GitHub records who approved, when, and which commit was deployed |
| Isolated secrets | AWS credentials are scoped to the environment, not mixed with other secrets |
| Protection against accidental deploy | Prevents deploying code that passed tests but has a logical issue |

---

## 17. HTTP Response Codes

| Code | Description                                      |
|------|--------------------------------------------------|
| 200  | Request processed successfully                   |
| 201  | Resource created successfully                    |
| 204  | Operation completed with no response body        |
| 400  | Invalid input data or missing required fields    |
| 401  | Not authenticated or invalid credentials         |
| 403  | Insufficient permission for the requested resource|
| 404  | Resource not found                               |
| 409  | Conflict: duplicate resource                     |
| 500  | Internal server error                            |
