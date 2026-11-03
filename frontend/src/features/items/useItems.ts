import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, jsonBody } from '../../api/client'
import type { ItemStatus, WorkItem } from '../../api/types'

export function useItems() {
  return useQuery({ queryKey: ['work-items'], queryFn: () => api<WorkItem[]>('/api/work-items') })
}

/** 서비스/고객사 태그 목록. 항목이 바뀌면 ['work-items'] 무효화로 함께 갱신된다. */
export function useTags() {
  return useQuery({ queryKey: ['work-items', 'tags'], queryFn: () => api<string[]>('/api/work-items/tags') })
}

/** 항목·근무 형태가 바뀌면 화면마다 달라지는 파생 데이터를 모두 다시 읽게 한다. */
export function useInvalidateAll() {
  const queryClient = useQueryClient()
  return () => {
    for (const key of ['work-items', 'days', 'day', 'weekly', 'effort', 'monthly']) {
      queryClient.invalidateQueries({ queryKey: [key] })
    }
  }
}

export interface CreateItemInput {
  title: string
  tag?: string
  recurring: boolean
  showInPlan: boolean
  status: Exclude<ItemStatus, 'DONE'>
}

export interface UpdateItemInput {
  id: number
  title: string
  tag: string
  recurring: boolean
  showInPlan: boolean
  startedAt?: string
  completedAt?: string
}

export function useCreateItem() {
  const invalidate = useInvalidateAll()
  return useMutation({
    mutationFn: (input: CreateItemInput) => api<WorkItem>('/api/work-items', jsonBody('POST', input)),
    onSuccess: invalidate,
  })
}

export function useUpdateItem() {
  const invalidate = useInvalidateAll()
  return useMutation({
    mutationFn: ({ id, ...patch }: UpdateItemInput) => api<WorkItem>(`/api/work-items/${id}`, jsonBody('PATCH', patch)),
    onSuccess: invalidate,
  })
}

export function useChangeStatus() {
  const invalidate = useInvalidateAll()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: ItemStatus }) =>
      api<WorkItem>(`/api/work-items/${id}/status`, jsonBody('PATCH', { status })),
    onSuccess: invalidate,
  })
}

export function useDeleteItem() {
  const invalidate = useInvalidateAll()
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/work-items/${id}`, { method: 'DELETE' }),
    onSuccess: invalidate,
  })
}

export function useReorder() {
  const invalidate = useInvalidateAll()
  return useMutation({
    mutationFn: (ids: number[]) => api<void>('/api/work-items/order', jsonBody('PUT', ids)),
    onSuccess: invalidate,
  })
}
