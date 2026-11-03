import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { WorkItem } from '../../api/types'

export function useItems() {
  return useQuery({ queryKey: ['work-items'], queryFn: () => api<WorkItem[]>('/api/work-items') })
}

/** 서비스/고객사 태그 목록. 항목이 바뀌면 ['work-items'] 무효화로 함께 갱신된다. */
export function useTags() {
  return useQuery({ queryKey: ['work-items', 'tags'], queryFn: () => api<string[]>('/api/work-items/tags') })
}
