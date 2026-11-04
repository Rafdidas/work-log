import type { ReactNode } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { DAY_TYPE_LABEL, type WeeklyReport } from '../../api/types'
import { Card } from '../../components/Card'
import { CopyButton } from '../../components/CopyButton'
import { weekdayKo, addWeeks, mondayOf, todaySeoul } from '../../lib/week'
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

function monthSlashDay(date: string) {
  const [, m, d] = date.split('-').map(Number)
  return `${m}/${d}`
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
              <ul className="divide-y divide-line">
                {report.byDay.map((g) => (
                  <li key={g.date} className="flex gap-3 py-2.5 first:pt-0 last:pb-0">
                    <div className="w-12 shrink-0">
                      <div className="font-semibold">{weekdayKo(g.date)}</div>
                      <div className="text-[12px] text-mute">{monthSlashDay(g.date)}</div>
                    </div>
                    <div className="min-w-0 flex-1 space-y-2">
                      {g.leave && <span className="inline-block rounded bg-ink/5 px-1.5 py-0.5 text-[12px] text-charcoal">{DAY_TYPE_LABEL[g.leave]}</span>}
                      {g.entries.length === 0 && !g.leave && <Empty />}
                      {g.entries.map((e) => (
                        <div key={e.itemId}>
                          <div className="flex flex-wrap items-baseline gap-x-2">
                            <span className="font-semibold">{e.title}</span>
                            <span className="text-[13px] text-charcoal">{formatMd(e.md)}</span>
                            {e.completed && <span className="text-[12px] text-charcoal">(완료)</span>}
                          </div>
                          {e.memo && <p className="mt-0.5 whitespace-pre-line text-[13px] text-charcoal">{e.memo.trim()}</p>}
                        </div>
                      ))}
                    </div>
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
