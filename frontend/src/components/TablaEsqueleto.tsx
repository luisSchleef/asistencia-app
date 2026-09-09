export default function TablaEsqueleto({ columnas, filas = 6 }: { columnas: number; filas?: number }) {
  return (
    <div className="tabla-contenedor" aria-busy="true" aria-label="Cargando datos">
      <table>
        <tbody>
          {Array.from({ length: filas }, (_, f) => (
            <tr key={f}>
              {Array.from({ length: columnas }, (_, c) => (
                <td key={c}>
                  <div className="esqueleto" style={{ height: 14, width: `${55 + ((f * 7 + c * 13) % 40)}%` }} />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
