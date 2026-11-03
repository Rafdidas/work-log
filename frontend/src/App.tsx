import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { LoginPage } from './features/auth/LoginPage'
import { RequireAuth } from './features/auth/RequireAuth'
import { DailyPage } from './features/daily/DailyPage'
import { ItemsPage } from './features/items/ItemsPage'
import { MonthlyPage } from './features/monthly/MonthlyPage'
import { WeeklyPage } from './features/weekly/WeeklyPage'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route path="/daily/:date?" element={<DailyPage />} />
          <Route path="/weekly/:monday?" element={<WeeklyPage />} />
          <Route path="/monthly" element={<MonthlyPage />} />
          <Route path="/items" element={<ItemsPage />} />
          <Route path="*" element={<Navigate to="/daily" replace />} />
        </Route>
      </Route>
    </Routes>
  )
}
