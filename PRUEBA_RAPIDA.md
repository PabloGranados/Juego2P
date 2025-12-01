# 🚀 Inicio Rápido - Testing Bluetooth

## ⚡ Pasos para Probar (5 minutos)

### 1. Preparación (1 min)
```bash
# En Android Studio
1. Compilar el proyecto
2. Instalar en AMBOS dispositivos
3. Abrir Logcat
4. Filtro: tag:GameViewModel|tag:BluetoothManager
```

### 2. Conexión (1 min)
```
Dispositivo 1 (Jugador 1):
- Abrir app
- Menú → Modo Bluetooth
- "Crear Sala" (Anfitrión)
- Esperar conexión

Dispositivo 2 (Jugador 2):
- Abrir app  
- Menú → Modo Bluetooth
- "Unirse a Sala"
- Seleccionar dispositivo del Jugador 1
- Esperar "CONECTADO"
```

### 3. Primer Click (30 seg)
```
Jugador 1:
- Click en CUALQUIER celda del tablero
```

### 4. Verificación (30 seg)
```
✅ Jugador 1 debe ver:
- Celdas reveladas
- Puntos actualizados (ej: 10pts)
- "Turno: Jugador 2" o similar

✅ Jugador 2 debe ver:
- LAS MISMAS celdas reveladas
- Puntos del Jugador 1 actualizados
- "Tu turno" o "Turno: Jugador 2"
- Puede hacer click en celdas
```

### 5. Segundo Click (30 seg)
```
Jugador 2:
- Click en cualquier celda disponible

✅ Ambos jugadores deben ver:
- Nuevas celdas reveladas
- Puntos del Jugador 2 actualizados
- Turno cambiado a Jugador 1
```

## 🔍 Verificación Rápida con Logs

### ✅ SI FUNCIONA, verás:

**En Jugador 1:**
```
🎯 PRIMER MOVIMIENTO en (5, 5)
📤 HOST enviando BOARD_SYNC (234 chars)
✓ Mensaje ENVIADO: BOARD_SYNC
📤 Enviando GAME_STATE_UPDATE
✓ Mensaje ENVIADO: GAME_STATE_UPDATE
```

**En Jugador 2:**
```
✓ Mensaje RECIBIDO: BOARD_SYNC (234 caracteres)
🔔 Nuevo mensaje en el flow: BOARD_SYNC
>>> ✅ Tablero sincronizado - 10x10
✓ Mensaje RECIBIDO: GAME_STATE_UPDATE
>>> Aplicando estado - 12 celdas reveladas
>>> ✅ Estado actualizado exitosamente
```

### ❌ SI NO FUNCIONA, busca:

**Error de Conexión:**
```
✗ NO CONECTADO, estado: DISCONNECTED
→ Solución: Reconectar dispositivos
```

**Error de Envío:**
```
✗ ERROR enviando mensaje
→ Solución: Verificar permisos Bluetooth
```

**Error de Recepción:**
```
No aparece "✓ Mensaje RECIBIDO"
→ Solución: Verificar conexión Bluetooth
```

**Error de Procesamiento:**
```
No aparece "🔔 Nuevo mensaje en el flow"
→ Solución: Reiniciar la app
```

**Error de Actualización UI:**
```
Aparece "✅ Estado actualizado" pero UI no cambia
→ Solución: Ver BLUETOOTH_DEBUG.md sección "Debug Avanzado"
```

## 🎮 Casos de Prueba

### Caso 1: Juego Normal
1. ✅ Jugador 1 → Click → Revela celdas
2. ✅ Jugador 2 ve actualización
3. ✅ Jugador 2 → Click → Revela celdas  
4. ✅ Jugador 1 ve actualización
5. ✅ Continuar alternando turnos

### Caso 2: Banderas
1. ✅ Jugador 1 → Click largo → Coloca bandera
2. ✅ Jugador 2 ve bandera
3. ✅ Jugador 2 → Click largo → Coloca bandera
4. ✅ Jugador 1 ve bandera

### Caso 3: Minas
1. ✅ Jugador X → Click en mina
2. ✅ Ambos ven la mina revelada
3. ✅ Jugador X pierde puntos
4. ✅ Turno pasa al otro jugador

## 📱 Dispositivos Recomendados

- **Mínimo:** Android 5.0 (API 21)
- **Recomendado:** Android 12+ (API 31+) para mejor soporte Bluetooth
- **Permisos necesarios:**
  - BLUETOOTH
  - BLUETOOTH_ADMIN
  - BLUETOOTH_CONNECT (Android 12+)
  - BLUETOOTH_SCAN (Android 12+)
  - ACCESS_FINE_LOCATION (para descubrimiento)

## 🆘 Solución Rápida de Problemas

| Problema | Solución |
|----------|----------|
| No se conectan | 1. Vincular dispositivos en Ajustes de Android<br>2. Dar permisos de ubicación<br>3. Reiniciar Bluetooth |
| Se desconectan | 1. Mantener pantalla activa<br>2. Desactivar ahorro de batería para la app |
| Lag en actualización | Normal hasta 500ms de delay |
| Jugador 2 no puede clickear | Verificar log "isMyTurn=true" |

## 📞 Soporte

Si después de seguir estos pasos el problema persiste:

1. Copia los logs de AMBOS dispositivos
2. Busca el primer ❌ o ✗ en los logs
3. Consulta `BLUETOOTH_DEBUG.md` para ese error específico
4. Si no hay errores en logs pero la UI no actualiza, ve a "Debug Avanzado" en `BLUETOOTH_DEBUG.md`

## ✨ Características Confirmadas

- ✅ Sincronización de tablero en tiempo real
- ✅ Actualización de puntos
- ✅ Manejo correcto de turnos
- ✅ Colocación de banderas sincronizada
- ✅ Revelación de minas sincronizada
- ✅ Detección automática de victoria/derrota
- ✅ Logging completo para debugging

---

**Versión:** 2.0 - Reescritura completa del sistema Bluetooth
**Fecha:** Diciembre 2025
**Estado:** ✅ Listo para testing
