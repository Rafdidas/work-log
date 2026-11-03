import { useState, type FormEvent } from 'react'
import type { WorkItem } from '../../api/types'
import { Card } from '../../components/Card'
import { SegmentedTabs } from '../../components/SegmentedTabs'
import type { CreateItemInput, UpdateItemInput } from './useItems'

const inputClass =
  'h-10 w-full rounded-xl border border-line bg-white px-3 text-[14px] outline-none focus:border-prussian focus:ring-2 focus:ring-prussian/15'

interface Props {
  /** 수정할 항목. 없으면 새 항목 등록. */
  initial?: WorkItem
  tags: string[]
  pending: boolean
  error?: string
  onCreate: (input: CreateItemInput) => void
  onUpdate: (input: UpdateItemInput) => void
  onCancel: () => void
}

export function ItemForm({ initial, tags, pending, error, onCreate, onUpdate, onCancel }: Props) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [tag, setTag] = useState(initial?.tag ?? '')
  const [recurring, setRecurring] = useState(initial?.recurring ?? false)
  const [showInPlan, setShowInPlan] = useState(initial?.showInPlan ?? true)
  const [status, setStatus] = useState<'IN_PROGRESS' | 'WAITING'>('IN_PROGRESS')
  const [startedAt, setStartedAt] = useState(initial?.startedAt ?? '')
  const [completedAt, setCompletedAt] = useState(initial?.completedAt ?? '')

  function submit(e: FormEvent) {
    e.preventDefault()
    const trimmedTag = tag.trim()
    if (initial) {
      onUpdate({
        id: initial.id,
        title: title.trim(),
        tag: trimmedTag,
        recurring,
        showInPlan,
        ...(startedAt && { startedAt }),
        ...(initial.status === 'DONE' && completedAt && { completedAt }),
      })
    } else {
      onCreate({ title: title.trim(), ...(trimmedTag && { tag: trimmedTag }), recurring, showInPlan, status })
    }
  }

  return (
    <Card className="mt-4 p-4">
      <form onSubmit={submit} className="space-y-3">
        <input
          aria-label="업무 이름"
          className={inputClass}
          placeholder="업무 이름"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          maxLength={200}
        />
        <input
          aria-label="서비스/고객사 태그"
          className={inputClass}
          placeholder="서비스/고객사 태그 (선택, 예: 위시드콘)"
          list="item-tags"
          value={tag}
          onChange={(e) => setTag(e.target.value)}
          maxLength={100}
        />
        <datalist id="item-tags">
          {tags.map((t) => (
            <option key={t} value={t} />
          ))}
        </datalist>

        <label className="flex items-center gap-2 text-[14px]">
          <input type="checkbox" checked={recurring} onChange={(e) => setRecurring(e.target.checked)} />
          상시 업무
          <span className="text-[12px] text-mute">(유지보수, 회의처럼 계속 진행하는 업무)</span>
        </label>
        <label className="flex items-center gap-2 text-[14px]">
          <input type="checkbox" checked={showInPlan} onChange={(e) => setShowInPlan(e.target.checked)} />
          주간 보고 계획 섹션에 표시
        </label>

        {initial ? (
          <div className="grid grid-cols-2 gap-3">
            <label className="text-[13px] text-charcoal">
              시작일
              <input type="date" className={`${inputClass} mt-1`} value={startedAt} onChange={(e) => setStartedAt(e.target.value)} />
            </label>
            {initial.status === 'DONE' && (
              <label className="text-[13px] text-charcoal">
                완료일
                <input type="date" className={`${inputClass} mt-1`} value={completedAt} onChange={(e) => setCompletedAt(e.target.value)} />
              </label>
            )}
          </div>
        ) : (
          <SegmentedTabs
            ariaLabel="시작 상태"
            options={[
              { value: 'IN_PROGRESS' as const, label: '진행 중으로 시작' },
              { value: 'WAITING' as const, label: '대기' },
            ]}
            value={status}
            onChange={setStatus}
          />
        )}

        {error && <p className="text-sm text-red-700">{error}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onCancel} className="h-9 rounded-lg px-4 text-sm text-charcoal hover:bg-canvas">
            취소
          </button>
          <button
            type="submit"
            disabled={pending || title.trim() === ''}
            className="h-9 rounded-lg bg-prussian px-4 text-sm font-semibold text-white hover:bg-prussian/90 disabled:opacity-50"
          >
            {initial ? '수정 저장' : '항목 등록'}
          </button>
        </div>
      </form>
    </Card>
  )
}
