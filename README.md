# <img src="app/src/main/res/drawable/logoapp.png" width="40" height="40"> RapiTask - Proyecto Final de Aplicación Móvil

RapiTask es una aplicación avanzada de gestión de notas y tareas diseñada para ofrecer una experiencia de usuario fluida y productiva, siguiendo los principios de Material Design 3.

<p align="left">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white" />
  <img src="https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design&logoColor=white" />
</p>

## Características Principales

- Gestión de Notas y Tareas: Crea notas con títulos, contenido enriquecido y seguimiento de estado (completado/pendiente).
- Editor de Texto Enriquecido: Soporte para formato de texto (negrita, subrayado) y múltiples tamaños de fuente directamente en la edición.
- Multimedia Integrada:
    - Notas de Voz: Grabación y reproducción de audio integrada con gestión de ciclo de vida.
    - Captura y Galería: Captura fotos directamente con la cámara del dispositivo o adjunta imágenes desde el almacenamiento con visualización a pantalla completa.
    - Archivos: Adjunta documentos y archivos relevantes a tus tareas.
- Subtareas: Desglosa tareas complejas en pasos más pequeños con una lista de verificación dinámica.
- Sistema de Recordatorios: Programación de notificaciones profesionales utilizando WorkManager para asegurar la ejecución de tareas en segundo plano.
- Seguridad y Cumplimiento:
    - Implementación de FileProvider para el intercambio seguro de archivos y capturas de cámara.
    - Protección de receptores de sistema contra accesos no autorizados de aplicaciones externas.
    - Configuración optimizada para el cumplimiento de políticas de permisos en Google Play Store.
- Organización Inteligente:
    - Categorías Dinámicas: Crea y gestiona tus propias secciones (ej. Escuela, Trabajo, Personal).
    - Favoritos y Anclado: Destaca tus notas más importantes para que aparezcan siempre al principio.
    - Bloqueo de Notas: Protege la privacidad de tus tareas marcándolas como bloqueadas.
- Vistas Personalizables: Alterna entre vista de lista y cuadrícula (Staggered Grid).
- Búsqueda y Filtrado: Localiza rápidamente cualquier nota por texto o categoría.
- Calendario: Visualización de fechas límite en una interfaz de calendario dedicada.

## Tecnologías Utilizadas

El proyecto está construido utilizando las últimas tecnologías y mejores prácticas de desarrollo Android:

- **Lenguaje:** Kotlin ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?logo=kotlin&logoColor=white&style=flat)
- **Arquitectura:** MVVM (Model-View-ViewModel) para una separación clara de responsabilidades.
- **Base de Datos:** Room Persistence Library ![Android](https://img.shields.io/badge/-Room-3DDC84?logo=android&logoColor=white&style=flat) para el almacenamiento local robusto.
- **Concurrencia:** Coroutines & Flow ![Kotlin](https://img.shields.io/badge/-Coroutines-7F52FF?logo=kotlin&logoColor=white&style=flat) para operaciones asíncronas.
- **Tareas en Segundo Plano:** WorkManager ![Android](https://img.shields.io/badge/-WorkManager-3DDC84?logo=android&logoColor=white&style=flat) para recordatorios confiables.
- **Seguridad:** FileProvider ![Android](https://img.shields.io/badge/-FileProvider-3DDC84?logo=android&logoColor=white&style=flat) para el manejo seguro de multimedia.
- **Interfaz de Usuario:**
    - Material Design 3 ![Material Design](https://img.shields.io/badge/-MD3-757575?logo=material-design&logoColor=white&style=flat)
    - ViewBinding ![Android](https://img.shields.io/badge/-ViewBinding-3DDC84?logo=android&logoColor=white&style=flat)
    - Splash Screen API ![Android](https://img.shields.io/badge/-SplashAPI-3DDC84?logo=android&logoColor=white&style=flat)
- **Librerías de Terceros:**
    - Glide ![Image](https://img.shields.io/badge/-Glide-464646?logo=google-photos&logoColor=white&style=flat) para gestión de imágenes.
    - Material CalendarView ![Calendar](https://img.shields.io/badge/-Calendar-FF5252?logo=google-calendar&logoColor=white&style=flat) para la gestión visual.

## Estructura del Proyecto

- NoteRepository / ReminderRepository: Capa de abstracción de datos para la gestión de fuentes de información.
- ViewModels: Gestión del estado de la interfaz de usuario y ejecución de la lógica de negocio.
- DAOs: Definición de contratos y consultas SQLite mediante la librería Room.
- Workers: Implementación de lógica para tareas en segundo plano y notificaciones persistentes.
- Adapters: Gestión eficiente de listas y colecciones de datos mediante ListAdapter y DiffUtil.

## Instalación

1. Clona este repositorio en tu máquina local.
2. Abre el proyecto en Android Studio (Ladybug o superior).
3. Sincroniza el proyecto con los archivos de configuración de Gradle.
4. Ejecuta la aplicación en un emulador o dispositivo físico con Android 8.0 (API 26) o superior.

---
Este proyecto fue desarrollado como parte de un proceso de aprendizaje en desarrollo móvil avanzado.
<p align="center">
  <img src="https://developer.android.com/static/images/brand/Android_Robot.png" width="100">
</p>
