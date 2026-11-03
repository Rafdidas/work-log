import type { DaySummary } from '../../api/types'
import { DAY_TYPE_LABEL } from '../../api/types'
import { addDays, formatMonthDay, weekDays, weekdayKo } from '../../lib/week'

interface Props {
  date: string
  monday: string
  summaries: DaySummary[]
  onSelect: (date: string) => void
  onShiftWeek: (weeks: number) => void
  onToday: () => void
}

export function WeekStrip({ date, monday, summaries, onSelect, onShiftWeek, onToday }: Props) {
  const byDate = new Map(summaries.map((s) => [s.date, s]))
  const arrow = 'grid h-8 w-8 place-items-center rounded-lg text-charcoal hover:bg-ink/5'
  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <div className="flex items-center gap-1">
          <button type="button" aria-label="이전 주" className={arrow} onClick={() => onShiftWeek(-1)}>
            ‹
          </button>
          <span className="text-[13px] text-charcoal">
            {formatMonthDay(monday)} ~ {formatMonthDay(addDays(monday, 4))}
          </span>
          <button type="button" aria-label="다음 주" className={arrow} onClick={() => onShiftWeek(1)}>
            ›
          </button>
        </div>
        <button type="button" onClick={onToday} className="text-[13px] font-medium text-charcoal hover:text-ink">
          오늘
        </button>
      </div>
      <div className="grid grid-cols-5 gap-1.5">
        {weekDays(monday).map((d) => {
          const summary = byDate.get(d)
          const selected = d === date
          const leave = summary && summary.dayType !== 'WORK' ? DAY_TYPE_LABEL[summary.dayType] : null
          return (
            <button
              key={d}
              type="button"
              aria-pressed={selected}
              onClick={() => onSelect(d)}
              className={`flex h-[58px] flex-col items-center justify-center rounded-xl border text-[13px] transition-colors ${
                selected ? 'border-prussian bg-prussian text-white' : 'border-line bg-white hover:bg-canvas'
              }`}
            >
              <span className={selected ? 'text-white/80' : 'text-mute'}>{weekdayKo(d)}</span>
              <span className="text-[15px] font-semibold">{Number(d.slice(8))}</span>
              <span className="flex h-3 items-center text-[10px] leading-none">
                {leave ?? (summary?.hasEntries ? '●' : '')}
              </span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
