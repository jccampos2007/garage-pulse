# Especificación Técnica de API REST - GaragePulse

Este documento detalla el diseño de la **Capa de Servicios REST API** y los correspondientes contratos HTTP requeridos por la aplicación **GaragePulse** para realizar la transición de un almacenamiento offline local (Room DB) a un sistema sincronizado basado en la nube.

La arquitectura móvil recomendada opera bajo un enfoque **Offline-First**, donde el cliente lee/escribe en su BD Room local y sincroniza de forma asíncrona hacia el backend REST empleando este catálogo de servicios.

---

## 1. Modelos de Datos en formato JSON (Data Schemas)

Para asegurar la intercomunicación entre el frontend de Android (Kotlin) y el backend REST, se deben validar y respetar los siguientes esquemas de transferencia JSON.

### 1.1. Perfil del Usuario (`UserProfile`)
Contiene los ajustes globales de las preferencias de visualización y datos generales del conductor.
```json
{
  "id": 1,
  "name": "Carlos Rodríguez",
  "email": "carlos.rod@garagepulse.app",
  "avatarUrl": "https://api.garagepulse.app/avatars/user_1.png",
  "useKm": true,
  "isPremium": false
}
```
> **Nota:** El campo `isPremium` controla el acceso a funciones avanzadas como el rastreo de telemetría en segundo plano.

### 1.2. Vehículo (`Vehicle`)
Representa cada auto o motocicleta controlado en la flota del garage.
```json
{
  "id": 1,
  "name": "2024 Chery Arauca",
  "brand": "Chery",
  "model": "Arauca",
  "year": 2024,
  "licensePlate": "AA2024XY",
  "status": "Optimal",
  "odometer": 42500.0,
  "isActive": true,
  "type": "Car",
  "photoUri": "https://lh3.googleusercontent.com/...",
  "initialKm": 42000.0,
  "initialDate": 1696104800000,
  "lastUpdatedDate": 1697104800000,
  "calculatedKpd": 15.5,
  "lastKnownLocation": "10.4806,-66.9036",
  "customIllustrationUrl": null,
  "usageType": "PARTICULAR"
}
```
> **Nota:** Los campos `initialKm`, `initialDate` y `calculatedKpd` son requeridos para la Salud Predictiva. El campo `usageType` define los perfiles de desgaste y los límites de prueba gratuita para la telemetría en caso de no ser Premium.

### 1.3. Registro de Servicio / Mantenimiento (`ServiceLog`)
Historial de cambio de consumibles, aceites, frenos o revisiones aplicadas a un vehículo específico.
```json
{
  "id": 10,
  "vehicleId": 1,
  "category": "Cambio de Aceite",
  "title": "Cambio de Aceite de Motor",
  "description": "Aceite sintético 5W-30 con filtros de aire nuevos",
  "cost": 85.00,
  "mileage": 45200.0,
  "date": 1697104800000,
  "type": "PREVENTIVO",
  "details": "Filtro de aceite, Filtro de aire"
}
```
> El campo `details` se envía como un String separado por comas (CSV). Este campo es **crítico** para el motor de *Salud Predictiva Granular*, ya que permite que el cliente móvil desglose los mantenimientos a nivel de sub-tareas individuales (Ej: evaluar independientemente el desgaste de 'Alineación' frente a 'Cambio de Llantas' dentro de la categoría 'Neumáticos').
> El campo `date` se transfiere como **Tiempo Unix en milisegundos (Long)** para garantizar compatibilidad horaria absoluta entre zonas de servidor y cliente.

---

## 2. Catálogo Completo de Endpoints REST

A continuación se enlistan los recursos expuestos por la API REST básica con sus correspondientes verbos HTTP, códigos de estado recomendados y comportamiento del servidor.

### 2.1. Autenticación y Registro (Onboarding)

#### `POST /api/auth/register`
* **Descripción:** Registra un nuevo conductor y crea simultáneamente su primer vehículo (Paso Obligatorio del onboarding guiado).
* **Cuerpo de Solicitud (JSON):**
  ```json
  {
    "name": "Carlos Rodríguez",
    "email": "carlos.rod@garagepulse.app",
    "password": "mi_password_seguro",
    "vehicleBrand": "Chery",
    "vehicleModel": "Arauca",
    "initialOdometer": 42500.0,
    "licensePlate": "AA2024XY"
  }
  ```
* **Códigos de respuesta:**
  * `201 Created`: El usuario y su vehículo fueron creados exitosamente. Retorna un JWT de acceso junto con el usuario y vehículo creados.
  * `400 Bad Request`: Formato de correo incorrecto, placa de vehículo inválida o campos incompletos.
  * `409 Conflict`: El email ingresado ya está registrado.

