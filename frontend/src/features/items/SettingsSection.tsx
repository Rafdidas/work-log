import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { api, jsonBody } from '../../api/client'
import type { Profile } from '../../api/types'
import { Card, SectionTitle } from '../../components/Card'
import { useToast } from '../../components/Toast'
import { useMe } from '../auth/useMe'

export function SettingsSection() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const me = useMe()
  const [teamLabel, setTeamLabel] = useState('')

  useEffect(() => {
    if (me.data) setTeamLabel(me.data.teamLabel)
  }, [me.data])

  const save = useMutation({
    mutationFn: () => api<Profile>('/api/me', jsonBody('PATCH', { teamLabel: teamLabel.trim() })),
    onSuccess: (profile) => {
      queryClient.setQueryData(['me'], profile)
      queryClient.invalidateQueries({ queryKey: ['weekly'] })
      toast('저장했어요')
    },
    onError: (e) => toast(e.message),
  })

  return (
    <section>
      <SectionTitle>설정</SectionTitle>
      <Card className="p-4">
        <label className="block text-[13px] text-charcoal">
          팀명 (주간 보고 제목에 들어가요)
          <div className="mt-1 flex gap-2">
            <input
              value={teamLabel}
              onChange={(e) => setTeamLabel(e.target.value)}
              maxLength={100}
              className="h-10 flex-1 rounded-xl border border-line bg-white px-3 text-[14px] text-ink outline-none focus:border-prussian focus:ring-2 focus:ring-prussian/15"
            />
            <button
              type="button"
              disabled={teamLabel.trim() === '' || save.isPending}
              onClick={() => save.mutate()}
              className="h-10 rounded-xl bg-prussian px-4 text-sm font-semibold text-white hover:bg-prussian/90 disabled:opacity-50"
            >
              팀명 저장
            </button>
          </div>
        </label>
      </Card>
    </section>
  )
}
