# 🎮 Actualización: Modo Multijugador Bluetooth

## ✨ Nuevas Características

### Modo Bluetooth Multidispositivo

El juego de Buscaminas ahora soporta **dos modos de juego**:

1. **Modo Local** (Original): Dos jugadores en el mismo dispositivo
2. **Modo Bluetooth** (NUEVO): Dos jugadores en dispositivos diferentes conectados por Bluetooth

## 🔧 Cambios Implementados

### 1. Permisos Bluetooth
Se agregaron los permisos necesarios en el `AndroidManifest.xml`:
- `BLUETOOTH_SCAN` - Para buscar dispositivos
- `BLUETOOTH_CONNECT` - Para conectar dispositivos
- `BLUETOOTH_ADVERTISE` - Para hacerse visible
- `ACCESS_FINE_LOCATION` - Requerido por Android para Bluetooth

### 2. Nueva Arquitectura

#### **BluetoothManager** (`bluetooth/BluetoothManager.kt`)
Gestor de comunicación Bluetooth que maneja:
- Conexión servidor/cliente
- Envío y recepción de mensajes
- Estados de conexión (Desconectado, Conectando, Conectado, Escuchando)
- Sincronización de movimientos entre dispositivos

#### **MenuScreen** (`ui/screens/MenuScreen.kt`)
Pantalla de menú principal con opciones:
- **Juego Local**: Modo original de dos jugadores
- **Juego Bluetooth**: Conectar dos dispositivos
- **Ver Estadísticas**: Consultar historial

#### **BluetoothSetupScreen** (`ui/screens/BluetoothSetupScreen.kt`)
Pantalla de configuración Bluetooth que permite:
- Verificar disponibilidad de Bluetooth
- Solicitar permisos necesarios
- **Crear partida** (modo anfitrión/servidor)
- **Unirse a partida** (modo invitado/cliente)
- Ver dispositivos vinculados
- Conectar con otro dispositivo

### 3. GameViewModel Mejorado

El `GameViewModel` ahora incluye:
- Gestión de conexión Bluetooth
- Sincronización de movimientos entre dispositivos
- Validación de turnos en modo multijugador
- Mensajes Bluetooth para:
  - Clics en celdas (`CELL_CLICK`)
  - Clics largos para banderas (`CELL_LONG_CLICK`)
  - Reinicio de partida (`RESET_GAME`)

### 4. Navegación Actualizada

Nueva estructura de navegación:
```
Menú Principal
├── Juego Local → GameScreen (modo local)
├── Juego Bluetooth → BluetoothSetupScreen → GameScreen (modo Bluetooth)
└── Estadísticas → StatsScreen
```

## 📱 Cómo Usar el Modo Bluetooth

### Paso 1: Vincular Dispositivos
Antes de usar la aplicación, vincula los dos dispositivos Android:
1. Ve a **Configuración** del sistema
2. Abre **Bluetooth**
3. Empareja los dos dispositivos que jugarán

### Paso 2: Crear Partida (Anfitrión)
En el **Dispositivo 1**:
1. Abre la app y selecciona **"Juego Bluetooth"**
2. Concede los permisos de Bluetooth si se solicitan
3. Presiona **"Crear partida (Anfitrión)"**
4. Espera a que el otro jugador se conecte
5. El anfitrión será el **Jugador 1** (Azul) y juega primero

### Paso 3: Unirse a Partida (Invitado)
En el **Dispositivo 2**:
1. Abre la app y selecciona **"Juego Bluetooth"**
2. Concede los permisos de Bluetooth
3. Selecciona el dispositivo anfitrión de la lista
4. Espera a conectar
5. El invitado será el **Jugador 2** (Rojo)

### Paso 4: Jugar
- Cada jugador solo puede interactuar durante su turno
- Los movimientos se sincronizan automáticamente entre dispositivos
- El juego funciona igual que el modo local, pero cada jugador usa su propio dispositivo

