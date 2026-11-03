import { useMutation, useQuery } from '@tanstack/react-query'
import { api, jsonBody } from '../../api/client'
import type { DayType, DayView, LeaveView } from '../../api/types'
import { useInvalidateAll } from './useItems'

export function useLeaves(from: string, to: string) {
  return useQuery({
    queryKey: ['days', 'leaves', from, to],
    queryFn: () => api<LeaveView[]>(`/api/days/leaves?from=${from}&to=${to}`),
  })
}

export function useSetDayType() {
  const invalidate = useInvalidateAll()
  return useMutation({
    mutationFn: ({ date, dayType, confirm }: { date: string; dayType: DayType; confirm: boolean }) =>
      api<DayView>(`/api/days/${date}/type`, jsonBody('PUT', { dayType, confirm })),
    onSuccess: invalidate,
  })
}
