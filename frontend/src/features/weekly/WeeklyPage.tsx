import type { ReactNode } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { DAY_TYPE_LABEL, type WeeklyReport } from '../../api/types'
import { Card } from '../../components/Card'
import { CopyButton } from '../../components/CopyButton'
import { formatDays, weekdayEnumKo, addWeeks, mondayOf, todaySeoul } from '../../lib/week'
import { formatMd } from '../../lib/md'
import { sectionBody } from '../../lib/reportText'
import { useWeekly } from './useWeekly'

function Section({ title, copyText, children }: { title: string; copyText: string; children: ReactNode }) {
  return (
    <Card className="mt-4">
      <div className="flex items-center justify-between border-b border-line px-4 py-3">
        <h2 className="text-[15px] font-semibold">{title}</h2>
        <CopyButton text={copyText} label={`${title} 복사`} />
      </div>
      <div className="px-4 py-3 text-[14px]">{children}</div>
    </Card>
  )
}

function Empty() {
  return <p className="text-[13px] text-mute">없음</p>
}

function leaveLine(date: string, type: keyof typeof DAY_TYPE_LABEL) {
  const [, m, d] = date.split('-').map(Number)
  const dow = ['일', '월', '화', '수', '목', '금', '토'][new Date(`${date}T00:00:00Z`).getUTCDay()]
  return `${m}/${d}(${dow}) ${DAY_TYPE_LABEL[type]}`
}

function PlanList({ items }: { items: WeeklyReport['todo'] }) {
  if (items.length === 0) return <Empty />
  return (
    <ul className="space-y-1.5">
      {items.map((i) => (
        <li key={i.itemId} className="flex gap-2">
          <span className="text-mute">-</span>
          <span>{i.title}</span>
        </li>
      ))}
    </ul>
  )
}

export function WeeklyPage() {
  const params = useParams()
  const navigate = useNavigate()
  const monday = mondayOf(params.monday ?? todaySeoul())
  const { data: report, isError } = useWeekly(monday)

  if (params.monday && params.monday !== monday) {
    return <Navigate to={`/weekly/${monday}`} replace />
  }

  const go = (target: string) => navigate(`/weekly/${target}`)

  return (
    <div>
      <div className="mb-2 flex items-center justify-between">
        <div className="flex items-center gap-1">
          <button type="button" aria-label="이전 주" className="grid h-8 w-8 place-items-center rounded-lg text-charcoal hover:bg-ink/5" onClick={() => go(addWeeks(monday, -1))}>
            ‹
          </button>
          <button type="button" aria-label="다음 주" className="grid h-8 w-8 place-items-center rounded-lg text-charcoal hover:bg-ink/5" onClick={() => go(addWeeks(monday, 1))}>
            ›
          </button>
        </div>
        <button type="button" onClick={() => go(mondayOf(todaySeoul()))} className="text-[13px] font-medium text-charcoal hover:text-ink">
          이번 주
        </button>
      </div>

      {isError && <p className="text-sm text-red-700">주간 보고를 불러오지 못했어요.</p>}
      {!report && !isError && <p className="text-sm text-charcoal">불러오는 중…</p>}

      {report && (
        <>
          <div className="flex items-start justify-between gap-3">
            <h1 className="text-[22px] font-semibold leading-snug tracking-tight">{report.title}</h1>
            <div className="flex shrink-0 gap-2">
              <CopyButton text={report.title} label="제목 복사" />
              <CopyButton text={report.text} label="전체 복사" primary />
            </div>
          </div>

          <Section title="해야 할 일" copyText={sectionBody(report.text, '해야 할 일')}>
            <PlanList items={report.todo} />
          </Section>

          <Section title="실제 달성한 일" copyText={sectionBody(report.text, '실제 달성한 일')}>
            {report.achieved.length === 0 && report.leaves.length === 0 ? (
              <Empty />
            ) : (
              <ul className="space-y-3">
                {report.achieved.map((a) => (
                  <li key={a.itemId}>
                    <div className="flex flex-wrap items-baseline gap-x-2">
                      <span className="font-semibold">{a.title}</span>
                      {a.completed && <span className="text-[12px] text-charcoal">(완료)</span>}
                      <span className="text-[13px] text-charcoal">{`${formatDays(a.days)} / ${formatMd(a.md)}`}</span>
                    </div>
                    {a.memos.length > 0 && (
                      <ul className="mt-1 space-y-0.5 text-[13px] text-charcoal">
                        {a.memos.map((m, i) => (
                          <li key={i} className="flex gap-2">
                            <span className="w-4 shrink-0 text-mute">{weekdayEnumKo(m.day)}</span>
                            <span className="whitespace-pre-line">{m.memo}</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </li>
                ))}
                {report.leaves.map((l) => (
                  <li key={l.date} className="text-[13px] text-charcoal">
                    {leaveLine(l.date, l.type)}
                  </li>
                ))}
              </ul>
            )}
          </Section>

          <Section title="차주 업무" copyText={sectionBody(report.text, '차주 업무')}>
            <PlanList items={report.next} />
          </Section>
        </>
      )}
    </div>
  )
}
