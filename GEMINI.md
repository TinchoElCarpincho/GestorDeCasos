# Reglas de Proyecto y Optimización de Tokens

Este archivo define el comportamiento del asistente para maximizar la calidad del código y minimizar el consumo de tokens (entrada y salida).

## 1. Directrices de Comunicación
- **Respuestas concisas y directas**: Evitar saludos, introducciones corteses, preámbulos, resúmenes redundantes o conclusiones de cortesía.
- **Sin repetición de código**: No imprimir archivos enteros en el chat ni repetir fragmentos de código inalterados. Explicar únicamente la lógica modificada o referenciar líneas específicas (`archivo.ext#L10-L25`).
- **Explicaciones breves**: Solo justificar decisiones técnicas no obvias o responder preguntas directas.

## 2. Operación Eficiente de Herramientas
- **Lectura quirúrgica**: Al inspeccionar código existente, usar siempre rangos específicos (`StartLine` y `EndLine`) con `view_file` en lugar de leer archivos completos.
- **Ediciones precisas**: Priorizar `replace_file_content` para cambios acotados en vez de sobreescribir archivos enteros con `write_to_file`.
- **Búsquedas delimitadas**: Al usar `grep_search` o `find_by_name`, acotar la ruta y extensiones (`Includes`, `Extensions`) para no saturar el contexto con resultados innecesarios.
- **Respetar exclusiones**: Nunca inspeccionar directorios de dependencias (`node_modules`, `venv`), builds (`dist`, `.next`) o archivos temporales.

## 3. Calidad y Modularidad de Código
- Desarrollar módulos pequeños y funciones con responsabilidad única (el código modular requiere menos tokens para ser leído y editado).
- Usar tipado estático/definiciones claras para evitar errores y consultas redundantes.
- Respuestas e interacciones en español técnico.
