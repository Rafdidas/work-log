import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { WeeklyReport } from '../../api/types'

export function useWeekly(monday: string) {
  return useQuery({
    queryKey: ['weekly', monday],
    queryFn: () => api<WeeklyReport>(`/api/reports/weekly?monday=${monday}`),
  })
}
