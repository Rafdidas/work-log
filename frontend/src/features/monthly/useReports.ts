import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { EffortReport, MonthlySummary } from '../../api/types'

/** target은 'tag:위시드콘' 또는 'item:12' 형태. */
export function useEffort(target: string, from: string, to: string) {
  return useQuery({
    queryKey: ['effort', target, from, to],
    enabled: target !== '' && from !== '' && to !== '' && from <= to,
    queryFn: () => {
      const [kind, ...rest] = target.split(':')
      const value = rest.join(':')
      const param = kind === 'tag' ? `tag=${encodeURIComponent(value)}` : `itemId=${value}`
      return api<EffortReport>(`/api/reports/effort?${param}&from=${from}&to=${to}`)
    },
  })
}

export function useMonthly(month: string) {
  return useQuery({
    queryKey: ['monthly', month],
    enabled: month !== '',
    queryFn: () => api<MonthlySummary>(`/api/reports/monthly?month=${month}`),
  })
}
