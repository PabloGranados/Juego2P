# 💣 Buscaminas Multijugador - Android

Una implementación moderna del clásico juego Buscaminas para **dos jugadores**, desarrollada con Jetpack Compose y siguiendo la arquitectura MVVM.

## 📱 Características Principales

### Sistema de Juego
- ✅ **Modo dos jugadores**: Alternancia de turnos entre dos jugadores
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
├── model/
│   ├── Cell.kt              # Modelo de celda del tablero
│   ├── Player.kt            # Modelo de jugador
│   └── GameState.kt         # Estado completo del juego
├── game/
│   └── Board.kt             # Lógica del tablero (minas, flood fill)
├── viewmodel/
│   └── GameViewModel.kt     # ViewModel con lógica de negocio
├── ui/
│   ├── components/
│   │   ├── CellView.kt      # Componente de celda individual
│   │   ├── PlayerInfo.kt    # Información del jugador
│   │   └── GameBoard.kt     # Tablero completo
│   ├── screens/
│   │   └── GameScreen.kt    # Pantalla principal del juego
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt          # Actividad principal
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
- **Android Studio**: IDE de desarrollo

## 📋 Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- Kotlin 1.9+
- Android SDK 24+ (Android 7.0)
- Gradle 8.0+

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
Este proyecto se inspiró en una implementación de Buscaminas multijugador en Python con arquitectura cliente-servidor, adaptándola para un contexto local de dos jugadores en dispositivos Android.

### Diferencias con la versión Python
- **Local vs Red**: Versión local sin necesidad de servidor
- **Dos jugadores fijos**: No hay sala de espera ni conexión múltiple
- **Interfaz gráfica nativa**: UI moderna con Compose vs terminal
- **Puntuación competitiva**: Sistema de puntos para determinar ganador

## 🎓 Conceptos Aplicados

- **MVVM**: Separación de lógica de negocio y UI
- **State Management**: Uso de StateFlow para estado reactivo
- **Jetpack Compose**: UI declarativa y composable
- **Material Design**: Guías de diseño de Google
- **Clean Code**: Código legible y mantenible
- **Animaciones**: Transiciones y feedback visual
- **Flood Fill**: Algoritmo para revelar celdas vacías

## 📄 Licencia

Este proyecto fue desarrollado con fines educativos.

---

**Desarrollado con ❤️ usando Jetpack Compose y Kotlin**
