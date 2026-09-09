import { Navigate, Outlet } from 'react-router-dom'
import { useSesion } from '../store/auth'

export default function ProtectedRoute() {
  const token = useSesion((s) => s.token)
  if (!token) return <Navigate to="/login" replace />
  return <Outlet />
}
