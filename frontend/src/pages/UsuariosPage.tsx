import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import * as endpoints from '../api/endpoints'
import ConfirmDialog from '../components/ConfirmDialog'
import Icon from '../components/Icon'
import Modal from '../components/Modal'
import TablaEsqueleto from '../components/TablaEsqueleto'
import { useSesion } from '../store/auth'
import { useToasts } from '../store/toast'
import type { ErrorResponse, Usuario } from '../types/api'

export default function UsuariosPage() {
  const actor = useSesion((s) => s.usuario)
  const notificar = useToasts((s) => s.notificar)
  const [usuarios, setUsuarios] = useState<Usuario[]>([])
  const [editando, setEditando] = useState<Usuario | null>(null)
  const [eliminando, setEliminando] = useState<Usuario | null>(null)
  const [cargandoEliminar, setCargandoEliminar] = useState(false)
  const [cargando, setCargando] = useState(true)

  const recargar = useCallback(() => {
    return endpoints.listarUsuarios()
      .then(setUsuarios)
      .catch((err) => {
        const r = (err as { response?: { data?: ErrorResponse } }).response?.data
        notificar('error', r?.mensaje ?? 'Error al cargar usuarios')
      })
  }, [notificar])

  useEffect(() => {
    recargar().finally(() => setCargando(false))
  }, [recargar])

  function pedirEliminar(u: Usuario) {
    if (!actor) return
    if (u.id === actor.id) {
      notificar('error', 'No puede eliminar su propio usuario')
      return
    }
    setEliminando(u)
  }

  async function confirmarEliminar() {
    if (!eliminando) return
    setCargandoEliminar(true)
    try {
      await endpoints.eliminarUsuario(eliminando.id)
      notificar('ok', 'Usuario eliminado correctamente')
      setEliminando(null)
      await recargar()
    } catch (err) {
      const r = (err as { response?: { data?: ErrorResponse } }).response?.data
      notificar('error', r?.mensaje ?? 'Error al eliminar')
    } finally {
      setCargandoEliminar(false)
    }
  }

  return (
    <main className="contenedor">
      <header className="cabecera">
        <div>
          <h1>Gestión de Usuarios</h1>
          <p className="subtitulo">{cargando ? 'Cargando…' : `${usuarios.length} usuarios registrados`}</p>
        </div>
        <button className="primario" onClick={() => setEditando({ id: 0, nombre: '', correo: '', rol: 'EMPLEADO' })}>
          <Icon nombre="mas" tamaño={16} />
          Crear usuario
        </button>
      </header>

      {editando && (
        <Modal
          titulo={editando.id === 0 ? 'Crear Usuario' : `Modificar: ${editando.nombre}`}
          onCerrar={() => setEditando(null)}
        >
          <FormularioUsuario
            editando={editando}
            esNuevo={editando.id === 0}
            onGuardado={(msg) => {
              setEditando(null)
              notificar('ok', msg)
              recargar()
            }}
            onCancelar={() => setEditando(null)}
          />
        </Modal>
      )}

      {eliminando && (
        <ConfirmDialog
          titulo="Eliminar usuario"
          mensaje={`¿Eliminar a ${eliminando.nombre}? También se eliminarán sus registros de asistencia. Esta acción no se puede deshacer.`}
          textoConfirmar="Eliminar"
          cargando={cargandoEliminar}
          onConfirmar={confirmarEliminar}
          onCancelar={() => setEliminando(null)}
        />
      )}

      {cargando ? (
        <TablaEsqueleto columnas={5} />
      ) : (
        <div className="tabla-contenedor">
          <table>
            <thead>
              <tr><th>ID</th><th>Nombre</th><th>Correo</th><th>Rol</th><th>Acciones</th></tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.nombre}</td>
                  <td>{u.correo}</td>
                  <td>
                    <span className={`insignia ${u.rol === 'ADMIN' ? 'insignia-aviso' : 'insignia-gris'}`}>
                      {u.rol}
                    </span>
                  </td>
                  <td className="acciones-tabla">
                    <button onClick={() => setEditando(u)}>
                      <Icon nombre="lapiz" tamaño={14} />
                      Modificar
                    </button>
                    <button className="peligro" onClick={() => pedirEliminar(u)}>
                      <Icon nombre="basura" tamaño={14} />
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}

function FormularioUsuario({ editando, esNuevo, onGuardado, onCancelar }: {
  editando: Usuario
  esNuevo: boolean
  onGuardado: (msg: string) => void
  onCancelar: () => void
}) {
  const notificar = useToasts((s) => s.notificar)
  const [nombre, setNombre] = useState(editando.nombre)
  const [correo, setCorreo] = useState(editando.correo)
  const [contrasena, setContrasena] = useState('')
  const [rol, setRol] = useState(editando.rol)
  const [erroresCampo, setErroresCampo] = useState<Record<string, string>>({})
  const [cargando, setCargando] = useState(false)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErroresCampo({})
    setCargando(true)
    try {
      if (esNuevo) {
        await endpoints.crearUsuario({ nombre: nombre.trim(), correo: correo.trim(), contrasena, rol })
        onGuardado('Usuario creado correctamente')
      } else {
        await endpoints.actualizarUsuario(editando.id, { nombre: nombre.trim(), correo: correo.trim(), contrasena, rol })
        onGuardado('Usuario modificado correctamente')
      }
    } catch (err) {
      const r = (err as { response?: { data?: ErrorResponse } }).response?.data
      if (r?.campos) setErroresCampo(r.campos)
      notificar('error', r?.mensaje ?? 'Error al guardar')
    } finally {
      setCargando(false)
    }
  }

  return (
    <form onSubmit={enviar}>
      <label>
        Nombre
        <input value={nombre} onChange={(e) => setNombre(e.target.value)} required />
        {erroresCampo.nombre && <span className="error-campo">{erroresCampo.nombre}</span>}
      </label>
      <label>
        Correo
        <input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} required />
        {erroresCampo.correo && <span className="error-campo">{erroresCampo.correo}</span>}
      </label>
      <label>
        Contraseña
        <input
          type="password"
          value={contrasena}
          onChange={(e) => setContrasena(e.target.value)}
          required={esNuevo}
        />
        {erroresCampo.contrasena && <span className="error-campo">{erroresCampo.contrasena}</span>}
      </label>
      <label>
        Rol
        <select value={rol} onChange={(e) => setRol(e.target.value as Usuario['rol'])}>
          <option value="EMPLEADO">EMPLEADO</option>
          <option value="ADMIN">ADMIN</option>
        </select>
      </label>
      <div className="modal-acciones">
        <button type="button" className="fantasma" onClick={onCancelar} disabled={cargando}>
          Cancelar
        </button>
        <button type="submit" className="primario" disabled={cargando}>
          {cargando && <span className="spinner" aria-hidden="true" />}
          {cargando ? 'Guardando...' : 'Guardar'}
        </button>
      </div>
    </form>
  )
}
