# Diseño del Sistema de Archivos miFS

## 1. Estrategia de asignación

El sistema utiliza **asignación contigua con reubicación dinámica**. Cada archivo o directorio ocupa N bloques consecutivos dentro de la zona de datos.

Cuando un archivo crece y ya no cabe en su espacio actual, el sistema intenta reubicar todos sus datos en una nueva región contigua de N bloques libres. Si la reubicación es posible, se actualizan los metadatos del archivo para apuntar al nuevo inicio; si no hay espacio contiguo disponible, los bloques originales se restauran y la operación retorna error.

---

## 2. Estructura del volumen

El comando `format <tam> <unidad>` construye el volumen en dos etapas:

### Etapa 1 — Creación del disco

Se calcula el tamaño total y el espacio se divide en las siguientes secciones, en orden:

| Sección          | Offset         | Tamaño                       |
|------------------|----------------|------------------------------|
| MBR              | 0              | 200 B                        |
| SuperBlock       | +200           | 80 B                         |
| Bitmap de bloques | siguiente      | ⌈totalBlocks / 8⌉ bytes      |
| Bitmap de archivos abiertos | siguiente | 10 B               |
| Usuarios         | siguiente      | 2 250 B                      |
| Grupos           | siguiente      | 1 375 B                      |
| FCBs             | siguiente      | 6 300 B                      |
| Zona de datos    | siguiente      | totalBlocks × 512 B          |

El MBR, el SuperBlock y el bitmap se escriben con su estado inicial; el resto de las tablas se inicializa en ceros.

### Etapa 2 — Formateo del filesystem

Se establece la estructura mínima funcional del sistema:

- **Grupo** `root` con el miembro 0 como único integrante.
- **FCB #0** → directorio `/` (sin padre, `parentId = -1`)
- **FCB #1** → directorio `/user` (hijo de FCB #0)
- **FCB #2** → directorio `/user/root` (hijo de FCB #1)
- **Usuario** `root` con contraseña, grupo 0 y directorio home apuntando al FCB #2.
- Las entradas de cada directorio quedan escritas en la zona de datos.

---

## 3. MBR — Master Boot Record

El MBR ocupa los primeros 200 bytes del disco y actúa como punto de entrada al volumen. Contiene tres campos:

| Campo        | Tipo     | Valor / Descripción                        |
|--------------|----------|--------------------------------------------|
| `nameLength` | `int`    | Longitud del identificador (4)             |
| `name`       | `byte[]` | Identificador del sistema: `"miFS"` (4 B)  |
| `volumeSize` | `long`   | Tamaño total del disco en bytes            |
| `volumeStart`| `long`   | Offset donde comienza el SuperBlock (200)  |

El MBR cumple dos funciones: identificar que el disco pertenece a miFS y proveer la dirección exacta del SuperBlock. Al cargar un disco existente, el sistema lee `volumeStart` desde el MBR para localizar el SuperBlock.