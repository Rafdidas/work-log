import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, jsonBody } from '../../api/client'
import type { DaySummary, DayType, DayView } from '../../api/types'
import { addDays } from '../../lib/week'

export function useDay(date: string) {
  return useQuery({ queryKey: ['day', date], queryFn: () => api<DayView>(`/api/days/${date}`) })
}

export function useWeekSummary(monday: string) {
  return useQuery({
    queryKey: ['days', 'week', monday],
    queryFn: () => api<DaySummary[]>(`/api/days?from=${monday}&to=${addDays(monday, 4)}`),
  })
}

export interface SaveInput {
  memo: string
  entries: { itemId: number; weight: number; memo: string }[]
}

function useAfterDayChange(date: string) {
  const queryClient = useQueryClient()
  return (view: DayView) => {
    queryClient.setQueryData(['day', date], view)
    for (const key of ['days', 'work-items', 'weekly', 'effort', 'monthly']) {
      queryClient.invalidateQueries({ queryKey: [key] })
    }
  }
}

export function useSaveDay(date: string) {
  const after = useAfterDayChange(date)
  return useMutation({
    mutationFn: (input: SaveInput) => api<DayView>(`/api/days/${date}`, jsonBody('PUT', input)),
    onSuccess: after,
  })
}

export function useChangeDayType(date: string) {
  const after = useAfterDayChange(date)
  return useMutation({
    mutationFn: (input: { dayType: DayType; confirm: boolean }) =>
      api<DayView>(`/api/days/${date}/type`, jsonBody('PUT', input)),
    onSuccess: after,
  })
}
