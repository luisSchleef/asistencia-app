import { Navigate, Route, Routes } from 'react-router-dom'
import AdminRoute from './components/AdminRoute'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import AsistenciasPage from './pages/AsistenciasPage'
import LoginPage from './pages/LoginPage'
import MenuPage from './pages/MenuPage'
import ReportesPage from './pages/ReportesPage'
import UsuariosPage from './pages/UsuariosPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<MenuPage />} />
          <Route element={<AdminRoute />}>
            <Route path="/asistencias" element={<AsistenciasPage />} />
            <Route path="/reportes" element={<ReportesPage />} />
            <Route path="/usuarios" element={<UsuariosPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
