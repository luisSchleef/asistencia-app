import Icon from './Icon'
import type { NombreIcono } from './Icon'

export default function EstadoVacio({ icono = 'lista', titulo, detalle }: {
  icono?: NombreIcono
  titulo: string
  detalle?: string
}) {
  return (
    <div className="vacio">
      <Icon nombre={icono} tamaño={32} />
      <strong>{titulo}</strong>
      {detalle && <span>{detalle}</span>}
    </div>
  )
}
