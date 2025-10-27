# 💣 Buscaminas Multijugador - Android

Una implementación moderna del clásico juego Buscaminas para **dos jugadores**, desarrollada con Jetpack Compose y siguiendo la arquitectura MVVM. Ahora con **soporte Bluetooth** para jugar en dos dispositivos diferentes.

## 📱 Características Principales

### Modos de Juego
- ✅ **Modo Local**: Dos jugadores en el mismo dispositivo (original)
- ✅ **Modo Bluetooth** ⭐ NUEVO: Juego multidispositivo mediante conexión Bluetooth
  - Conexión servidor/cliente
  - Sincronización en tiempo real
  - Validación de turnos por dispositivo

### Sistema de Juego
- ✅ **Alternancia de turnos**: Entre dos jugadores
- ✅ **Sistema de puntuación**: 
  - +10 puntos por cada celda revelada
  - +5 puntos por cada bandera colocada
- ✅ **Validación de movimientos**: Solo se permiten movimientos válidos
- ✅ **Detección automática de victoria/empate**:
  - Victoria: Jugador con más puntos cuando se revelan todas las celdas seguras
  - Empate: Ambos jugadores con mismos puntos
  - Derrota: Jugador que toca una mina pierde automáticamente
- ✅ **Reinicio de partida**: Botón para nueva partida sin cerrar la app
- ✅ **Interfaz intuitiva**: Diseño claro y fácil de entender

