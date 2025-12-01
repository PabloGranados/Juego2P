# Guía de Debugging para Bluetooth en Buscaminas

## Cambios Realizados

### 1. Logging Mejorado
Se agregaron logs detallados para rastrear el flujo de mensajes Bluetooth:

- **En el Jugador 1 (Host):**
  - `>>> PRIMER MOVIMIENTO - Generando minas`
  - `>>> HOST enviando BOARD_SYNC al cliente`
  - `>>> Enviando GAME_STATE_UPDATE después de revelar celdas`

- **En el Jugador 2 (Cliente):**
  - `>>> CLIENTE recibió BOARD_SYNC`
  - `>>> Recibido GAME_STATE_UPDATE`
  - `>>> Estado deserializado`
  - `>>> ACTUALIZACIÓN COMPLETA`

### 2. Verificaciones en BluetoothManager
- `✓ Mensaje ENVIADO: [tipo]` - Confirma que el mensaje se envió
- `✓ Mensaje RECIBIDO: [tipo]` - Confirma que el mensaje llegó
- `✗ NO CONECTADO` - Error si no hay conexión

## Cómo Verificar los Logs

### Usando Android Studio:

1. Conecta ambos dispositivos (si es posible) o usa Logcat con filtros
2. Ve a **Logcat** en la parte inferior
3. Filtra por:
   - Tag: `GameViewModel` para ver la lógica del juego
   - Tag: `BluetoothManager` para ver la comunicación Bluetooth

### Filtros Útiles:

```
tag:GameViewModel|tag:BluetoothManager
```

O busca por:
```
>>>
✓
✗
```

## Qué Buscar en los Logs

### Flujo Normal (Cuando funciona correctamente):

1. **Jugador 1 hace click:**
   ```
   GameViewModel: >>> PRIMER MOVIMIENTO - Generando minas
   GameViewModel: >>> HOST enviando BOARD_SYNC al cliente
   BluetoothManager: ✓ Mensaje ENVIADO: BOARD_SYNC
   GameViewModel: >>> Enviando GAME_STATE_UPDATE
   BluetoothManager: ✓ Mensaje ENVIADO: GAME_STATE_UPDATE
   ```

2. **Jugador 2 recibe:**
   ```
   BluetoothManager: ✓ Mensaje RECIBIDO: BOARD_SYNC
   GameViewModel: >>> CLIENTE recibió BOARD_SYNC
   GameViewModel: >>> Tablero sincronizado exitosamente
   BluetoothManager: ✓ Mensaje RECIBIDO: GAME_STATE_UPDATE
   GameViewModel: >>> Recibido GAME_STATE_UPDATE
   GameViewModel: >>> Celdas reveladas en el estado: [número]
   GameViewModel: >>> ACTUALIZACIÓN COMPLETA
   ```

## Problemas Comunes y Soluciones

### Problema 1: No se reciben mensajes
**Síntomas:** Solo ves "ENVIADO" pero nunca "RECIBIDO"

**Verificar:**
- Estado de conexión Bluetooth
- Logs: `✗ NO CONECTADO, estado: [estado]`

**Solución:**
- Verificar que ambos dispositivos estén conectados
- Reiniciar la conexión Bluetooth

### Problema 2: Se reciben pero no se actualizan
**Síntomas:** Ves "RECIBIDO" pero la UI no cambia

**Verificar:**
- `>>> Celdas reveladas en el estado: [número]` debe ser > 0
- `>>> Estado local actualizado completamente` debe aparecer

**Solución:**
- Verificar que la deserialización funcione correctamente
- Asegurarse de que el StateFlow se actualice

### Problema 3: Sincronización del tablero falla
**Síntomas:** El cliente no puede hacer clicks

**Verificar:**
- `>>> boardSynced establecido a true` debe aparecer
- `boardSynced=${_boardSynced.value}` debe ser `true`

## Información Adicional en los Logs

Los logs ahora incluyen:
- Número de caracteres en los mensajes
- Cantidad de celdas reveladas
- Estado del turno después de cada acción
- Puntos de ambos jugadores
- Estado de sincronización del tablero

## Si el Problema Persiste

Revisa estos puntos en orden:

1. ✅ Conexión Bluetooth establecida (CONNECTED)
2. ✅ Mensajes se ENVÍAN correctamente
3. ✅ Mensajes se RECIBEN correctamente
4. ✅ Mensajes se DESERIALIZAN correctamente
5. ✅ Estado se ACTUALIZA en el ViewModel
6. ✅ UI se RECOMPONE con el nuevo estado

El problema estará en el paso donde falte el ✅
