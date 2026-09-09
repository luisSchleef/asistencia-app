import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import * as endpoints from '../api/endpoints'
import Icon from '../components/Icon'
import { useSesion } from '../store/auth'
import { useMarcas, type TipoMarca } from '../store/marcas'
import { useToasts } from '../store/toast'
import type { ErrorResponse } from '../types/api'

const ESPERA_MS = 30_000

export default function MenuPage() {
  const usuario = useSesion((s) => s.usuario)
  const notificar = useToasts((s) => s.notificar)

  const { entrada, salida, bloqueoHasta, registrar, bloquear, desbloquear, sincronizar } = useMarcas()
  const [ahora, setAhora] = useState(() => new Date())

  useEffect(() => {
    const intervalo = setInterval(() => {
      if (usuario) sincronizar(usuario.id)
      setAhora(new Date())
    }, 1000)
    return () => clearInterval(intervalo)
  }, [usuario, sincronizar])

  const espera = Math.max(0, Math.ceil((bloqueoHasta - ahora.getTime()) / 1000))

  async function marcar(tipo: TipoMarca) {
    if (tipo === 'ENTRADA' ? entrada : salida) return
    if (tipo === 'SALIDA' && !entrada) return
    bloquear(ESPERA_MS)
    try {
      const registro = tipo === 'ENTRADA' ? await endpoints.marcarEntrada() : await endpoints.marcarSalida()
      registrar(tipo, registro.hora)
      notificar('ok', `${registro.tipo} registrada a las ${registro.hora.slice(0, 8)}`)
    } catch (err) {
      const respuesta = (err as { response?: { data?: ErrorResponse } }).response?.data
      notificar('error', respuesta?.mensaje ?? 'No se pudo registrar')
      desbloquear()
    }
  }

  const horaTexto = ahora.toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  const fechaTexto = ahora.toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

  return (
    <main className="contenedor">
      <header className="cabecera">
        <div>
          <h1>Hola, {usuario?.nombre}</h1>
          <p className="subtitulo">Registra tu jornada del día</p>
        </div>
        <div className="reloj" aria-live="off">
          <span className="reloj-hora">{horaTexto}</span>
          <span className="reloj-fecha">{fechaTexto}</span>
        </div>
      </header>

      <section className="grilla-marcas">
        <button className="marca-boton entrada" disabled={!!entrada || espera > 0} onClick={() => marcar('ENTRADA')}>
          <Icon nombre="entrar" tamaño={26} />
          Marcar Entrada
          <small>
            {entrada
              ? `Registrada a las ${entrada.slice(0, 8)}`
              : espera > 0
                ? `Disponible en ${espera}s`
                : 'Registra el inicio de tu jornada'}
          </small>
        </button>
        <button className="marca-boton salida" disabled={!!salida || !entrada || espera > 0} onClick={() => marcar('SALIDA')}>
          <Icon nombre="salir" tamaño={26} />
          Marcar Salida
          <small>
            {salida
              ? `Registrada a las ${salida.slice(0, 8)}`
              : !entrada
                ? 'Primero registra tu entrada'
                : espera > 0
                  ? `Disponible en ${espera}s`
                  : 'Registra el fin de tu jornada'}
          </small>
        </button>
      </section>

      {usuario?.rol === 'ADMIN' && (
        <section className="acciones">
          <h2>Administración</h2>
          <nav className="grilla-accesos">
            <Link to="/asistencias" className="acceso">
              <Icon nombre="lista" tamaño={22} />
              Asistencias
              <small>Historial completo de registros</small>
            </Link>
            <Link to="/reportes" className="acceso">
              <Icon nombre="grafico" tamaño={22} />
              Reportes
              <small>Atrasos, salidas e inasistencias</small>
            </Link>
            <Link to="/usuarios" className="acceso">
              <Icon nombre="usuarios" tamaño={22} />
              Usuarios
              <small>Crear, modificar y eliminar</small>
            </Link>
          </nav>
        </section>
      )}
    </main>
  )
}