### Diseño Visual
- 🎨 **Interfaz atractiva y moderna** con Material Design 3
- 🌈 **Colores distintivos** para cada jugador:
  - Jugador 1: Azul (#2196F3)
  - Jugador 2: Rojo (#F44336)
- ✨ **Animaciones fluidas**:
  - Animación al revelar celdas
  - Animación al colocar banderas
  - Transiciones suaves entre estados
- 💫 **Retroalimentación visual**:
  - Indicador visual del jugador actual
  - Animaciones de rebote al interactuar con celdas
  - Diálogo animado al finalizar el juego

### Arquitectura Técnica
- 🏗️ **Arquitectura MVVM**: Separación clara de responsabilidades
- 🔄 **StateFlow**: Manejo reactivo del estado del juego
- 📦 **Jetpack Compose**: UI declarativa y moderna
- ♻️ **Manejo de ciclo de vida**: Soporte completo para rotación de pantalla
- 🧩 **Componentes reutilizables**: Modularización del código

## 🎮 Cómo Jugar

### Modos de Juego

#### 🏠 Modo Local
Dos jugadores comparten el mismo dispositivo:
1. Desde el menú principal, selecciona **"Juego Local"**
2. Los jugadores se alternan en el mismo dispositivo
3. ¡Empieza a jugar!

#### 📱 Modo Bluetooth (NUEVO)
Juega con un amigo en dispositivos separados:

**Preparación:**
1. Vincula ambos dispositivos desde **Configuración → Bluetooth** de Android
2. Abre la app en ambos dispositivos

**Dispositivo 1 (Anfitrión):**
1. Selecciona **"Juego Bluetooth"**
2. Presiona **"Crear partida (Anfitrión)"**
3. Espera a que el otro jugador se conecte
4. Serás el **Jugador 1** (Azul)

**Dispositivo 2 (Invitado):**
1. Selecciona **"Juego Bluetooth"**
2. Elige el dispositivo anfitrión de la lista
3. Espera la conexión
4. Serás el **Jugador 2** (Rojo)

**Durante el juego:**
- Solo puedes jugar durante tu turno
- Los movimientos se sincronizan automáticamente
- Mantén los dispositivos a menos de 10 metros

### Reglas Básicas
1. **Primer movimiento**: El primer clic siempre es seguro y genera el tablero
2. **Revelar celdas**: Toca una celda para revelarla
   - Si está vacía, se revelan automáticamente las celdas adyacentes
   - Si tiene un número, indica cuántas minas hay alrededor
   - Si es una mina, el jugador pierde y el oponente gana
3. **Colocar banderas**: Mantén presionado sobre una celda para marcarla
4. **Turnos**: Los jugadores se alternan después de cada acción válida
5. **Victoria**: El juego termina cuando:
   - Se revelan todas las celdas seguras (gana quien tenga más puntos)
   - Un jugador toca una mina (gana el oponente)

### Controles
- **Toque simple**: Revelar celda
- **Toque largo**: Colocar/quitar bandera
- **Botón "Nueva Partida"**: Reiniciar el juego

### Sistema de Puntos
- **Revelar celda**: +10 puntos
- **Colocar bandera**: +5 puntos
- **Quitar bandera**: No cambia turno, sin puntos

## 📂 Estructura del Proyecto

```
app/src/main/java/com/example/buscaminas/
├── bluetooth/                    # ⭐ NUEVO: Sistema Bluetooth
│   └── BluetoothManager.kt      # Gestión de conexión y mensajes
├── model/
│   ├── Cell.kt                  # Modelo de celda del tablero
│   ├── Player.kt                # Modelo de jugador
│   └── GameState.kt             # Estado completo del juego
├── game/
│   └── Board.kt                 # Lógica del tablero (minas, flood fill)
├── viewmodel/
│   └── GameViewModel.kt         # ViewModel con lógica de negocio + Bluetooth
├── ui/
│   ├── components/
│   │   ├── CellView.kt          # Componente de celda individual
│   │   ├── PlayerInfo.kt        # Información del jugador
│   │   └── GameBoard.kt         # Tablero completo
│   ├── screens/
│   │   ├── MenuScreen.kt        # ⭐ NUEVO: Menú principal
│   │   ├── BluetoothSetupScreen.kt  # ⭐ NUEVO: Configuración Bluetooth
│   │   ├── GameScreen.kt        # Pantalla principal del juego
│   │   └── StatsScreen.kt       # Estadísticas del juego
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── data/                         # Persistencia de datos
│   └── repository/
└── MainActivity.kt               # Actividad principal con navegación
```

## 🔧 Configuración del Tablero

Puedes personalizar el tablero modificando las constantes en `GameViewModel.kt`:

```kotlin
private val boardRows = 10      // Filas del tablero
private val boardCols = 10      // Columnas del tablero
private val minesCount = 15     // Número de minas

private val pointsPerCell = 10  // Puntos por celda revelada
private val pointsPerFlag = 5   // Puntos por bandera colocada
```

## 🚀 Tecnologías Utilizadas

- **Kotlin**: Lenguaje de programación principal
- **Jetpack Compose**: Framework de UI moderno
- **Material Design 3**: Sistema de diseño
- **StateFlow**: Manejo de estado reactivo
- **Coroutines**: Programación asíncrona
- **ViewModel**: Arquitectura MVVM
- **Bluetooth Classic (RFCOMM)**: ⭐ Comunicación entre dispositivos
- **Room Database**: Persistencia de estadísticas
- **Navigation Compose**: Navegación entre pantallas
- **Android Studio**: IDE de desarrollo

## 📋 Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- Kotlin 1.9+
- Android SDK 24+ (Android 7.0)
- Gradle 8.0+
- **Bluetooth habilitado** (para modo multijugador)
- **Dos dispositivos Android** (para modo Bluetooth)

## 🔐 Permisos Requeridos

La aplicación solicita los siguientes permisos:

**Para modo Bluetooth:**
- `BLUETOOTH_SCAN` - Buscar dispositivos
- `BLUETOOTH_CONNECT` - Conectar dispositivos
- `BLUETOOTH_ADVERTISE` - Hacerse visible
- `ACCESS_FINE_LOCATION` - Requerido por Android para Bluetooth

**Nota:** Estos permisos solo se solicitan cuando seleccionas el modo Bluetooth.

## 🎯 Características Implementadas

### Sistema de Turnos ✅
- Alternancia automática entre jugadores
- Indicador visual claro del jugador actual
- Borde resaltado en el panel del jugador activo

### Sistema de Puntuación ✅
- Contador de puntos en tiempo real
- Historial de victorias por jugador
- Comparación de puntos al finalizar

### Validación de Movimientos ✅
- Verificación de celdas ya reveladas
- Validación de banderas en celdas reveladas
- Prevención de acciones inválidas

### Detección de Victoria/Empate ✅
- Detección automática al completar el tablero
- Detección de mina (derrota inmediata)
- Cálculo de ganador por puntos
- Manejo de empates

### Reinicio de Partida ✅
- Botón de nueva partida siempre visible
- Conservación del historial de victorias
- Reseteo completo del tablero

### Interfaz Intuitiva ✅
- Diseño limpio y moderno
- Instrucciones claras en pantalla
- Feedback visual inmediato
- Diálogos informativos

### Modo Bluetooth Multidispositivo ✅ (NUEVO)
- Conexión servidor/cliente
- Sincronización automática de movimientos
- Validación de turnos
- Indicadores de conexión
- Manejo robusto de errores
- Soporte para desconexión y reconexión

## 🎨 Paleta de Colores

| Elemento | Color | Código |
|----------|-------|--------|
| Jugador 1 | Azul | #2196F3 |
| Jugador 2 | Rojo | #F44336 |
| Celda revelada | Gris claro | #E0E0E0 |
| Celda sin revelar | Azul claro | #90CAF9 |
| Celda con mina | Rojo claro | #E57373 |
| Fondo | Gris muy claro | #F5F5F5 |
| Botón éxito | Verde | #4CAF50 |

## 🐛 Manejo de Estados

El juego maneja correctamente:
- ✅ Rotación de pantalla (sin pérdida de estado)
- ✅ Cambios de configuración
- ✅ Ciclo de vida de la actividad
- ✅ Cambios de tema del sistema

## 📝 Notas de Desarrollo

### Inspiración
Este proyecto se inspiró en una implementación de Buscaminas multijugador en Python con arquitectura cliente-servidor, evolucionando para soportar tanto modo local como conexión Bluetooth entre dispositivos Android.

### Evolución del Proyecto
- **Versión 1.0**: Modo local de dos jugadores en un dispositivo
- **Versión 2.0**: ⭐ Agregado modo Bluetooth multidispositivo con:
  - Sistema completo de comunicación Bluetooth
  - Sincronización en tiempo real
  - Menú de selección de modo
  - Validación de turnos por dispositivo

### Características Bluetooth
- **Protocolo**: Bluetooth Classic (RFCOMM)
- **Arquitectura**: Cliente-Servidor
- **Mensajes**: Formato estructurado con delimitadores
- **Estados**: Desconectado, Escuchando, Conectando, Conectado
- **Sincronización**: Bidireccional en tiempo real

## 🎓 Conceptos Aplicados

- **MVVM**: Separación de lógica de negocio y UI
- **State Management**: Uso de StateFlow para estado reactivo
- **Jetpack Compose**: UI declarativa y composable
- **Material Design**: Guías de diseño de Google
- **Clean Code**: Código legible y mantenible
- **Animaciones**: Transiciones y feedback visual
- **Flood Fill**: Algoritmo para revelar celdas vacías
- **Bluetooth Classic (RFCOMM)**: Comunicación entre dispositivos
- **Protocolo de Mensajes**: Sincronización cliente-servidor
- **Manejo de Permisos**: Runtime permissions para Android 12+

## 🔍 Solución de Problemas

### Bluetooth no se conecta
- ✅ Verifica que ambos dispositivos tienen Bluetooth activado
- ✅ Asegúrate de que los dispositivos estén emparejados previamente
- ✅ Revisa que los permisos estén otorgados en ambos dispositivos
- ✅ Intenta reiniciar la aplicación
- ✅ Verifica que no haya otras apps usando Bluetooth

### El otro jugador no ve mis movimientos
- ✅ Verifica que ambos dispositivos muestren "Conectado"
- ✅ Asegúrate de que estás jugando en tu turno correcto
- ✅ Intenta desconectar y volver a conectar

### Errores de permisos
- ✅ Ve a **Configuración → Aplicaciones → Buscaminas → Permisos**
- ✅ Otorga permisos de Ubicación y Dispositivos cercanos (Bluetooth)
- ✅ Reinicia la aplicación

## 📄 Licencia

Este proyecto fue desarrollado con fines educativos.

---

**Desarrollado con ❤️ usando Jetpack Compose y Kotlin**
