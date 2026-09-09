import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useTema } from '../hooks/useTema'
import { useSesion } from '../store/auth'
import Icon from './Icon'
import Toasts from './Toasts'

export default function Layout() {
  const navegar = useNavigate()
  const usuario = useSesion((s) => s.usuario)
  const logout = useSesion((s) => s.logout)
  const { tema, alternar } = useTema()

  function cerrarSesion() {
    logout()
    navegar('/login', { replace: true })
  }

  return (
    <>
      <header className="navbar">
        <div className="navbar-interior">
          <NavLink to="/" className="marca">
            <Icon nombre="reloj" tamaño={22} />
            Asistencias
          </NavLink>

          {usuario?.rol === 'ADMIN' && (
            <nav className="nav-links" aria-label="Administración">
              <NavLink to="/asistencias" className={({ isActive }) => (isActive ? 'activa' : '')}>
                Registros
              </NavLink>
              <NavLink to="/reportes" className={({ isActive }) => (isActive ? 'activa' : '')}>
                Reportes
              </NavLink>
              <NavLink to="/usuarios" className={({ isActive }) => (isActive ? 'activa' : '')}>
                Usuarios
              </NavLink>
            </nav>
          )}

          <div className="navbar-derecha">
            <button
              className="icono-boton"
              onClick={alternar}
              title={tema === 'claro' ? 'Activar modo oscuro' : 'Activar modo claro'}
              aria-label="Cambiar tema"
            >
              <Icon nombre={tema === 'claro' ? 'luna' : 'sol'} />
            </button>
            <span className="usuario-chip">
              <span className="usuario-nombre">{usuario?.nombre}</span>
              <span className="rol">{usuario?.rol}</span>
            </span>
            <button className="icono-boton" onClick={cerrarSesion} title="Cerrar sesión" aria-label="Cerrar sesión">
              <Icon nombre="salir" />
            </button>
          </div>
        </div>
      </header>

      <Outlet />
      <Toasts />
    </>
  )
}
