# **Bank Cards API**

**Банковское приложение** с JWT авторизацией, шифрованием номеров карт и Role-Based Access Control (RBAC).

***

##  **Быстрый старт (30 секунд)**

```bash
git clone https://github.com/mmarkov100/effective_bank_test
cd bank
./start.bat # Powershell
start.bat # для linux и mac
```

Готово! Открой:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health**: http://localhost:8080/actuator/health

##  **Запуск тестов**
```bash
mvn clean test
```

***

##  **Аккаунты по умолчанию**

| Роль | Username | Password |
|------|----------|----------|
| **ADMIN** | `admin` | `admin123` |
| **USER** | Создай через `/auth/register` | - |

***
##  **Структура проекта**

```
bank/
├── src/main/java/com/effective/bank/
│   ├── controller/     # REST API
│   ├── dto/           # Data Transfer Objects
│   ├── entity/        # JPA Entities
│   ├── service/       # Business Logic
│   ├── repository/    # Spring Data JPA
│   ├── security/      # JWT + RBAC
│   └── util/          # CardSecurityUtil (AES)
├── db/changelog/      # Liquibase миграции
├── docker-compose.yml
├── start.bat          # Запуск
├── restart.bat        # Перезапуск
├── reset.bat          # Полная очистка
└── logs.bat           # Логи
```

***

##  **Управление сервисом**

| Скрипт | Действие | БД |
|--------|----------|----|
| `start.bat` | **Первый запуск** | ✅ Создаёт |
| `restart.bat` | **Быстрый перезапуск** | ✅ Сохраняет |
| `reset.bat` | **Полная перезагрузка** | ❌ Удаляет |
| `logs.bat` | **Просмотр логов** | - |
| `stop.bat` | **Остановка** | - |

***

##  **API Спецификация (Swagger)**

```
Authentication: POST /auth/login, /auth/register
Cards:          GET/POST/PUT/DELETE /api/cards
Transfers:      POST /api/transfers
History:        GET /api/transfers/card/{id}
Health:         GET /actuator/health
Swagger:        GET /swagger-ui.html
```

***

##  **Docker Compose**

```yaml
services:
  postgres: 
    image: postgres:15-alpine
    ports: ["5433:5432"]
  app:
    build: .
    ports: ["8080:8080"]
    depends_on:
      - postgres
```

***

##  **База данных**

```sql
-- После start.bat и reset.bat
users:
cards:
transactions:
```

***

##  **Что реализовано**

- [x] **JWT авторизация** (login/register)
- [x] **RBAC** (ADMIN/USER роли)
- [x] **Шифрование** номеров карт (AES)
- [x] **Пагинация + сортировка** карт
- [x] **Валидация** входных данных
- [x] **Liquibase** миграции
- [x] **Docker** оркестрация
- [x] **Health Checks**
- [x] **Swagger** документация
- [x] **Postman** автотесты

***

## 📱 **Тестирование через Postman**

**Имеются curl файлы** для быстрого ввода в Postman и проверки всех эндпоинтов (в папке `curl_requests`):

```
📁 curl_requests/
├── auth.curl              # Регистрация + логины
├── create_card.curl       # Создание карт (ADMIN)
├── list_cards.curl        # Список карт + пагинация
├── get_card.curl          # Карта по ID (RBAC)
├── update_card.curl       # Обновление/удаление
├── transactions.curl      # Переводы + история
├── test_security.curl     # Тесты безопасности (403)
└── swagger.curl           # Health + Swagger
```

### **🔧 Как использовать:**

1. **Создай Environment:**
```
base_url → http://localhost:8080
ADMIN_TOKEN → 
USER_TOKEN → 
USER2_TOKEN → 
```

2. **Import → Raw Text → auth.curl** → **Сохрани токены**
3. **Последовательно импортируй остальные файлы**

***

