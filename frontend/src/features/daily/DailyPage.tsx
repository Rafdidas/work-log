import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../../api/client'
import { DAY_TYPE_LABEL, type DayType } from '../../api/types'
import { Card } from '../../components/Card'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { useToast } from '../../components/Toast'
import { previewAllocation } from '../../lib/md'
import { addWeeks, formatDayTitle, formatMonthDay, mondayOf, todaySeoul } from '../../lib/week'
import { toDraft, toSaveInput, type Draft } from './draft'
import { EffortPanel } from './EffortPanel'
import { EntryRow } from './EntryRow'
import { useChangeDayType, useDay, useSaveDay, useWeekSummary } from './useDay'
import { WeekStrip } from './WeekStrip'

const UNSAVED = '저장하지 않은 변경이 있어요. 그래도 이동할까요?'

export function DailyPage() {
  const params = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const date = params.date ?? todaySeoul()
  const monday = mondayOf(date)

  const { data: day, isError } = useDay(date)
  const { data: summaries = [] } = useWeekSummary(monday)
  const save = useSaveDay(date)
  const changeType = useChangeDayType(date)

  const [draft, setDraft] = useState<Draft | null>(null)
  const baseline = useRef('')
  const [showWaiting, setShowWaiting] = useState(false)
  const [pendingType, setPendingType] = useState<DayType | null>(null)

  useEffect(() => {
    if (day) {
      const next = toDraft(day)
      setDraft(next)
      baseline.current = JSON.stringify(next)
    }
  }, [day])

  const dirty = draft !== null && JSON.stringify(draft) !== baseline.current

  const allocation = useMemo(() => {
    if (!day || !draft) return {}
    const ids = [...day.entries.map((e) => e.itemId), ...day.waitingItems.map((w) => w.itemId)]
    return previewAllocation(
      day.workMd,
      ids.map((id) => ({ id, weight: draft.rows[id]?.weight ?? 0 })),
    )
  }, [day, draft])

  if (isError) return <p className="text-sm text-red-700">일일 기록을 불러오지 못했어요.</p>
  if (!day || !draft || draft.date !== date) return <p className="text-sm text-charcoal">불러오는 중…</p>

  const locked = day.dayType === 'ANNUAL' || day.dayType === 'HOLIDAY'

  function go(target: string) {
    if (target === date) return
    if (dirty && !window.confirm(UNSAVED)) return
    navigate(`/daily/${target}`)
  }

  function patchRow(itemId: number, patch: { weight?: number; memo?: string }) {
    setDraft((d) => d && { ...d, rows: { ...d.rows, [itemId]: { ...d.rows[itemId], ...patch } } })
  }

  function onSave() {
    save.mutate(toSaveInput(draft!), {
      onSuccess: () => toast('저장했어요'),
      onError: (e) => toast(e.message),
    })
  }

  function onTypeChange(type: DayType) {
    if (dirty) {
      toast('먼저 저장한 뒤 근무 형태를 바꿔 주세요')
      return
    }
    changeType.mutate(
      { dayType: type, confirm: false },
      {
        onError: (e) => {
          if (e instanceof ApiError && e.code === 'ENTRIES_EXIST') setPendingType(type)
          else toast(e.message)
        },
      },
    )
  }

  function confirmType() {
    if (!pendingType) return
    changeType.mutate({ dayType: pendingType, confirm: true })
    setPendingType(null)
  }

  const effortRows = [
    ...day.entries.map((e) => ({ id: e.itemId, title: e.title, md: allocation[e.itemId] ?? 0 })),
    ...day.waitingItems.map((w) => ({ id: w.itemId, title: w.title, md: allocation[w.itemId] ?? 0 })),
  ]

  return (
    <div>
      <WeekStrip
        date={date}
        monday={monday}
        summaries={summaries}
        onSelect={go}
        onShiftWeek={(n) => go(addWeeks(date, n))}
        onToday={() => go(todaySeoul())}
      />

      <div className="mt-5 flex items-end justify-between gap-3">
        <h1 className="text-[26px] font-semibold tracking-tight">{formatDayTitle(date)}</h1>
        <label className="flex items-center gap-2 text-[13px] text-charcoal">
          근무 형태
          <select
            value={day.dayType}
            onChange={(e) => onTypeChange(e.target.value as DayType)}
            className="h-8 rounded-lg border border-line bg-white px-2 text-[13px] text-ink"
          >
            {(Object.keys(DAY_TYPE_LABEL) as DayType[]).map((t) => (
              <option key={t} value={t}>
                {DAY_TYPE_LABEL[t]}
              </option>
            ))}
          </select>
        </label>
      </div>

      {locked && (
        <p className="mt-3 rounded-xl bg-ink/5 px-4 py-3 text-[13px] text-charcoal">
          {day.dayTypeLabel}인 날에는 업무 비중을 입력할 수 없어요.
        </p>
      )}

      <Card className="mt-4 divide-y divide-line">
        {day.entries.map((e) => (
          <EntryRow
            key={e.itemId}
            item={e}
            weight={draft.rows[e.itemId]?.weight ?? 0}
            memo={draft.rows[e.itemId]?.memo ?? ''}
            md={allocation[e.itemId] ?? 0}
            locked={locked}
            onChange={(patch) => patchRow(e.itemId, patch)}
          />
        ))}
        {day.entries.length === 0 && (
          <p className="px-4 py-6 text-center text-[13px] text-mute">진행 중인 업무가 없어요. 업무 탭에서 추가해 주세요.</p>
        )}
      </Card>

      {day.waitingItems.length > 0 && (
        <div className="mt-3">
          <button
            type="button"
            onClick={() => setShowWaiting((v) => !v)}
            className="text-[13px] font-medium text-charcoal hover:text-ink"
          >
            {showWaiting ? '대기 항목 숨기기' : `대기 항목 보기 (${day.waitingItems.length})`}
          </button>
          {showWaiting && (
            <Card className="mt-2 divide-y divide-line">
              {day.waitingItems.map((w) => (
                <EntryRow
                  key={w.itemId}
                  item={{ ...w, status: 'WAITING', recurring: false }}
                  weight={draft.rows[w.itemId]?.weight ?? 0}
                  memo={draft.rows[w.itemId]?.memo ?? ''}
                  md={allocation[w.itemId] ?? 0}
                  locked={locked}
                  onChange={(patch) => patchRow(w.itemId, patch)}
                />
              ))}
            </Card>
          )}
        </div>
      )}

      <textarea
        rows={2}
        value={draft.memo}
        placeholder="하루 메모 (선택)"
        onChange={(e) => setDraft({ ...draft, memo: e.target.value })}
        className="mt-4 w-full resize-none rounded-2xl border border-line bg-white px-4 py-3 text-[14px] outline-none placeholder:text-mute/70 focus:ring-2 focus:ring-prussian/15"
      />

      <div className="mt-4">
        <EffortPanel rows={effortRows} workMd={day.workMd} />
      </div>

      <div className="fixed inset-x-0 bottom-0 z-20 border-t border-line bg-white/80 backdrop-blur-md">
        <div className="mx-auto max-w-3xl px-4 py-3">
          <button
            type="button"
            onClick={onSave}
            disabled={save.isPending || !dirty}
            className="h-12 w-full rounded-xl bg-prussian text-[15px] font-semibold text-white transition-colors hover:bg-prussian/90 disabled:bg-prussian/40"
          >
            {`${formatMonthDay(date)} 체크인 저장`}
          </button>
        </div>
      </div>

      <ConfirmDialog
        open={pendingType !== null}
        title="기록을 삭제하고 바꿀까요?"
        message={`이 날에 입력한 업무 기록이 있어요. ${pendingType ? DAY_TYPE_LABEL[pendingType] : ''}(으)로 바꾸면 업무 기록이 삭제됩니다. 하루 메모는 남아요.`}
        confirmLabel="삭제하고 변경"
        onConfirm={confirmType}
        onCancel={() => setPendingType(null)}
      />
    </div>
  )
}
