# 🏗️ Arquitectura del Proyecto - Buscaminas

## Patrón MVVM (Model-View-ViewModel)

Este proyecto implementa el patrón arquitectónico MVVM recomendado por Google para aplicaciones Android.

### Capas de la Arquitectura

```
┌─────────────────────────────────────────────────────┐
│                     View Layer                      │
│  (Jetpack Compose - UI Components & Screens)       │
│  • GameScreen.kt                                    │
│  • CellView.kt, PlayerInfo.kt, GameBoard.kt       │
└──────────────────┬──────────────────────────────────┘
                   │ Observa StateFlow
                   │ Llama funciones
                   ▼
┌─────────────────────────────────────────────────────┐
│                  ViewModel Layer                    │
│              (GameViewModel.kt)                     │
│  • Maneja la lógica de negocio                     │
│  • Expone StateFlow para el estado del juego       │
│  • Procesa eventos de usuario                      │
│  • No conoce detalles de la UI                     │
└──────────────────┬──────────────────────────────────┘
                   │ Utiliza
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│                   Model Layer                       │
│         (Data Classes & Game Logic)                │
│  • Cell.kt, Player.kt, GameState.kt (Models)      │
│  • Board.kt (Game Logic)                           │
└─────────────────────────────────────────────────────┘
```

## 📦 Componentes Principales

### 1. Model Layer (Capa de Modelo)

#### **Cell.kt**
Representa una celda individual del tablero.

```kotlin
data class Cell(
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val adjacentMines: Int = 0
)
```

**Responsabilidades:**
- Almacenar el estado de una celda
- Validar si puede ser revelada o marcada con bandera

#### **Player.kt**
Representa un jugador en el juego.

```kotlin
data class Player(
    val id: Int,
    val name: String,
    val color: Color,
    val wins: Int = 0,
    val points: Int = 0
)
```

**Responsabilidades:**
- Mantener información del jugador
- Gestionar puntos y victorias
- Proporcionar métodos para actualizar estado

#### **GameState.kt**
Estado completo del juego (Single Source of Truth).

```kotlin
data class GameState(
    val board: List<List<Cell>>,
    val player1: Player,
    val player2: Player,
    val currentPlayer: Int,
    val gameStatus: GameStatus,
    val remainingCells: Int,
    val totalFlags: Int,
    val placedFlags: Int,
    val isFirstMove: Boolean,
    val lastRevealedBy: Int?
)
```

**Responsabilidades:**
- Ser la fuente única de verdad del estado
- Proporcionar métodos de ayuda para consultar estado
- Ser inmutable (data class)

#### **Board.kt**
Lógica del tablero de Buscaminas.

```kotlin
class Board(
    private val rows: Int,
    private val cols: Int,
    private val minesCount: Int
)
```

**Responsabilidades:**
- Generar minas aleatoriamente (evitando el primer clic)
- Calcular números de minas adyacentes
- Revelar celdas con flood fill
- Alternar banderas
- Contar celdas restantes

**Algoritmos implementados:**
- **Flood Fill**: Revelar celdas vacías adyacentes recursivamente
- **Generación de minas**: Colocación aleatoria evitando primera celda
- **Cálculo de adyacencias**: Contar minas en las 8 direcciones

### 2. ViewModel Layer (Capa de ViewModel)

#### **GameViewModel.kt**
Gestiona toda la lógica de negocio del juego.

```kotlin
class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(createInitialGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private val _lastAction = MutableStateFlow<Pair<Int, Int>?>(null)
    val lastAction: StateFlow<Pair<Int, Int>?> = _lastAction.asStateFlow()
}
```

**Responsabilidades:**
- Exponer el estado del juego mediante StateFlow
- Procesar eventos de usuario (clicks, long clicks)
- Coordinar la lógica del tablero (Board)
- Gestionar turnos y puntuación
- Detectar condiciones de victoria/derrota
- Manejar reinicio del juego
- Sobrevivir a cambios de configuración

**Flujo de eventos:**

```
Usuario toca celda
       ↓
   onCellClick()
       ↓
Validar movimiento
       ↓
Generar minas (si es primer movimiento)
       ↓
Revelar celda (board.revealCell)
       ↓
Actualizar puntos
       ↓
Verificar victoria/derrota
       ↓
Cambiar turno
       ↓
Emitir nuevo estado
       ↓
UI se actualiza automáticamente
```

### 3. View Layer (Capa de Vista)

#### **GameScreen.kt**
Pantalla principal que orquesta todos los componentes UI.

**Responsabilidades:**
- Observar el estado del juego
- Mostrar diálogo de fin de juego
- Coordinar componentes UI
- Pasar callbacks al ViewModel

#### **CellView.kt**
Componente individual de celda.

