import { useQueryClient } from '@tanstack/react-query'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { SegmentedTabs } from './SegmentedTabs'

const TABS = [
  { value: 'daily', label: '일일' },
  { value: 'weekly', label: '주간' },
  { value: 'monthly', label: '월별' },
  { value: 'items', label: '업무' },
]

export function AppShell() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const current = TABS.find((t) => pathname.startsWith(`/${t.value}`))?.value ?? 'daily'

  async function logout() {
    await api('/api/auth/logout', { method: 'POST' })
    queryClient.clear()
    navigate('/login')
  }

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-20 border-b border-line bg-white/70 backdrop-blur-md">
        <div className="mx-auto flex max-w-3xl flex-wrap items-center justify-between gap-x-3 gap-y-2 px-4 py-3">
          <span className="text-[17px] font-semibold tracking-tight">Work Log</span>
          <SegmentedTabs
            className="order-last w-full justify-between sm:order-none sm:w-auto"
            ariaLabel="화면 선택"
            options={TABS}
            value={current}
            onChange={(tab) => navigate(`/${tab}`)}
          />
          <button type="button" onClick={logout} className="text-[13px] text-mute hover:text-ink">
            로그아웃
          </button>
        </div>
      </header>
      <main className="mx-auto max-w-3xl px-4 pb-32 pt-6">
        <Outlet />
      </main>
    </div>
  )
}
