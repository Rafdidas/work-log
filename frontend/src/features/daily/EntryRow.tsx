import type { ItemStatus } from '../../api/types'
import { formatMd } from '../../lib/md'
import { WeightSegment } from './WeightSegment'

export interface EntryRowItem {
  itemId: number
  title: string
  tag: string | null
  status: ItemStatus
  recurring: boolean
  completedBefore?: boolean
}

function statusLabel(item: EntryRowItem): string {
  if (item.status === 'DONE' || item.completedBefore) return '완료됨'
  if (item.status === 'WAITING') return '대기'
  return item.recurring ? '상시' : '진행 중'
}

interface Props {
  item: EntryRowItem
  weight: number
  memo: string
  md: number
  locked: boolean
  onChange: (patch: { weight?: number; memo?: string }) => void
}

export function EntryRow({ item, weight, memo, md, locked, onChange }: Props) {
  return (
    <div data-testid={`entry-${item.itemId}`} className="px-4 py-3">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <div className="truncate text-[15px] font-semibold">{item.title}</div>
          <div className="text-xs text-mute">
            {[item.tag, statusLabel(item)].filter(Boolean).join(' · ')}
            {weight > 0 && <span className="ml-2 text-charcoal">{formatMd(md)}</span>}
          </div>
        </div>
        <WeightSegment title={item.title} value={weight} disabled={locked} onChange={(w) => onChange({ weight: w })} />
      </div>
      <textarea
        rows={1}
        value={memo}
        placeholder="메모 추가…"
        onChange={(e) => onChange({ memo: e.target.value })}
        className="field-sizing-content mt-2 w-full resize-none rounded-lg bg-canvas px-3 py-2 text-[13px] outline-none placeholder:text-mute/70 focus:ring-2 focus:ring-prussian/15"
      />
    </div>
  )
}