## 🎯 Validación de Turnos

En modo Bluetooth:
- El **Anfitrión** controla al **Jugador 1** (Azul)
- El **Invitado** controla al **Jugador 2** (Rojo)
- Solo puedes hacer movimientos durante tu turno
- Los movimientos del oponente aparecen automáticamente en tu pantalla

## 🔒 Seguridad

- La conexión Bluetooth usa el protocolo RFCOMM estándar
- Solo se pueden conectar dispositivos previamente vinculados
- Los mensajes están codificados con delimitadores para evitar corrupción

## 🐛 Solución de Problemas

### No puedo ver dispositivos
- Verifica que Bluetooth esté activado en ambos dispositivos
- Asegúrate de que los dispositivos estén vinculados en Configuración
- Otorga todos los permisos solicitados

### La conexión falla
- Intenta desvincular y volver a vincular los dispositivos
- Cierra y reabre la aplicación
- Verifica que no haya otras aplicaciones usando Bluetooth

### Los movimientos no se sincronizan
- Verifica la conexión Bluetooth
- Si persiste, desconecta y reconecta
- Como último recurso, reinicia ambas aplicaciones

### "No hay permisos Bluetooth"
- Ve a Configuración → Apps → Buscaminas → Permisos
- Otorga los permisos de Bluetooth y Ubicación

## 📊 Tecnologías Utilizadas

- **Bluetooth Classic** (RFCOMM): Para comunicación entre dispositivos
- **Kotlin Coroutines**: Para operaciones asíncronas de Bluetooth
- **StateFlow**: Para gestionar estados de conexión
- **Jetpack Compose**: Para UI reactiva

## 🎨 Arquitectura Bluetooth

```
┌─────────────────────────────────────────────────────┐
│                BluetoothManager                     │
│  • Gestión de conexión                             │
│  • Envío/recepción de mensajes                     │
│  • Estados de conexión                             │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│               GameViewModel                         │
│  • Sincronización de estado                        │
│  • Validación de turnos                            │
│  • Procesamiento de mensajes                       │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│            BluetoothSetupScreen                     │
│  • Configuración de conexión                       │
│  • Selección de rol (Anfitrión/Invitado)          │
└─────────────────────────────────────────────────────┘
```

## 🔄 Protocolo de Mensajes

Los mensajes Bluetooth siguen el formato:
```
[TIPO_MENSAJE]|[DATOS]\n
```

Tipos de mensaje:
- `CELL_CLICK|row,col` - Clic en celda
- `CELL_LONG_CLICK|row,col` - Clic largo (bandera)
- `RESET_GAME|` - Reiniciar partida
- `GAME_STATE|...` - Sincronizar estado completo

## 📝 Notas Importantes

1. **Compatibilidad**: Ambos dispositivos deben tener Android 7.0 (API 24) o superior
2. **Distancia**: Los dispositivos deben estar a máximo 10 metros de distancia
3. **Batería**: El Bluetooth consume batería, especialmente en modo servidor
4. **Estadísticas**: Se guardan localmente en cada dispositivo
5. **Desconexión**: Si se pierde la conexión, vuelve al menú principal

## 🚀 Próximas Mejoras Posibles

- [ ] Reconexión automática si se pierde la conexión
- [ ] Chat entre jugadores
- [ ] Modo observador (permitir espectadores)
- [ ] Historial de partidas multijugador
- [ ] Estadísticas sincronizadas en la nube
- [ ] Soporte para Wi-Fi Direct como alternativa

## 🎓 Aprendizajes

Este proyecto demuestra:
- Comunicación Bluetooth en Android
- Arquitectura cliente-servidor local
- Sincronización de estado entre dispositivos
- Manejo de permisos en tiempo de ejecución
- Programación asíncrona con Coroutines

---

**¡Disfruta jugando Buscaminas con tus amigos en modo Bluetooth!** 🎮📱💣
