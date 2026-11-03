import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { Profile } from '../../api/types'

export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: () => api<Profile>('/api/me'), staleTime: 5 * 60_000 })
}