#### **Bypass / Modo Prueba (Skip Onboarding)**
Para demostraciones rápidas y usuarios de prueba, la interfaz permite omitir este formulario configurando el siguiente usuario predeterminado en la base de datos local y simulada:
* **Usuario:** `Usuario Beta` (`beta@garagepulse.com`)
* **Vehículo:** `Toyota Hilux` con kilometraje inicial `15,000.0 KM` y placa patente `PRUEBA`.

---

### 2.2. Gestión del Perfil del Conductor

#### `GET /api/user/profile`
* **Descripción:** Recupera la información del perfil del usuario logueado en la sesión.
* **Cabeceras obligatorias:** `Authorization: Bearer <token>`
* **Código de respuesta exitosa:** `200 OK`
* **Respuesta (JSON):** Modelo `UserProfile`

#### `PUT /api/user/profile`
* **Descripción:** Modifica los campos configurables del usuario (incluyendo las unidades métricas preferred KM/MI).
* **Cuerpo de Solicitud (JSON):**
  ```json
  {
    "name": "Carlos Rodríguez Modificado",
    "useKm": false
  }
  ```
* **Código de respuesta exitosa:** `200 OK` (retorna el objeto modificado completo).

---

### 2.3. Gestión de Vehículos

#### `GET /api/vehicles`
* **Descripción:** Obtiene todos los vehículos registrados por el usuario.
* **Código de respuesta exitosa:** `200 OK`
* **Respuesta (JSON Array):**
  ```json
  [
    { "id": 1, "name": "2024 Chery Arauca", "licensePlate": "AA2024XY", ... },
    { "id": 2, "name": "Toyota Hilux", "licensePlate": "TX-8839", ... }
  ]
  ```

#### `POST /api/vehicles`
* **Descripción:** Registra un nuevo vehículo en el garage general. Puede ser de tipo `Car` (Automóvil) o `Motorcycle` (Motocicleta).
* **Cuerpo de Solicitud (JSON):**
  ```json
  {
    "name": "Suzuki V-Strom 650",
    "brand": "Suzuki",
    "model": "V-Strom 650",
    "year": 2023,
    "licensePlate": "MC-Y482",
    "status": "Optimal",
    "odometer": 12400.0,
    "isActive": false,
    "type": "Motorcycle",
    "photoUri": "https://api.garagepulse.app/photos/suzuki_vstrom.png"
  }
  ```
* **Código de respuesta exitosa:** `201 Created`
* **Respuesta (JSON):** Modelo `Vehicle` creado e indexado con su nuevo ID numérico generado por la base de datos backend.

#### `PUT /api/vehicles/{id}`
* **Descripción:** Actualiza un vehículo específico (por ejemplo, el kilometraje actual del odómetro o vincular una nueva ilustración preset).
* **Parámetros de Ruta:** `id` -> ID numérico del vehículo.
* **Código de respuesta exitosa:** `200 OK` o `404 Not Found` (si no existe el ID del vehículo).

#### `DELETE /api/vehicles/{id}`
* **Descripción:** Remueve un vehículo del garage y cascadea la eliminación de todos sus servicios asociados en la base de datos remota.
* **Código de respuesta exitosa:** `204 No Content`

---

### 2.4. Control de Servicios de Mantenimiento

#### `GET /api/services`
* **Descripción:** Lista completa del historial acumulado de mantenimiento de toda la flota del usuario.
* **Parámetros opcionales de Consulta (Query Params):**
  - `?category=Aceite` (Filtra por tipo de servicio)
  - `?type=PREVENTIVO` (Filtra si es preventivo o una reparación correctiva)
* **Código de respuesta exitosa:** `200 OK`

#### `GET /api/vehicles/{vehicleId}/services`
* **Descripción:** Recupera la bitácora específica de un único vehículo para armar la gráfica analítica o el listado de historiales individuales.
* **Código de respuesta exitosa:** `200 OK`

#### `POST /api/services`
* **Descripción:** Publica un nuevo registro de mantenimiento completado.
* **Cuerpo de Solicitud (JSON):** Modelo `ServiceLog`.
* **Regla de Negocio en Servidor:** Al crear este registro con un kilometraje (`mileage`) mayor al kilometraje actual del odómetro del vehículo en la base de datos, el servidor debe automáticamente actualizar la columna `odometer` del vehículo correspondiente para mantener la consistencia sincrónica de las placas.
* **Código de respuesta exitosa:** `201 Created`

#### `DELETE /api/services/{id}`
* **Descripción:** Cancela y elimina una entrada histórica de mantenimiento específica.
* **Código de respuesta exitosa:** `204 No Content`

---

## 3. Lógica del Servidor y Base de Datos Backend (Diseño Físico)

Para transformar esta simulación en un servicio en la nube real, se recomiendan las siguientes directrices estructurales de desarrollo.

### 3.1. Esquema de Base de Datos Remoto (Relacional sugerido: PostgreSQL / SQLite)

