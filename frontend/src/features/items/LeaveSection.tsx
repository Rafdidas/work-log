import { useState } from 'react'
import { ApiError } from '../../api/client'
import { DAY_TYPE_LABEL, type DayType } from '../../api/types'
import { Card, SectionTitle } from '../../components/Card'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { useToast } from '../../components/Toast'
import { addDays, formatMonthDay, todaySeoul, weekdayKo } from '../../lib/week'
import { useLeaves, useSetDayType } from './useLeaves'

const LEAVE_TYPES: DayType[] = ['ANNUAL', 'HALF_AM', 'HALF_PM', 'HOLIDAY']
const fieldClass =
  'h-10 rounded-xl border border-line bg-white px-3 text-[14px] outline-none focus:border-prussian focus:ring-2 focus:ring-prussian/15'

export function LeaveSection() {
  const toast = useToast()
  const today = todaySeoul()
  const [date, setDate] = useState(today)
  const [type, setType] = useState<DayType>('ANNUAL')
  const [pending, setPending] = useState<{ date: string; type: DayType } | null>(null)
  const leaves = useLeaves(today, addDays(today, 120))
  const setDayType = useSetDayType()

  function submit(target: { date: string; dayType: DayType }) {
    setDayType.mutate(
      { ...target, confirm: false },
      {
        onSuccess: () => toast('저장했어요'),
        onError: (e) => {
          if (e instanceof ApiError && e.code === 'ENTRIES_EXIST') setPending({ date: target.date, type: target.dayType })
          else toast(e.message)
        },
      },
    )
  }

  return (
    <section>
      <SectionTitle>연차 · 반차 · 휴일</SectionTitle>
      <Card className="p-4">
        <div className="flex flex-wrap items-center gap-2">
          <input aria-label="날짜" type="date" className={fieldClass} value={date} onChange={(e) => setDate(e.target.value)} />
          <select aria-label="종류" className={fieldClass} value={type} onChange={(e) => setType(e.target.value as DayType)}>
            {LEAVE_TYPES.map((t) => (
              <option key={t} value={t}>
                {DAY_TYPE_LABEL[t]}
              </option>
            ))}
          </select>
          <button
            type="button"
            disabled={date === '' || setDayType.isPending}
            onClick={() => submit({ date, dayType: type })}
            className="h-10 rounded-xl bg-prussian px-4 text-sm font-semibold text-white hover:bg-prussian/90 disabled:opacity-50"
          >
            일정 등록
          </button>
        </div>

        {(leaves.data?.length ?? 0) > 0 && (
          <ul className="mt-4 divide-y divide-line border-t border-line text-[14px]">
            {leaves.data!.map((l) => (
              <li key={l.date} className="flex items-center justify-between py-2">
                <span>
                  {`${formatMonthDay(l.date)} (${weekdayKo(l.date)})`}
                  <span className="ml-2 text-charcoal">{l.dayTypeLabel}</span>
                </span>
                <button
                  type="button"
                  className="rounded-md px-2 py-1 text-[12px] text-charcoal hover:bg-ink/5"
                  onClick={() => submit({ date: l.date, dayType: 'WORK' })}
                >
                  취소
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <ConfirmDialog
        open={pending !== null}
        title="기록을 삭제하고 등록할까요?"
        message={`${pending ? formatMonthDay(pending.date) : ''}에 입력한 업무 기록이 있어요. 등록하면 업무 기록이 삭제됩니다. 하루 메모는 남아요.`}
        confirmLabel="삭제하고 등록"
        onConfirm={() => {
          if (pending) setDayType.mutate({ date: pending.date, dayType: pending.type, confirm: true })
          setPending(null)
        }}
        onCancel={() => setPending(null)}
      />
    </section>
  )
}