**Características:**
- Animación de escala al interactuar
- Colores dinámicos según estado
- Gestión de clicks y long clicks
- Mostrar emojis (mina, bandera) o números

#### **PlayerInfo.kt**
Panel de información del jugador.

**Muestra:**
- Nombre y color del jugador
- Indicador de turno actual
- Puntos actuales
- Victorias totales

#### **GameBoard.kt**
Tablero completo del juego.

**Responsabilidades:**
- Renderizar matriz de celdas
- Coordinar animaciones
- Propagar eventos de click

## 🔄 Flujo de Datos

### Flujo Unidireccional (UDF)

```
┌──────────────────────────────────────────────────┐
│                    UI (View)                     │
│  • Observa StateFlow                            │
│  • Se recompone cuando cambia el estado         │
└──────────────┬───────────────────────────────────┘
               │
               │ Eventos de usuario
               │ (clicks, etc.)
               ▼
┌──────────────────────────────────────────────────┐
│              ViewModel                           │
│  • Procesa eventos                              │
│  • Actualiza estado                             │
│  • Emite nuevo estado                           │
└──────────────┬───────────────────────────────────┘
               │
               │ Emite StateFlow
               │
               ▼
┌──────────────────────────────────────────────────┐
│           Estado (GameState)                     │
│  • Inmutable                                    │
│  • Single Source of Truth                       │
└──────────────────────────────────────────────────┘
```

### Estado Reactivo con StateFlow

```kotlin
// En ViewModel
private val _gameState = MutableStateFlow(initialState)
val gameState: StateFlow<GameState> = _gameState.asStateFlow()

// En UI (Composable)
val gameState by viewModel.gameState.collectAsState()

// La UI se recompone automáticamente cuando cambia el estado
```

## 🎯 Ventajas de esta Arquitectura

### Separación de Responsabilidades
- **View**: Solo renderiza UI, no contiene lógica de negocio
- **ViewModel**: Contiene toda la lógica, no conoce la UI
- **Model**: Datos puros, sin dependencias de Android

### Testabilidad
- **ViewModel**: Testeable sin necesidad de contexto Android
- **Board**: Lógica pura, fácil de testear
- **Models**: Data classes simples de verificar

### Mantenibilidad
- Código organizado y modular
- Fácil de entender y modificar
- Componentes reutilizables

### Escalabilidad
- Fácil agregar nuevas funcionalidades
- Posible migrar a múltiples jugadores online
- Simple integrar persistencia de datos

### Manejo de Configuración
- StateFlow sobrevive a rotaciones de pantalla
- ViewModel mantiene el estado
- No se pierde progreso del juego

## 🧪 Testing Strategy

### Unit Tests (Recomendados)

**GameViewModel Tests:**
```kotlin
- testInitialState()
- testRevealCell()
- testPlaceFlag()
- testSwitchTurns()
- testVictoryDetection()
- testGameOverOnMineHit()
```

**Board Tests:**
```kotlin
- testMineGeneration()
- testFloodFill()
- testAdjacentMinesCount()
- testToggleFlag()
```

### UI Tests

**GameScreen Tests:**
```kotlin
- testCellClickRevealsCell()
- testLongClickPlacesFlag()
- testGameOverDialogShown()
- testPlayerInfoUpdates()
```

## 🔐 Principios SOLID Aplicados

### Single Responsibility Principle
- Cada clase tiene una única responsabilidad
- `Cell` solo representa una celda
- `Board` solo maneja lógica del tablero
- `GameViewModel` solo coordina el juego

### Open/Closed Principle
- Clases abiertas para extensión
- Posible agregar nuevos tipos de celdas
- Fácil cambiar algoritmo de generación

### Dependency Inversion
- ViewModel no depende de detalles de UI
- View depende de abstracciones (StateFlow)

## 📊 Gestión del Estado

### Estado Inmutable
- Todos los cambios crean un nuevo estado
- Uso de `copy()` en data classes
- Previene bugs de concurrencia

### Single Source of Truth
- `GameState` es la única fuente de verdad
- No hay múltiples copias del estado
- Consistencia garantizada

### Reactive Updates
- UI se actualiza automáticamente
- No hay necesidad de notificaciones manuales
- StateFlow maneja todo

## 🚀 Posibles Mejoras Futuras

1. **Persistencia**: Guardar progreso con Room o DataStore
2. **Online**: Migrar a arquitectura cliente-servidor
3. **IA**: Agregar modo un jugador con IA
4. **Dificultades**: Múltiples niveles de dificultad
5. **Temas**: Soporte para diferentes temas visuales
6. **Sonidos**: Efectos de sonido y música
7. **Achievements**: Sistema de logros
8. **Leaderboard**: Tabla de clasificación

---

Esta arquitectura proporciona una base sólida, escalable y mantenible para el juego de Buscaminas multijugador.
