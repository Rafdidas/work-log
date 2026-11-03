import type { ItemStatus, WorkItem } from '../../api/types'
import { formatMd } from '../../lib/md'

interface Props {
  item: WorkItem
  canUp: boolean
  canDown: boolean
  hint?: string
  onEdit: () => void
  onMove: (direction: -1 | 1) => void
  onStatus: (status: ItemStatus) => void
  onDelete: () => void
}

const action = 'rounded-md px-2 py-1 text-[12px] font-medium text-charcoal hover:bg-ink/5 disabled:opacity-30'

export function ItemRow({ item, canUp, canDown, hint, onEdit, onMove, onStatus, onDelete }: Props) {
  const labels = [
    item.recurring && '상시',
    !item.showInPlan && '계획 미표시',
    item.startedAt && `시작 ${item.startedAt}`,
    item.completedAt && `완료 ${item.completedAt}`,
  ].filter(Boolean)

  return (
    <div data-testid={`item-${item.id}`} className="px-4 py-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="truncate text-[15px] font-semibold">{item.title}</span>
            {item.tag && <span className="shrink-0 rounded-md bg-ink/5 px-1.5 py-0.5 text-[11px] text-charcoal">{item.tag}</span>}
          </div>
          <div className="mt-0.5 text-[12px] text-mute">
            {[...labels, `누적 ${formatMd(item.totalMd)}`].join(' · ')}
          </div>
        </div>
        <div className="flex shrink-0 flex-wrap justify-end">
          <button type="button" className={action} aria-label="위로" disabled={!canUp} onClick={() => onMove(-1)}>
            ↑
          </button>
          <button type="button" className={action} aria-label="아래로" disabled={!canDown} onClick={() => onMove(1)}>
            ↓
          </button>
        </div>
      </div>
      <div className="mt-2 flex flex-wrap gap-1">
        <button type="button" className={action} onClick={onEdit}>
          수정
        </button>
        {item.status === 'IN_PROGRESS' && (
          <>
            <button type="button" className={action} onClick={() => onStatus('DONE')}>
              완료
            </button>
            <button type="button" className={action} onClick={() => onStatus('WAITING')}>
              대기로
            </button>
          </>
        )}
        {item.status === 'WAITING' && (
          <button type="button" className={action} onClick={() => onStatus('IN_PROGRESS')}>
            진행 시작
          </button>
        )}
        {item.status === 'DONE' && (
          <button type="button" className={action} onClick={() => onStatus('IN_PROGRESS')}>
            다시 진행
          </button>
        )}
        <button type="button" className={action} onClick={onDelete}>
          삭제
        </button>
      </div>
      {hint && <p className="mt-2 text-[12px] text-red-700">{hint}</p>}
    </div>
  )
}
