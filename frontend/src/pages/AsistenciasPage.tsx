import { useEffect, useState } from 'react'
import { listarAsistencias } from '../api/endpoints'
import EstadoVacio from '../components/EstadoVacio'
import TablaEsqueleto from '../components/TablaEsqueleto'
import type { Asistencia, ErrorResponse } from '../types/api'

export default function AsistenciasPage() {
  const [filas, setFilas] = useState<Asistencia[]>([])
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    listarAsistencias()
      .then(setFilas)
      .catch((err) => {
        const r = (err as { response?: { data?: ErrorResponse } }).response?.data
        setError(r?.mensaje ?? 'Error al cargar asistencias')
      })
      .finally(() => setCargando(false))
  }, [])

  return (
    <main className="contenedor">
      <header className="cabecera">
        <div>
          <h1>Registro de Asistencias</h1>
          <p className="subtitulo">{cargando ? 'Cargando…' : `${filas.length} registros en total`}</p>
        </div>
      </header>

      {error && <p className="error" role="alert">{error}</p>}

      {cargando ? (
        <TablaEsqueleto columnas={5} />
      ) : filas.length === 0 ? (
        <EstadoVacio
          icono="calendario"
          titulo="Sin registros todavía"
          detalle="Cuando los usuarios marquen entrada o salida, aparecerán aquí."
        />
      ) : (
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr>
                <th>ID</th><th>Nombre</th><th>Tipo</th><th>Fecha</th><th>Hora</th>
              </tr>
            </thead>
            <tbody>
              {filas.map((a) => (
                <tr key={a.id}>
                  <td>{a.id}</td>
                  <td>{a.usuarioNombre}</td>
                  <td>
                    <span className={`insignia ${a.tipo === 'ENTRADA' ? 'insignia-ok' : 'insignia-aviso'}`}>
                      {a.tipo}
                    </span>
                  </td>
                  <td>{a.fecha}</td>
                  <td>{a.hora.slice(0, 8)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}
