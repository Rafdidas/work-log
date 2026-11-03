import { Card } from '../../components/Card'
import { formatMd, mdToHours } from '../../lib/md'

export interface EffortRow {
  id: number
  title: string
  md: number
}

export function EffortPanel({ rows, workMd }: { rows: EffortRow[]; workMd: number }) {
  const visible = rows.filter((r) => r.md > 0).sort((a, b) => b.md - a.md)
  const total = visible.reduce((sum, r) => sum + r.md, 0)
  return (
    <section aria-label="오늘 투입 공수">
      <Card className="p-4">
        <h2 className="mb-3 text-[15px] font-semibold">오늘 투입 공수</h2>
        {visible.length === 0 ? (
          <p className="text-[13px] text-mute">업무 비중을 고르면 공수가 계산돼요.</p>
        ) : (
          <div className="space-y-3 text-[13px]">
            {visible.map((r) => (
              <div key={r.id}>
                <div className="flex justify-between gap-3">
                  <span className="truncate">{r.title}</span>
                  <span className="shrink-0 font-medium">{`${formatMd(r.md)} · ${mdToHours(r.md)}`}</span>
                </div>
                <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-ink/5">
                  <div
                    className="h-full rounded-full bg-prussian transition-[width] duration-300"
                    style={{ width: `${workMd > 0 ? (r.md / workMd) * 100 : 0}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        )}
        <div className="mt-3 flex justify-between border-t border-line pt-3 text-[13px] font-semibold">
          <span>합계</span>
          <span>{`${formatMd(total)} (${mdToHours(total)})`}</span>
        </div>
      </Card>
    </section>
  )
}
