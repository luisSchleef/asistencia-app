import { useEffect, useState } from 'react'
import * as endpoints from '../api/endpoints'
import EstadoVacio from '../components/EstadoVacio'
import TablaEsqueleto from '../components/TablaEsqueleto'
import type { Asistencia, ErrorResponse, Inasistencia } from '../types/api'

type Pestaña = 'atrasos' | 'salidas' | 'inasistencias'

export default function ReportesPage() {
  const [pestaña, setPestaña] = useState<Pestaña>('atrasos')
  const [atrasos, setAtrasos] = useState<Asistencia[]>([])
  const [salidas, setSalidas] = useState<Asistencia[]>([])
  const [inasistencias, setInasistencias] = useState<Inasistencia[]>([])
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    Promise.all([
      endpoints.reporteAtrasos(),
      endpoints.reporteSalidasAnticipadas(),
      endpoints.reporteInasistencias(),
    ])
      .then(([a, s, i]) => {
        setAtrasos(a)
        setSalidas(s)
        setInasistencias(i)
      })
      .catch((err) => {
        const r = (err as { response?: { data?: ErrorResponse } }).response?.data
        setError(r?.mensaje ?? 'Error al cargar reportes')
      })
      .finally(() => setCargando(false))
  }, [])

  return (
    <main className="contenedor">
      <header className="cabecera">
        <div>
          <h1>Reportes</h1>
          <p className="subtitulo">Atrasos, salidas anticipadas e inasistencias del periodo</p>
        </div>
      </header>

      {error && <p className="error" role="alert">{error}</p>}

      {cargando ? (
        <TablaEsqueleto columnas={3} />
      ) : (
        <>
          <nav className="pestañas" aria-label="Tipo de reporte">
            <button className={pestaña === 'atrasos' ? 'activa' : ''} onClick={() => setPestaña('atrasos')}>
              Atrasos ({atrasos.length})
            </button>
            <button className={pestaña === 'salidas' ? 'activa' : ''} onClick={() => setPestaña('salidas')}>
              Salidas anticipadas ({salidas.length})
            </button>
            <button className={pestaña === 'inasistencias' ? 'activa' : ''} onClick={() => setPestaña('inasistencias')}>
              Inasistencias ({inasistencias.length})
            </button>
          </nav>

          {pestaña === 'atrasos' && (
            <TablaAsistencias titulo="Entradas después de las 09:30" filas={atrasos} vacio="Nadie llegó atrasado" />
          )}
          {pestaña === 'salidas' && (
            <TablaAsistencias titulo="Salidas antes de las 17:30" filas={salidas} vacio="Nadie salió antes de hora" />
          )}
          {pestaña === 'inasistencias' && (
            inasistencias.length === 0 ? (
              <EstadoVacio icono="check" titulo="Sin inasistencias" detalle="Todos los usuarios registraron asistencia." />
            ) : (
              <div className="tabla-contenedor">
                <table>
                  <thead>
                    <tr><th>Nombre</th><th>Fecha ausencia</th></tr>
                  </thead>
                  <tbody>
                    {inasistencias.map((i, idx) => (
                      <tr key={`${i.usuarioId}-${i.fecha}-${idx}`}>
                        <td>{i.usuarioNombre}</td>
                        <td>{i.fecha}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}
        </>
      )}
    </main>
  )
}

function TablaAsistencias({ titulo, filas, vacio }: { titulo: string; filas: Asistencia[]; vacio: string }) {
  if (filas.length === 0) {
    return <EstadoVacio icono="check" titulo={vacio} detalle={titulo} />
  }
  return (
    <>
      <p className="subtitulo">{titulo}</p>
      <div className="tabla-contenedor">
        <table>
          <thead>
            <tr><th>Nombre</th><th>Fecha</th><th>Hora</th></tr>
          </thead>
          <tbody>
            {filas.map((a) => (
              <tr key={a.id}>
                <td>{a.usuarioNombre}</td>
                <td>{a.fecha}</td>
                <td>{a.hora.slice(0, 8)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
