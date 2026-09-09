import Modal from './Modal'

export default function ConfirmDialog({ titulo, mensaje, textoConfirmar = 'Confirmar', cargando = false, onConfirmar, onCancelar }: {
  titulo: string
  mensaje: string
  textoConfirmar?: string
  cargando?: boolean
  onConfirmar: () => void
  onCancelar: () => void
}) {
  return (
    <Modal titulo={titulo} onCerrar={onCancelar}>
      <p className="modal-texto">{mensaje}</p>
      <div className="modal-acciones">
        <button type="button" className="fantasma" onClick={onCancelar} disabled={cargando}>
          Cancelar
        </button>
        <button type="button" className="peligro" onClick={onConfirmar} disabled={cargando}>
          {cargando && <span className="spinner" aria-hidden="true" />}
          {cargando ? 'Eliminando...' : textoConfirmar}
        </button>
      </div>
    </Modal>
  )
}
