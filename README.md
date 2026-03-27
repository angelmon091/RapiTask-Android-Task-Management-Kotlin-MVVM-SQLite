# <img src="app/src/main/res/drawable/logoapp.png" width="40" height="40"> RapiTask - Proyecto Final de Aplicación Móvil

RapiTask es una aplicación avanzada de gestión de notas y tareas diseñada para ofrecer una experiencia de usuario fluida y productiva, siguiendo los principios de **Material Design 3**.

<p align="left">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white" />
  <img src="https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design&logoColor=white" />
</p>

## Características Principales

- **Gestión de Notas y Tareas:** Crea notas con títulos, contenido enriquecido y seguimiento de estado (completado/pendiente).
- **Editor de Texto Enriquecido:** Soporte para formato de texto (negrita, subrayado) y múltiples tamaños de fuente directamente en la edición.
- **Multimedia Integrada:**
    - **Notas de Voz:** Grabación y reproducción de audio integrada.
    - **Imágenes:** Adjunta imágenes desde la galería con visualización a pantalla completa.
    - **Archivos:** Adjunta documentos y archivos relevantes a tus tareas.
- **Subtareas:** Desglosa tareas complejas en pasos más pequeños con una lista de verificación dinámica.
- **Sistema de Recordatorios:** Programación de notificaciones profesionales utilizando **WorkManager** para asegurar que no olvides ninguna fecha de vencimiento.
- **Organización Inteligente:**
    - **Categorías Dinámicas:** Crea y gestiona tus propias secciones (ej. Escuela, Trabajo, Personal).
    - **Favoritos (Anclar):** Destaca tus notas más importantes para que aparezcan siempre al principio.
    - **Bloqueo de Notas:** Protege la privacidad de tus tareas marcándolas como bloqueadas.
- **Vistas Personalizables:** Alterna entre vista de lista y cuadrícula (Staggered Grid).
- **Búsqueda y Filtrado:** Localiza rápidamente cualquier nota por texto o categoría.
- **Calendario:** Visualización de fechas límite en una interfaz de calendario dedicada.

## Tecnologías Utilizadas

El proyecto está construido utilizando las últimas tecnologías y mejores prácticas de desarrollo Android:

- **Lenguaje:** [Kotlin](https://kotlinlang.org/) ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?logo=kotlin&logoColor=white&style=flat)
- **Arquitectura:** MVVM (Model-View-ViewModel) para una separación clara de responsabilidades.
- **Base de Datos:** [Room Persistence Library](https://developer.android.com/training/data-storage/room) para el almacenamiento local robusto.
- **Concurrencia:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) y **Flow** para operaciones asíncronas y reactivas.
- **Tareas en Segundo Plano:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) para la programación de recordatorios.
- **Interfaz de Usuario:**
    - **Material Design 3:** Componentes modernos y temas dinámicos.
    - **ViewBinding:** Para una interacción segura con las vistas.
    - **Splash Screen API:** Pantalla de bienvenida nativa.
- **Librerías de Terceros:**
    - **Glide:** Para la carga y gestión eficiente de imágenes.
    - **Material CalendarView:** Para la gestión visual del calendario.

## Estructura del Proyecto

- `NoteRepository` / `ReminderRepository`: Capa de abstracción de datos.
- `ViewModels`: Gestión del estado de la UI y lógica de negocio.
- `DAOs`: Definición de consultas SQLite mediante Room.
- `Workers`: Lógica de ejecución de tareas en segundo plano (notificaciones).
- `Adapters`: Gestión eficiente de listas con `ListAdapter` y `DiffUtil`.

## Instalación

1. Clona este repositorio.
2. Abre el proyecto en **Android Studio (Ladybug o superior)**.
3. Sincroniza el proyecto con los archivos Gradle.
4. Ejecuta la aplicación en un emulador o dispositivo físico con Android 8.0 (API 26) o superior.

---
<p align="center">
  <i>Este proyecto fue desarrollado como parte de un proceso de aprendizaje en desarrollo móvil avanzado.</i><br>
  <img src="https://developer.android.com/static/images/brand/Android_Robot.png" width="100">
</p>
