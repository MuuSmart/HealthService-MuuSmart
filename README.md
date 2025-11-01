# Health Service (MuuSmart)

Microservicio para gestionar registros de salud de animales (diagnóstico, tratamiento, vacunas, notas y penalidad de salud). Expone una API REST segura con JWT y control de acceso por roles.

- Lenguaje/Runtime: Java 17, Spring Boot 3
- Persistencia: Spring Data JPA + MySQL
- Seguridad: Spring Security + JWT (HS256)


## Tabla de contenido
- [Arquitectura y stack](#arquitectura-y-stack)
- [Modelo de datos](#modelo-de-datos)
- [Seguridad (JWT y roles)](#seguridad-jwt-y-roles)
  - [Formato del token requerido](#formato-del-token-requerido)
  - [Generar un token válido (ejemplo Java/JJWT)](#generar-un-token-válido-ejemplo-javajjwt)
  - [Diferencia 401 vs 403](#diferencia-401-vs-403)
- [Configuración](#configuración)
- [Ejecución](#ejecución)
- [API REST](#api-rest)
  - [Crear registro](#crear-registro)
  - [Obtener registro por ID](#obtener-registro-por-id)
  - [Listar por animal](#listar-por-animal)
  - [Listar visibles para el usuario](#listar-visibles-para-el-usuario)
  - [Actualizar registro](#actualizar-registro)
  - [Eliminar registro](#eliminar-registro)
  - [Calcular penalidad de salud](#calcular-penalidad-de-salud)
- [Solución de problemas (401/403)](#solución-de-problemas-401403)
- [Mejoras sugeridas](#mejoras-sugeridas)

---

## Arquitectura y stack

- Spring Boot Web para exponer endpoints REST
- Spring Security con filtro `JwtAuthenticationFilter` para autenticar con JWT
- Autorización por anotaciones `@PreAuthorize` en el controlador
- JPA/Hibernate con MySQL. Repositorio `HealthRecordRepository` para CRUD.

Archivos clave:
- `security/SecurityConfig.java`: Habilita seguridad, exige autenticación para todo, añade el filtro JWT.
- `security/JwtAuthenticationFilter.java`: Lee el header `Authorization: Bearer <token>`, valida y construye el `Authentication` con autoridades desde el claim `roles`.
- `security/JwtUtil.java`: Valida y parsea JWT (HS256). IMPORTANTE: usa un secreto fijo dentro del código.
- `controller/HealthController.java`: Endpoints con `@PreAuthorize("hasAnyRole('USER','ADMIN')")`.
- `service/HealthService.java`: Lógica de negocio y validación de propietario vs admin.
- `model/HealthRecord.java`: Entidad JPA `health_records`.

## Modelo de datos

Entidad `HealthRecord`:
- id (Long, PK)
- animalId (Long)
- diagnosis (String)
- treatment (String)
- vaccine (String)
- date (LocalDate)
- penalty (Double)
- notes (String)
- ownerUsername (String)

DTO `HealthRequest` (entrada API) exige `animalId` y valida `penalty >= 0`.


## Seguridad (JWT y roles)

El servicio exige JWT para TODOS los endpoints. El header debe ser:

```
Authorization: Bearer <tu_token_jwt>
```

Las anotaciones `@PreAuthorize("hasAnyRole('USER','ADMIN')")` requieren que el usuario tenga alguno de los roles:
- `ROLE_USER`
- `ROLE_ADMIN`

El filtro extrae roles del claim `roles` (lista) o `role` (string/coma-separado) y los carga como autoridades exactamente como vienen. Por ello, para que `hasAnyRole('USER','ADMIN')` funcione, tus autoridades deben contener `ROLE_USER` o `ROLE_ADMIN` (con prefijo `ROLE_`).

### Formato del token requerido

- Algoritmo: HS256
- Claims mínimos:
  - `sub`: nombre de usuario (String)
  - `roles`: lista o string con `ROLE_USER` o `ROLE_ADMIN`
  - `iat` y `exp`: fechas de emisión y expiración
- Debe estar firmado con el MISMO secreto que usa el servicio.

Secreto actual (codificado en `JwtUtil`):
```
ReplaceThisSecretWithAStrongKeyForProduction
```
Si generas el token con otro secreto, la firma fallará y el servicio ignorará el token.

### Generar un token válido (ejemplo Java/JJWT)

Ejemplo para generar un token compatible con este servicio usando la misma librería JJWT:

```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.List;

public class TokenGenerator {
  public static void main(String[] args) {
    String secret = "ReplaceThisSecretWithAStrongKeyForProduction"; // Debe coincidir con JwtUtil
    long now = System.currentTimeMillis();
    String jwt = Jwts.builder()
      .setSubject("Maycol")
      .claim("roles", List.of("ROLE_USER"))
      .setIssuedAt(new Date(now))
      .setExpiration(new Date(now + 3600_000)) // 1 hora
      .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
      .compact();
    System.out.println(jwt);
  }
}
```

Con ese token podrás invocar los endpoints. Ejemplo cURL (recuerda anteponer `Bearer`):

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/health
```

### Diferencia 401 vs 403
- 401 Unauthorized: El request no está autenticado (token ausente, malformado, firma inválida o expirado). El filtro no establece un `Authentication` válido.
- 403 Forbidden: Estás autenticado pero no autorizado (roles no válidos para `@PreAuthorize`, por ejemplo autoridad `USER` sin prefijo `ROLE_`).


## Configuración

Editar `src/main/resources/application.properties`:

```
spring.datasource.url=jdbc:mysql://localhost:3306/health_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=<tu_usuario_mysql>
spring.datasource.password=<tu_password_mysql>
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
server.port=8083
```

Requisitos locales:
- Java 17
- Maven 3.8+
- MySQL 8 en ejecución

Nota: El secreto JWT está HARD-CODED en `JwtUtil`. En producción, muévelo a variables de entorno o `application.properties` y cámbialo por uno robusto.


## Ejecución

Compilar y ejecutar:

```bash
mvn clean package
mvn spring-boot:run
```

El servicio quedará en `http://localhost:8083`.


## API REST

Todas las rutas requieren `Authorization: Bearer <token>` con `ROLE_USER` o `ROLE_ADMIN`.

### Crear registro
- POST `/health`
- Body (JSON):
```json
{
  "animalId": 1,
  "diagnosis": "Fiebre aftosa",
  "treatment": "Antiinflamatorio",
  "vaccine": "Aftosa-2025",
  "notes": "Observación leve",
  "date": "2025-11-01",
  "penalty": 0.1
}
```
- Respuesta: `200 OK` con el `HealthRecord` creado. El `ownerUsername` se toma del `sub` del token.
- cURL:
```bash
curl -X POST http://localhost:8083/health \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "animalId": 1,
    "diagnosis": "Fiebre aftosa",
    "treatment": "Antiinflamatorio",
    "vaccine": "Aftosa-2025",
    "notes": "Observación leve",
    "date": "2025-11-01",
    "penalty": 0.1
  }'
```

### Obtener registro por ID
- GET `/health/{id}`
- Autorización: dueño del registro o `ROLE_ADMIN`.
- cURL:
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/health/1
```

### Listar por animal
- GET `/health/animal/{animalId}`
- Admin ve todos; un usuario normal solo si todos los registros de ese animal le pertenecen.
- cURL:
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/health/animal/1
```

### Listar visibles para el usuario
- GET `/health`
- Admin: todos. Usuario: solo sus registros.
- cURL:
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/health
```

### Actualizar registro
- PUT `/health/{id}`
- Autorización: dueño o admin. Solo admin puede reasignar `ownerUsername` (no viene en el DTO por defecto).
- Body igual que `HealthRequest` (sin `animalId` si no deseas cambiarlo; el código actual no lo actualiza en update).
- cURL:
```bash
curl -X PUT http://localhost:8083/health/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "diagnosis": "Recuperado",
    "treatment": "Reposo",
    "vaccine": "Refuerzo-2025",
    "notes": "Sin síntomas",
    "date": "2025-11-05",
    "penalty": 0.0,
    "animalId": 1
  }'
```

### Eliminar registro
- DELETE `/health/{id}`
- Autorización: dueño o admin.
- cURL:
```bash
curl -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8083/health/1
```

### Calcular penalidad de salud
- GET `/health/condition/{animalId}`
- Suma de `penalty` de los registros del animal.
- cURL:
```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/health/condition/1
```


## Solución de problemas (401/403)

- 401 Unauthorized:
  - Falta el header `Authorization` o no empieza con `Bearer `.
  - Token expirado (`exp`) o reloj del sistema desfasado.
  - Firma inválida: generaste el token con un secreto diferente a `ReplaceThisSecretWithAStrongKeyForProduction`.
- 403 Forbidden:
  - El token fue aceptado pero no tienes rol requerido. Asegúrate de que el claim `roles` contenga `ROLE_USER` o `ROLE_ADMIN` (con prefijo `ROLE_`). Si pones `USER` sin prefijo, fallará la autorización.

Tu token compartido en el comentario tiene `roles: ["ROLE_USER"]`, lo cual está bien; si obtienes 403, casi siempre es porque:
1) El servicio no está leyendo ese claim (clave distinta) o
2) Tus autoridades quedaron sin prefijo (`USER`), o
3) Estás accediendo a un recurso que no te pertenece (endpoints que validan dueño vs admin) y tu usuario no es el owner.

Verifica también:
- El `sub` del token (p. ej., `Maycol`) será el `ownerUsername` al crear registros; para leer/actualizar/eliminar, ese `sub` debe coincidir con el `ownerUsername` del registro o debes ser `ROLE_ADMIN`.


## Mejoras sugeridas
- Externalizar el secreto JWT a `application.properties` o variables de entorno.
- Añadir OpenAPI/Swagger para documentación interactiva.
- Manejo de excepciones con `@ControllerAdvice` para respuestas de error consistentes.
- Tests unitarios/integración para reglas de autorización y servicio.

