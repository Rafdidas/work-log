import { Navigate, Outlet } from 'react-router-dom'
import { useMe } from './useMe'

export function RequireAuth() {
  const { data, isLoading } = useMe()
  if (isLoading) return <div className="p-8 text-sm text-charcoal">불러오는 중…</div>
  if (!data) return <Navigate to="/login" replace />
  return <Outlet />
}