```sql
-- Tabla de Perfil
CREATE TABLE user_profiles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    avatar_url TEXT,
    use_km BOOLEAN DEFAULT TRUE,
    is_premium BOOLEAN DEFAULT FALSE
);

-- Tabla de Vehículos 
CREATE TABLE vehicles (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES user_profiles(id),
    name VARCHAR(150) NOT NULL,
    brand VARCHAR(100),
    model VARCHAR(100),
    year INT NOT NULL,
    license_plate VARCHAR(30) NOT NULL,
    status VARCHAR(50) DEFAULT 'Optimal',
    odometer DOUBLE PRECISION DEFAULT 0.0,
    is_active BOOLEAN DEFAULT FALSE,
    type VARCHAR(30) DEFAULT 'Car',
    photo_uri TEXT,
    initial_km DOUBLE PRECISION,
    initial_date BIGINT,
    last_updated_date BIGINT,
    calculated_kpd DOUBLE PRECISION DEFAULT 0.0,
    last_known_location TEXT,
    custom_illustration_url TEXT,
    usage_type VARCHAR(50) DEFAULT 'PARTICULAR'
);

-- Tabla de Bitácora de Servicios
CREATE TABLE service_logs (
    id SERIAL PRIMARY KEY,
    vehicle_id INT REFERENCES vehicles(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    cost NUMERIC(10, 2) NOT NULL,
    mileage DOUBLE PRECISION NOT NULL,
    date BIGINT NOT NULL, -- Unix timestamp
    type VARCHAR(30) NOT NULL, -- 'PREVENTIVO' / 'REPARACIONES'
    details TEXT -- CSV format: 'Alineación, Cambio de Llantas'. Crucial for granular predictive health
);

-- Índices recomendados para Optimización de Consultas API
CREATE INDEX idx_vehicle_user ON vehicles(user_id);
CREATE INDEX idx_services_vehicle ON service_logs(vehicle_id);
```

### 3.2. Ruta de Backend (Ejemplo en Node.js - Express)

Un fragmento simple de la lógica requerida para sincronizar servicios y auto-actualizar el odómetro del auto:

```javascript
const express = require('express');
const router = express.Router();
const db = require('./database'); // pool de postgres

// Guardar nuevo servicio y auto-propagar kilometraje
router.post('/api/services', async (req, res) => {
    const { vehicleId, category, title, description, cost, mileage, date, type, details } = req.body;
    
    try {
        // 1. Iniciar transacción
        await db.query('BEGIN');
        
        // 2. Insertar historial de servicio
        const insertQuery = `
            INSERT INTO service_logs (vehicle_id, category, title, description, cost, mileage, date, type, details)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) RETURNING *;
        `;
        const result = await db.query(insertQuery, [vehicleId, category, title, description, cost, mileage, date, type, details]);
        const newService = result.rows[0];

        // 3. Consultar odómetro actual del vehículo
        const vehResult = await db.query('SELECT odometer FROM vehicles WHERE id = $1', [vehicleId]);
        
        if (vehResult.rows.length > 0) {
            const currentOdometer = vehResult.rows[0].odometer;
            
            // 4. Si el kilometraje reportado por el mecánico es superior, actualizar odómetro
            if (mileage > currentOdometer) {
                await db.query('UPDATE vehicles SET odometer = $1 WHERE id = $2', [mileage, vehicleId]);
            }
        }
        
        await db.query('COMMIT');
        res.status(201).json(newService);
    } catch (error) {
        await db.query('ROLLBACK');
        console.error(error);
        res.status(500).json({ error: "Error interno al registrar mantenimiento." });
    }
});
```

---

## 4. Estrategia de Sincronización Offline-First en Android

Para asegurar que la app no falle u obstruya la experiencia del usuario cuando haya fallas de internet (Edge / Zonas rurales), se aplica la siguiente lógica en el móvil:

```
[Usuario Registra Mantenimiento]
         │
         ▼
[Guardar localmente en Room DB] ───► (La UI se actualiza inmediatamente en <16ms)
         │
         ▼
[Verificar Conexión de Red]
         ├──► [HAY INTERNET]: Invoca 'POST /api/services' al backend de inmediato.
         │
         └──► [NO HAY INTERNET]: Registra acción en una cola de pendientes local (Sync Queue).
                                 Cuando se restablezca la red, un Worker de Android (WorkManager)
                                 despacha las peticiones en segundo plano en lote.
```

### Ventajas de la Capa de Servicios Mock implementada:
1. **Verificación Rápida de UI:** Permite probar animaciones de carga, spinners, modales de error y alertas de éxito de forma inmediata.
2. **Aislamiento en Testing:** Facilita la creación de pruebas unitarias robustas prescindiendo de dependencias externas inestables.
3. **Paralelismo de Desarrollo:** El equipo móvil puede avanzar en layouts interactivos de nube sin esperar a que el API del Backend esté lista en AWS o Google Cloud.
