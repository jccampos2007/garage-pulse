# GaragePulse 🚗⚡ (Flota & Mantenimiento Inteligente con IA)

**GaragePulse** es una moderna aplicación móvil con arquitectura **Offline-First** diseñada para centralizar, controlar y predecir el estado de salud y mantenimiento de tus vehículos (automóviles y motocicletas). 

La aplicación integra control de consumo, registro de odómetro, alertas de vencimientos de servicios y un panel interactivo de salud predictivo, todo envuelto en una estética visual inmersiva de vanguardia.

---

## ✨ Características Principales

- **Dashboard de Control de Flota:** Conmutable instantáneamente para admitir múltiples vehículos. Admite cambio entre automóviles (`Car`) e ilustraciones vectoriales nativas para motocicletas (`Motorcycle`) escaladas y centradas con proporciones perfectas.
- **Odómetro Interactivo & Promedio Diario:** Registra el kilometraje actual y calcula de forma inteligente el promedio recorrido por día para predecir cuándo tocará el siguiente servicio.
- **Ingreso Guiado de Servicios Avanzado:**
  - Categorías: Cambio de Aceite, Frenos, Neumáticos, Filtros, Sistema Eléctrico, etc.
  - Tipificación de Mantenimientos: Preventivos, Correctivos o Predictivos.
  - Grabadora de Notas de Voz: Graba directamente explicaciones o diagnósticos audibles del mecánico.
  - Adjunto Fotográfico: Captura el recibo o comprobante directo del taller.
- **Salud Predictiva Inteligente (Granular):** Monitorea y diagnostica en tiempo real la salud acumulativa. Su motor interno desglosa los cálculos a nivel de sub-tareas específicas (ej. Alineación vs Cambio de Llantas), agrupando inteligentemente y "burbujeando" la advertencia del componente más crítico a la tarjeta principal, manteniendo la interfaz simple y amigable.
- **Dynamic Maps Component:** Incorpora un mapa simulado de asistencia técnica diseñado con paletas de color y estilos oscuros idénticos a los del sistema de posicionamiento real para guiar al conductor.
- **Gestión Avanzada del Perfil:** Modificadores de metas de perfil, ajustes métricos de Kilómetros o Millas globales de conversión instantánea, y herramientas de reinicio de sincronización cloud.

---

## 🎨 Identidad Visual y Diseño (Cosmic Obsidian Theme)

La interfaz de **GaragePulse** ha sido construida siguiendo los estándares más rigurosos de **Material Design 3 (M3)** y los principios de diseño sofisticados:

- **Paleta de Colores de Alto Contraste:** 
  - Base: Negros puros (`#000000`) y grises obsidiana profundos.
  - Acento Principal: Naranja vibrante deportivo (`#E75C31`), que resalta zonas interactivas clave, botones FAB, botones de bypass, interruptores y métricas críticas.
  - Énfasis de Salud: Verdes puros para estados óptimos y rojos/naranjas de alta saturación para ventanas críticas de re-emplazo.
- **Tipografías y Espaciado:** Armonía y contraste aplicando una cuadrícula fundamental de 8dp. Textos de sistema acompañados por íconos explicativos precisos, eliminando ruido innecesario y maximizando el espacio negativo confortable.
- **Perfect Crop Auto-Sizing (Soporte de Ilustración):** El componente `VehiclePhotoOrIllustration` realiza un ajuste inteligente de bordes interiores (`0.68f` del ancho disponible para vectores nativos y `fillMaxSize` para tomas fotográficas reales). Esto previene que los vehículos se estiren de forma desproporcionada o colapsen el espacio del dashboard.

---

## 🛠️ Arquitectura de Software & Stack Tecnológico

La app está cimentada sobre las mejores prácticas y patrones actuales del ecosistema nativo de Android:

- **Kotlin:** Código robusto, fuertemente tipado y conciso.
- **Jetpack Compose:** Interfaz de usuario reactiva, modular descrita enteramente mediante programación declarativa moderna.
- **MVVM (Model-View-ViewModel):** Separación nítida de responsabilidades del estado visual a través de `ViewModels` y estados observados.
- **Room SQLite Persistence:** Almacenamiento local ultrarrápido y seguro de perfiles, flotillas y registros de servicios.
- **Coroutines & Kotlin Flows:** Propagación de datos bidireccional y reactividad inmediata en hilos de fondo estables.
- **SubcomposeAsyncImage (Coil Integration):** Cargado asíncrono de presets y fotos de red con buffers de progreso y placeholders personalizados.

---

## 🔄 Políticas de Sincronización Móvil Recomendadas

Para trascender de la simulación interactiva local a un entorno productivo robusto en la nube, la app adopta las siguientes guías de sincronización detallas en la [Especificación de API REST](./API_REST_SPECIFICATION.md):

1. **Persistencia como Fuente de Verdad (SSOT):** Los componentes visuales consumen corrientes de datos directamente de Room. La capa de red actualiza silenciosamente Room, y la pantalla reacciona de forma automática.
2. **Cola de Sincronización en Segundo Plano:** El uso de herramientas del sistema como `WorkManager` de Android encola operaciones pendientes (creaciones, ediciones, imágenes y notas de audio adjuntas) para despacharlas en bloques tan pronto como el sistema registre red Wi-Fi ilimitada.
3. **Actualización Optimista:** Al registrar un mantenimiento o servicio, la UI calcula de manera instantánea el nuevo odómetro y actualiza los indicadores predictivos. Si la sincronización asíncrona hacia la base de datos remota llegase a dar error, se revierte con elegancia informando al usuario sin frustrar el flujo activo.
4. **Backoff Exponencial:** Para evitar sobrecargar servidores backend en momentos de saturación, el programador de red duplica el tiempo de reintento sistemáticamente (`5s`, `15s`, `45s`, `90s`).

---

## 🧪 Pruebas Unitarias y de Regresión

- **Robolectric & Roborazzi:** El proyecto está estructurado para permitir Unit Tests y de integración sobre la JVM local sin emuladores, con soporte completo para captura de estados visuales y screenshots automáticos del layout en múltiples temas oscuros o claros.
- **Validación del Compilador:** Para asegurar la salud del código, puedes utilizar el validador estático incorporado:
  ```bash
  gradle compileDebugKotlin
  ```
