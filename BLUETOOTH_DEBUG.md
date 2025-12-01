# Guía de Debugging para Bluetooth en Buscaminas

## 🔧 Cambios Realizados (Versión Reescrita)

### 1. Sistema de Logging con Emojis
Los logs ahora usan emojis para facilitar la identificación rápida:

- 🔔 **Mensaje recibido en el flow**
- 🔷 **Procesando mensaje**
- 🎯 **Primer movimiento**
- 📤 **Enviando mensaje**
- ✅ **Operación exitosa**
- ❌ **Error**
- ✓ **Bluetooth: mensaje enviado**
- ✗ **Bluetooth: error**

### 2. Dispatcher Principal
- Todo el manejo de mensajes se ejecuta en `Dispatchers.Main`
- Garantiza que las actualizaciones del UI se disparen correctamente
- Los envíos de mensajes se hacen en `Dispatchers.IO`

### 3. Flujo Simplificado
```
Jugador 1 hace click
  ↓
Genera minas (si es primer movimiento)
  ↓
Envía BOARD_SYNC al Jugador 2 (si es host)
  ↓
Revela celdas
  ↓
Actualiza estado local
  ↓
Cambia turno
  ↓
Envía GAME_STATE_UPDATE al Jugador 2
```

## 📊 Cómo Verificar los Logs

### Filtro en Logcat:
```
tag:GameViewModel|tag:BluetoothManager
```

### Buscar por emojis:
```
🔔
📤
✅
❌
```

## ✅ Flujo Correcto Paso a Paso

### 1️⃣ Jugador 1 (Host) hace el primer click:

```
GameViewModel: 🎯 PRIMER MOVIMIENTO en (5, 5)
GameViewModel: 📤 HOST enviando BOARD_SYNC (234 chars)
BluetoothManager: ✓ Mensaje ENVIADO: BOARD_SYNC (234 caracteres)
GameViewModel: 📤 Enviando GAME_STATE_UPDATE
GameViewModel:    Turno → 2
GameViewModel:    P1: 10pts, P2: 0pts
GameViewModel:    Reveladas: 12 celdas
BluetoothManager: ✓ Mensaje ENVIADO: GAME_STATE_UPDATE (567 caracteres)
GameViewModel: ✅ GAME_STATE_UPDATE enviado (567 chars)
```

### 2️⃣ Jugador 2 (Cliente) recibe mensajes:

```
BluetoothManager: ✓ Mensaje RECIBIDO: BOARD_SYNC (234 caracteres)
GameViewModel: 🔔 Nuevo mensaje en el flow: BOARD_SYNC
GameViewModel: 🔷 Procesando mensaje: BOARD_SYNC
GameViewModel: >>> CLIENTE recibió BOARD_SYNC
GameViewModel: >>> ✅ Tablero sincronizado - 10x10
GameViewModel: >>> Cliente listo. Turno=2

BluetoothManager: ✓ Mensaje RECIBIDO: GAME_STATE_UPDATE (567 caracteres)
GameViewModel: 🔔 Nuevo mensaje en el flow: GAME_STATE_UPDATE
GameViewModel: 🔷 Procesando mensaje: GAME_STATE_UPDATE
GameViewModel: >>> Procesando GAME_STATE_UPDATE (isHost=false)
GameViewModel: >>> Estado recibido - Turno: 2, P1: 10, P2: 0
GameViewModel: >>> Aplicando estado - 12 celdas reveladas
GameViewModel: >>> ✅ Estado actualizado exitosamente
```

### 3️⃣ Ahora es el turno del Jugador 2:

El Jugador 2 debería poder hacer click en cualquier celda porque:
- `boardSynced = true`
- `currentPlayer = 2`
- `isHost = false`
- La condición `!isHost && currentPlayer == 2` es `true`

## 🐛 Problemas Comunes

### ❌ Problema: "No es tu turno"
**Log esperado:**
```
GameViewModel: onCellClick(row=3, col=4) - isHost=false, currentPlayer=2, isMyTurn=true, boardSynced=true
```

**Si ves:**
```
GameViewModel: No es tu turno. Esperando al oponente.
```

**Verificar:**
1. ¿El mensaje GAME_STATE_UPDATE llegó?
2. ¿El turno se cambió correctamente? Debe ser `currentPlayer=2`
3. ¿El `isHost` es correcto? Debe ser `false` para el cliente

### ❌ Problema: No se reciben mensajes
**Verificar:**
```
BluetoothManager: ✗ NO CONECTADO, estado: DISCONNECTED
```

**Solución:**
- Reconectar los dispositivos
- Verificar que ambos tengan Bluetooth habilitado
- Reiniciar la aplicación

### ❌ Problema: Se reciben pero no se actualizan
**Verificar que aparezcan TODOS estos logs:**
```
🔔 Nuevo mensaje en el flow
🔷 Procesando mensaje
>>> ✅ Estado actualizado exitosamente
```

**Si falta alguno:**
- El flow no está funcionando → Revisar init
- El mensaje no se está procesando → Revisar handleBluetoothMessage
- El estado no se actualiza → Revisar StateFlow

## 🎮 Testing Checklist

Prueba esto en orden:

1. ✅ **Conexión Bluetooth**
   - [ ] Ambos dispositivos conectados
   - [ ] Log: Estado CONNECTED

2. ✅ **Primer Movimiento (Host)**
   - [ ] Jugador 1 hace click
   - [ ] Log: 🎯 PRIMER MOVIMIENTO
   - [ ] Log: 📤 HOST enviando BOARD_SYNC
   - [ ] Log: ✓ Mensaje ENVIADO

3. ✅ **Recepción (Cliente)**
   - [ ] Log: ✓ Mensaje RECIBIDO: BOARD_SYNC
   - [ ] Log: 🔔 Nuevo mensaje en el flow
   - [ ] Log: >>> ✅ Tablero sincronizado

4. ✅ **Estado del Juego (Cliente)**
   - [ ] Log: ✓ Mensaje RECIBIDO: GAME_STATE_UPDATE
   - [ ] Log: >>> Aplicando estado - X celdas reveladas (X > 0)
   - [ ] Log: >>> ✅ Estado actualizado exitosamente

5. ✅ **UI Actualizada (Cliente)**
   - [ ] El tablero muestra las celdas reveladas
   - [ ] Los puntos del Jugador 1 se actualizaron
   - [ ] El turno cambió a Jugador 2

6. ✅ **Jugador 2 puede jugar**
   - [ ] Click funciona
   - [ ] Log: isMyTurn=true
   - [ ] Celdas se revelan

## 🔍 Debug Avanzado

Si todo lo anterior funciona pero la UI no se actualiza:

1. **Verificar que collectAsState() esté configurado:**
   ```kotlin
   val gameState by viewModel.gameState.collectAsState()
   ```

2. **Verificar que GameBoard use el estado:**
   ```kotlin
   GameBoard(
       board = gameState.board,  // ← Debe ser del estado
       ...
   )
   ```

3. **Forzar recomposición:**
   - Agregar un `key(gameState.board.hashCode())` alrededor de GameBoard

## 📝 Logs Importantes

Guarda estos logs si el problema persiste:
- Todos los logs desde que se conectan hasta el primer click del Jugador 2
- Incluir logs de ambos dispositivos
- Filtrar por: `tag:GameViewModel|tag:BluetoothManager`
