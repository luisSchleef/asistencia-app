import { Navigate, Outlet } from 'react-router-dom'
import { useSesion } from '../store/auth'

export default function AdminRoute() {
  const usuario = useSesion((s) => s.usuario)
  if (usuario?.rol !== 'ADMIN') return <Navigate to="/" replace />
  return <Outlet />
}
