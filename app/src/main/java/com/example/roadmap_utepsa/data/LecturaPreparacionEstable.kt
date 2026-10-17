package com.example.roadmap_utepsa.data

/**
 * La consulta de estados es un snapshot único. Rodearla con metadatos iguales
 * evita mezclar estados de un grupo con el diario de otro. Se comparan también
 * timestamps: las operaciones del diario avanzan de forma monotónica.
 */
internal suspend fun <M, E> leerPreparacionEstable(
    metadatos: suspend () -> M,
    estados: suspend () -> E
): Pair<M, E> {
    repeat(3) {
        val antes = metadatos()
        val datos = estados()
        if (antes == metadatos()) return antes to datos
    }
    error("El historial sigue cambiando; vuelve a comprobar el resultado")
}
